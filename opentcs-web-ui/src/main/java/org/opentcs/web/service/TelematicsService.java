// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory latest-value store for MQTT/RabbitMQ telematics bridge messages. */
public enum TelematicsService {
  INSTANCE;

  private final ConcurrentHashMap<String, Map<String, Object>> latest = new ConcurrentHashMap<>();

  public Map<String, Object> upsert(JsonNode message) {
    String vehicleId = text(message, "vehicleId", text(message, "vehicle", ""));
    if (vehicleId.isBlank()) {
      throw new IllegalArgumentException("vehicleId is required for telematics updates.");
    }
    Map<String, Object> value = new LinkedHashMap<>();
    value.put("vehicleId", vehicleId);
    put(value, "timestamp", text(message, "timestamp", Instant.now().toString()));
    put(value, "source", text(message, "source", "telematics-bridge"));
    put(value, "latitude", number(message, "latitude"));
    put(value, "longitude", number(message, "longitude"));
    put(value, "speed", number(message, "speed"));
    put(value, "heading", number(message, "heading"));
    put(value, "battery", number(message, "battery"));
    put(value, "fuel", number(message, "fuel"));
    put(value, "odometer", number(message, "odometer"));
    put(value, "engineHours", number(message, "engineHours"));
    put(value, "status", text(message, "status", "Telemetry received"));
    put(value, "topic", text(message, "topic", "dpw/fms/vehicles/" + vehicleId + "/telematics"));
    latest.put(vehicleId, value);
    return value;
  }

  public List<Map<String, Object>> latest() {
    return latest.values().stream().sorted(
        Comparator.comparing(value -> String.valueOf(value.get("vehicleId")))
    ).toList();
  }

  private static void put(Map<String, Object> target, String key, Object value) {
    if (value != null && !String.valueOf(value).isBlank()) {
      target.put(key, value);
    }
  }

  private static String text(JsonNode node, String field, String fallback) {
    return node == null || node.path(field).isMissingNode() ? fallback : node.path(field).asText(
        fallback
    );
  }

  private static Object number(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.path(field);
    return value == null || !value.isNumber() ? null : value.numberValue();
  }
}
