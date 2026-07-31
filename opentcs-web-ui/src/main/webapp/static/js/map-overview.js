/* SPDX-FileCopyrightText: DPW FMS Contributors; SPDX-License-Identifier: MIT */
'use strict';
const TERMINAL_BOUNDS={minLat:24.970,maxLat:25.000,minLng:55.012,maxLng:55.044};
let bounds={...TERMINAL_BOUNDS},fleet,config,provider,selectedId=null,selectedRoute=null,timer;
const layers={vehicles:true,chargingStations:true,fuelStations:true,locations:true,alerts:true,routes:true};
const coordinates=(lat,lng)=>({x:(lng-bounds.minLng)/(bounds.maxLng-bounds.minLng)*100,y:(bounds.maxLat-lat)/(bounds.maxLat-bounds.minLat)*100});
const svgElement=name=>document.createElementNS('http://www.w3.org/2000/svg',name);

class FleetMapProvider {
  initialize() {} destroy() {} update() {} setCenter() {} setZoom() {}
  getViewport(){return{bounds:{...bounds},zoom:config.zoom}}
  restoreViewport(viewport){if(viewport?.bounds)bounds={...viewport.bounds}}
  fitBounds(){fitFleetBounds();this.update()}
  fitRoute(){if(!selectedRoute)return;fitCoordinateBounds(selectedRoute);this.update()}
  setLayerVisibility(name,value){layers[name]=value}
  setSelectedAsset(id){selectedId=id}
}

class OfflineUaeMapProvider extends FleetMapProvider {
  async initialize(){
    this.root=document.getElementById('map');this.root.replaceChildren();this.root.className='offline-fleet-map';
    this.svg=svgElement('svg');this.svg.setAttribute('viewBox','0 0 100 100');this.svg.classList.add('terminal-basemap');this.root.append(this.svg);
    const response=await fetch(ctx+'/offline-map/data/uae.geojson',{cache:'force-cache'});
    if(!response.ok)throw new Error('Bundled UAE map data is unavailable.');
    this.baseMap=await response.json();this.update();
  }
  destroy(){this.root?.replaceChildren()}
  update(){
    this.svg.replaceChildren();const background=svgElement('rect');Object.entries({x:0,y:0,width:100,height:100,fill:'#0b1a2b'}).forEach(([key,value])=>background.setAttribute(key,value));this.svg.append(background);
    this.baseMap.features.forEach(feature=>this.drawBaseFeature(feature));
    if(layers.routes)fleet.routes.forEach(route=>this.drawRoute(route,route===selectedRoute));
    if(selectedRoute)this.drawRoute({id:'selected-route',status:'Selected',geometry:selectedRoute},true);
    [['locations','location','◆'],['chargingStations','charging','⚡'],['fuelStations','fuel','F'],['alerts','alert','!'],['vehicles','vehicle','▣']].forEach(([key,kind,icon])=>{if(layers[key])fleet[key].forEach(asset=>this.drawMarker(asset,kind,icon))});
  }
  drawBaseFeature(feature){
    const geometry=feature.geometry;if(!geometry)return;
    if(geometry.type==='LineString'){const line=svgElement('polyline');line.setAttribute('points',geometry.coordinates.map(([lng,lat])=>{const point=coordinates(lat,lng);return`${point.x},${point.y}`}).join(' '));line.setAttribute('class','offline-road');this.svg.append(line)}
    if(geometry.type==='Polygon'){const polygon=svgElement('polygon');polygon.setAttribute('points',geometry.coordinates[0].map(([lng,lat])=>{const point=coordinates(lat,lng);return`${point.x},${point.y}`}).join(' '));polygon.setAttribute('class','offline-land');this.svg.append(polygon)}
    if(geometry.type==='Point'){const [lng,lat]=geometry.coordinates,point=coordinates(lat,lng);if(point.x<0||point.x>100||point.y<0||point.y>100)return;const text=svgElement('text');text.setAttribute('x',point.x);text.setAttribute('y',point.y);text.setAttribute('class','offline-label');text.textContent=feature.properties.name;this.svg.append(text)}
  }
  drawRoute(route,selected=false){const line=svgElement('polyline');line.setAttribute('points',route.geometry.map(([lng,lat])=>{const point=coordinates(lat,lng);return`${point.x},${point.y}`}).join(' '));line.setAttribute('class',`mock-route ${String(route.status).toLowerCase()}${selected?' selected':''}`);line.dataset.id=route.id;this.svg.append(line)}
  drawMarker(asset,kind,icon){
    if(asset.latitude==null||asset.longitude==null)return;const point=coordinates(asset.latitude,asset.longitude);if(point.x<0||point.x>100||point.y<0||point.y>100)return;
    const group=svgElement('g');group.setAttribute('transform',`translate(${point.x} ${point.y})`);group.setAttribute('class',`asset-marker ${kind} status-${String(asset.status).toLowerCase().replaceAll(' ','-')}${asset.id===selectedId?' selected':''}`);group.dataset.id=asset.id;
    const circle=svgElement('circle');circle.setAttribute('r',kind==='vehicle'?2.2:1.8);const text=svgElement('text');text.textContent=icon;text.setAttribute('text-anchor','middle');text.setAttribute('y','.8');group.append(circle,text);group.addEventListener('click',()=>selectAsset(asset));this.svg.append(group);
  }
}

