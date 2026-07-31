<!-- SPDX-FileCopyrightText: DPW FMS Contributors -->
<!-- SPDX-License-Identifier: CC-BY-4.0 -->
# RFC: DPW Fleet Management System web deployment

- **RFC ID:** DPW-FMS-001
- **Status:** Draft for stakeholder review
- **Authors:** DPW FMS engineering
- **Created:** 2026-07-30
- **Target release:** To be assigned
- **Reviewers:** Operations, OT security, fleet integration, platform engineering, data governance

## 1. Summary

This RFC proposes deploying the DPW FMS browser console as the primary operational map for an existing openTCS-compatible kernel. The console preserves the kernel contracts for plant topology, vehicle state, transport orders and routing while adding a provider-neutral map, an isolated Jebel Ali demonstration mode, reporting, Docker packaging and a local UAE reference-map fallback.

## 2. Motivation and goals

Operators need a map-first view rather than requiring the desktop Plant Overview for routine monitoring. The solution must:

1. Start at the live map and show kernel vehicles, locations, directed paths and calculated routes.
2. Preserve transport-order and vehicle command APIs.
3. Offer deterministic training/demo data without writing into a real kernel.
4. Support Google Maps online and a same-origin map when internet or Google authentication is unavailable.
5. Keep credentials server-side except for a referrer-restricted Google browser key.
6. Remain deployable as the existing kernel and Tomcat Compose services.

Non-goals for this RFC are autonomous vehicle safety control, street-level offline navigation, replacing the kernel router, and treating the compact UAE GeoJSON reference package as a certified navigation database.

## 3. Current architecture

The kernel exposes its service web API on port 55200. Tomcat serves the JSP application on port 8080 and its `KernelHttpClient` calls the kernel over the private Compose network. `ApiController` is the browser-facing façade. In production mode it combines the plant model, vehicle endpoint and transport-order endpoint into one map-state response. In demonstration mode it reads the isolated `MockFleetService` singleton.

```text
Browser -> Tomcat /api/fleet -----> Kernel service API
        -> Tomcat /api/routes ----> Kernel routing service
        -> /offline-map/* --------> WAR-local GeoJSON/style/license
        -> Google Maps JS --------> only when configured and selected
```

The desktop Plant Overview remains an engineering view and is not removed.

## 4. Proposed behavior

### 4.1 Map-first operation

`/` forwards to `/map-overview`. The brand link and first navigation item also open the live map. In kernel mode, `/api/fleet` adapts:

- Plant points and locations into selectable map nodes.
- Plant paths into blocked/available route segments.
- Vehicle positions into geographic markers using configured coordinate calibration.
- Transport orders into the common job collection.

The From/To controls call `/api/routes`. Kernel mode delegates to `WebRoutingService`, which respects directed and locked paths. Demo mode selects deterministic terminal route geometry. The chosen geometry is highlighted and `Fit active route` adjusts the provider viewport.

### 4.2 State ownership

`FMS_MOCK_DATA_ENABLED=false` is the production setting. No mock state is seeded, and kernel data remains authoritative. With the flag enabled, `MockFleetService` is authoritative for the training tenant. Stable IDs and one seed version prevent duplicates. Browser refreshes are read-only.

### 4.3 Map provider contract

Business code uses a shared provider contract for initialization, teardown, viewport, bounds, route display, marker display, selection and layer visibility. Google and offline adapters translate that state into provider objects. Google load failure, authentication callback, network error or timeout activates the offline adapter when enabled. Forced offline mode never constructs a Google URL.

### 4.4 Offline data scope

The bundled ODbL GeoJSON is a compact UAE context/reference layer, with all seven emirates, selected ports/cities and principal logistics corridors. It is not sufficiently detailed for street routing. Terminal operations, asset positions and route geometry are separate application overlays. A later RFC may replace it with MapLibre and a versioned UAE PMTiles artifact if street-level offline detail is approved and an artifact-distribution process is available.

## 5. Interfaces

| Endpoint | Mode | Purpose |
|---|---|---|
| `GET /api/fleet` | Both | Common map state containing assets, jobs, paths and routing nodes |
| `GET /api/routes?source=&destination=` | Both | Calculated/highlightable route |
| `GET /api/status` | Both | Kernel or demo connectivity/KPIs |
| `GET /api/map/config` | Both | Safe browser map settings |
| `GET /api/map/diagnostics` | Both | Non-secret offline/provider diagnostics |
| `GET /api/vehicles` | Both | Existing vehicle table contract |
| `GET /api/transport-orders` | Both | Existing order table contract |

