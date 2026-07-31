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

The minimal renderer selected for this JSP application is an application-owned local SVG/GeoJSON adapter; the package format is GeoJSON rather than PMTiles, avoiding a tile server and byte-range/CORS complexity. It is served from the same WAR under `/offline-map/`. All JavaScript, styling, operational markers, route geometry, attribution and map data needed by this mode are local.

The bundled compact OpenStreetMap-derived UAE **reference** extract (ODbL 1.0) covers the country outline, all seven emirate labels, Abu Dhabi, Dubai, Al Ain, Jebel Ali, Khalifa Port/KEZAD, Hamriyah and Fujairah ports, and key E11/E611/E44 corridors. It supports useful UAE context at zoom 5–15 and high-detail application overlays in the Jebel Ali demonstration extent. It deliberately does **not** claim street-level buildings, navigation, glyphs or turn-by-turn routing. The current archive is under 10 KiB, so image-size impact is negligible. Attribution is always visible.

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
* **CORS/range errors:** the GeoJSON architecture is same-origin and does not require range requests. A reverse proxy must preserve `/offline-map/*` and `/api/*` paths.
* **Incorrect bounds:** validate `FMS_MAP_DEFAULT_LAT/LNG`; demonstration overlays should remain near Jebel Ali.
* **Docker volume problems:** inspect only `docker volume inspect dpfms_kernel-data`; do not remove unrelated volumes. Mock records are not stored in that volume.
* **Disconnected production dashboard:** confirm the kernel is reachable at the configured internal URL and that access keys match.

## Live kernel map and RFC

The live map is the default landing page. With `FMS_MOCK_DATA_ENABLED=false`, `/api/fleet` combines the kernel plant model, vehicles and transport orders into the same map contract used by the demonstration. Configure `MAP_ORIGIN_LATITUDE`, `MAP_ORIGIN_LONGITUDE`, `MAP_SCALE_METERS_PER_UNIT` and `MAP_ROTATION_DEGREES` for the actual plant. Map source/destination controls call the existing directed, lock-aware routing service; Plant Overview remains available as the engineering view.

The architecture, security review items, rollout plan, acceptance criteria and decision template are documented in [`docs/RFC-DPW-FMS.md`](../docs/RFC-DPW-FMS.md).
