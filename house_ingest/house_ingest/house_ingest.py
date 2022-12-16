"""Main module."""
from typing import Any, Dict, List, Optional

from elasticsearch import Elasticsearch
from itertools import repeat
from scrapemove import scrapemove
from scrapemove.models import CombinedDetails
import requests
from planparser.planparser import PlanParser
from tqdm import tqdm
from multiprocessing.dummy import Pool as ThreadPool
from io import BytesIO

from house_ingest.config import Config
from house_ingest.elastic import ElasticClient
from house_ingest.models import IndexData


def head(l: List[Any]) -> Optional[Any]:
    return next(iter(l), None)


def _calculate_area(data: CombinedDetails) -> IndexData:
    # Request the first floorplan
    floorplan_url = head(data.additional_details.floorplans)
    if floorplan_url is None:
        return IndexData(scraped=data, area_sqft=None)
    response = requests.get(floorplan_url)
    image = BytesIO(response.content)
    print(f"Parsing area from URL {floorplan_url}")
    area = PlanParser.parse(image).area
    return IndexData(scraped=data, area_sqft=area)


def iterate(d: CombinedDetails, progress) -> IndexData:
    result = _calculate_area(d)
    progress.update(1)
    return result



class HouseIngestor:
    def __init__(self, config: Config) -> None:
        self._config = config
        self._es = ElasticClient(Elasticsearch(config.ES_URL))

    def scrape(self, parallelism: int=1) -> List[CombinedDetails]:
        return scrapemove.request(
            self._config.QUERY_URL, detailed=True, parallelism=parallelism
        )

    def calculate_area(self, data: List[CombinedDetails], parallelism: int=1) -> List[IndexData]:
        print(f"Calculating area for {len(data)} properties")
        progress = tqdm(unit="properties", total=len(data))
        with ThreadPool(parallelism) as p:
            return p.starmap(iterate, zip(data, repeat(progress)))

    def index(self, data: List[IndexData]) -> None:
        records = self._to_records(data)
        self._es.bulk_index(records, self._config.INDEX_NAME)

    def create_index(self, index_name=None) -> None:
        self._es.create_index(
            self._config.INDEX_NAME if index_name is None else index_name
        )

    def reindex(self, source: str, destination: str) -> None:
        self._es.reindex(source, destination)

    @classmethod
    def _to_records(cls, data: List[IndexData]) -> List[Dict[str, Any]]:
        return [d.merged_dict() for d in data]
