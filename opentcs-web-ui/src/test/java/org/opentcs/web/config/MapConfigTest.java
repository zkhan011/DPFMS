// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;

/**
 * Tests safe map provider selection.
 *
 * @author Zishan Khan
 */
class MapConfigTest {
  @Test
  void invalidProviderFallsBackToOsm() {
    assertThat(MapProviderType.from("invalid")).isEqualTo(MapProviderType.OSM);
  }

  @Test
  void googleWithoutKeyFallsBackToOsm() {
    var config = new MapConfig(
        MapProviderType.GOOGLE, "", OptionalDouble.of(1),
        OptionalDouble.of(2), 1, 0, 18
    );
    assertThat(config.effectiveProvider()).isEqualTo(MapProviderType.OSM);
    assertThat(config.warning()).contains("falling back");
  }

  @Test
  void googleWithKeyRemainsSelected() {
    var config = new MapConfig(
        MapProviderType.GOOGLE, "configured-at-runtime",
        OptionalDouble.of(1), OptionalDouble.of(2), 1, 0, 18
    );
    assertThat(config.effectiveProvider()).isEqualTo(MapProviderType.GOOGLE);
  }
}
