<!-- SPDX-FileCopyrightText: DPW FMS Contributors -->
<!-- SPDX-License-Identifier: MIT -->
# DPW FMS fleet operations demonstration

DPW FMS is a production-compatible JSP fleet console backed by the existing kernel integration. In production mode it keeps using the kernel service API. Optional demonstration mode supplies one deterministic Jebel Ali terminal state containing 20 vehicles, 32 jobs, 19 operational locations, five charging stations, four fuel stations, 18 alerts, routes and seven days of reporting history.

## Run

Docker Engine with Compose v2 is the only runtime prerequisite:

```bash
cp .env.example .env
docker compose build
docker compose up -d
docker compose ps
docker compose logs -f
```

Open <http://localhost:8080>. The kernel API remains at <http://localhost:55200/v1>. Stop with `docker compose down`. The `kernel-data` volume contains kernel data; do not delete it in production. Mock state is deterministic in-memory demonstration state and is reset by restarting only `web` (`docker compose restart web`), without deleting any real or kernel records.

A manual Java 21 build is available with `./gradlew :opentcs-web-ui:war`.

## Configuration

| Variable | Default | Meaning |
|---|---:|---|
| `FMS_MOCK_DATA_ENABLED` | `true` in Compose | Enable the isolated demonstration repository and simulation |
| `FMS_MOCK_UPDATE_INTERVAL_MS` | `3000` | Browser refresh interval (validated to 500–60000 ms) |
| `FMS_MAP_PROVIDER` | `auto` | `auto`, `google`, or `offline` |
| `FMS_OFFLINE_MAP_ENABLED` | `true` | Permit the local UAE fallback |
| `GOOGLE_MAPS_API_KEY` | empty | Restricted browser key; never logged by DPW FMS |
| `FMS_MAP_DEFAULT_LAT/LNG` | `24.9857/55.0273` | Jebel Ali centre |
| `FMS_MAP_DEFAULT_ZOOM` | `14` | Initial terminal zoom |
| `FMS_OFFLINE_STYLE_URL` | `/offline-map/style/style.json` | Same-origin local style |
| `FMS_OFFLINE_MIN_ZOOM/MAX_ZOOM` | `5/15` | Advertised reference-map range |
| `OPENTCS_WEB_KERNEL_API_BASE_URL` | `http://kernel:55200/v1` | Internal production kernel bridge |

Invalid providers, coordinates, zooms, paths and intervals fall back to documented safe defaults. `GET /api/map/diagnostics` reports enablement, local asset health, zooms and whether a Google key is configured, but never returns the key.

### Provider modes

* **Automatic:** leave `FMS_MAP_PROVIDER=auto`. DPW FMS tries Google only when a key exists and switches to the local renderer on a missing key, script error, authentication callback, initialization error or eight-second timeout.
* **Google preferred:** set `FMS_MAP_PROVIDER=google`, `FMS_OFFLINE_MAP_ENABLED=true`, and a restricted browser key. Enable the **Maps JavaScript API** in Google Cloud. Restrict the key to that API and HTTP referrers such as `https://fleet.example.com/*` (or `http://localhost:8080/*` for development). Google content is neither cached nor redistributed.
* **Fully offline:** set `FMS_MAP_PROVIDER=offline`, `FMS_OFFLINE_MAP_ENABLED=true`, and leave `GOOGLE_MAPS_API_KEY=` empty; then run `docker compose down && docker compose up -d`. Forced offline mode never creates a Google script element or requests a public tile/CDN service.

The provider switch preserves shared selected entity, filters and layer state; viewport preservation is supported by the Google adapter. A non-blocking indicator reports the active provider.

## Mock architecture and simulation

`MockFleetService` is the single typed backend state owner. Stable keys (`DPW-001`, `JOB-001`, etc.) and seed version `dpw-jebel-ali-v1` make initialization idempotent: the singleton seeds exactly once per web process, browser refreshes only read snapshots, and nothing is inserted into the kernel or a database. Disabling `FMS_MOCK_DATA_ENABLED` makes `/api/fleet` unavailable and leaves all original kernel-backed APIs unchanged.

Moving vehicles interpolate deterministically along shared multi-segment route geometry. Each snapshot updates position, heading, route progress and timestamp. Charging, refuelling, maintenance, offline, idle and waiting vehicles remain stationary. The same `/api/fleet` snapshot drives KPIs, dashboard alerts, map overlays, search, tables and reports, preventing independently hard-coded UI values. Timers and provider resources are released on `pagehide`.

Reports at `/reports` provide fleet utilization, SLA, duration, alert and job performance with vehicle/status/type filters and CSV export. In a production deployment, protect this servlet using the existing container identity proxy or Tomcat role policy; the demonstration image intentionally has no bundled credentials.

