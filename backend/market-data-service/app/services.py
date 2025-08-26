import yfinance as yf
from app.models import PriceResponse
from datetime import datetime, timedelta
from typing import List

# Simple in-memory cache
cache = {}  # key: symbol, value: (PriceResponse, timestamp)
CACHE_TTL = timedelta(seconds=120) #so that our IP does not get blocked by Yahoo Finance!

def fetch_price_single(symbol: str) -> PriceResponse:
    # Check cache
    now = datetime.now()
    if symbol in cache:
        cached_response, ts = cache[symbol]
        if now - ts < CACHE_TTL:
            return cached_response

    # Fetch from yfinance
    symbol_nse = f"{symbol}.NS"
    ticker = yf.Ticker(symbol_nse)
    hist = ticker.history(period="2d")
    if hist.empty:
        response = PriceResponse(symbol=symbol, lastPrice=0, pnl=0, dayChange=0, dayChangePercentage=0)
    else:
        last_price = hist['Close'].iloc[-1]
        prev_close = hist['Close'].iloc[-2]
        day_change = last_price - prev_close
        day_change_pct = (day_change / prev_close) * 100

        response = PriceResponse(
            symbol=symbol,
            lastPrice=round(last_price, 2),
            pnl=0,
            dayChange=round(day_change, 2),
            dayChangePercentage=round(day_change_pct, 2)
        )

    # Update cache
    cache[symbol] = (response, now)
    return response

def fetch_prices_batch(symbols: List[str]) -> List[PriceResponse]:
    results = []
    symbols_to_fetch = []

    # Check cache
    now = datetime.now()
    for sym in symbols:
        if sym in cache:
            cached_response, ts = cache[sym]
            if now - ts < CACHE_TTL:
                results.append(cached_response)
                continue
        symbols_to_fetch.append(sym)

    if symbols_to_fetch:
        # Batch fetch from yfinance
        symbols_nse = [f"{s}.NS" for s in symbols_to_fetch]
        data = yf.download(symbols_nse, period="2d", group_by='ticker', threads=True)
        for i, sym in enumerate(symbols_to_fetch):
            try:
                hist = data[symbols_nse[i]] if len(symbols_to_fetch) > 1 else data
                last_price = hist['Close'].iloc[-1]
                prev_close = hist['Close'].iloc[-2]
                day_change = last_price - prev_close
                day_change_pct = (day_change / prev_close) * 100

                response = PriceResponse(
                    symbol=sym,
                    lastPrice=round(last_price, 2),
                    pnl=0,
                    dayChange=round(day_change, 2),
                    dayChangePercentage=round(day_change_pct, 2)
                )
            except:
                response = PriceResponse(symbol=sym, lastPrice=0, pnl=0, dayChange=0, dayChangePercentage=0)

            # Update cache
            cache[sym] = (response, now)
            results.append(response)

    return results
