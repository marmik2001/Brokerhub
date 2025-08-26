from fastapi import FastAPI, Query
from typing import List
from app.models import PriceResponse, BatchPriceResponse
from app.services import fetch_price_single, fetch_prices_batch

app = FastAPI(title="BrokerHub YFinance Service")

@app.get("/price/{symbol}", response_model=PriceResponse)
async def get_price(symbol: str):
    return fetch_price_single(symbol)

@app.get("/prices", response_model=BatchPriceResponse)
async def get_prices(symbols: List[str] = Query(..., description="Comma separated list of symbols")):
    results = fetch_prices_batch(symbols)
    return BatchPriceResponse(results=results)
