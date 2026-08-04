#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
"""Build the checked-in compact MBTiles bootstrap from the licensed local GeoJSON."""
import gzip,json,math,sqlite3,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; source=ROOT/'opentcs-web-ui/src/main/webapp/offline-map/data/uae.geojson'; output=ROOT/'deployment/maps/jebel-ali.mbtiles'
B=(55.012,24.970,55.044,25.000); EXTENT=4096

def varint(n):
 out=bytearray()
 while n>127: out.append((n&127)|128); n>>=7
 out.append(n); return bytes(out)
def field(n,wire,value): return varint((n<<3)|wire)+(varint(value) if wire==0 else varint(len(value))+value)
def zigzag(n): return (n<<1)^(n>>31)
def point(lon,lat,z,x,y):
 n=2**z; wx=(lon+180)/360*n; wy=(1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*n
 return round((wx-x)*EXTENT),round((wy-y)*EXTENT)
def geometry(coords,kind,z,x,y):
 pts=[point(*p,z,x,y) for p in coords]; out=bytearray(); px=py=0
 out+=varint((1<<3)|1); dx,dy=pts[0][0]-px,pts[0][1]-py;out+=varint(zigzag(dx))+varint(zigzag(dy));px,py=pts[0]
 if len(pts)>1:
  out+=varint(((len(pts)-1)<<3)|2)
  for nx,ny in pts[1:]: out+=varint(zigzag(nx-px))+varint(zigzag(ny-py));px,py=nx,ny
 if kind==3: out+=varint((1<<3)|7)
 return bytes(out)
def value(v): return field(1,2,str(v).encode())
def layer(name,features,z,x,y):
 keys=['name','kind']; values=[]; value_index={}; encoded=[]
 for i,f in enumerate(features,1):
  props=f.get('properties',{}); tags=[]
  for ki,k in enumerate(keys):
   v=str(props.get(k,'')); idx=value_index.setdefault(v,len(value_index)); tags.extend([ki,idx])
  g=f['geometry']; typ={'Point':1,'LineString':2,'Polygon':3}[g['type']]; coords=[g['coordinates']] if typ==1 else g['coordinates'] if typ==2 else g['coordinates'][0]
  geom=geometry(coords,typ,z,x,y); packed_tags=b''.join(varint(v) for v in tags)
  feature=field(1,0,i)+field(2,2,packed_tags)+field(3,0,typ)+field(4,2,geom);encoded.append(field(2,2,feature))
 body=field(15,0,2)+field(1,2,name.encode())+b''.join(encoded)+b''.join(field(3,2,k.encode()) for k in keys)
 for v,_ in sorted(value_index.items(),key=lambda p:p[1]): body+=field(4,2,value(v))
 body+=field(5,0,EXTENT); return field(3,2,body)
def tile(features,z,x,y):
 groups={'boundary':[],'transportation':[],'place':[],'landuse':[],'building':[],'water':[]}
 for f in features:
  kind=f['properties'].get('kind','landuse')
  groups['transportation' if kind in ('motorway','primary') else 'place' if f['geometry']['type']=='Point' else 'boundary'].append(f)
 raw=b''.join(layer(name,fs,z,x,y) for name,fs in groups.items()); return gzip.compress(raw,mtime=0)
def xyz(lon,lat,z):
 n=2**z;return int((lon+180)/360*n),int((1-math.asinh(math.tan(math.radians(lat)))/math.pi)/2*n)
data=json.loads(source.read_text()); output.unlink(missing_ok=True);db=sqlite3.connect(output)
db.executescript('CREATE TABLE metadata(name TEXT,value TEXT);CREATE TABLE tiles(zoom_level INTEGER,tile_column INTEGER,tile_row INTEGER,tile_data BLOB,UNIQUE(zoom_level,tile_column,tile_row));CREATE UNIQUE INDEX tile_index ON tiles(zoom_level,tile_column,tile_row);')
meta={'name':'DPW FMS Jebel Ali operating area','type':'baselayer','version':'1','description':'Compact OSM-derived bootstrap; regenerate with download-offline-map.sh for full detail','format':'pbf','bounds':','.join(f'{v:.3f}' for v in B),'center':'55.0273,24.9857,14','minzoom':'12','maxzoom':'16','attribution':'© OpenStreetMap contributors','json':json.dumps({'vector_layers':[{'id':x,'fields':{'name':'String','kind':'String'}} for x in ['boundary','transportation','place','landuse','building','water']]})}
db.executemany('INSERT INTO metadata VALUES (?,?)',meta.items())
for z in range(12,17):
 x0,y1=xyz(B[0],B[1],z);x1,y0=xyz(B[2],B[3],z)
 for x in range(x0,x1+1):
  for y in range(y0,y1+1): db.execute('INSERT INTO tiles VALUES (?,?,?,?)',(z,x,(1<<z)-1-y,tile(data['features'],z,x,y)))
db.commit();db.execute('ANALYZE');db.commit();db.close();print(output,output.stat().st_size)
