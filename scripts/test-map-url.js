#!/usr/bin/env node
/* SPDX-FileCopyrightText: DPW FMS Contributors; SPDX-License-Identifier: MIT */
'use strict';
const assert=require('node:assert/strict');
const {absoluteTileTemplate}=require('../opentcs-web-ui/src/main/webapp/static/js/map-url.js');
assert.equal(absoluteTileTemplate('http://localhost:8080','','/api/map/tiles/{z}/{x}/{y}.pbf'),'http://localhost:8080/api/map/tiles/{z}/{x}/{y}.pbf');
assert.equal(absoluteTileTemplate('https://fleet.example','/dpw','/api/map/tiles/{z}/{x}/{y}.pbf'),'https://fleet.example/dpw/api/map/tiles/{z}/{x}/{y}.pbf');
assert.doesNotThrow(()=>new Request(absoluteTileTemplate('http://localhost:8080','','/api/map/tiles/14/10696/7017.pbf'.replace('14','{z}').replace('10696','{x}').replace('7017','{y}')).replace('{z}','14').replace('{x}','10696').replace('{y}','7017')));
assert.throws(()=>absoluteTileTemplate('http://localhost:8080','','https://tiles.example/{z}/{x}/{y}.pbf'));
assert.throws(()=>absoluteTileTemplate('not-an-origin','','/api/map/tiles/{z}/{x}/{y}.pbf'));
console.log('Map tile URL tests passed');
