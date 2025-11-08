# services.py
import logging
import threading
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional, Tuple

import yfinance as yf
import pandas as pd

from app.models import PriceResponse

logger = logging.getLogger(__name__)

# thread-safe in-memory cache: symbol -> (PriceResponse, timestamp)
_cache_lock = threading.Lock()
_cache: Dict[str, Tuple[PriceResponse, datetime]] = {}
CACHE_TTL = timedelta(seconds=120)  # keep short to avoid rate limits

def _now() -> datetime:
    """Return timezone-aware UTC time."""
    return datetime.now(timezone.utc)

def _make_price_response_from_hist(symbol: str, hist: Optional[pd.DataFrame]) -> PriceResponse:
    """
    Given a DataFrame 'hist' (expected to contain a 'Close' column with >=2 rows),
    return a PriceResponse (lastPrice=0 on failure).
    """
    try:
        if hist is None or hist.empty:
            raise ValueError("empty history")

        # locate Close column robustly
        if "Close" not in hist.columns:
            # if MultiIndex columns (ticker, field) -> try to find Close in second level
            if isinstance(hist.columns, pd.MultiIndex):
                # collapse to get second level names
                # we'll attempt to select 'Close' columns across levels
                close_cols = [c for c in hist.columns if c[1] == "Close"]
                if close_cols:
                    # pick the first Close series
                    close_series = hist.loc[:, close_cols[0]]
                else:
                    raise ValueError("Close column not found in MultiIndex")
            else:
                raise ValueError("Close column not present")
        else:
            close_series = hist["Close"]

        if len(close_series) < 2:
            raise ValueError("not enough history rows to compute change")

        last_price = float(close_series.iloc[-1])
        prev_close = float(close_series.iloc[-2])

        day_change = last_price - prev_close
        day_change_pct = (day_change / prev_close) * 100 if prev_close != 0 else 0.0

        return PriceResponse(
            symbol=symbol,
            lastPrice=round(last_price, 2),
            dayChange=round(day_change, 2),
            dayChangePercentage=round(day_change_pct, 2),
        )
    except Exception as exc:
        logger.debug("Failed to create PriceResponse for %s: %s", symbol, exc)
        return PriceResponse(symbol=symbol, lastPrice=0, dayChange=0, dayChangePercentage=0)

def fetch_price_single(symbol: str, force_refresh: bool = False) -> PriceResponse:
    """Fetch a single symbol using the batch path for consistency."""
    return fetch_prices_batch([symbol], force_refresh=force_refresh)[0]

def fetch_prices_batch(symbols: List[str], force_refresh: bool = False) -> List[PriceResponse]:
    """
    Fetch prices for the provided symbols (plain tickers, e.g., 'RELIANCE').
    Returns results in the same order as input.
    """
    if not symbols:
        return []

    now = _now()
    results_map: Dict[str, PriceResponse] = {}
    to_fetch: List[str] = []

    # 1) Try to serve from cache
    with _cache_lock:
        for sym in symbols:
            cached = _cache.get(sym)
            if cached and not force_refresh:
                resp, ts = cached
                if now - ts < CACHE_TTL:
                    results_map[sym] = resp
                    continue
            to_fetch.append(sym)

    # 2) Batch fetch remaining symbols via yfinance
    if to_fetch:
        symbols_nse = [f"{s}.NS" for s in to_fetch]
        joined = " ".join(symbols_nse)
        data = None

        try:
            tickers = yf.Tickers(joined)
            # history returns DataFrame (single ticker) or a DataFrame with MultiIndex columns
            data = tickers.history(period="2d", group_by="ticker", threads=True)
        except Exception as exc:
            logger.warning("yfinance batch fetch failed for %s: %s", symbols_nse, exc)
            data = None

        for i, sym in enumerate(to_fetch):
            key = symbols_nse[i]
            hist = None

            if data is None:
                hist = None
            else:
                # Case A: yfinance returns a DataFrame with MultiIndex columns:
                #         columns like ( 'RELIANCE.NS', 'Close' )
                try:
                    if isinstance(data, pd.DataFrame) and isinstance(data.columns, pd.MultiIndex):
                        # try selecting by top-level equal to ticker key
                        if key in data.columns.get_level_values(0):
                            hist = data[key]
                        else:
                            # fallback: try selecting by contains key (some yfinance versions tweak names)
                            matching = [lvl for lvl in data.columns.get_level_values(0).unique() if key in str(lvl)]
                            if matching:
                                hist = data[matching[0]]
                    elif isinstance(data, pd.DataFrame):
                        # data might be a single-ticker DataFrame (when only one symbol requested)
                        hist = data
                    elif isinstance(data, dict):
                        # older yfinance versions might return a dict-like mapping
                        hist = data.get(key)
                except Exception as e:
                    logger.debug("Error while deducing history for %s: %s", key, e)
                    hist = None

            response = _make_price_response_from_hist(sym, hist)
            if response.lastPrice and response.lastPrice != 0:
                with _cache_lock:
                    _cache[sym] = (response, now)
            results_map[sym] = response

    # 3) Build ordered results
    results: List[PriceResponse] = []
    for sym in symbols:
        resp = results_map.get(sym)
        if resp is None:
            resp = PriceResponse(symbol=sym, lastPrice=0, dayChange=0, dayChangePercentage=0)
        results.append(resp)

    return results
