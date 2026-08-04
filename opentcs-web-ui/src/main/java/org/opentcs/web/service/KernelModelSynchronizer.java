// SPDX-FileCopyrightText: DPW FMS Contributors
// SPDX-License-Identifier: MIT
package org.opentcs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.opentcs.web.config.FmsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Idempotently installs the demo topology in an empty kernel for routing and dispatching. */
public class KernelModelSynchronizer
    implements
      ServletContextListener {
  private static final Logger LOG = LoggerFactory.getLogger(KernelModelSynchronizer.class);
  private static final String MODEL_NAME = "DPW-FMS-JEBEL-ALI-V1";
  private static final int MAX_ATTEMPTS = 30;
  private ScheduledExecutorService executor;
  private int attempts;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    FmsConfig config = FmsConfig.load();
    if (!config.mockEnabled() || !config.kernelModelSyncEnabled()) return;
    executor = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    executor.scheduleWithFixedDelay(this::synchronize, 1, 2, TimeUnit.SECONDS);
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (executor != null) executor.shutdownNow();
  }

  private void synchronize() {
    if (++attempts > MAX_ATTEMPTS) {
      LOG.error("DPW FMS kernel model synchronization timed out after {} attempts.", MAX_ATTEMPTS);
      executor.shutdown();
      return;
    }
    try {
      KernelHttpClient client = new KernelHttpClient();
      JsonNode existing = client.get("/plantModel");
      String name = existing.path("name").asText("");
      if (MODEL_NAME.equals(name)) {
        LOG.info("DPW FMS kernel model is already installed; synchronization is idempotent.");
        executor.shutdown();
        return;
      }
      if (!name.isBlank() && !"unnamed".equalsIgnoreCase(name)
          && existing.path("points").size() > 0) {
        LOG.warn("Kernel already contains model '{}'; DPW FMS will not overwrite it.", name);
        executor.shutdown();
        return;
      }
      client.sendJson("PUT", "/plantModel", modelJson());
      client.sendJson("POST", "/plantModel/topologyUpdateRequest", "{\"paths\":[]}");
      JsonNode installed = client.get("/plantModel");
      if (!MODEL_NAME.equals(installed.path("name").asText())) {
        throw new IOException("Kernel did not return the synchronized model.");
      }
      LOG.info(
          "Installed persistent DPW FMS topology with {} points and {} paths.",
          installed.path("points").size(), installed.path("paths").size()
      );
      executor.shutdown();
    }
    catch (Exception exc) {
      LOG.info(
          "Waiting for kernel model synchronization (attempt {}): {}", attempts, exc.getMessage()
      );
    }
  }

  private String modelJson()
      throws IOException {
    try (InputStream stream = getClass().getResourceAsStream(
        "/kernel-model/dpw-fms-plant-model.json"
    )) {
      if (stream == null) throw new IOException("Bundled kernel model is missing.");
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
