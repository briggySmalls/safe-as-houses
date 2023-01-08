#!/bin/sh

set -e

. /venv/bin/activate
# Ensure the index has been created (configures the mappings)
houseingest create-index
# Index
houseingest execute --parallelism "${PARALLELISM:-10}"