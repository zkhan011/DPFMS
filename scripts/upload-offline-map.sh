#!/bin/sh
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
# Validate and install a cropped vector MBTiles file into the deployment map package.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
SOURCE=${1:-}
TARGET=${FMS_OFFLINE_MBTILES_PATH:-$ROOT/deployment/maps/jebel-ali.mbtiles}
BBOX=${FMS_MAP_BBOX:-55.012,24.970,55.044,25.000}
[ -n "$SOURCE" ] || { echo "Usage: $0 /path/to/cropped.mbtiles" >&2; exit 2; }
[ -s "$SOURCE" ] || { echo "Source MBTiles is missing or empty: $SOURCE" >&2; exit 2; }
mkdir -p "$(dirname "$TARGET")"
TMP="$(dirname "$TARGET")/.upload-$(basename "$TARGET")"
cp "$SOURCE" "$TMP"
python3 "$ROOT/scripts/validate-offline-map.py" --mbtiles "$TMP" --bounds "$BBOX"
mv "$TMP" "$TARGET"
(cd "$(dirname "$TARGET")" && sha256sum "$(basename "$TARGET")" > "$(basename "$TARGET").sha256" && base64 -w 76 "$(basename "$TARGET")" > "$(basename "$TARGET").base64")
echo "Installed validated offline map at $TARGET"
echo "Updated $(basename "$TARGET").sha256 and $(basename "$TARGET").base64 for Docker packaging."
