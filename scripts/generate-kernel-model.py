#!/usr/bin/env python3
# SPDX-FileCopyrightText: DPW FMS Contributors
# SPDX-License-Identifier: MIT
"""Generate the stable, text-only kernel plant model used by demo synchronization."""
import json,math
from pathlib import Path
origin=(24.9857,55.0273); location_types=['Entry gate','Exit gate','Security checkpoint','Weighbridge','Container yard block','Loading zone','Unloading zone','Empty-container yard','Loaded-container yard','Warehouse','Workshop','Maintenance bay','Parking area','Holding area','Charging area','Fuel area','Administration building','Inspection zone','Emergency assembly point']
routes=[]
for r in range(6):routes.append([[55.018+r*.002+p*.0013,24.978+(p%3)*.002+r*.0005] for p in range(6)])
points=[]
for r,coords in enumerate(routes,1):
 for i,(lng,lat) in enumerate(coords,1):
  name=f'R{r:02d}-P{i:02d}';east=(lng-origin[1])*111320*math.cos(math.radians(origin[0]));north=(lat-origin[0])*111320
  points.append({'name':name,'position':{'x':round(east*1000),'y':round(north*1000),'z':0},'properties':[{'name':'latitude','value':f'{lat:.7f}'},{'name':'longitude','value':f'{lng:.7f}'}],'layout':{'position':{'x':round(east*1000),'y':round(north*1000)},'labelOffset':{'x':0,'y':0},'layerId':0}})
paths=[]
def path(a,b,n):
 pa=next(x for x in points if x['name']==a)['position'];pb=next(x for x in points if x['name']==b)['position'];length=max(1,round(math.hypot(pa['x']-pb['x'],pa['y']-pb['y'])));return {'name':n,'srcPointName':a,'destPointName':b,'length':length,'maxVelocity':6000,'maxReverseVelocity':3000,'locked':False}
for r in range(1,7):
 for i in range(1,6):
  a=f'R{r:02d}-P{i:02d}';b=f'R{r:02d}-P{i+1:02d}';paths.extend([path(a,b,f'R{r:02d}-{i:02d}-F'),path(b,a,f'R{r:02d}-{i:02d}-R')])
 if r<6:
  a=f'R{r:02d}-P06';b=f'R{r+1:02d}-P01';paths.extend([path(a,b,f'CONNECT-{r:02d}-F'),path(b,a,f'CONNECT-{r:02d}-R')])
locations=[]
for i,kind in enumerate(location_types):
 point=points[i];locations.append({'name':f'{kind} {chr(65+i)}','typeName':'DPW_OPERATION','position':point['position'],'links':[{'pointName':point['name'],'allowedOperations':['MOVE']}],'properties':point['properties']})
model={'name':'DPW-FMS-JEBEL-ALI-V1','points':points,'paths':paths,'locationTypes':[{'name':'DPW_OPERATION','allowedOperations':['MOVE']}],'locations':locations,'blocks':[],'vehicles':[{'name':f'DPW-{i:03d}'} for i in range(1,21)],'visualLayout':{'name':'DPW FMS Jebel Ali','scaleX':50,'scaleY':50},'properties':[{'name':'dpw.fms.seedVersion','value':'1'},{'name':'map.bounds','value':'55.012,24.970,55.044,25.000'}]}
out=Path('opentcs-web-ui/src/main/resources/kernel-model/dpw-fms-plant-model.json');out.write_text(json.dumps(model,indent=2)+'\n');print(out,len(points),len(paths),len(locations))
