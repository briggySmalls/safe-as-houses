#!/bin/sh

set -e

. /venv/bin/activate
houseingest execute --parallelism "${PARALLELISM:-10}"