## Offline UAE renderer and data

The offline renderer is MapLibre GL JS bundled as a pinned WebJar. A private, read-only tile service serves the cropped vector MBTiles archive through Tomcat same-origin endpoints; operational markers and route geometry remain separate application overlays. No renderer, font, tile or style CDN is needed at runtime.

The runtime MBTiles package is strictly cropped to the Jebel Ali operating extent and licensed under ODbL 1.0. The checked-in bootstrap is 84 KiB at zooms 12–16; the production refresh pipeline adds current roads, buildings, land use, water, boundaries and named places from the configured OSM extract. It is a visualization basemap, not turn-by-turn navigation data. Attribution is always visible.

To update or replace it, generate a legally redistributable GeoJSON extract, replace `offline-map/data/uae.geojson`, update the attribution/date, and run:

```bash
(cd opentcs-web-ui/src/main/webapp/offline-map/data && sha256sum uae.geojson > uae.geojson.sha256)
python3 scripts/validate-offline-map.py
```

Docker verifies non-empty data/style/license files, checksum, and forbidden remote references and fails clearly when validation fails. OpenStreetMap public tiles are not used. License notices are packaged in `offline-map/licenses`.

## Offline verification

Build once while dependencies are available, start the stack, then disconnect the host and restart the existing images with the forced-offline variables above. Refresh `http://localhost:8080`, exercise map markers, layers, local search, routes, reports and movement, and inspect the browser Network panel. All required runtime requests should target `localhost:8080`; the kernel-to-web bridge remains on the private Compose network.

## Troubleshooting

* **Blank Google map:** verify Maps JavaScript API enablement, billing, referrer/API restrictions and browser console; automatic mode displays the offline map instead.
* **Missing offline map/style:** run the validator, check the checksum, and confirm the assets exist inside `ROOT.war` under `offline-map/`.
* **Missing labels/details:** the compact reference package intentionally has no sprite/glyph layer or street-level tile detail; operational labels and icons are application overlays.
* **CORS/tile errors:** preserve `/api/map/tiles/*` and `/api/map/metadata` through the reverse proxy, check the internal `tiles` service health and confirm that `Content-Encoding: gzip` is preserved.
* **`Request constructor ... is not a valid URL`:** use the current build, which resolves the application-relative tile template to an absolute same-origin URL before handing it to the MapLibre worker. Reverse proxies must forward the original HTTP(S) host/protocol and application context path.
* **Incorrect bounds:** validate `FMS_MAP_DEFAULT_LAT/LNG`; demonstration overlays should remain near Jebel Ali.
* **Docker volume problems:** inspect only `docker volume inspect dpfms_kernel-data`; do not remove unrelated volumes. Mock records are not stored in that volume.
* **Disconnected production dashboard:** confirm the kernel is reachable at the configured internal URL and that access keys match.

## Live kernel map and RFC

The live map is the default landing page. With `FMS_MOCK_DATA_ENABLED=false`, `/api/fleet` combines the kernel plant model, vehicles and transport orders into the same map contract used by the demonstration. Configure `MAP_ORIGIN_LATITUDE`, `MAP_ORIGIN_LONGITUDE`, `MAP_SCALE_METERS_PER_UNIT` and `MAP_ROTATION_DEGREES` for the actual plant. Map source/destination controls call the existing directed, lock-aware routing service; Plant Overview remains available as the engineering view.

The architecture, security review items, rollout plan, acceptance criteria and decision template are documented in [`docs/RFC-DPW-FMS.md`](../docs/RFC-DPW-FMS.md).

## Jebel Ali offline MBTiles

The configured operating extent is `55.012,24.970,55.044,25.000` (west,south,east,north), centered on `24.9857,55.0273`. The checked-in text artifact `deployment/maps/jebel-ali.mbtiles.base64` materializes to an 84 KiB vector MBTiles bootstrap covering only that 3.2 km × 3.3 km extent at zooms 12–16. Docker decodes and checksum-verifies it before copying `/opt/dpw-fms/maps/jebel-ali.mbtiles`; it is never fetched at runtime.

The Tomcat endpoint `GET /api/map/tiles/{z}/{x}/{y}.pbf` reads this database in read-only mode and converts XYZ rows to TMS with `tileRow = (1 << z) - 1 - y`. `GET /api/map/metadata` supplies bounds, center, zoom range, PBF format and attribution to the locally bundled MapLibre GL JS renderer. Fleet overlays remain separate from the base map.

To refresh from current OpenStreetMap data, run:

```bash
FMS_MAP_BBOX=55.012,24.970,55.044,25.000 \
FMS_OSM_SOURCE_URL=https://download.geofabrik.de/asia/gcc-states-latest.osm.pbf \
./scripts/download-offline-map.sh
```

