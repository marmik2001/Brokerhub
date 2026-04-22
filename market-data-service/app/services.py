"""
Business logic for fetching and caching market data using yfinance and Redis.
"""
import logging
import json
import os
import random
import threading
from typing import Dict, List, Optional, Tuple

import yfinance as yf
import pandas as pd
import redis

from app.models import PriceResponse

logger = logging.getLogger(__name__)

# local in-flight dedupe only (cache is Redis)
_cache_lock = threading.Lock()
_inflight: Dict[str, threading.Event] = {}  # per-symbol in-flight marker
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379/0")
CACHE_TTL_SECONDS = int(os.getenv("CACHE_TTL_SECONDS", "1200"))
CACHE_TTL_JITTER_SECONDS = int(os.getenv("CACHE_TTL_JITTER_SECONDS", "30"))
_BATCH_CHUNK = 50  # safety: don't request too many tickers in one yfinance call

_redis_client: Optional[redis.Redis] = None

try:
    _redis_client = redis.Redis.from_url(REDIS_URL, decode_responses=True)
    _redis_client.ping()
    logger.info("Connected to Redis for market-data cache: %s", REDIS_URL)
except Exception as exc:
    logger.warning("Redis unavailable (%s). Running without shared cache.", exc)
    _redis_client = None

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
                close_cols = [c for c in hist.columns if c[1] == "Close"]
                if close_cols:
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
        logger.error("Failed to create PriceResponse for %s", symbol, exc_info=True)
        return PriceResponse(symbol=symbol, lastPrice=0, dayChange=0, dayChangePercentage=0)

def fetch_price_single(symbol: str, force_refresh: bool = False) -> PriceResponse:
    """Fetch a single symbol using the batch path for consistency."""
    return fetch_prices_batch([symbol], force_refresh=force_refresh)[0]

def _cache_key(sym: str) -> str:
    return f"md:price:{sym.upper()}"


def _serialize_price(resp: PriceResponse) -> str:
    payload = resp.model_dump() if hasattr(resp, "model_dump") else resp.dict()
    return json.dumps(payload)


def _get_cached_if_fresh(sym: str) -> Optional[PriceResponse]:
    """Return cached PriceResponse from Redis if present (TTL handled by Redis)."""
    if _redis_client is None:
        return None
    try:
        raw = _redis_client.get(_cache_key(sym))
        if raw:
            return PriceResponse(**json.loads(raw))
    except Exception as exc:
        logger.error("Redis get failed for %s", sym, exc_info=True)
    return None


def _cache_response(sym: str, response: PriceResponse) -> None:
    if _redis_client is None:
        return
    ttl = CACHE_TTL_SECONDS + random.randint(0, max(0, CACHE_TTL_JITTER_SECONDS))
    try:
        _redis_client.setex(_cache_key(sym), ttl, _serialize_price(response))
    except Exception as exc:
        logger.error("Redis set failed for %s", sym, exc_info=True)

def fetch_prices_batch(symbols: List[str], force_refresh: bool = False) -> List[PriceResponse]:
    """
    Fetch prices for the provided symbols (plain tickers, e.g., 'RELIANCE').
    Returns results in the same order as input.
    """
    if not symbols:
        return []

    results_map: Dict[str, PriceResponse] = {}
    to_fetch: List[str] = []

    # 1) Try to serve from cache or detect inflight fetches
    for sym in symbols:
        if not force_refresh:
            cached_resp = _get_cached_if_fresh(sym)
            if cached_resp:
                results_map[sym] = cached_resp
                continue

        # if another thread is already fetching this symbol, wait for it (dedupe)
        wait_event = None
        with _cache_lock:
            wait_event = _inflight.get(sym)
        if wait_event:
            # wait a short time for the inflight fetch to complete
            wait_event.wait(timeout=10)
            # try cache again
            cached_resp = _get_cached_if_fresh(sym)
            if cached_resp:
                results_map[sym] = cached_resp
                continue
            # if still not present, fall through to add to_fetch

        to_fetch.append(sym)

    # 2) Batch fetch remaining symbols via yfinance (chunked)
    if to_fetch:
        # chunk to avoid very large requests
        for start in range(0, len(to_fetch), _BATCH_CHUNK):
            chunk = to_fetch[start : start + _BATCH_CHUNK]
            symbols_nse = [f"{s}.NS" for s in chunk]
            joined = " ".join(symbols_nse)
            data = None

            # mark these symbols as in-flight so other threads wait
            events_created: List[Tuple[str, threading.Event]] = []
            with _cache_lock:
                for s in chunk:
                    if s not in _inflight:
                        ev = threading.Event()
                        _inflight[s] = ev
                        events_created.append((s, ev))
                    # if already present, we won't overwrite (unlikely because we checked earlier)

            try:
                try:
                    tickers = yf.Tickers(joined)
                    data = tickers.history(period="2d", group_by="ticker", threads=True)
                except Exception as exc:
                    logger.error("yfinance batch fetch failed for %s", symbols_nse, exc_info=True)
                    data = None

                for i, sym in enumerate(chunk):
                    key = symbols_nse[i]
                    hist = None

                    if data is None:
                        hist = None
                    else:
                        try:
                            if isinstance(data, pd.DataFrame) and isinstance(data.columns, pd.MultiIndex):
                                if key in data.columns.get_level_values(0):
                                    hist = data[key]
                                else:
                                    matching = [lvl for lvl in data.columns.get_level_values(0).unique() if key in str(lvl)]
                                    if matching:
                                        hist = data[matching[0]]
                            elif isinstance(data, pd.DataFrame):
                                hist = data
                            elif isinstance(data, dict):
                                hist = data.get(key)
                        except Exception as e:
                            logger.error("Error while deducing history for %s", key, exc_info=True)
                            hist = None

                    response = _make_price_response_from_hist(sym, hist)
                    # cache valid results
                    if response.lastPrice and response.lastPrice != 0:
                        _cache_response(sym, response)
                    else:
                        # do not aggressively cache failures; still put in results_map so caller gets fallback
                        pass
                    results_map[sym] = response
            finally:
                # signal waiters and cleanup in-flight markers for this chunk
                with _cache_lock:
                    for s, ev in events_created:
                        ev.set()
                        # remove only if same event (defensive)
                        cur = _inflight.get(s)
                        if cur is ev:
                            del _inflight[s]

    # 3) Build ordered results
    results: List[PriceResponse] = []
    for sym in symbols:
        resp = results_map.get(sym)
        if resp is None:
            resp = PriceResponse(symbol=sym, lastPrice=0, dayChange=0, dayChangePercentage=0)
        results.append(resp)

    return results
