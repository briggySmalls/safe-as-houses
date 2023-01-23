from typing import Any, Optional

from pydantic import BaseModel
from scrapemove.models import CombinedDetails


_DEFAULT_PRICE_PER_SQFT = 10000.0


class IndexData(BaseModel):
    scraped: CombinedDetails
    area_sqft: Optional[float]

    @property
    def price_per_sqft(self):
        if self.area_sqft:
            return self.scraped.property.price.amount / self.area_sqft
        else:
            return _DEFAULT_PRICE_PER_SQFT

    def merged_dict(self):
        return {
            **self.scraped.merged_dict(),
            "area_sqft": self.area_sqft,
            "price_per_sqft": self.price_per_sqft,
        }
