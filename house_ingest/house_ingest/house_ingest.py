"""Main module."""
from typing import Any, Dict, List

from elasticsearch import Elasticsearch
from scrapemove import scrapemove
from scrapemove.models import CombinedDetails

from house_ingest.config import Config
from house_ingest.elastic import ElasticClient


class HouseIngestor:
    def __init__(self, config: Config) -> None:
        self._config = config
        self._es = ElasticClient(Elasticsearch(config.ES_URL))

    def scrape(self, parallelism: int) -> List[CombinedDetails]:
        return scrapemove.request(
            self._config.QUERY_URL, detailed=True, parallelism=parallelism
        )

    def index(self, data: List[CombinedDetails]) -> None:
        records = self._to_records(data)
        self._es.bulk_index(records, self._config.INDEX_NAME)

    def create_index(self, index_name=None) -> None:
        self._es.create_index(
            self._config.INDEX_NAME if index_name is None else index_name
        )

    def reindex(self, source: str, destination: str) -> None:
        self._es.reindex(source, destination)

    @classmethod
    def _to_records(cls, data: List[CombinedDetails]) -> List[Dict[str, Any]]:
        return [d.merged_dict() for d in data]