class GoogleMapProvider extends FleetMapProvider {
  async initialize(){await loadGoogle(config.googleApiKey);this.map=new google.maps.Map(document.getElementById('map'),{center:{lat:config.defaultLatitude,lng:config.defaultLongitude},zoom:config.zoom,mapTypeControl:true,streetViewControl:false});this.objects=[];this.update()}
  destroy(){this.objects.forEach(object=>object.setMap(null));this.objects=[];document.getElementById('map').replaceChildren()}
  update(){this.objects.forEach(object=>object.setMap(null));this.objects=[];if(layers.routes)fleet.routes.forEach(route=>this.drawRoute(route.geometry,route.status));if(selectedRoute)this.drawRoute(selectedRoute,'Selected');[['vehicles','▣'],['chargingStations','⚡'],['fuelStations','F'],['locations','◆'],['alerts','!']].forEach(([key,label])=>{if(!layers[key])return;fleet[key].forEach(asset=>{if(asset.latitude==null||asset.longitude==null)return;const marker=new google.maps.Marker({map:this.map,position:{lat:asset.latitude,lng:asset.longitude},title:`${asset.id} ${asset.name||asset.type}`,label});marker.addListener('click',()=>selectAsset(asset));this.objects.push(marker)})})}
  drawRoute(geometry,status){this.objects.push(new google.maps.Polyline({path:geometry.map(([lng,lat])=>({lng,lat})),strokeColor:status==='Blocked'?'#ef4444':status==='Selected'?'#f59e0b':'#3b82f6',strokeWeight:status==='Selected'?7:4,map:this.map}))}
  getViewport(){return{center:this.map.getCenter().toJSON(),zoom:this.map.getZoom()}}
  restoreViewport(viewport){if(viewport?.center){this.map.setCenter(viewport.center);this.map.setZoom(viewport.zoom)}}
  fitBounds(){const value=new google.maps.LatLngBounds();allCoordinates().forEach(([lng,lat])=>value.extend({lng,lat}));if(!value.isEmpty())this.map.fitBounds(value)}
  fitRoute(){if(!selectedRoute)return;const value=new google.maps.LatLngBounds();selectedRoute.forEach(([lng,lat])=>value.extend({lng,lat}));this.map.fitBounds(value)}
}

