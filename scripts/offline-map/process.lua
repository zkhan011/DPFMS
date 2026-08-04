-- SPDX-FileCopyrightText: DPW FMS Contributors
-- SPDX-License-Identifier: MIT
function node_function(node)
  if node:Find("place")~="" or node:Find("amenity")~="" then node:Layer("place",false); node:Attribute("name",node:Find("name")); node:Attribute("kind",node:Find("place")) end
end
function way_function(way)
  local highway=way:Find("highway")
  if highway~="" then way:Layer("transportation",false); way:Attribute("name",way:Find("name")); way:Attribute("kind",highway) end
  if way:Find("building")~="" then way:Layer("building",true); way:Attribute("name",way:Find("name")) end
  if way:Find("landuse")~="" or way:Find("natural")=="sand" then way:Layer("landuse",true); way:Attribute("kind",way:Find("landuse")); way:Attribute("name",way:Find("name")) end
  if way:Find("natural")=="water" or way:Find("waterway")~="" then way:Layer("water",way:IsClosed()); way:Attribute("name",way:Find("name")) end
  if way:Find("boundary")~="" then way:Layer("boundary",false); way:Attribute("name",way:Find("name")) end
end
function relation_function(relation)
  if relation:Find("boundary")~="" then relation:Layer("boundary",true); relation:Attribute("name",relation:Find("name")) end
  if relation:Find("natural")=="water" then relation:Layer("water",true); relation:Attribute("name",relation:Find("name")) end
end
