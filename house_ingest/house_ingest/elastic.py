from typing import List, Dict, Any
from tqdm import tqdm
from elasticsearch import Elasticsearch
from elasticsearch.helpers import streaming_bulk

_INDEX_NAME = "house-index"
_INDEX_CONFIG = {
    "settings": {"number_of_shards": 1},
    "mappings": {
        "properties": {
            "price": {"type": "integer"},
            "type": {"type": "text"},
            "address": {"type": "text"},
            "url": {"type": "text"},
            "agent_url": {"type": "text"},
            "postcode": {"type": "text"},
            "full_postcode": {"type": "text"},
            "bedrooms": {"type": "integer"},
            "floorplan_url": {"type": "text"}
        }
    },
}


class ElasticClient:
    def __init__(self, es: Elasticsearch) -> None:
        self._es = es

    def create_index(self):
        """Creates an index in Elasticsearch if one isn't already there."""
        self._es.indices.create(
            index=_INDEX_NAME,
            body=_INDEX_CONFIG,
            ignore=400,
        )

    def bulk_index(self, data: List[Dict[str, Any]]) -> None:
        # Convert house data to ES format
        docs = [self._convert(h) for h in data]
        progress = tqdm(unit="docs", total=len(docs))
        successes = 0
        for ok, action in streaming_bulk(
            client=self._es, index=_INDEX_NAME, actions=docs,
        ):
            progress.update(1)
            successes += ok
        return successes

    @classmethod
    def _convert(cls, data: Dict[str, Any]) -> Dict[str, Any]:
        id = data.pop("id")
        return {"_id": id, **data}
