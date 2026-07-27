<!-- SPDX-FileCopyrightText: Zishan Khan -->
<!-- SPDX-License-Identifier: MIT -->
# openTCS JSP Web UI

Developer: Zishan Khan

This additional frontend keeps the existing desktop Plant Overview, Model Editor, Operations Desk and Kernel Control Center unchanged.

## Features

- Dashboard and safe Control Center status view
- Plant Overview rendered as browser SVG
- Vehicle monitoring and supported pause/resume/withdraw actions
- Transport order monitoring and creation
- AJAX live updates every two seconds
- Route previews with `DEFAULT` (Dijkstra behavior) or optional `ASTAR`
- OpenStreetMap/Leaflet map view and optional Google Maps view
- Configurable conversion of local plant coordinates to latitude/longitude

## Build and run

Requires Java 21.

### Docker Compose (recommended)

Docker Engine with Compose v2 is the only prerequisite. From the repository root, run:

```shell
docker compose up --build
```

Open <http://localhost:8080> once the containers have started. The compose stack builds and runs
both the headless openTCS kernel and the Tomcat-hosted web UI. The kernel API is also available on
<http://localhost:55200/v1>. Stop the stack with `docker compose down`, or remove its persisted
kernel data too with `docker compose down -v`.

Runtime settings can be supplied before the command, for example:

```shell
MAP_ORIGIN_LATITUDE=50.0 MAP_ORIGIN_LONGITUDE=8.0 ROUTING_STRATEGY=ASTAR \
  docker compose up --build
```

To follow startup logs, use `docker compose logs -f`. The first build downloads Gradle and Maven
dependencies and will take longer than later cached builds.

### Manual development build

```shell
./gradlew :opentcs-web-ui:war
```

Start the openTCS kernel (its service web API defaults to port 55200), then deploy
`opentcs-web-ui/build/libs/opentcs-web-ui-*.war` to a Jakarta Servlet 6/JSP container such as
Tomcat 11. Configuration can be supplied as JVM system properties or equivalent upper-case
environment variables documented below.

## Kernel bridge

```properties
opentcs.web.kernelApiBaseUrl=http://localhost:55200/v1
opentcs.web.accessKey=
```

The WAR calls the existing service web API for plant models, vehicles, transport orders, version
status and supported commands. It does not expose kernel shutdown.

## Routing configuration

Kernel routing uses the existing openTCS shortest-path selector:

```properties
defaultrouter.shortestpath.algorithm = DIJKSTRA
# or
defaultrouter.shortestpath.algorithm = ASTAR
```

Browser route previews use:

```properties
routing.strategy=DEFAULT
# or
routing.strategy=ASTAR
```

## Map configuration

OpenStreetMap (default, no API key):

```properties
map.provider=OSM
google.maps.apiKey=
map.origin.latitude=50.000000
map.origin.longitude=8.000000
map.scale.metersPerUnit=0.001
map.rotation.degrees=0.0
map.default.zoom=18
```

Google Maps (key supplied at runtime; never commit it):

```properties
map.provider=GOOGLE
google.maps.apiKey=${GOOGLE_MAPS_API_KEY}
map.origin.latitude=50.000000
map.origin.longitude=8.000000
map.scale.metersPerUnit=0.001
map.rotation.degrees=0.0
map.default.zoom=18
```

openTCS physical point/location coordinates are local millimetres. Choose the scale to match the
model units (typically `0.001` metres per millimetre), then calibrate origin and rotation. If the
origin is missing, the SVG view remains available and the map displays a safe configuration message.
Google Maps without a key automatically falls back to OpenStreetMap. OSM attribution remains visible.
