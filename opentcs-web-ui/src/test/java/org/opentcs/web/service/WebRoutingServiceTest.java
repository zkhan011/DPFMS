// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentcs.web.dto.WebDtos.PathDto;
import org.opentcs.web.dto.WebDtos.PlantModelDto;
import org.opentcs.web.dto.WebDtos.PointDto;

/**
 * Tests route preview behavior shared by SVG and map views.
 *
 * @author Zishan Khan
 */
class WebRoutingServiceTest {
  private final WebRoutingService service = new WebRoutingService();

  @AfterEach
  void resetStrategy() {
    System.clearProperty("routing.strategy");
  }

  @Test
  void selectsShortestPath() {
    var route = service.route(model(false, true), "A", "C");
    assertThat(route.paths()).containsExactly("AB", "BC");
    assertThat(route.totalCost()).isEqualTo(2.0);
  }

  @Test
  void returnsNoRoute() {
    assertThat(service.route(model(true, false), "A", "C").found()).isFalse();
  }

  @Test
  void ignoresBlockedEdge() {
    assertThat(service.route(model(false, true), "A", "C").paths()).doesNotContain("AC");
  }

  @Test
  void handlesSameSourceAndDestination() {
    var route = service.route(model(false, false), "A", "A");
    assertThat(route.found()).isTrue();
    assertThat(route.totalCost()).isZero();
  }

  @Test
  void respectsOneWayPaths() {
    assertThat(service.route(model(false, false), "C", "A").found()).isFalse();
  }

  @Test
  void handlesEqualCostRoutes() {
    var route = service.route(model(false, false), "A", "C");
    assertThat(route.found()).isTrue();
    assertThat(route.totalCost()).isEqualTo(2.0);
  }

  @Test
  void astarUsesConfiguredStrategy() {
    System.setProperty("routing.strategy", "ASTAR");
    assertThat(service.route(model(false, false), "A", "C").strategy()).isEqualTo("ASTAR");
  }

  @Test
  void unavailableResourceIsSkipped() {
    assertThat(service.route(model(false, true), "A", "C").points()).containsExactly("A", "B", "C");
  }

  private PlantModelDto model(boolean blockAb, boolean blockAc) {
    var points = List.of(point("A", 0, 0), point("B", 1, 0), point("C", 2, 0));
    var paths = List.of(
        path("AB", "A", "B", 1, blockAb), path("BC", "B", "C", 1, false),
        path("AC", "A", "C", 2, blockAc)
    );
    return new PlantModelDto("test", points, paths, List.of());
  }

  private PointDto point(String name, double x, double y) {
    return new PointDto(name, x, y, null, null);
  }

  private PathDto path(
      String name, String source, String destination, double cost, boolean blocked
  ) {
    return new PathDto(name, source, destination, cost, blocked);
  }
}
