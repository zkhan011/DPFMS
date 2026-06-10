// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.opentcs.web.config.WebUiConfig;
import org.opentcs.web.dto.WebDtos.CoordinateDto;
import org.opentcs.web.dto.WebDtos.LocationDto;
import org.opentcs.web.dto.WebDtos.PathDto;
import org.opentcs.web.dto.WebDtos.PlantModelDto;
import org.opentcs.web.dto.WebDtos.PointDto;
import org.opentcs.web.util.PlantCoordinateMapper;

/**
 * Reads and maps the openTCS plant model for browser rendering.
 *
 * @author Zishan Khan
 */
public class WebPlantModelService {
  private final KernelHttpClient client;
  private final PlantCoordinateMapper coordinateMapper;

  public WebPlantModelService(KernelHttpClient client) {
    this.client = client;
    this.coordinateMapper = new PlantCoordinateMapper(WebUiConfig.mapConfig());
  }

  /** Fetches the current plant model. */
  public PlantModelDto fetch()
      throws IOException,
        InterruptedException {
    JsonNode root = client.get("/plantModel");
    List<PointDto> points = new ArrayList<>();
    for (JsonNode node : root.path("points")) {
      double x = node.path("position").path("x").asDouble();
      double y = node.path("position").path("y").asDouble();
      CoordinateDto mapped = coordinateMapper.map(
          x, y, property(node, "latitude"),
          property(node, "longitude")
      ).orElse(null);
      points.add(
          new PointDto(
              node.path("name").asText(), x, y,
              mapped == null ? null : mapped.latitude(), mapped == null ? null : mapped.longitude()
          )
      );
    }
    List<PathDto> paths = new ArrayList<>();
    for (JsonNode node : root.path("paths")) {
      paths.add(
          new PathDto(
              node.path("name").asText(), node.path("srcPointName").asText(),
              node.path("destPointName").asText(), node.path("length").asDouble(1.0),
              node.path("locked").asBoolean()
          )
      );
    }
    List<LocationDto> locations = new ArrayList<>();
    for (JsonNode node : root.path("locations")) {
      double x = node.path("position").path("x").asDouble();
      double y = node.path("position").path("y").asDouble();
      CoordinateDto mapped = coordinateMapper.map(
          x, y, property(node, "latitude"),
          property(node, "longitude")
      ).orElse(null);
      locations.add(
          new LocationDto(
              node.path("name").asText(), x, y,
              mapped == null ? null : mapped.latitude(), mapped == null ? null : mapped.longitude(),
              node.path("locked").asBoolean()
          )
      );
    }
    return new PlantModelDto(
        root.path("name").asText("Unnamed plant model"), points, paths, locations
    );
  }

  private Double property(JsonNode node, String key) {
    for (JsonNode property : node.path("properties")) {
      if (key.equalsIgnoreCase(property.path("key").asText())) {
        try {
          return Double.valueOf(property.path("value").asText());
        }
        catch (NumberFormatException exc) {
          return null;
        }
      }
    }
    return null;
  }

}
