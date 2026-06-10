// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * Vehicle monitoring and safe action bridge.
 *
 * @author Zishan Khan
 */
public class WebVehicleService {
  private final KernelHttpClient client;

  public WebVehicleService(KernelHttpClient client) {
    this.client = client;
  }

  public JsonNode fetch()
      throws IOException,
        InterruptedException {
    return client.get("/vehicles");
  }

  public JsonNode pause(String name, boolean paused)
      throws IOException,
        InterruptedException {
    return client.sendJson(
        "PUT", "/vehicles/" + KernelHttpClient.encode(name) + "/paused", String.valueOf(paused)
    );
  }

  public JsonNode withdraw(String name)
      throws IOException,
        InterruptedException {
    return client.sendJson(
        "POST", "/vehicles/" + KernelHttpClient.encode(name) + "/withdrawal", "{}"
    );
  }
}
