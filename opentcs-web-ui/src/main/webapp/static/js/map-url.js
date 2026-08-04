/* SPDX-FileCopyrightText: DPW FMS Contributors; SPDX-License-Identifier: MIT */
'use strict';
(function(root,factory){
  const api=factory();
  if(typeof module==='object'&&module.exports)module.exports=api;
  else root.FmsMapUrls=api;
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  function absoluteTileTemplate(origin,contextPath,endpoint){
    if(!/^https?:\/\/[^/]+(?::\d+)?$/i.test(origin))throw new Error('A valid HTTP origin is required.');
    const context=(contextPath||'').replace(/^\/+|\/+$/g,'');
    const path=String(endpoint||'').replace(/^\/+/, '');
    if(!path||path.includes('://')||!path.includes('{z}')||!path.includes('{x}')||!path.includes('{y}'))throw new Error('A local XYZ tile endpoint is required.');
    return `${origin}/${context?context+'/':''}${path}`;
  }
  return {absoluteTileTemplate};
});
