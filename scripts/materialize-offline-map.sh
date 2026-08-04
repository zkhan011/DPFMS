#!/bin/sh
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
TARGET=${1:-$ROOT/deployment/maps/jebel-ali.mbtiles}
mkdir -p "$(dirname "$TARGET")"
base64 -d "$ROOT/deployment/maps/jebel-ali.mbtiles.base64" > "$TARGET.tmp"
mv "$TARGET.tmp" "$TARGET"
EXPECTED=$(cut -d" " -f1 "$ROOT/deployment/maps/jebel-ali.mbtiles.sha256")
ACTUAL=$(sha256sum "$TARGET" | cut -d" " -f1)
[ "$EXPECTED" = "$ACTUAL" ] || { echo "MBTiles checksum mismatch" >&2; rm -f "$TARGET"; exit 1; }
echo "Materialized $TARGET"
