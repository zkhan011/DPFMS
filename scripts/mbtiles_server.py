#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
"""Read-only internal HTTP service for vector MBTiles."""
import json,os,sqlite3
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from urllib.parse import urlparse
archive=os.environ.get('FMS_OFFLINE_MBTILES_PATH','/maps/jebel-ali.mbtiles');port=int(os.environ.get('PORT','8090'))
def xyz_to_tms(z,y):
 if not 0<=z<=30 or not 0<=y<(1<<z): raise ValueError('tile coordinate outside grid')
 return (1<<z)-1-y
def connect(): return sqlite3.connect(f'file:{archive}?mode=ro',uri=True)
def metadata():
 with connect() as db:return dict(db.execute('select name,value from metadata'))
def tile(z,x,y):
 if not 0<=x<(1<<z):raise ValueError('tile coordinate outside grid')
 with connect() as db:
  row=db.execute('select tile_data from tiles where zoom_level=? and tile_column=? and tile_row=?',(z,x,xyz_to_tms(z,y))).fetchone();return row[0] if row else None
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  try:
   path=urlparse(self.path).path
   if path=='/health': self.send_response(200);self.end_headers();self.wfile.write(b'ok');return
   if path=='/metadata': body=json.dumps(metadata()).encode();kind='application/json';encoding=None
   else:
    parts=path.strip('/').split('/');
    if len(parts)!=3 or not parts[2].endswith('.pbf'):raise ValueError('invalid path')
    body=tile(int(parts[0]),int(parts[1]),int(parts[2][:-4]));kind='application/vnd.mapbox-vector-tile';encoding='gzip'
    if body is None:self.send_error(404,'Vector tile not found');return
   self.send_response(200);self.send_header('Content-Type',kind);self.send_header('Cache-Control','public,max-age=86400,immutable')
   if encoding:self.send_header('Content-Encoding',encoding)
   self.send_header('Content-Length',str(len(body)));self.end_headers();self.wfile.write(body)
  except (ValueError,sqlite3.Error,OSError) as exc:self.send_error(400,str(exc))
 def log_message(self,format,*args):pass
if __name__=='__main__':
 required={'format':'pbf','bounds':None,'center':None,'minzoom':None,'maxzoom':None,'attribution':None};values=metadata()
 for key,expected in required.items():
  if not values.get(key) or expected and values[key]!=expected:raise SystemExit(f'Invalid MBTiles metadata: {key}')
 ThreadingHTTPServer(('0.0.0.0',port),Handler).serve_forever()
