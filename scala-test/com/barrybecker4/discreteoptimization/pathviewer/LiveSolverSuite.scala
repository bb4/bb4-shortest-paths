package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.pathviewer.AlgorithmKind.*
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class LiveSolverSuite extends AnyFunSuite {

  /** 0→1 (1), 1→2 (2), 0→2 (4). Shortest 0→2 is 3 via 1. */
  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("maxVertexId is the larger of numVertices-1 and any edge endpoint") {
    assert(LiveSolver.maxVertexId(triangle) == 2)
    val sparse = DirectedGraph(1, IndexedSeq(DirectedEdge(0, 4, 1.0)))
    assert(LiveSolver.maxVertexId(sparse) == 4)
  }

  test("Dijkstra from 0 returns a path of weight 3 to vertex 2") {
    val result = LiveSolver.solve(triangle, SolveRequest(Dijkstra, 0, 2, 3))
    result match {
      case SolveResult.ToAll(solution, _) =>
        val to2 = solution.paths.find(_.lastNode == 2).get
        assert(to2 == Path(3.0, List(0, 1, 2)))
      case other => fail(s"expected ToAll, got $other")
    }
  }

  test("Modified Dijkstra returns only the s-t path and a non-empty trace") {
    val result = LiveSolver.solve(triangle, SolveRequest(ModifiedDijkstra, 0, 2, 3))
    result match {
      case SolveResult.OnePath(path, trace) =>
        assert(path == Path(3.0, List(0, 1, 2)))
        assert(trace.events.collect { case com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.Settled(v) => v } ==
          Vector(0, 1, 2))
      case other => fail(s"expected OnePath, got $other")
    }
  }

  test("Dijkstra solve keeps a trace whose settled order is 0, 1, 2") {
    val result = LiveSolver.solve(triangle, SolveRequest(Dijkstra, 0, 2, 3))
    result match {
      case SolveResult.ToAll(_, trace) =>
        assert(trace.events.collect { case com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.Settled(v) => v } ==
          Vector(0, 1, 2))
      case other => fail(s"expected ToAll, got $other")
    }
  }

  test("Yen returns two loopless 0-to-2 paths, cheapest first") {
    val result = LiveSolver.solve(triangle, SolveRequest(Yen, 0, 2, 2))
    result match {
      case SolveResult.KPaths(solution) =>
        assert(solution.shortestPaths.map(_.weight) == Seq(3.0, 4.0))
        assert(solution.shortestPaths.head.nodes == List(0, 1, 2))
      case other => fail(s"expected KPaths, got $other")
    }
  }

  test("rejects a target outside the graph, Yen with source == target, and k outside 1..10") {
    assert(LiveSolver.validate(triangle, SolveRequest(Dijkstra, 0, 9, 3)) ==
      Left("Target must be from 0 to 2"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 1, 1, 3)) ==
      Left("Yen needs different source and target"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 0, 2, 0)) ==
      Left("k must be from 1 to 10"))
    assert(LiveSolver.validate(triangle, SolveRequest(Yen, 0, 2, 11)) ==
      Left("k must be from 1 to 10"))
  }

  test("Modified Dijkstra with no path returns the empty path") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val result = LiveSolver.solve(graph, SolveRequest(ModifiedDijkstra, 1, 0, 3))
    result match {
      case SolveResult.OnePath(path, _) => assert(path == Path.EMPTY_PATH)
      case other => fail(s"expected OnePath, got $other")
    }
  }
}