No endpoint returns the kernel access key. The browser receives only the Google key intended for browser use.

## 6. Configuration and deployment

The reviewed deployment must explicitly set `FMS_MOCK_DATA_ENABLED=false` for production and must set map calibration to the real plant coordinate origin, scale and rotation. Docker Compose continues exposing only 8080 and the intentionally public kernel API port 55200. A production ingress should expose Tomcat, restrict kernel access to trusted networks and terminate TLS.

Required release checks:

1. `./gradlew clean build`
2. `python3 scripts/validate-offline-map.py`
3. `docker compose config`
4. `docker compose build`
5. Start the stack and verify `/api/status`, `/api/fleet`, `/api/routes` and `/api/map/diagnostics`.
6. Verify a real vehicle marker follows kernel position updates.
7. Verify a locked path is not used by a calculated route.
8. Disconnect external networking and repeat forced-offline smoke tests.

## 7. Security and safety

- Restrict the Google browser key by API and HTTPS referrer.
- Never place kernel access keys in browser configuration.
- Put authentication/authorization at the existing identity proxy or servlet container before production rollout.
- Treat the UI as an operational aid, not the safety PLC or emergency-stop channel.
- Sanitize popup fields; current popup construction uses text nodes.
- Restrict direct port 55200 access in production.
- Retain audit logging for commands at the kernel/integration layer.
- Validate map assets and checksums during image construction.

## 8. Availability and fallback

The kernel remains usable if the external basemap fails. In automatic mode the client attempts Google once, waits at most eight seconds and falls back without a retry loop. Provider changes preserve selected assets, enabled layers and viewport where supported. If both Google and offline mapping are disabled, the UI displays an explicit error rather than an empty panel.

## 9. Observability

Platform monitoring should check Tomcat `/dashboard`, kernel `/v1/kernel/version`, and the safe diagnostics endpoint. Logs must record provider failures without including the Google key. Recommended metrics include kernel request latency/error rate, map fallback count, vehicle freshness, order failures and disconnected vehicles.

## 10. Data migration and rollback

There is no production data migration. Mock data is in-memory and isolated. Rollback consists of deploying the previous WAR/image; kernel state and the `kernel-data` volume are unchanged. The desktop Plant Overview remains available throughout rollout.

## 11. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Incorrect coordinate calibration | Site acceptance test with surveyed reference points; document origin/scale/rotation |
| Browser Google key abuse | API/referrer restrictions, quotas and monitoring |
| Offline reference map mistaken for navigation data | Persistent attribution and explicit scope messaging |
| Kernel/API schema drift | Contract tests against the target kernel version |
| UI freshness mistaken for vehicle truth | Display last-update/connectivity state; kernel remains authoritative |
| Public kernel exposure | Firewall/ingress restriction and access key configuration |

## 12. Rollout plan

1. Engineering integration with mock mode.
2. Staging against a non-production kernel and representative model.
3. OT security review and penetration test.
4. Operator usability review at Jebel Ali.
5. Shadow operation alongside Plant Overview.
6. Controlled map-first rollout with rollback image retained.
7. Post-rollout review after two weeks.

## 13. Acceptance criteria

- Root URL opens the live map.
- A production kernel model renders without enabling mock mode.
- Live vehicles and orders update from kernel endpoints.
- Source/destination routing calls the kernel-compatible router and highlights the route.
- Locked/directed path behavior is retained.
- Demo mode remains deterministic and isolated.
- Google failure produces a usable offline view.
- Forced offline mode produces no external map requests.
- Existing dashboard, vehicle, order, reports, control center and engineering views remain reachable.
- Docker build, tests, health checks and offline-asset validation pass in CI.

## 14. Open questions requiring approval

1. Which identity provider and roles protect operations and reporting?
2. Should port 55200 remain host-exposed in production?
3. What surveyed coordinate calibration applies to each terminal model?
4. Is street-level UAE offline detail required, and if so where will a PMTiles artifact be stored and updated?
5. Which operational actions require dual authorization and audit retention?
6. What browser/device matrix is supported in control rooms?

## 15. Decision record

Approval requires named sign-off from Operations, OT Security and Platform Engineering. Record decisions, conditions and follow-up RFCs below.

| Reviewer | Decision | Date | Conditions |
|---|---|---|---|
| Operations | Pending | — | — |
| OT Security | Pending | — | — |
| Platform Engineering | Pending | — | — |
