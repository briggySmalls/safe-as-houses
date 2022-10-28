from pydantic import BaseModel, HttpUrl, Field
from typing import Optional


class HouseData(BaseModel):
	price: int
	type: str
	bedrooms: int = Field(alias="number_bedrooms")
	address: str
	url: HttpUrl
	agent_url: HttpUrl
	postcode: Optional[str]
	full_postcode: Optional[str]
	floorplan_url: Optional[HttpUrl]
