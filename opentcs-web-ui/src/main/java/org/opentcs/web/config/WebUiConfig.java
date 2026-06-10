// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.config;

import java.util.OptionalDouble;

/**
 * Loads web frontend settings from system properties and environment variables.
 *
 * @author Zishan Khan
 */
public final class WebUiConfig {
  private WebUiConfig() {
  }

  /** Returns the kernel service web API base URL. */
  public static String kernelApiBaseUrl() {
    return value(
        "opentcs.web.kernelApiBaseUrl", "OPENTCS_WEB_KERNEL_API_BASE_URL",
        "http://localhost:55200/v1"
    );
  }

  /** Returns the optional service web API access key. */
  public static String accessKey() {
    return value("opentcs.web.accessKey", "OPENTCS_WEB_ACCESS_KEY", "");
  }

  /** Returns the selected route strategy for browser route previews. */
  public static String routingStrategy() {
    String value = value("routing.strategy", "ROUTING_STRATEGY", "DEFAULT").toUpperCase();
    return value.equals("ASTAR") ? value : "DEFAULT";
  }

  /** Returns map configuration. */
  public static MapConfig mapConfig() {
    return new MapConfig(
        MapProviderType.from(value("map.provider", "MAP_PROVIDER", "OSM")),
        value("google.maps.apiKey", "GOOGLE_MAPS_API_KEY", ""),
        optionalDouble("map.origin.latitude", "MAP_ORIGIN_LATITUDE"),
        optionalDouble("map.origin.longitude", "MAP_ORIGIN_LONGITUDE"),
        positiveDouble(value("map.scale.metersPerUnit", "MAP_SCALE_METERS_PER_UNIT", "1.0"), 1.0),
        finiteDouble(value("map.rotation.degrees", "MAP_ROTATION_DEGREES", "0.0"), 0.0),
        integer(value("map.default.zoom", "MAP_DEFAULT_ZOOM", "18"), 18)
    );
  }

  private static String value(String property, String environment, String fallback) {
    String configured = System.getProperty(property);
    if (configured == null || configured.isBlank()) {
      configured = System.getenv(environment);
    }
    return configured == null || configured.isBlank() ? fallback : configured.trim();
  }

  private static OptionalDouble optionalDouble(String property, String environment) {
    String configured = value(property, environment, "");
    if (configured.isBlank()) {
      return OptionalDouble.empty();
    }
    try {
      double result = Double.parseDouble(configured);
      return Double.isFinite(result) ? OptionalDouble.of(result) : OptionalDouble.empty();
    }
    catch (NumberFormatException exc) {
      return OptionalDouble.empty();
    }
  }

  private static double positiveDouble(String value, double fallback) {
    double parsed = finiteDouble(value, fallback);
    return parsed > 0 ? parsed : fallback;
  }

  private static double finiteDouble(String value, double fallback) {
    try {
      double parsed = Double.parseDouble(value);
      return Double.isFinite(parsed) ? parsed : fallback;
    }
    catch (NumberFormatException exc) {
      return fallback;
    }
  }

  private static int integer(String value, int fallback) {
    try {
      return Integer.parseInt(value);
    }
    catch (NumberFormatException exc) {
      return fallback;
    }
  }
}
