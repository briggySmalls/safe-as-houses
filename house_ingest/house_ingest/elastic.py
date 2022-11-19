import json
from pathlib import Path
from typing import Any, Dict, List

from elasticsearch import Elasticsearch
from elasticsearch.helpers import streaming_bulk
from tqdm import tqdm

_ROOT_DIR = Path(__file__).parent
_RESOURCES_DIR = _ROOT_DIR / "resources"
_INDEX_CONFIG_FILE = _RESOURCES_DIR / "index-config.json"


class ElasticClient:
    def __init__(self, es: Elasticsearch) -> None:
        self._es = es

    def create_index(self, index_name: str) -> None:
        """Creates an index in Elasticsearch if one isn't already there."""
        with _INDEX_CONFIG_FILE.open() as f:
            index_config = json.load(f)
        self._es.indices.create(
            index=index_name,
            body=index_config,
            ignore=400,
        )

    def reindex(self, source: str, destination: str) -> None:
        self._es.reindex(dest={"index": destination}, source={"index": source})

    def bulk_index(self, data: List[Dict[str, Any]], index_name: str) -> None:
        # Convert house data to ES format
        docs = [self._convert(h) for h in data]
        print(f"Indexing {len(docs)} docs into {index_name}")
        progress = tqdm(unit="docs", total=len(docs))
        successes = 0
        for ok, action in streaming_bulk(
            client=self._es,
            index=index_name,
            actions=docs,
        ):
            progress.update(1)
            successes += ok
        return successes

    @classmethod
    def _convert(cls, data: Dict[str, Any]) -> Dict[str, Any]:
        # Pull out an ID
        id = data.pop("id")
        # Rename location fields to geo_point compatible format
        old_location = data.pop("location")
        data["location"] = {
            "lat": old_location["latitude"],
            "lon": old_location["longitude"],
        }
        return {"_id": id, **data}
