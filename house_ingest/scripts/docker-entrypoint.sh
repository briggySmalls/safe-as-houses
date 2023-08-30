#!/bin/sh

set -e

. /venv/bin/activate

# Determine the new index name
CURRENT_DATE=`date +"%Y-%m-%d-%H-%M-%S"`
FULL_INDEX_NAME="${INDEX_PREFIX}-${CURRENT_DATE}"

# Create a new index for this session
houseingest create-index --index-name "$FULL_INDEX_NAME"
# Index
houseingest execute --index-name "$FULL_INDEX_NAME" --parallelism "${PARALLELISM:-10}"
# Roll index
houseingest move-alias "$FULL_INDEX_NAME" "$INDEX_ALIAS_NAME"