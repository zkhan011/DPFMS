#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
"""Fail builds when mandatory offline assets are invalid or reference remote services."""
import json
from pathlib import Path

ROOT = Path("opentcs-web-ui/src/main/webapp/offline-map")
mandatory = [ROOT / "data/uae.geojson", ROOT / "style/style.json", ROOT / "licenses/ATTRIBUTION.txt", ROOT / "licenses/ODbL-1.0.txt"]
for path in mandatory:
    if not path.is_file() or path.stat().st_size == 0:
        raise SystemExit(f"Required offline map asset is missing or empty: {path}")
for path in [ROOT / "data/uae.geojson", ROOT / "style/style.json"]:
    json.loads(path.read_text(encoding="utf-8"))
forbidden = ("maps.googleapis.com", "tile.googleapis.com", "fonts.googleapis.com", "fonts.gstatic.com", "api.mapbox.com", "tile.openstreetmap.org", "carto.com", "arcgis.com", "bing.com", "http://", "https://")
for path in [ROOT / "data/uae.geojson", ROOT / "style/style.json"]:
    text = path.read_text(encoding="utf-8").lower()
    for value in forbidden:
        if value in text:
            raise SystemExit(f"Forbidden remote reference {value!r} in {path}")
print("DPW FMS offline UAE map assets validated")
