"""Main module."""
from house_ingest.config import Config
from rightmove_webscraper import RightmoveData
import pandas as pd
from typing import Dict, List
from house_ingest.elastic import ElasticClient
from elasticsearch import Elasticsearch
from house_ingest.house_data import HouseData


class HouseIngestor:
    def __init__(self, config: Config) -> None:
        self._config = config
        self._es = ElasticClient(Elasticsearch(config.ES_URL))

    def scrape(self) -> pd.DataFrame:
        rm = RightmoveData(self._config.QUERY_URL, get_floorplans=True)
        return rm.get_results

    def index(self, df: pd.DataFrame) -> None:
        df = df.where(df.notnull(), None)
        records = self._to_records(df)
        self._es.bulk_index(records)

    @classmethod
    def _to_records(cls, df: pd.DataFrame) -> List[HouseData]:
        return [HouseData(**r) for r in df.to_dict("records")]
