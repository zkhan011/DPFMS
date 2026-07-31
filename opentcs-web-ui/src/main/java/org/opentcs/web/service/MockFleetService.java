// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Single, deterministic and idempotent source of truth for the Jebel Ali demonstration. */
public final class MockFleetService {
  public static final MockFleetService INSTANCE = new MockFleetService();
  private static final String SEED_VERSION = "dpw-jebel-ali-v1";
  private final Instant started = Instant.now();
  private final List<Map<String, Object>> vehicles = new ArrayList<>();
  private final List<Map<String, Object>> jobs = new ArrayList<>();
  private final List<Map<String, Object>> locations = new ArrayList<>();
  private final List<Map<String, Object>> chargers = new ArrayList<>();
  private final List<Map<String, Object>> fuelStations = new ArrayList<>();
  private final List<Map<String, Object>> alerts = new ArrayList<>();
  private final List<Map<String, Object>> routes = new ArrayList<>();

  private MockFleetService() {
    seed();
  }

  public synchronized Map<String, Object> snapshot() {
    simulate();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("seedVersion", SEED_VERSION);
    result.put("generatedAt", Instant.now().toString());
    result.put("vehicles", vehicles);
    result.put("jobs", jobs);
    result.put("locations", locations);
    result.put("routingNodes", locations);
    result.put("chargingStations", chargers);
    result.put("fuelStations", fuelStations);
    result.put("alerts", alerts);
    result.put("routes", routes);
    result.put("kpis", kpis());
    result.put("history", history());
    return result;
  }

