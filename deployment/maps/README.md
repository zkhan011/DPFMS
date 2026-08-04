<!-- SPDX-FileCopyrightText: DPW FMS Contributors -->
<!-- SPDX-License-Identifier: CC-BY-4.0 -->
# Jebel Ali vector map package

`jebel-ali.mbtiles` is the deployable, cropped vector archive for bounds `55.012,24.970,55.044,25.000`, zooms 12–16. It is 86,016 bytes and contains 75 tiles. The adjacent SHA-256 and license sidecars travel with it.

The checked-in archive is a bootstrap generated from the repository's previously attributed OSM-derived GeoJSON because the execution network returned HTTP 403 for all approved extract endpoints. Before production release, run `../../scripts/download-offline-map.sh` from the repository root in an internet-enabled environment. That process replaces this archive with current Geofabrik/OSM content, including roads, buildings, land use, water, boundaries and named places, but retains the strict operating bounds.

Do not commit `source/operating-area-source.osm.pbf`; it is a temporary regional source removed by the generation script.

## Binary-safe source control

The Git artifact is `jebel-ali.mbtiles.base64`, not the SQLite binary, because some review/upload systems reject binary patches. `scripts/materialize-offline-map.sh` decodes and checksum-verifies it locally. Docker performs the same decode in the map-validation stage and copies only the verified binary into runtime images. Map refresh regenerates both the Base64 text and checksum.
