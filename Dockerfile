# SPDX-FileCopyrightText: Zishan Khan
# SPDX-License-Identifier: MIT

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /source
COPY . .
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

FROM tomcat:11-jre21-temurin AS web
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=builder /source/opentcs-web-ui/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl --fail --silent http://localhost:8080/dashboard >/dev/null || exit 1
