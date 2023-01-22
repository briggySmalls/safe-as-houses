from typing import Any, Optional

from pydantic import BaseModel
from scrapemove.models import CombinedDetails


class IndexData(BaseModel):
    scraped: CombinedDetails
    area_sqft: Optional[float]

    @property
    def price_per_sqft(self):
        if area_sqft:
            return scraped.price.amount / area_sqft
        else:
            return float('inf')

    def merged_dict(self):
        return {
            **self.scraped.merged_dict(),
            "area_sqft": self.area_sqft,
            "price_per_sqft": self.area_sqft,
        }
