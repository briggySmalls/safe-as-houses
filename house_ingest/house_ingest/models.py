from typing import Any, Optional

from pydantic import BaseModel
from scrapemove.models import CombinedDetails


class IndexData(BaseModel):
    scraped: CombinedDetails
    area_sqft: Optional[float]

    def merged_dict(self):
        return {**self.scraped.merged_dict(), "area_sqft": self.area_sqft}
