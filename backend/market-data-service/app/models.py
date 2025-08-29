from pydantic import BaseModel
from typing import List

class PriceResponse(BaseModel):
    symbol: str
    lastPrice: float
    dayChange: float
    dayChangePercentage: float

class BatchPriceResponse(BaseModel):
    results: List[PriceResponse]