The refresh process downloads an approved Geofabrik OpenStreetMap PBF only during generation, invokes the pinned `tilemaker/tilemaker:3.0.0` image, strictly clips output to the configured bounds, includes transportation, building, land-use, water, boundary and place layers, validates SQLite integrity/metadata/tiles, and deletes the temporary broad source. It refuses malformed coordinates and unexpectedly broad bounding boxes. Commit only the cropped MBTiles result, never the temporary regional source.

Validate an existing package with:

```bash
python3 scripts/validate-offline-map.py \
  --mbtiles deployment/maps/jebel-ali.mbtiles \
  --bounds 55.012,24.970,55.044,25.000
```

The current execution environment denied all external downloads with HTTP 403, so the committed 84 KiB archive was reproducibly generated from the repository's existing attributed OSM-derived reference GeoJSON using `scripts/build-bootstrap-mbtiles.py`. Run the refresh command in an internet-enabled build environment before production acceptance to populate full current road/building/land-use detail. The validator and Docker build reject missing, empty, corrupt, incorrectly bounded, tile-less or unattributed archives.

## Kernel routing-model persistence

The MBTiles archive is a visual basemap only; the kernel never routes directly on map pixels or OSM road geometry. For routing and dispatching, DPW FMS packages a separate validated plant model at `src/main/resources/kernel-model/dpw-fms-plant-model.json`. It contains 36 halt points, 70 bidirectional paths, 19 linked operational locations with the `MOVE` operation and 20 stable vehicle definitions. Every point also carries latitude/longitude properties so the kernel topology and MapLibre overlays share the same coordinates.

When both `FMS_MOCK_DATA_ENABLED=true` and `FMS_KERNEL_MODEL_SYNC_ENABLED=true`, the web application waits for the kernel and idempotently installs model `DPW-FMS-JEBEL-ALI-V1` only when the kernel is empty/unnamed. It never overwrites a non-empty user model. The kernel is configured to save the operating model on shutdown into its persisted `kernel-data` volume, so Compose restarts retain the routing topology. Production deployments should set both flags to `false` and manage their reviewed plant model through the normal engineering workflow.

The topology source is JSON text (not a binary artifact) and can be regenerated with `python3 scripts/generate-kernel-model.py`. The offline MBTiles is also represented in Git as text-only `jebel-ali.mbtiles.base64` to avoid binary-diff/upload errors; Docker decodes it and verifies `jebel-ali.mbtiles.sha256` before packaging. For local tooling, run `./scripts/materialize-offline-map.sh`.

## MQTT and RabbitMQ telematics

Compose now starts a local RabbitMQ broker with the MQTT and Web MQTT plugins enabled. The management UI is available at <http://localhost:15672> with the development credentials from `.env` (`dpwfms` / `dpwfms` by default), and browser/bridge MQTT-over-WebSocket is available on `ws://localhost:15675/ws`.

DPW FMS exposes a small ingestion endpoint for bridge processes and device simulators:

```bash
curl -X POST http://localhost:8080/api/telematics \
  -H 'Content-Type: application/json' \
  -d '{"vehicleId":"DPW-001","source":"mqtt","latitude":24.9857,"longitude":55.0273,"speed":18,"heading":92,"battery":76,"status":"Moving"}'
```

The map page polls `GET /api/telematics` and displays the latest message per vehicle in the **Live telematics** panel. A production MQTT/RabbitMQ bridge should subscribe to `FMS_TELEMATICS_MQTT_TOPIC` or bind `FMS_TELEMATICS_RABBITMQ_EXCHANGE`, normalize messages to the JSON fields above, and forward them to `/api/telematics`. This keeps broker credentials out of the browser and lets the DPW FMS UI work with MQTT devices, RabbitMQ exchange consumers or an existing telematics gateway.

### Clean startup checklist

1. Copy configuration: `cp .env.example .env`.
2. Build and validate map assets: `docker compose build`.
3. Start all services: `docker compose up -d`.
4. Confirm containers: `docker compose ps`.
5. Follow startup logs: `docker compose logs -f kernel tiles web rabbitmq`.
6. Open the map-first UI: <http://localhost:8080/map-overview>.
7. If the basemap is blank, check <http://localhost:8080/api/map/metadata>, `docker compose logs tiles`, and browser Network entries for `/api/map/tiles/{z}/{x}/{y}.pbf`. The UI also loads the bundled same-origin GeoJSON reference layer so fleet markers and terminal context remain visible even when a vector tile is missing or delayed.
8. Post the sample telematics `curl` above and verify the vehicle appears in the Live telematics panel.
