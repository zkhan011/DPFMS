// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.config;

/** Validated DPW FMS demonstration and map configuration. */
public record FmsConfig(
    boolean mockEnabled, boolean kernelModelSyncEnabled, int updateIntervalMs, String provider,
    boolean offlineEnabled,
    double latitude, double longitude, int zoom, int offlineMinZoom, int offlineMaxZoom,
    String offlineStyleUrl, String googleApiKey, String mbtilesPath, String tileEndpoint,
    String tileServiceUrl
) {
  public static FmsConfig load() {
    String provider = env("FMS_MAP_PROVIDER", "auto").toLowerCase();
    if (!java.util.Set.of("auto", "google", "offline").contains(provider)) provider = "auto";
    return new FmsConfig(
        bool("FMS_MOCK_DATA_ENABLED", false), bool("FMS_KERNEL_MODEL_SYNC_ENABLED", false), integer(
            "FMS_MOCK_UPDATE_INTERVAL_MS", 3000, 500, 60000
        ),
        provider, bool("FMS_OFFLINE_MAP_ENABLED", true), decimal(
            "FMS_MAP_DEFAULT_LAT", 24.9857, -90, 90
        ),
        decimal("FMS_MAP_DEFAULT_LNG", 55.0273, -180, 180), integer(
            "FMS_MAP_DEFAULT_ZOOM", 14, 5, 20
        ),
        integer("FMS_OFFLINE_MIN_ZOOM", 5, 0, 15), integer("FMS_OFFLINE_MAX_ZOOM", 16, 5, 20),
        localPath(env("FMS_OFFLINE_STYLE_URL", "/offline-map/style/style.json")),
        env("GOOGLE_MAPS_API_KEY", ""),
        env("FMS_OFFLINE_MBTILES_PATH", "deployment/maps/jebel-ali.mbtiles"),
        localPath(env("FMS_OFFLINE_TILE_ENDPOINT", "/api/map/tiles/{z}/{x}/{y}.pbf")),
        internalUrl(env("FMS_OFFLINE_TILE_SERVICE_URL", "http://tiles:8090"))
    );
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private static boolean bool(String name, boolean fallback) {
    String value = env(name, String.valueOf(fallback));
    return value.equalsIgnoreCase("true") ? true : value.equalsIgnoreCase("false") ? false
        : fallback;
  }

  private static int integer(String name, int fallback, int min, int max) {
    try {
      int value = Integer.parseInt(env(name, ""));
      return value >= min && value <= max ? value : fallback;
    }
    catch (NumberFormatException exc) {
      return fallback;
    }
  }

  private static double decimal(String name, double fallback, double min, double max) {
    try {
      double value = Double.parseDouble(env(name, ""));
      return Double.isFinite(value) && value >= min && value <= max ? value : fallback;
    }
    catch (NumberFormatException exc) {
      return fallback;
    }
  }

  private static String internalUrl(String value) {
    return value.matches("https?://[a-zA-Z0-9.-]+(:[0-9]+)?") ? value : "http://tiles:8090";
  }

  private static String localPath(String value) {
    return value.startsWith("/") && !value.contains("://") ? value
        : "/offline-map/style/style.json";
  }
}
