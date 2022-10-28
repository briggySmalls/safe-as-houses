"""Console script for house_ingest."""

import sys

import click
import pandas as pd

from house_ingest.house_ingest import HouseIngestor
from house_ingest.config import Config


@click.group()
@click.pass_context
def main(ctx):
    """Console script for house_ingest."""
    ctx.ensure_object(dict)
    config = Config.from_env()
    ctx.obj["config"] = config
    ctx.obj["ingestor"] = HouseIngestor(config)
    return 0


@main.command()
@click.argument('output', type=click.Path())
@click.pass_context
def pickle(ctx, output):
    data = ctx.obj["ingestor"].scrape()
    data.to_pickle(output)


@main.command()
@click.argument('input', type=click.Path(exists=True))
@click.pass_context
def index(ctx, input):
    df = pd.read_pickle(input)
    ctx.obj["ingestor"].index(df)


if __name__ == "__main__":
    sys.exit(main())  # pragma: no cover
