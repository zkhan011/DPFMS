// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/**
 * Transport order monitoring and creation bridge.
 *
 * @author Zishan Khan
 */
public class WebTransportOrderService {
  private final KernelHttpClient client;
  private final ObjectMapper mapper = new ObjectMapper();

  public WebTransportOrderService(KernelHttpClient client) {
    this.client = client;
  }

  public JsonNode fetch()
      throws IOException,
        InterruptedException {
    return client.get("/transportOrders");
  }

  /** Creates a two-destination MOVE order using the existing service web API contract. */
  public JsonNode create(String name, String source, String destination, String intendedVehicle)
      throws IOException,
        InterruptedException {
    if (name == null || name.isBlank() || source == null || source.isBlank()
        || destination == null || destination.isBlank()) {
      throw new IllegalArgumentException("Order name, source and destination are required.");
    }
    ObjectNode request = mapper.createObjectNode();
    ArrayNode destinations = request.putArray("destinations");
    destinations.add(destination(source));
    destinations.add(destination(destination));
    if (intendedVehicle != null && !intendedVehicle.isBlank()) {
      request.put("intendedVehicle", intendedVehicle);
    }
    return client.sendJson(
        "POST", "/transportOrders/" + KernelHttpClient.encode(name), request.toString()
    );
  }

  private ObjectNode destination(String location) {
    ObjectNode destination = mapper.createObjectNode();
    destination.put("locationName", location);
    destination.put("operation", "MOVE");
    return destination;
  }
}
