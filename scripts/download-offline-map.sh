#!/bin/sh
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
SOURCE_URL=${FMS_OSM_SOURCE_URL:-https://download.geofabrik.de/asia/gcc-states-latest.osm.pbf}
BBOX=${FMS_MAP_BBOX:-55.012,24.970,55.044,25.000}
OUTPUT=${FMS_OFFLINE_MBTILES_PATH:-$ROOT/deployment/maps/jebel-ali.mbtiles}
TILEMAKER_IMAGE=${TILEMAKER_IMAGE:-tilemaker/tilemaker:3.0.0}
case "$BBOX" in *[!0-9.,-]*|*,,*) echo "Invalid FMS_MAP_BBOX: $BBOX" >&2; exit 2;; esac
python3 - "$BBOX" <<'PY'
import sys
try: w,s,e,n=map(float,sys.argv[1].split(','))
except Exception: raise SystemExit('Bounding box must contain west,south,east,north')
if not (-180<=w<e<=180 and -90<=s<n<=90): raise SystemExit('Bounding box coordinates/order are invalid')
if (e-w)*(n-s)>.01: raise SystemExit('Bounding box is unexpectedly large; refusing broad map download')
PY
command -v docker >/dev/null || { echo 'Docker is required to run pinned tilemaker 3.0.0' >&2; exit 3; }
mkdir -p "$ROOT/deployment/maps/source" "$(dirname "$OUTPUT")"
PBF="$ROOT/deployment/maps/source/operating-area-source.osm.pbf"
echo "Downloading OSM source from $SOURCE_URL (temporary source; output is strictly clipped to $BBOX)"
curl --fail --location --retry 3 --user-agent 'DPW-FMS-offline-map-builder/1.0' "$SOURCE_URL" -o "$PBF.tmp"
mv "$PBF.tmp" "$PBF"
TMP_OUTPUT="$(dirname "$OUTPUT")/jebel-ali.tmp.mbtiles"
rm -f "$TMP_OUTPUT"
docker run --rm -v "$ROOT:/work" "$TILEMAKER_IMAGE" \
  --input /work/deployment/maps/source/operating-area-source.osm.pbf \
  --output /work/deployment/maps/jebel-ali.tmp.mbtiles \
  --bbox "$BBOX" --config /work/scripts/offline-map/tilemaker-config.json \
  --process /work/scripts/offline-map/process.lua
mv "$TMP_OUTPUT" "$OUTPUT"
"$ROOT/scripts/validate-offline-map.py" --mbtiles "$OUTPUT" --bounds "$BBOX"
(cd "$(dirname "$OUTPUT")" && sha256sum "$(basename "$OUTPUT")" > "$(basename "$OUTPUT").sha256" && base64 -w 76 "$(basename "$OUTPUT")" > "$(basename "$OUTPUT").base64")
rm -f "$PBF"
echo "Generated $OUTPUT ($(du -h "$OUTPUT" | cut -f1)), tilemaker image $TILEMAKER_IMAGE"