  private void seed() {
    String[] locationTypes = {"Entry gate", "Exit gate", "Security checkpoint", "Weighbridge",
        "Container yard block", "Loading zone", "Unloading zone", "Empty-container yard",
        "Loaded-container yard", "Warehouse", "Workshop", "Maintenance bay", "Parking area",
        "Holding area", "Charging area", "Fuel area", "Administration building", "Inspection zone",
        "Emergency assembly point"};
    for (int i = 0; i < locationTypes.length; i++) locations.add(
        item(
            "LOC-%02d".formatted(i + 1), Map.of(
                "name", locationTypes[i] + " %s".formatted((char) ('A' + i)), "type",
                locationTypes[i], "latitude", 24.976 + i * .0011, "longitude", 55.017 + (i % 7)
                    * .0018, "status", i == 11 ? "Maintenance" : "Operational", "capacity", 20 + i
                        * 3, "occupancy", (i * 7) % 25, "description",
                "Jebel Ali terminal operational asset"
            )
        )
    );
    String[] statuses = {"Moving", "Moving", "Idle", "Charging", "Waiting", "Refuelling", "Waiting",
        "Assigned", "Loading", "Unloading", "Maintenance", "Offline", "Alarm", "Available",
        "Stopped", "Moving", "Idle", "Charging", "Delayed", "Available"};
    String[] types = {"Electric terminal tractor", "Yard truck", "Prime mover", "Shuttle vehicle",
        "Electric truck", "Diesel truck", "Service vehicle", "Maintenance vehicle"};
    for (int i = 0; i < 20; i++) {
      String id = "DPW-%03d".formatted(i + 1);
      String route = "R-%02d".formatted(i % 6 + 1);
      vehicles.add(
          item(
              id, Map.ofEntries(
                  Map.entry("name", "Fleet Unit %02d".formatted(i + 1)), Map.entry(
                      "type", types[i % types.length]
                  ), Map.entry("status", statuses[i]), Map.entry(
                      "availability", statuses[i].equals("Available") ? "Available" : "Allocated"
                  ), Map.entry("latitude", 24.979 + (i % 5) * .002), Map.entry(
                      "longitude", 55.019 + (i % 7) * .002
                  ), Map.entry("speed", statuses[i].equals("Moving") ? 18 + i % 8 : 0), Map.entry(
                      "heading", 45 + i * 13
                  ), Map.entry("battery", i % 2 == 0 ? (i == 4 ? 12 : 55 + i) : 0), Map.entry(
                      "fuel", i % 2 == 1 ? (i == 18 ? 9 : 48 + i) : 0
                  ), Map.entry("energyType", i % 2 == 0 ? "Electric" : "Diesel"), Map.entry(
                      "currentJob", i < 14 ? "JOB-%03d".formatted(i + 1) : ""
                  ), Map.entry(
                      "queuedJobs", i < 3 ? List.of(
                          "JOB-%03d".formatted(21 + i), "JOB-%03d".formatted(24 + i)
                      ) : List.of()
                  ), Map.entry(
                      "operator", i == 11 ? "Unassigned" : "Operator %02d".formatted(i + 1)
                  ), Map.entry(
                      "maintenance", i == 10 ? "In workshop" : i == 16 ? "Overdue" : "Current"
                  ), Map.entry("alarm", i == 12 ? "Critical" : "None"), Map.entry(
                      "currentLocation", locations.get(i % locations.size()).get("name")
                  ), Map.entry(
                      "destination", locations.get((i + 4) % locations.size()).get("name")
                  ), Map.entry("odometerKm", 22000 + i * 1370), Map.entry(
                      "operatingHours", 1900 + i * 83
                  ), Map.entry("connected", i != 11), Map.entry("routeId", route), Map.entry(
                      "routeProgress", i * 4 % 100
                  ), Map.entry("lastUpdate", Instant.now().toString())
              )
          )
      );
    }
    String[] jobStates = {"Pending", "Assigned", "In progress", "Completed", "Delayed", "Cancelled",
        "Failed", "Waiting", "On hold"};
    String[] jobTypes = {"Container pickup", "Container drop-off", "Yard transfer", "Gate movement",
        "Charging", "Refuelling", "Maintenance inspection", "Workshop transfer",
        "Equipment delivery", "Empty-container movement", "Loaded-container movement",
        "Loading-zone transfer", "Unloading-zone transfer"};
    for (int i = 0; i < 32; i++) {
      Instant created = Instant.now().minus(2 + i * 4, ChronoUnit.HOURS);
      String state = jobStates[i % jobStates.length];
      jobs.add(
          item(
              "JOB-%03d".formatted(i + 1), Map.ofEntries(
                  Map.entry("type", jobTypes[i % jobTypes.length]), Map.entry(
                      "priority", i % 7 == 0 ? "Critical" : i % 3 == 0 ? "High" : "Normal"
                  ), Map.entry(
                      "assignedVehicle", i % 6 == 0 ? "" : "DPW-%03d".formatted(i % 20 + 1)
                  ), Map.entry("origin", locations.get(i % locations.size()).get("name")), Map
                      .entry("destination", locations.get((i + 5) % locations.size()).get("name")),
                  Map.entry("created", created.toString()), Map.entry(
                      "plannedStart", created.plus(30, ChronoUnit.MINUTES).toString()
                  ), Map.entry("estimatedCompletion", created.plus(2, ChronoUnit.HOURS).toString()),
                  Map.entry("status", state), Map.entry(
                      "progress", state.equals("Completed") ? 100 : (i * 13) % 95
                  ), Map.entry("routeId", "R-%02d".formatted(i % 6 + 1)), Map.entry(
                      "currentStep", state.equals("Completed") ? "Completed" : "Transit step " + (i
                          % 4 + 1)
                  ), Map.entry("delayReason", state.equals("Delayed") ? "Gate congestion" : ""), Map
                      .entry(
                          "cancellationReason", state.equals("Cancelled") ? "Customer request" : ""
                      ), Map.entry("containerNumber", "DPWU%07d".formatted(1000000 + i)), Map.entry(
                          "equipmentId", "EQ-%04d".formatted(500 + i)
                      ), Map.entry("slaTargetMinutes", 120), Map.entry(
                          "slaStatus", state.equals("Delayed") || state.equals("Failed") ? "Missed"
                              : "On track"
                      )
              )
          )
      );
    }
    chargers.add(
        station("CHG-01", "North Electric Hub", 24.983, 55.023, 8, 6, 2, 0, "Partially occupied")
    );
    chargers.add(
        station("CHG-02", "Yard Fast Charge", 24.988, 55.030, 4, 0, 4, 0, "Fully occupied")
    );
    chargers.add(station("CHG-03", "Workshop Chargers", 24.981, 55.035, 6, 5, 0, 1, "Faulted"));
    chargers.add(station("CHG-04", "South Charge Point", 24.974, 55.026, 4, 4, 0, 0, "Available"));
    chargers.add(station("CHG-05", "Legacy Charge Point", 24.990, 55.018, 2, 0, 0, 2, "Offline"));
    fuelStations.add(fuel("FUEL-01", "Main Diesel Island", 24.984, 55.020, 6, 3, 2, 1, "Busy"));
    fuelStations.add(fuel("FUEL-02", "South Fuel Point", 24.975, 55.032, 4, 4, 0, 0, "Available"));
    fuelStations.add(
        fuel("FUEL-03", "Gate Fuel Point", 24.990, 55.027, 3, 0, 3, 0, "Fully occupied")
    );
    fuelStations.add(fuel("FUEL-04", "Workshop Pump", 24.980, 55.036, 2, 0, 0, 2, "Maintenance"));
    String[] alertTypes = {"Low battery", "Critical battery", "Low fuel", "Vehicle offline",
        "Communication lost", "Charging station fault", "Charging interrupted",
        "Fuel pump unavailable", "Refuelling delay", "Job delayed", "Job SLA missed",
        "Route blocked", "Geofence violation", "Maintenance overdue", "Excessive idle time",
        "Vehicle stopped unexpectedly", "Overspeed warning", "Workshop capacity exceeded"};
    for (int i = 0; i < 18; i++) alerts.add(
        item(
            "ALT-%03d".formatted(i + 1), Map.of(
                "severity", i % 5 == 0 ? "Critical" : i % 2 == 0 ? "Warning" : "Information",
                "type", alertTypes[i], "message", alertTypes[i] + " requires operator review",
                "relatedEntity", i < 12 ? "DPW-%03d".formatted(i + 1) : "JOB-%03d".formatted(i + 1),
                "latitude", 24.978 + (i % 6) * .002, "longitude", 55.019 + (i % 7) * .002,
                "created", Instant.now().minus(i * 37L, ChronoUnit.MINUTES).toString(),
                "acknowledged", i % 3 == 0, "resolved", i % 7 == 0, "owner", i % 3 == 0
                    ? "Shift supervisor" : "Operations desk"
            )
        )
    );
    for (int i = 0; i < 6; i++) {
      List<List<Double>> geometry = new ArrayList<>();
      for (int p = 0; p < 6; p++) geometry.add(
          List.of(55.018 + i * .002 + p * .0013, 24.978 + (p % 3) * .002 + i * .0005)
      );
      routes.add(
          item(
              "R-%02d".formatted(i + 1), Map.of(
                  "name", "Terminal route " + (i + 1), "status", i == 4 ? "Blocked" : i == 3
                      ? "Delayed" : "Active", "geometry", geometry
              )
          )
      );
    }
  }

