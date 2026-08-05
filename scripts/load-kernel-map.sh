#!/bin/sh
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
# Load the bundled DPW FMS plant/routing model into the kernel API.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
KERNEL_URL=${OPENTCS_WEB_KERNEL_API_BASE_URL:-http://localhost:55200/v1}
MODEL=${FMS_KERNEL_MODEL_JSON:-$ROOT/opentcs-web-ui/src/main/resources/kernel-model/dpw-fms-plant-model.json}
ACCESS_KEY=${OPENTCS_WEB_ACCESS_KEY:-}
[ -s "$MODEL" ] || { echo "Kernel model JSON is missing or empty: $MODEL" >&2; exit 2; }
HEADER=${ACCESS_KEY:+-H X-Api-Access-Key:$ACCESS_KEY}
curl --fail --silent --show-error -X PUT "$KERNEL_URL/plantModel" $HEADER -H 'Content-Type: application/json' --data-binary "@$MODEL" >/dev/null
curl --fail --silent --show-error -X POST "$KERNEL_URL/plantModel/topologyUpdateRequest" $HEADER -H 'Content-Type: application/json' -d '{"paths":[]}' >/dev/null
echo "Loaded DPW FMS kernel routing model from $MODEL into $KERNEL_URL"
