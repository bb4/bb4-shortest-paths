package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.graph.GraphTstUtil
import com.barrybecker4.graph.algorithms.shortestpaths.NegativeCycle

class BellmanFordPathSolverSuite extends BaseSolverSuite {

  test("negative_edges_01") {
    verify("negative_edges_01")
  }

  test("findPathsOrCycle reports the cycle on negative_cycle_01") {
    val graph = GraphTstUtil.getGraph("negative_cycle_01")
    new BellmanFordPathSolver().findPathsOrCycle(graph, 0) match {
      case Left(cycle) =>
        assert(cycle == NegativeCycle(List(1, 2, 1), -2.0))
      case Right(solution) =>
        fail(s"expected a negative cycle, got $solution")
    }
  }

  test("findPaths throws on negative_cycle_01") {
    val graph = GraphTstUtil.getGraph("negative_cycle_01")
    intercept[IllegalStateException] {
      new BellmanFordPathSolver().findPaths(graph, 0)
    }
  }

  override def createSolver(): ShortestPathsSolver = BellmanFordPathSolver()

  override def solverName(): String = BellmanFordPathSolver.BASE_NAME
}
