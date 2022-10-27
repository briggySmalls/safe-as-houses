"""Console script for house_ingest."""

import sys

import click

from house_ingest.house_ingest import HouseIngestor
from house_ingest.config import Config


@click.command()
def main():
    """Console script for house_ingest."""
    config = Config.from_env()
    hi = HouseIngestor(config)
    click.echo(hi.scrape())
    return 0


if __name__ == "__main__":
    sys.exit(main())  # pragma: no cover
