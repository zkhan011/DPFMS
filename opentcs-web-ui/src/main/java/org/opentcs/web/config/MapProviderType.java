// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.config;

/**
 * Supported browser map providers.
 *
 * @author Zishan Khan
 */
public enum MapProviderType {
  OSM,
  GOOGLE;

  /**
   * Parses a value, falling back to OSM for invalid input.
   *
   * @param value The configured value.
   * @return The selected provider.
   */
  public static MapProviderType from(String value) {
    try {
      return value == null ? OSM : valueOf(value.trim().toUpperCase());
    }
    catch (IllegalArgumentException exc) {
      return OSM;
    }
  }
}
