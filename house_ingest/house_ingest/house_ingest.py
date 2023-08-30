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
    # First check if we already have the area
    supplied_area = next((s for s in data.additional_details.sizings if s.unit == "sqft"), None)
    if supplied_area:
        print(f"Skipping, as area supplied for {data.property.id}")
        return IndexData(scraped=data, area_sqft=supplied_area.maximum_size)

    # Request the first floorplan
    floorplan_url = head(data.additional_details.floorplans)
    if floorplan_url is None:
        return IndexData(scraped=data, area_sqft=None)
    response = requests.get(floorplan_url)
    image = BytesIO(response.content)
    print(f"Parsing area for {data.property.id} from URL {floorplan_url}")
    area = PlanParser.parse(image).area
    return IndexData(scraped=data, area_sqft=area)


def _index(index: str, data: IndexData, client: ElasticClient) -> None:
    record = data.merged_dict()
    client.index(index=index, data=record)


def iterate(index:str, data: CombinedDetails, client: ElasticClient, progress) -> IndexData:
    result = _calculate_area(data)
    _index(index, result, client)
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

    def execute(
        self, data: List[CombinedDetails], index_name: bool = None, parallelism: int = 1
    ) -> None:
        progress = tqdm(unit="properties", total=len(data), disable=None)
        name = self._config.INDEX_NAME if index_name is None else index_name
        args = zip(repeat(name), data, repeat(self._es), repeat(progress))
        with ThreadPool(parallelism) as p:
            return p.starmap(iterate, args)

    def create_index(self, index_name=None) -> None:
        self._es.create_index(
            self._config.INDEX_NAME if index_name is None else index_name
        )

    def reindex(self, source: str, destination: str) -> None:
        self._es.reindex(source, destination)

