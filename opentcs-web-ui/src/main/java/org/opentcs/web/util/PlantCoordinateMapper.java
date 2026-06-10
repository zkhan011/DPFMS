// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.util;

import java.util.Optional;
import org.opentcs.web.config.MapConfig;
import org.opentcs.web.dto.WebDtos.CoordinateDto;

/**
 * Converts local plant coordinates to geographic coordinates.
 *
 * @author Zishan Khan
 */
public class PlantCoordinateMapper {
  private static final double METERS_PER_DEGREE = 111_320.0;
  private final MapConfig config;

  public PlantCoordinateMapper(MapConfig config) {
    this.config = config;
  }

  /** Uses direct geographic coordinates when present, otherwise maps local coordinates. */
  public Optional<CoordinateDto> map(double x, double y, Double latitude, Double longitude) {
    if (latitude != null && longitude != null && Double.isFinite(latitude)
        && Double.isFinite(longitude) && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180) {
      return Optional.of(new CoordinateDto(latitude, longitude));
    }
    return map(x, y);
  }

  /** Maps local coordinates, returning empty when no valid origin is configured. */
  public Optional<CoordinateDto> map(double x, double y) {
    if (!config.calibrated() || !Double.isFinite(x) || !Double.isFinite(y)) {
      return Optional.empty();
    }
    double radians = Math.toRadians(config.rotationDegrees());
    double scaledX = x * config.metersPerUnit();
    double scaledY = y * config.metersPerUnit();
    double east = scaledX * Math.cos(radians) - scaledY * Math.sin(radians);
    double north = scaledX * Math.sin(radians) + scaledY * Math.cos(radians);
    double latitude = config.originLatitude().getAsDouble() + north / METERS_PER_DEGREE;
    double longitudeDivisor = METERS_PER_DEGREE
        * Math.cos(Math.toRadians(config.originLatitude().getAsDouble()));
    if (Math.abs(longitudeDivisor) < 0.000001) {
      return Optional.empty();
    }
    double longitude = config.originLongitude().getAsDouble() + east / longitudeDivisor;
    return Optional.of(new CoordinateDto(latitude, longitude));
  }
}
