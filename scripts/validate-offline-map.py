#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
"""Validate local map assets, MBTiles integrity, metadata, coverage and offline URLs."""
import argparse,json,sqlite3
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('--mbtiles',default='deployment/maps/jebel-ali.mbtiles');parser.add_argument('--bounds',default='55.012,24.970,55.044,25.000');args=parser.parse_args()
root=Path('opentcs-web-ui/src/main/webapp/offline-map'); mandatory=[root/'data/uae.geojson',root/'style/style.json',root/'licenses/ATTRIBUTION.txt',root/'licenses/ODbL-1.0.txt',Path(args.mbtiles)]
for path in mandatory:
 if not path.is_file() or path.stat().st_size==0: raise SystemExit(f'Required offline map asset is missing or empty: {path}')
for path in [root/'data/uae.geojson',root/'style/style.json']: json.loads(path.read_text(encoding='utf-8'))
forbidden=('maps.googleapis.com','tile.googleapis.com','fonts.googleapis.com','fonts.gstatic.com','api.mapbox.com','tile.openstreetmap.org','carto.com','arcgis.com','bing.com','http://','https://')
for path in [root/'data/uae.geojson',root/'style/style.json']:
 text=path.read_text(encoding='utf-8').lower()
 for value in forbidden:
  if value in text: raise SystemExit(f'Forbidden remote reference {value!r} in {path}')
try:
 db=sqlite3.connect(f'file:{Path(args.mbtiles).resolve()}?mode=ro',uri=True); integrity=db.execute('PRAGMA integrity_check').fetchone()[0]
 metadata=dict(db.execute('SELECT name,value FROM metadata')); tile_count=db.execute('SELECT count(*) FROM tiles').fetchone()[0]
except sqlite3.Error as exc: raise SystemExit(f'Invalid MBTiles database: {exc}') from exc
finally:
 try: db.close()
 except NameError: pass
if integrity!='ok': raise SystemExit(f'MBTiles integrity check failed: {integrity}')
for key in ('name','format','bounds','center','minzoom','maxzoom','attribution'):
 if not metadata.get(key): raise SystemExit(f'MBTiles metadata missing: {key}')
if metadata['format']!='pbf': raise SystemExit('MBTiles format must be pbf')
if metadata['bounds']!=args.bounds: raise SystemExit(f"MBTiles bounds {metadata['bounds']} do not match configured {args.bounds}")
if tile_count==0: raise SystemExit('MBTiles archive contains no tiles')
if 'OpenStreetMap contributors' not in metadata['attribution']: raise SystemExit('MBTiles attribution is missing OpenStreetMap contributors')
print(f"DPW FMS offline map validated: {tile_count} tiles, zoom {metadata['minzoom']}–{metadata['maxzoom']}, bounds {metadata['bounds']}")
