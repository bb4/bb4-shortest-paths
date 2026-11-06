package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.GraphTstUtil
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class BellmanFordAlgorithmSuite extends AnyFunSuite {

  /** 0->1 (1), 1->2 (2), 0->2 (4): shortest 0->2 is 3 via node 1. */
  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("prefers cheaper two-hop path over direct edge") {
    val sp = BellmanFordAlgorithm(triangle).findShortestPaths(0)
    val p = sp.pathToVertex(2)
    assert(p.weight == 3.0)
    assert(p.nodes == List(0, 1, 2))
  }

  test("distances match Dijkstra on tiny_graph_01") {
    val graph = GraphTstUtil.getGraph("tiny_graph_01")
    val bf = BellmanFordAlgorithm(graph).findShortestPaths(0)
    val dj = DijkstrasAlgorithm(graph).findShortestPaths(0)
    for (v <- 0 until graph.numVertices) {
      assert(bf.distToVertex(v) == dj.distToVertex(v), s"vertex $v")
    }
  }

  test("unreachable vertex has infinite cost and an empty node list") {
    val graph = DirectedGraph(3, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val sp = BellmanFordAlgorithm(graph).findShortestPaths(0)
    assert(sp.pathToVertex(2).weight.isPosInfinity)
    assert(sp.pathToVertex(2).nodes.isEmpty)
    assert(sp.pathToVertex(1) == Path(1.0, List(0, 1)))
  }

  test("zero-weight edges are allowed") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 0.0),
        DirectedEdge(1, 2, 0.0),
        DirectedEdge(0, 2, 5.0)
      )
    )
    assert(BellmanFordAlgorithm(graph).findShortestPaths(0).pathToVertex(2) == Path(0.0, List(0, 1, 2)))
  }

  test("rejects a source outside 0 .. n-1") {
    val graph = DirectedGraph(3, IndexedSeq.empty)
    assertThrows[IllegalArgumentException] {
      BellmanFordAlgorithm(graph).find(3)
    }
    assertThrows[IllegalArgumentException] {
      BellmanFordAlgorithm(graph).find(-1)
    }
  }

  test("zero-weight cycle stays Paths") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 1.0),
        DirectedEdge(1, 2, 1.0),
        DirectedEdge(2, 1, -1.0)
      )
    )
    BellmanFordAlgorithm(graph).find(0) match {
      case BellmanFordResult.Paths(paths) =>
        assert(paths.pathToVertex(1) == Path(1.0, List(0, 1)))
        assert(paths.pathToVertex(2) == Path(2.0, List(0, 1, 2)))
      case BellmanFordResult.Cycle(cycle) =>
        fail(s"unexpected cycle $cycle")
    }
  }

  test("negative cycle not reachable from the source stays Paths") {
    val graph = DirectedGraph(
      4,
      IndexedSeq(
        DirectedEdge(0, 1, 1.0),
        DirectedEdge(2, 3, 1.0),
        DirectedEdge(3, 2, -3.0)
      )
    )
    BellmanFordAlgorithm(graph).find(0) match {
      case BellmanFordResult.Paths(paths) =>
        assert(paths.pathToVertex(1) == Path(1.0, List(0, 1)))
        assert(paths.pathToVertex(2).weight.isPosInfinity)
        assert(paths.pathToVertex(3).weight.isPosInfinity)
      case BellmanFordResult.Cycle(cycle) =>
        fail(s"unexpected cycle $cycle")
    }
  }

  test("reachable negative cycle is reported canonically") {
    val graph = GraphTstUtil.getGraph("negative_cycle_01")
    BellmanFordAlgorithm(graph).find(0) match {
      case BellmanFordResult.Cycle(cycle) =>
        assert(cycle.vertices == List(1, 2, 1))
        assert(cycle.weight == -2.0)
      case BellmanFordResult.Paths(_) =>
        fail("expected a negative cycle")
    }
  }

  test("negative self-loop is a NegativeCycle") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(1, 1, -1.0)))
    BellmanFordAlgorithm(graph).find(0) match {
      case BellmanFordResult.Cycle(cycle) =>
        assert(cycle.vertices == List(1, 1))
        assert(cycle.weight == -1.0)
      case BellmanFordResult.Paths(_) =>
        fail("expected a negative cycle")
    }
  }

  test("findShortestPaths throws when a negative cycle is reachable") {
    val graph = GraphTstUtil.getGraph("negative_cycle_01")
    val thrown = intercept[IllegalStateException] {
      BellmanFordAlgorithm(graph).findShortestPaths(0)
    }
    assert(thrown.getMessage.contains("Negative cycle"))
  }

  test("negative_edges_01 paths match the planned distances") {
    val graph = GraphTstUtil.getGraph("negative_edges_01")
    val sp = BellmanFordAlgorithm(graph).findShortestPaths(0)
    assert(sp.pathToVertex(0) == Path(0.0, List(0)))
    assert(sp.pathToVertex(1) == Path(2.0, List(0, 1)))
    assert(sp.pathToVertex(2) == Path(-1.0, List(0, 1, 2)))
    assert(sp.pathToVertex(3) == Path(0.0, List(0, 1, 2, 3)))
  }
}
