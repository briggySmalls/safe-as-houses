"""Console script for house_ingest."""

import pickle
import sys
import logging

import click

from house_ingest.config import Config
from house_ingest.house_ingest import HouseIngestor


@click.group()
@click.pass_context
def main(ctx):
    """Console script for house_ingest."""
    ctx.ensure_object(dict)
    config = Config.from_env()
    ctx.obj["config"] = config
    ctx.obj["ingestor"] = HouseIngestor(config)
    logging.basicConfig(level=logging.INFO)
    return 0


@main.command()
@click.argument("output", type=click.File("wb"))
@click.option("--parallelism", default=1)
@click.pass_context
def search(ctx, output, parallelism):
    data = ctx.obj["ingestor"].scrape(parallelism)
    pickle.dump(data, output)


@main.command()
@click.argument("input", type=click.File("rb"))
@click.argument("output", type=click.File("wb"))
@click.option("--parallelism", default=1)
@click.pass_context
def calculate_area(ctx, input, output, parallelism):
    data = pickle.load(input)
    data_with_area = ctx.obj["ingestor"].calculate_area(data, parallelism)
    pickle.dump(data_with_area, output)


@main.command()
@click.option("--index-name", help="Override index to targe")
@click.option("--parallelism", default=1)
@click.pass_context
def execute(ctx, index_name, parallelism):
    data = ctx.obj["ingestor"].scrape(parallelism)
    data_with_area = ctx.obj["ingestor"].execute(data, index_name, parallelism)


@main.command()
@click.option("--index-name", help="Override index to targe")
@click.pass_context
def create_index(ctx, index_name):
    ctx.obj["ingestor"].create_index(index_name)


@main.command()
@click.pass_context
@click.argument("source")
@click.argument("destination")
def reindex(ctx, source, destination):
    ctx.obj["ingestor"].reindex(source, destination)


@main.command()
@click.pass_context
@click.argument("destination")
@click.argument("alias")
def move_alias(ctx, destination, alias):
    ctx.obj["ingestor"].move_alias(destination, alias)


if __name__ == "__main__":
    sys.exit(main())  # pragma: no cover
