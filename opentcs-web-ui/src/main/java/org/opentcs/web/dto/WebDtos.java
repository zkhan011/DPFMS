// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.dto;

import java.util.List;

/**
 * DTOs exposed by the JSP frontend API.
 *
 * @author Zishan Khan
 */
public final class WebDtos {
  private WebDtos() {
  }

  public record PointDto(String name, double x, double y, Double latitude, Double longitude) {}

  public record PathDto(
      String name, String source, String destination, double cost, boolean blocked
  ) {}

  public record LocationDto(
      String name, double x, double y, Double latitude, Double longitude, boolean locked
  ) {}

  public record PlantModelDto(
      String name, List<PointDto> points, List<PathDto> paths, List<LocationDto> locations
  ) {}

  public record RouteDto(
      boolean found, String strategy, double totalCost, List<String> points, List<String> paths,
      String message
  ) {}

  public record MapConfigDto(
      String provider, String requestedProvider, boolean calibrated, int zoom, String warning,
      String googleApiKey
  ) {}

  public record CoordinateDto(double latitude, double longitude) {}
}
