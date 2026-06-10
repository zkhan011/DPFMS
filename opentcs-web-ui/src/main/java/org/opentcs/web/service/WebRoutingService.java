// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import org.opentcs.web.config.WebUiConfig;
import org.opentcs.web.dto.WebDtos.PathDto;
import org.opentcs.web.dto.WebDtos.PlantModelDto;
import org.opentcs.web.dto.WebDtos.PointDto;
import org.opentcs.web.dto.WebDtos.RouteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates read-only route previews for the web frontend.
 *
 * @author Zishan Khan
 */
public class WebRoutingService {
  private static final Logger LOG = LoggerFactory.getLogger(WebRoutingService.class);

  /** Calculates a route, respecting path direction and locks. */
  public RouteDto route(PlantModelDto model, String source, String destination) {
    String strategy = WebUiConfig.routingStrategy();
    LOG.info("Calculating {} web route from {} to {}.", strategy, source, destination);
    if (source == null || destination == null || source.isBlank() || destination.isBlank()) {
      return missing(strategy, "Source and destination are required.");
    }
    if (source.equals(destination)) {
      return new RouteDto(true, strategy, 0.0, List.of(source), List.of(), "");
    }
    Map<String, PointDto> points = new HashMap<>();
    model.points().forEach(point -> points.put(point.name(), point));
    if (!points.containsKey(source) || !points.containsKey(destination)) {
      return missing(strategy, "Unknown source or destination point.");
    }
    Map<String, List<PathDto>> outgoing = new HashMap<>();
    model.paths().stream().filter(path -> !path.blocked())
        .forEach(
            path -> outgoing.computeIfAbsent(path.source(), key -> new ArrayList<>()).add(path)
        );
    Map<String, Double> gScore = new HashMap<>();
    Map<String, String> cameFrom = new HashMap<>();
    Map<String, PathDto> cameFromPath = new HashMap<>();
    PriorityQueue<NodeScore> open = new PriorityQueue<>(
        Comparator.comparingDouble(NodeScore::score)
    );
    Set<String> closed = new HashSet<>();
    gScore.put(source, 0.0);
    open.add(
        new NodeScore(source, heuristic(strategy, points.get(source), points.get(destination)))
    );
    while (!open.isEmpty()) {
      String current = open.remove().name();
      if (!closed.add(current)) {
        continue;
      }
      if (current.equals(destination)) {
        return reconstruct(
            strategy, source, destination, gScore.get(destination), cameFrom, cameFromPath
        );
      }
      for (PathDto edge : outgoing.getOrDefault(current, List.of())) {
        double tentative = gScore.get(current) + Math.max(0.0, edge.cost());
        if (tentative < gScore.getOrDefault(edge.destination(), Double.POSITIVE_INFINITY)) {
          cameFrom.put(edge.destination(), current);
          cameFromPath.put(edge.destination(), edge);
          gScore.put(edge.destination(), tentative);
          open.add(
              new NodeScore(
                  edge.destination(), tentative
                      + heuristic(strategy, points.get(edge.destination()), points.get(destination))
              )
          );
        }
      }
    }
    LOG.warn("No web route found from {} to {}.", source, destination);
    return missing(
        strategy, "No route found. Locked, unavailable and reverse-only paths are excluded."
    );
  }

  private double heuristic(String strategy, PointDto source, PointDto target) {
    return strategy.equals("ASTAR") && source != null && target != null
        ? Math.hypot(source.x() - target.x(), source.y() - target.y())
        : 0.0;
  }

  private RouteDto reconstruct(
      String strategy, String source, String destination, double cost,
      Map<String, String> cameFrom, Map<String, PathDto> cameFromPath
  ) {
    List<String> points = new ArrayList<>();
    List<String> paths = new ArrayList<>();
    String current = destination;
    points.add(current);
    while (!current.equals(source)) {
      PathDto path = cameFromPath.get(current);
      if (path == null) {
        return missing(strategy, "Route reconstruction failed.");
      }
      paths.add(path.name());
      current = cameFrom.get(current);
      points.add(current);
    }
    java.util.Collections.reverse(points);
    java.util.Collections.reverse(paths);
    LOG.info(
        "Calculated {} web route from {} to {} with cost {}.", strategy, source, destination, cost
    );
    return new RouteDto(true, strategy, cost, points, paths, "");
  }

  private RouteDto missing(String strategy, String message) {
    return new RouteDto(false, strategy, 0.0, List.of(), List.of(), message);
  }

  private record NodeScore(String name, double score) {}
}
