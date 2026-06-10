// SPDX-FileCopyrightText: The openTCS Authors
// SPDX-License-Identifier: MIT
package org.opentcs.strategies.basic.routing.jgrapht;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import java.util.Map;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.opentcs.components.kernel.routing.Edge;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Triple;
import org.opentcs.strategies.basic.routing.PointRouter;

/**
 * Creates {@link PointRouter} instances based on the A* algorithm.
 *
 * <p>Custom A* routing addition by Zishan Khan.</p>
 *
 * @author Zishan Khan
 */
public class AStarPointRouterFactory
    extends
      AbstractPointRouterFactory {

  private final boolean useEuclideanHeuristic;

  /**
   * Creates a new instance.
   *
   * @param graphProvider Provides routing graphs for vehicles.
   * @param configuration The shortest-path configuration.
   */
  @Inject
  public AStarPointRouterFactory(
      @Nonnull
      GraphProvider graphProvider,
      ShortestPathConfiguration configuration
  ) {
    super(graphProvider);
    this.useEuclideanHeuristic = configuration.edgeEvaluators().equals(
        java.util.List.of("DISTANCE")
    );
  }

  @Override
  protected ShortestPathAlgorithm<Vertex, Edge> createShortestPathAlgorithm(
      Graph<Vertex, Edge> graph,
      Map<String, Point> points
  ) {
    AStarAdmissibleHeuristic<Vertex> heuristic = (source, target) -> {
      if (!useEuclideanHeuristic) {
        return 0.0;
      }
      Point sourcePoint = points.get(source.getPoint().getName());
      Point targetPoint = points.get(target.getPoint().getName());
      if (sourcePoint == null || targetPoint == null) {
        return 0.0;
      }
      Triple sourcePosition = sourcePoint.getPose().getPosition();
      Triple targetPosition = targetPoint.getPose().getPosition();
      double deltaX = sourcePosition.getX() - targetPosition.getX();
      double deltaY = sourcePosition.getY() - targetPosition.getY();
      return Math.hypot(deltaX, deltaY);
    };
    return new AStarShortestPath<>(graph, heuristic);
  }
}
