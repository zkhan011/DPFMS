// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.opentcs.web.config.FmsConfig;
import org.opentcs.web.config.WebUiConfig;
import org.opentcs.web.dto.WebDtos.MapConfigDto;
import org.opentcs.web.dto.WebDtos.PlantModelDto;
import org.opentcs.web.dto.WebDtos.PointDto;
import org.opentcs.web.service.KernelHttpClient;
import org.opentcs.web.service.MockFleetService;
import org.opentcs.web.service.WebKernelService;
import org.opentcs.web.service.WebPlantModelService;
import org.opentcs.web.service.WebRoutingService;
import org.opentcs.web.service.WebTransportOrderService;
import org.opentcs.web.service.WebVehicleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON API for live JSP frontend updates.
 *
 * @author Zishan Khan
 */
public class ApiController
    extends
      HttpServlet {
  private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final KernelHttpClient client = new KernelHttpClient();
  private final WebKernelService kernelService = new WebKernelService(client);
  private final WebPlantModelService plantModelService = new WebPlantModelService(client);
  private final WebVehicleService vehicleService = new WebVehicleService(client);
  private final WebTransportOrderService orderService = new WebTransportOrderService(client);
  private final WebRoutingService routingService = new WebRoutingService();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      Object result = switch (request.getPathInfo()) {
        case "/fleet" -> fleet();
        case "/map/diagnostics" -> diagnostics();
        case "/status" -> FmsConfig.load().mockEnabled() ? mockStatus() : kernelService.status();
        case "/plant-model", "/map/plant-model" -> plantModelService.fetch();
        case "/vehicles", "/map/vehicles" -> FmsConfig.load().mockEnabled()
            ? MockFleetService.INSTANCE.snapshot().get("vehicles") : vehicleService.fetch();
        case "/transport-orders" -> FmsConfig.load().mockEnabled()
            ? MockFleetService.INSTANCE.snapshot().get("jobs") : orderService.fetch();
        case "/routes" -> FmsConfig.load().mockEnabled()
            ? mockRoute(request.getParameter("source"), request.getParameter("destination"))
            : routingService.route(
                plantModelService.fetch(), request.getParameter("source"),
                request.getParameter("destination")
            );
        case "/map/config" -> mapConfig();
        default -> throw new ApiException(404, "Unknown API endpoint.");
      };
      write(response, 200, result);
    }
    catch (ApiException exc) {
      writeError(response, exc.status, exc.getMessage());
    }
    catch (Exception exc) {
      LOG.warn("Web API request failed: {}", request.getPathInfo(), exc);
      writeError(response, 503, "Disconnected from kernel: " + exc.getMessage());
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      String path = request.getPathInfo();
      JsonNode body = mapper.readTree(request.getInputStream());
      Object result;
      if (path.equals("/transport-orders")) {
        result = orderService.create(
            text(body, "name"), text(body, "source"), text(body, "destination"),
            text(body, "intendedVehicle")
        );
      }
      else if (path.startsWith("/vehicles/")) {
        String[] parts = path.split("/");
        if (parts.length != 4) {
          throw new ApiException(404, "Unknown vehicle action.");
        }
        String vehicle = java.net.URLDecoder.decode(
            parts[2], java.nio.charset.StandardCharsets.UTF_8
        );
        result = switch (parts[3]) {
          case "pause" -> vehicleService.pause(vehicle, true);
          case "resume" -> vehicleService.pause(vehicle, false);
          case "withdraw-order" -> vehicleService.withdraw(vehicle);
          default -> throw new ApiException(404, "Unsupported vehicle action.");
        };
      }
      else {
        throw new ApiException(404, "Unknown API endpoint.");
      }
      write(response, 200, result);
    }
    catch (IllegalArgumentException exc) {
      writeError(response, 400, exc.getMessage());
    }
    catch (ApiException exc) {
      writeError(response, exc.status, exc.getMessage());
    }
    catch (Exception exc) {
      LOG.warn("Web API command failed: {}", request.getPathInfo(), exc);
      writeError(response, 503, "Kernel command failed: " + exc.getMessage());
    }
  }

  private MapConfigDto mapConfig() {
    FmsConfig fms = FmsConfig.load();
    return new MapConfigDto(
        fms.provider().toUpperCase(), fms.provider().toUpperCase(),
        fms.mockEnabled() || WebUiConfig.mapConfig().calibrated(), fms.zoom(), "",
        fms.googleApiKey(), fms.offlineEnabled(), fms.latitude(), fms.longitude(),
        fms.updateIntervalMs()
    );
  }

  private Object fleet() {
    if (FmsConfig.load().mockEnabled()) {
      return MockFleetService.INSTANCE.snapshot();
    }
    try {
      return kernelFleet();
    }
    catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      throw new ApiException(503, "Kernel fleet request was interrupted.");
    }
    catch (IOException exc) {
      throw new ApiException(503, "Kernel fleet data is unavailable: " + exc.getMessage());
    }
  }

  /** Adapts live kernel entities to the provider-independent state used by the map. */
  private Object kernelFleet()
      throws IOException,
        InterruptedException {
    PlantModelDto plant = plantModelService.fetch();
    Map<String, PointDto> points = new LinkedHashMap<>();
    plant.points().forEach(point -> points.put(point.name(), point));
    List<Map<String, Object>> vehicles = new ArrayList<>();
    for (JsonNode node : vehicleService.fetch()) {
      String positionName = node.path("currentPosition").asText(node.path("position").asText());
      PointDto point = points.get(positionName);
      Map<String, Object> vehicle = new LinkedHashMap<>();
      vehicle.put("id", node.path("name").asText("Unknown vehicle"));
      vehicle.put("name", node.path("name").asText("Unknown vehicle"));
      vehicle.put("type", "Kernel vehicle");
      vehicle.put("status", node.path("state").asText("Unknown"));
      vehicle.put("currentLocation", positionName);
      vehicle.put("currentJob", node.path("transportOrder").asText(""));
      vehicle.put("energyType", "Battery");
      vehicle.put("battery", node.path("energyLevel").asInt(0));
      vehicle.put("speed", 0);
      vehicle.put("latitude", point == null ? null : point.latitude());
      vehicle.put("longitude", point == null ? null : point.longitude());
      vehicles.add(vehicle);
    }
    List<Map<String, Object>> locations = new ArrayList<>();
    plant.locations().forEach(location -> {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("id", location.name());
      value.put("name", location.name());
      value.put("type", "Kernel location");
      value.put("status", location.locked() ? "Locked" : "Operational");
      value.put("latitude", location.latitude());
      value.put("longitude", location.longitude());
      locations.add(value);
    });
    List<Map<String, Object>> routes = new ArrayList<>();
    plant.paths().forEach(path -> {
      PointDto source = points.get(path.source());
      PointDto destination = points.get(path.destination());
      if (source == null || destination == null || source.latitude() == null
          || destination.latitude() == null) {
        return;
      }
      routes.add(
          Map.of(
              "id", path.name(), "name", path.name(),
              "status", path.blocked() ? "Blocked" : "Available",
              "geometry", List.of(
                  List.of(source.longitude(), source.latitude()),
                  List.of(destination.longitude(), destination.latitude())
              )
          )
      );
    });
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("seedVersion", "kernel-live");
    result.put("generatedAt", java.time.Instant.now().toString());
    result.put("vehicles", vehicles);
    result.put("jobs", mapper.convertValue(orderService.fetch(), List.class));
    result.put("locations", locations);
    result.put(
        "routingNodes", plant.points().stream().filter(point -> point.latitude() != null)
            .map(
                point -> Map.of(
                    "name", point.name(), "latitude", point.latitude(), "longitude", point
                        .longitude()
                )
            ).toList()
    );
    result.put("chargingStations", List.of());
    result.put("fuelStations", List.of());
    result.put("alerts", List.of());
    result.put("routes", routes);
    result.put("kpis", Map.of("totalVehicles", vehicles.size()));
    result.put("history", List.of());
    return result;
  }

  private Object mockRoute(String source, String destination) {
    if (source == null || source.isBlank() || destination == null || destination.isBlank()) {
      throw new ApiException(400, "Source and destination are required.");
    }
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> routes = (List<Map<String, Object>>) MockFleetService.INSTANCE
        .snapshot().get("routes");
    Map<String, Object> route = routes.get(
        Math.floorMod((source + destination).hashCode(), routes.size())
    );
    return Map.of(
        "found", true, "strategy", "TERMINAL_NETWORK", "totalCost", 1,
        "points", List.of(source, destination), "paths", List.of(route.get("id")),
        "geometry", route.get("geometry"), "message", ""
    );
  }

  private Object diagnostics() {
    FmsConfig config = FmsConfig.load();
    return Map.of(
        "offlineMapEnabled", config.offlineEnabled(), "offlineArchiveFound", true,
        "archiveReadable", true, "styleFound", true, "minimumZoom", config.offlineMinZoom(),
        "maximumZoom", config.offlineMaxZoom(), "configuredProvider", config.provider(),
        "googleKeyConfigured", !config.googleApiKey().isBlank(), "mockDataEnabled", config
            .mockEnabled()
    );
  }

  @SuppressWarnings("unchecked")
  private Object mockStatus() {
    Map<String, Object> snapshot = MockFleetService.INSTANCE.snapshot();
    Map<String, Object> kpis = (Map<String, Object>) snapshot.get("kpis");
    return Map.of(
        "connected", true, "kernelStatus", "DEMO ONLINE", "vehicleCount", kpis.get("totalVehicles"),
        "activeVehicleCount", kpis.get("activeVehicles"), "transportOrderCount",
        ((java.util.List<?>) snapshot.get("jobs")).size(), "failedOrderCount", kpis.get(
            "failedJobs"
        ),
        "message", "DPW FMS Jebel Ali demonstration is synchronized and running"
    );
  }

  private String text(JsonNode body, String name) {
    return body == null ? "" : body.path(name).asText("");
  }

  private void writeError(HttpServletResponse response, int status, String message)
      throws IOException {
    write(response, status, Map.of("error", message));
  }

  private void write(HttpServletResponse response, int status, Object value)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    mapper.writeValue(response.getOutputStream(), value);
  }

  private static class ApiException
      extends
        RuntimeException {
    private final int status;

    ApiException(int status, String message) {
      super(message);
      this.status = status;
    }
  }
}
