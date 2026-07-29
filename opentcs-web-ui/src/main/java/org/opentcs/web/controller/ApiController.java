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
import java.util.Map;
import org.opentcs.web.config.FmsConfig;
import org.opentcs.web.config.MapConfig;
import org.opentcs.web.config.WebUiConfig;
import org.opentcs.web.dto.WebDtos.MapConfigDto;
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
        case "/routes" -> routingService.route(
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
    if (fms.mockEnabled()) {
      return new MapConfigDto(
          fms.provider().toUpperCase(), fms.provider().toUpperCase(), true, fms.zoom(), "",
          fms.googleApiKey(), fms.offlineEnabled(), fms.latitude(), fms.longitude(),
          fms.updateIntervalMs()
      );
    }
    MapConfig config = WebUiConfig.mapConfig();
    return new MapConfigDto(
        config.effectiveProvider().name(), config.requestedProvider().name(),
        config.calibrated(), config.defaultZoom(), config.warning(), config.googleApiKey(), true,
        config.originLatitude().orElse(24.9857), config.originLongitude().orElse(55.0273), 2000
    );
  }

  private Object fleet() {
    if (!FmsConfig.load().mockEnabled()) {
      throw new ApiException(404, "DPW FMS demonstration mode is disabled.");
    }
    return MockFleetService.INSTANCE.snapshot();
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
