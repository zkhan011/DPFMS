// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.opentcs.web.config.FmsConfig;

/** Same-origin, read-only proxy for the internal MBTiles service. */
public class MapTileController
    extends
      HttpServlet {
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
      .build();

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    FmsConfig config = FmsConfig.load();
    if (!config.offlineEnabled()) {
      response.sendError(404, "Offline map is disabled.");
      return;
    }
    String suffix = request.getServletPath().endsWith("/metadata") ? "/metadata" : request
        .getPathInfo();
    try {
      HttpResponse<byte[]> upstream = client.send(
          HttpRequest.newBuilder(URI.create(config.tileServiceUrl() + suffix)).timeout(
              Duration.ofSeconds(5)
          ).GET().build(),
          HttpResponse.BodyHandlers.ofByteArray()
      );
      response.setStatus(upstream.statusCode());
      upstream.headers().firstValue("content-type").ifPresent(response::setContentType);
      upstream.headers().firstValue("content-encoding").ifPresent(
          value -> response.setHeader("Content-Encoding", value)
      );
      response.setHeader(
          "Cache-Control", upstream.headers().firstValue("cache-control").orElse("no-store")
      );
      response.setContentLength(upstream.body().length);
      response.getOutputStream().write(upstream.body());
    }
    catch (InterruptedException exc) {
      Thread.currentThread().interrupt();
      response.sendError(503, "Offline tile request interrupted.");
    }
    catch (IllegalArgumentException exc) {
      response.sendError(500, "Offline tile service URL is invalid.");
    }
  }
}
