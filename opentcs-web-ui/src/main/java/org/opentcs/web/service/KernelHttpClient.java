// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.opentcs.web.config.WebUiConfig;

/**
 * HTTP bridge from the JSP frontend to the openTCS service web API.
 *
 * @author Zishan Khan
 */
public class KernelHttpClient {
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
      .build();
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final String baseUrl;
  private final String accessKey;

  public KernelHttpClient() {
    this(WebUiConfig.kernelApiBaseUrl(), WebUiConfig.accessKey());
  }

  public KernelHttpClient(String baseUrl, String accessKey) {
    this.baseUrl = baseUrl.replaceAll("/+$", "");
    this.accessKey = accessKey;
  }

  /** Performs a GET and parses JSON. */
  public JsonNode get(String path)
      throws IOException,
        InterruptedException {
    return send(request(path).GET().build());
  }

  /** Sends JSON with the requested method and parses JSON when present. */
  public JsonNode sendJson(String method, String path, String json)
      throws IOException,
        InterruptedException {
    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(json == null ? "" : json);
    return send(
        request(path).header("Content-Type", "application/json").method(method, body).build()
    );
  }

  /** URL-encodes a path segment. */
  public static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private HttpRequest.Builder request(String path) {
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
        .timeout(Duration.ofSeconds(8))
        .header("Accept", "application/json");
    if (!accessKey.isBlank()) {
      builder.header("X-Api-Access-Key", accessKey);
    }
    return builder;
  }

  private JsonNode send(HttpRequest request)
      throws IOException,
        InterruptedException {
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException(
          "Kernel API returned HTTP " + response.statusCode() + ": " + response.body()
      );
    }
    return response.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(response.body());
  }
}
