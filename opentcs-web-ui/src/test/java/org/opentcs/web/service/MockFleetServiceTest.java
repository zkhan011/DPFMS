// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockFleetServiceTest {
  @Test
  void seedsCompleteStableFleetWithoutDuplicates() {
    Map<String, Object> first = MockFleetService.INSTANCE.snapshot();
    Map<String, Object> second = MockFleetService.INSTANCE.snapshot();
    List<?> vehicles = (List<?>) first.get("vehicles");
    List<?> jobs = (List<?>) first.get("jobs");
    assertEquals("dpw-jebel-ali-v1", first.get("seedVersion"));
    assertEquals(20, vehicles.size());
    assertEquals(32, jobs.size());
    assertEquals(20, ids(vehicles).size());
    assertEquals(ids(vehicles), ids((List<?>) second.get("vehicles")));
    assertEquals(32, ids(jobs).size());
    assertEquals(18, ((List<?>) first.get("alerts")).size());
  }

  @Test
  void providesOperationalDistributionAndConsistentRelationships() {
    Map<String, Object> state = MockFleetService.INSTANCE.snapshot();
    List<Map<String, Object>> vehicles = cast(state.get("vehicles"));
    List<Map<String, Object>> jobs = cast(state.get("jobs"));
    assertTrue(vehicles.stream().anyMatch(v -> "Offline".equals(v.get("status"))));
    assertTrue(vehicles.stream().anyMatch(v -> "Charging".equals(v.get("status"))));
    assertTrue(vehicles.stream().anyMatch(v -> ((List<?>) v.get("queuedJobs")).size() > 1));
    assertTrue(jobs.stream().anyMatch(j -> "".equals(j.get("assignedVehicle"))));
    assertTrue(
        jobs.stream().filter(j -> !"".equals(j.get("assignedVehicle")))
            .allMatch(j -> ids(vehicles).contains(j.get("assignedVehicle")))
    );
  }

  @Test
  void stationOccupancyAndKpisComeFromSeededState() {
    Map<String, Object> state = MockFleetService.INSTANCE.snapshot();
    List<Map<String, Object>> chargers = cast(state.get("chargingStations"));
    List<Map<String, Object>> fuel = cast(state.get("fuelStations"));
    Map<String, Object> kpis = castMap(state.get("kpis"));
    assertTrue(chargers.stream().anyMatch(s -> "Fully occupied".equals(s.get("status"))));
    assertTrue(
        chargers.stream().anyMatch(s -> ((Number) s.get("outOfServiceConnectors")).intValue() > 0)
    );
    assertTrue(fuel.stream().anyMatch(s -> "Fully occupied".equals(s.get("status"))));
    assertEquals(20, kpis.get("totalVehicles"));
    assertEquals(3L, kpis.get("movingVehicles"));
    assertEquals(7, ((List<?>) state.get("history")).size());
  }

  private static HashSet<Object> ids(List<?> values) {
    HashSet<Object> result = new HashSet<>();
    values.forEach(value -> result.add(((Map<?, ?>) value).get("id")));
    return result;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> cast(Object value) {
    return (List<Map<String, Object>>) value;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> castMap(Object value) {
    return (Map<String, Object>) value;
  }
}