  private void simulate() {
    long tick = Math.max(0, java.time.Duration.between(started, Instant.now()).toSeconds() / 3);
    for (Map<String, Object> vehicle : vehicles) {
      if (!"Moving".equals(vehicle.get("status"))) continue;
      int n = Integer.parseInt(vehicle.get("routeId").toString().substring(2)) - 1;
      @SuppressWarnings("unchecked")
      List<List<Double>> path = (List<List<Double>>) routes.get(n).get("geometry");
      double position = (tick % 100) / 100d * (path.size() - 1);
      int a = Math.min((int) position, path.size() - 2);
      double f = position - a;
      List<Double> x = path.get(a), y = path.get(a + 1);
      vehicle.put("longitude", x.get(0) + (y.get(0) - x.get(0)) * f);
      vehicle.put("latitude", x.get(1) + (y.get(1) - x.get(1)) * f);
      vehicle.put("routeProgress", (int) (tick % 100));
      vehicle.put("heading", Math.toDegrees(Math.atan2(y.get(0) - x.get(0), y.get(1) - x.get(1))));
      vehicle.put("lastUpdate", Instant.now().toString());
    }
  }

  private Map<String, Object> kpis() {
    Map<String, Object> k = new LinkedHashMap<>();
    k.put("totalVehicles", vehicles.size());
    k.put(
        "activeVehicles", vehicles.stream().filter(
            v -> !java.util.Set.of("Idle", "Offline", "Available", "Stopped").contains(
                v.get("status")
            )
        ).count()
    );
    for (String s : List.of(
        "Moving", "Idle", "Offline", "Available", "Charging", "Refuelling", "Maintenance", "Alarm"
    )) k.put(
        Character.toLowerCase(s.charAt(0)) + s.substring(1) + "Vehicles", vehicles.stream().filter(
            v -> s.equals(v.get("status"))
        ).count()
    );
    for (String s : List.of("Pending", "Assigned", "In progress", "Completed", "Delayed", "Failed"))
      k.put(
          s.toLowerCase().replace(" ", "") + "Jobs", jobs.stream().filter(
              j -> s.equals(j.get("status"))
          ).count()
      );
    k.put(
        "criticalAlerts", alerts.stream().filter(
            a -> "Critical".equals(a.get("severity")) && !Boolean.TRUE.equals(a.get("resolved"))
        ).count()
    );
    k.put(
        "warningAlerts", alerts.stream().filter(
            a -> "Warning".equals(a.get("severity")) && !Boolean.TRUE.equals(a.get("resolved"))
        ).count()
    );
    k.put("fleetUtilization", 65);
    k.put("onTimeCompletion", 82);
    k.put("averageJobDurationMinutes", 94);
    return k;
  }

