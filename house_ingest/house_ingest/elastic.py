import json
from pathlib import Path
from typing import Any, Dict, List

from elasticsearch import Elasticsearch, NotFoundError
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
            index=index, id=id, body={"doc": doc, "doc_as_upsert": True}
        )

    def move_alias(self, destination: str, alias: str) -> None:
        # Get the current alias
        try:
            existing_alias = next(iter(self._es.indices.get_alias(name=alias).keys()), None)
        except NotFoundError:
            existing_alias = None
        # Build the request
        actions = [{"add": {"index": destination, "alias": alias}}]
        if existing_alias is not None:
            actions += {"remove": {"index": existing_alias, "alias": alias}},
        # Move the alias
        self._es.indices.update_aliases(actions=actions)

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
