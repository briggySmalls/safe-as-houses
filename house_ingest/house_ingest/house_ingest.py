"""Main module."""
from house_ingest.config import Config
from typing import Dict, List, Any
from house_ingest.elastic import ElasticClient
from elasticsearch import Elasticsearch
from scrapemove.models import CombinedDetails
from scrapemove import scrapemove


class HouseIngestor:
    def __init__(self, config: Config) -> None:
        self._config = config
        self._es = ElasticClient(Elasticsearch(config.ES_URL))

    def scrape(self, parallelism: int) -> List[CombinedDetails]:
        return scrapemove.request(self._config.QUERY_URL, detailed=True, parallelism=parallelism)

    def index(self, data: List[CombinedDetails]) -> None:
        records = self._to_records(data)
        self._es.bulk_index(records, self._config.INDEX_NAME)

    def create_index(self, index_name=None) -> None:
        self._es.create_index(self._config.INDEX_NAME if index_name is None else index_name)

    @classmethod
    def _to_records(cls, data: List[CombinedDetails]) -> List[Dict[str, Any]]:
        return [d.merged_dict() for d in data]
