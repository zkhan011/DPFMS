# SPDX-FileCopyrightText: Zishan Khan
# SPDX-License-Identifier: MIT

FROM python:3.13.5-alpine AS map-validator
WORKDIR /source
COPY deployment/maps/jebel-ali.mbtiles.base64 deployment/maps/jebel-ali.mbtiles.base64
COPY deployment/maps/jebel-ali.mbtiles.sha256 deployment/maps/jebel-ali.mbtiles.sha256
COPY opentcs-web-ui/src/main/webapp/offline-map/ opentcs-web-ui/src/main/webapp/offline-map/
COPY scripts/validate-offline-map.py scripts/validate-offline-map.py
RUN cd deployment/maps && base64 -d jebel-ali.mbtiles.base64 > jebel-ali.mbtiles \
    && sha256sum -c jebel-ali.mbtiles.sha256 && cd /source \
    && python3 scripts/validate-offline-map.py && touch /map-assets-validated

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /source
COPY . .
COPY --from=map-validator /map-assets-validated /map-assets-validated
COPY --from=map-validator /source/deployment/maps/jebel-ali.mbtiles /source/deployment/maps/jebel-ali.mbtiles
RUN test -s opentcs-web-ui/src/main/webapp/offline-map/data/uae.geojson \
    && test -s opentcs-web-ui/src/main/webapp/offline-map/style/style.json \
    && test -s opentcs-web-ui/src/main/webapp/offline-map/licenses/ATTRIBUTION.txt \
    && cd opentcs-web-ui/src/main/webapp/offline-map/data \
    && sha256sum -c uae.geojson.sha256 \
    && cd /source \
    && ! grep -ERi "maps.googleapis.com|tile.googleapis.com|fonts.googleapis.com|api.mapbox.com|tile.openstreetmap.org|carto.com|arcgis.com|bing.com|https?://" opentcs-web-ui/src/main/webapp/offline-map/data opentcs-web-ui/src/main/webapp/offline-map/style \
    && ./gradlew --no-daemon :opentcs-kernel:installDist :opentcs-web-ui:war

FROM eclipse-temurin:21-jre AS kernel
WORKDIR /opt/opentcs
COPY --from=builder /source/opentcs-kernel/build/install/opentcs-kernel/ ./
EXPOSE 1099 55000-55002 55200
ENTRYPOINT ["sh", "./startKernel.sh"]

FROM python:3.13.5-alpine AS tiles
WORKDIR /opt/dpw-fms
COPY --from=builder /source/deployment/maps/jebel-ali.mbtiles /maps/jebel-ali.mbtiles
COPY --from=builder /source/scripts/mbtiles_server.py /opt/dpw-fms/mbtiles_server.py
ENV FMS_OFFLINE_MBTILES_PATH=/maps/jebel-ali.mbtiles PORT=8090
EXPOSE 8090
HEALTHCHECK --interval=10s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O - http://localhost:8090/health >/dev/null || exit 1
ENTRYPOINT ["python3", "/opt/dpw-fms/mbtiles_server.py"]

FROM tomcat:11-jre21-temurin AS web
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=builder /source/opentcs-web-ui/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
COPY --from=builder /source/deployment/maps/jebel-ali.mbtiles /opt/dpw-fms/maps/jebel-ali.mbtiles
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://localhost:8080/dashboard >/dev/null || exit 1