function loadGoogle(key){return new Promise((resolve,reject)=>{if(!key)return reject(new Error('missing-key'));let finished=false,timeout;const finish=callback=>()=>{if(finished)return;finished=true;clearTimeout(timeout);callback()};window.gm_authFailure=finish(()=>reject(new Error('authentication')));window.dpwFmsGoogleReady=finish(resolve);const script=document.createElement('script');script.src=`https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}&callback=dpwFmsGoogleReady`;script.async=true;script.onerror=finish(()=>reject(new Error('network')));document.head.append(script);timeout=setTimeout(finish(()=>reject(new Error('timeout'))),8000)})}
async function activate(name,reason=''){const viewport=provider?.getViewport(),message=document.getElementById('map-message');provider?.destroy();if(name==='google'&&config.googleApiKey){try{provider=new GoogleMapProvider();await provider.initialize();if(viewport)provider.restoreViewport(viewport);setIndicator('Online map: Google Maps','online');return}catch(error){reason='Google Maps unavailable — offline UAE map activated'}}else if(name==='google')reason='Google Maps key is not configured — offline UAE map activated';if(!config.offlineEnabled)throw new Error('No map provider is available. Enable the offline map or configure Google Maps.');provider=new OfflineUaeMapProvider();await provider.initialize();if(viewport)provider.restoreViewport(viewport);provider.update();setIndicator('Offline UAE map active','offline');message.hidden=!reason;message.textContent=reason;document.getElementById('provider-switch').value='offline'}
function setIndicator(text,state){const element=document.getElementById('map-provider');element.textContent=text;element.className=`pill ${state}`}
function selectAsset(asset){selectedId=asset.id;provider.setSelectedAsset(asset.id);provider.update();const box=document.getElementById('map-results');box.replaceChildren();const card=document.createElement('div');card.className='asset-popup';[['ID',asset.id],['Name',asset.name||asset.type],['Status',asset.status||asset.severity],['Type',asset.type],['Speed',asset.speed!=null?`${asset.speed} km/h`:null],['Energy',asset.energyType==='Electric'?`${asset.battery}% battery`:asset.fuel!=null?`${asset.fuel}% fuel`:null],['Job',asset.currentJob],['Operator',asset.operator],['Location',asset.currentLocation],['Destination',asset.destination],['Updated',asset.lastUpdate||asset.created]].filter(([,value])=>value!=null&&value!=='').forEach(([key,value])=>{const row=document.createElement('p'),label=document.createElement('b');label.textContent=`${key}: `;row.append(label,document.createTextNode(String(value)));card.append(row)});box.append(card)}
function allCoordinates(){return ['vehicles','chargingStations','fuelStations','locations','alerts'].flatMap(key=>fleet[key]).filter(asset=>asset.latitude!=null&&asset.longitude!=null).map(asset=>[asset.longitude,asset.latitude])}
function fitCoordinateBounds(values){const lngs=values.map(value=>value[0]),lats=values.map(value=>value[1]),padding=.002;bounds={minLat:Math.min(...lats)-padding,maxLat:Math.max(...lats)+padding,minLng:Math.min(...lngs)-padding,maxLng:Math.max(...lngs)+padding}}
function fitFleetBounds(){const values=allCoordinates();if(values.length)fitCoordinateBounds(values)}
function populateRouting(){const locations=(fleet.routingNodes||fleet.locations).filter(location=>location.latitude!=null);['route-source','route-destination'].forEach((id,index)=>{const select=document.getElementById(id);select.replaceChildren(...locations.map((location,position)=>{const option=document.createElement('option');option.value=location.name;option.textContent=location.name;if(position===index)option.selected=true;return option}))})}
async function calculateRoute(){try{const source=document.getElementById('route-source').value,destination=document.getElementById('route-destination').value,result=await api(`/routes?source=${encodeURIComponent(source)}&destination=${encodeURIComponent(destination)}`);if(!result.found)throw new Error(result.message||'No route was found.');if(result.geometry)selectedRoute=result.geometry;else{const pathNames=new Set(result.paths||[]);selectedRoute=fleet.routes.filter(route=>pathNames.has(route.id)).flatMap(route=>route.geometry)}document.getElementById('route-result').textContent=`${result.strategy} route · cost ${result.totalCost}`;provider.update();provider.fitRoute()}catch(error){showMessage(`Routing failed: ${error.message}`)}}
function localSearch(){const query=document.getElementById('map-search').value.toLowerCase();if(!query)return;const result=['vehicles','jobs','chargingStations','fuelStations','locations','alerts'].flatMap(key=>fleet[key]).find(asset=>Object.values(asset).some(value=>String(value).toLowerCase().includes(query)));if(result)selectAsset(result);else showMessage('No local DPW FMS asset matches that search.')}
async function refreshFleet(){fleet=await api('/fleet');provider?.update()}
async function init(){try{config=await api('/map/config');bounds={minLat:config.defaultLatitude-.015,maxLat:config.defaultLatitude+.015,minLng:config.defaultLongitude-.016,maxLng:config.defaultLongitude+.016};fleet=await api('/fleet');populateRouting();const forced=config.provider.toLowerCase(),preference=localStorage.getItem('dpw-fms-map-provider'),choice=forced==='offline'?'offline':preference||((forced==='google'||forced==='auto')?'google':'offline');document.getElementById('provider-switch').querySelector('[value=google]').disabled=!config.googleApiKey||forced==='offline';await activate(choice);const requested=new URLSearchParams(location.search).get('asset');if(requested){const asset=['vehicles','jobs','locations'].flatMap(key=>fleet[key]).find(value=>value.id===requested);if(asset)selectAsset(asset)}timer=setInterval(()=>refreshFleet().catch(()=>{}),config.updateIntervalMs)}catch(error){const message=document.getElementById('map-message');message.hidden=false;message.textContent=`Fleet map unavailable: ${error.message}`}}
document.getElementById('provider-switch').addEventListener('change',event=>{localStorage.setItem('dpw-fms-map-provider',event.target.value);activate(event.target.value).catch(error=>showMessage(error.message))});document.querySelectorAll('[data-layer]').forEach(element=>element.addEventListener('change',()=>{provider.setLayerVisibility(element.dataset.layer,element.checked);provider.update()}));document.getElementById('map-search').addEventListener('change',localSearch);document.getElementById('fit-assets').addEventListener('click',()=>provider.fitBounds());document.getElementById('fit-route').addEventListener('click',()=>provider.fitRoute());document.getElementById('calculate-route').addEventListener('click',calculateRoute);window.addEventListener('pagehide',()=>{clearInterval(timer);provider?.destroy()},{once:true});init();