  private List<Map<String, Object>> history() {
    List<Map<String, Object>> h = new ArrayList<>();
    for (int d = 6; d >= 0; d--) h.add(
        Map.of(
            "date", Instant.now().minus(d, ChronoUnit.DAYS).toString(), "completedJobs", 32 - d * 2,
            "delayedJobs", 2 + d % 3, "utilization", 72 - d % 4 * 3, "energyKwh", 820 + d * 21,
            "fuelLitres", 610 + d * 17, "alerts", 4 + d % 5
        )
    );
    return h;
  }

  private static Map<String, Object> item(String id, Map<String, ?> values) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", id);
    m.putAll(values);
    return m;
  }

  private static Map<String, Object> station(
      String id, String name, double lat, double lng, int total, int available, int occupied,
      int out, String status
  ) {
    return item(
        id, Map.ofEntries(
            Map.entry("name", name), Map.entry("latitude", lat), Map.entry("longitude", lng), Map
                .entry("connectorCount", total), Map.entry("availableConnectors", available), Map
                    .entry("occupiedConnectors", occupied), Map.entry(
                        "outOfServiceConnectors", out
                    ), Map.entry("connectorTypes", List.of("CCS2", "MCS")), Map.entry(
                        "powerRatingKw", 350
                    ), Map.entry(
                        "vehiclesCharging", occupied == 0 ? List.of() : List.of("DPW-004")
                    ), Map.entry(
                        "vehiclesWaiting", status.equals("Fully occupied") ? List.of("DPW-005")
                            : List.of()
                    ), Map.entry("queueLength", status.equals("Fully occupied") ? 1 : 0), Map.entry(
                        "estimatedWaitMinutes", status.equals("Fully occupied") ? 22 : 0
                    ), Map.entry("status", status), Map.entry(
                        "lastUpdate", Instant.now().toString()
                    )
        )
    );
  }

  private static Map<String, Object> fuel(
      String id, String name, double lat, double lng, int total, int available, int occupied,
      int out, String status
  ) {
    return item(
        id, Map.ofEntries(
            Map.entry("name", name), Map.entry("latitude", lat), Map.entry("longitude", lng), Map
                .entry("fuelType", "Ultra-low sulphur diesel"), Map.entry("pumpCount", total), Map
                    .entry("availablePumps", available), Map.entry("occupiedPumps", occupied), Map
                        .entry("outOfServicePumps", out), Map.entry(
                            "queueLength", status.equals("Fully occupied") ? 2 : 0
                        ), Map.entry(
                            "vehiclesRefuelling", occupied == 0 ? List.of() : List.of("DPW-006")
                        ), Map.entry(
                            "vehiclesWaiting", status.equals("Fully occupied") ? List.of(
                                "DPW-007", "DPW-009"
                            ) : List.of()
                        ), Map.entry(
                            "estimatedWaitMinutes", status.equals("Fully occupied") ? 18 : 0
                        ), Map.entry("status", status), Map.entry(
                            "lastUpdate", Instant.now().toString()
                        )
        )
    );
  }
}
