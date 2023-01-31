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

    def index(self, index: str, data: Dict[str, Any]) -> None:
        id, doc = self._convert(data)
        return self._es.update(
            index=index,
            id=id,
            body={
                "doc": doc,
                "doc_as_upsert": True
            }
        )

    @classmethod
    def _convert(cls, data: Dict[str, Any]) -> Dict[str, Any]:
        # Grab the ID (but keep it in the main _source payload)
        id = data["id"]
        # Rename location fields to geo_point compatible format
        old_location = data.pop("location")
        data["location"] = {
            "lat": old_location["latitude"],
            "lon": old_location["longitude"],
        }
        return id, data
