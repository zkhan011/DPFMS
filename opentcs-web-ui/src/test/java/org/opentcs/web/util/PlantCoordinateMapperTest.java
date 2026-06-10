// SPDX-FileCopyrightText: Zishan Khan
// SPDX-License-Identifier: MIT
package org.opentcs.web.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import org.opentcs.web.config.MapConfig;
import org.opentcs.web.config.MapProviderType;

/**
 * Tests coordinate calibration for the map view.
 *
 * @author Zishan Khan
 */
class PlantCoordinateMapperTest {
  @Test
  void mapsOriginAndScale() {
    var mapper = new PlantCoordinateMapper(config(50.0, 8.0, 2.0, 0.0));
    var mapped = mapper.map(10.0, 0.0).orElseThrow();
    assertThat(mapped.latitude()).isEqualTo(50.0);
    assertThat(mapped.longitude()).isGreaterThan(8.0);
  }

  @Test
  void appliesRotation() {
    var mapper = new PlantCoordinateMapper(config(50.0, 8.0, 1.0, 90.0));
    var mapped = mapper.map(10.0, 0.0).orElseThrow();
    assertThat(mapped.latitude()).isGreaterThan(50.0);
    assertThat(mapped.longitude()).isCloseTo(8.0, within(0.0000001));
  }

  @Test
  void usesDirectLatitudeAndLongitude() {
    var mapper = new PlantCoordinateMapper(config(50.0, 8.0, 1.0, 0.0));
    var mapped = mapper.map(1000, 1000, 51.0, 9.0).orElseThrow();
    assertThat(mapped.latitude()).isEqualTo(51.0);
    assertThat(mapped.longitude()).isEqualTo(9.0);
  }

  @Test
  void missingOriginReturnsEmpty() {
    var config = new MapConfig(
        MapProviderType.OSM, "", OptionalDouble.empty(),
        OptionalDouble.empty(), 1.0, 0.0, 18
    );
    assertThat(new PlantCoordinateMapper(config).map(1.0, 1.0)).isEmpty();
  }

  private MapConfig config(double latitude, double longitude, double scale, double rotation) {
    return new MapConfig(
        MapProviderType.OSM, "", OptionalDouble.of(latitude),
        OptionalDouble.of(longitude), scale, rotation, 18
    );
  }

  private org.assertj.core.data.Offset<Double> within(double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
