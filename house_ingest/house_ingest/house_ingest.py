"""Main module."""
from house_ingest.config import Config
from rightmove_webscraper import RightmoveData
import pandas as pd
from typing import Dict


class HouseIngestor:
	def __init__(self, config: Config) -> None:
		self._config = config

	def scrape(self) -> pd.DataFrame:
		rm = RightmoveData(self._config.QUERY_URL, get_floorplans=True)
		return rm.get_results
