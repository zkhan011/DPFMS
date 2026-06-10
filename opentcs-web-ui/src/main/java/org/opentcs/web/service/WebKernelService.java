// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Aggregates kernel status for the dashboard and control center.
 *
 * @author Zishan Khan
 */
public class WebKernelService {
  private final KernelHttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  public WebKernelService(KernelHttpClient client) {
    this.client = client;
  }

  /** Returns a safe status object even when the kernel is disconnected. */
  public JsonNode status() {
    ObjectNode result = mapper.createObjectNode();
    try {
      JsonNode version = client.get("/kernel/version");
      JsonNode vehicles = client.get("/vehicles");
      JsonNode orders = client.get("/transportOrders");
      result.put("connected", true);
      result.put("kernelStatus", "ONLINE");
      result.set("version", version);
      result.put("vehicleCount", vehicles.isArray() ? vehicles.size() : 0);
      result.put(
          "activeVehicleCount", vehicles.isArray()
              ? java.util.stream.StreamSupport.stream(vehicles.spliterator(), false)
                  .filter(
                      vehicle -> !java.util.Set.of("IDLE", "UNKNOWN", "UNAVAILABLE")
                          .contains(vehicle.path("state").asText())
                  )
                  .count()
              : 0
      );
      result.put("transportOrderCount", orders.isArray() ? orders.size() : 0);
      result.put(
          "failedOrderCount", orders.isArray()
              ? java.util.stream.StreamSupport.stream(orders.spliterator(), false)
                  .filter(
                      order -> java.util.Set.of("FAILED", "UNROUTABLE")
                          .contains(order.path("state").asText())
                  )
                  .count()
              : 0
      );
      result.put("message", "Connected to openTCS service web API");
    }
    catch (Exception exc) {
      result.put("connected", false);
      result.put("kernelStatus", "DISCONNECTED");
      result.put("vehicleCount", 0);
      result.put("activeVehicleCount", 0);
      result.put("transportOrderCount", 0);
      result.put("failedOrderCount", 0);
      result.put("message", "Disconnected from kernel: " + exc.getMessage());
    }
    return result;
  }
}
