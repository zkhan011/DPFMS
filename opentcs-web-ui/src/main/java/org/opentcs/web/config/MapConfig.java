// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.config;

import java.util.OptionalDouble;

/**
 * Immutable map configuration for the web frontend.
 *
 * @author Zishan Khan
 */
public record MapConfig(
    MapProviderType requestedProvider,
    String googleApiKey,
    OptionalDouble originLatitude,
    OptionalDouble originLongitude,
    double metersPerUnit,
    double rotationDegrees,
    int defaultZoom
) {
  /** Returns the effective provider, including the safe Google fallback. */
  public MapProviderType effectiveProvider() {
    return requestedProvider == MapProviderType.GOOGLE && googleApiKey.isBlank()
        ? MapProviderType.OSM
        : requestedProvider;
  }

  /** Returns whether local coordinates can be calibrated. */
  public boolean calibrated() {
    return originLatitude.isPresent() && originLongitude.isPresent();
  }

  /** Returns a safe user-facing configuration warning. */
  public String warning() {
    if (requestedProvider == MapProviderType.GOOGLE && googleApiKey.isBlank()) {
      return "Google Maps API key is not configured. Falling back to OpenStreetMap.";
    }
    if (!calibrated()) {
      return "Map origin is not configured. Please configure map.origin.latitude and map.origin.longitude.";
    }
    return "";
  }
}
