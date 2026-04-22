"""
Data models for the market-data-service.
"""
from pydantic import BaseModel
from typing import List

class PriceResponse(BaseModel):
    """Represents the price and daily change of a single ticker symbol."""
    symbol: str
    lastPrice: float
    dayChange: float
    dayChangePercentage: float

class BatchPriceResponse(BaseModel):
    """Wrapper for a batch of PriceResponse objects."""
    results: List[PriceResponse]
