package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class DijkstrasAlgorithmSuite extends AnyFunSuite {

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
    val sp = DijkstrasAlgorithm(triangle).findShortestPaths(0)
    val p = sp.pathToVertex(2)
    assert(p.weight == 3.0)
    assert(p.nodes == List(0, 1, 2))
  }

  test("findShortestPaths honors a non-zero source") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(1, 2, 1.0))
    )
    val sp = DijkstrasAlgorithm(graph).findShortestPaths(1)
    assert(sp.pathToVertex(1) == Path(0.0, List(1)))
    assert(sp.pathToVertex(2) == Path(1.0, List(1, 2)))
    assert(sp.pathToVertex(0).weight.isPosInfinity)
    assert(sp.pathToVertex(0).nodes.isEmpty)
  }

  test("unreachable vertex has infinite cost and an empty node list") {
    val graph = DirectedGraph(3, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val sp = DijkstrasAlgorithm(graph).findShortestPaths(0)
    assert(sp.pathToVertex(2).weight.isPosInfinity)
    assert(sp.pathToVertex(2).nodes.isEmpty)
    assert(sp.pathToVertex(1) == Path(1.0, List(0, 1)))
  }

  test("single-vertex graph costs 0") {
    val sp = DijkstrasAlgorithm(DirectedGraph(1, IndexedSeq.empty)).findShortestPaths(0)
    assert(sp.pathToVertex(0) == Path(0.0, List(0)))
  }

  test("graph with no edges costs 0 only at the source") {
    val sp = DijkstrasAlgorithm(DirectedGraph(3, IndexedSeq.empty)).findShortestPaths(0)
    assert(sp.pathToVertex(0) == Path(0.0, List(0)))
    assert(sp.pathToVertex(1).weight.isPosInfinity)
    assert(sp.pathToVertex(2).weight.isPosInfinity)
  }

  test("picks the cheaper of two parallel edges") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )
    assert(DijkstrasAlgorithm(graph).findShortestPaths(0).pathToVertex(1) == Path(1.0, List(0, 1)))
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
    assert(DijkstrasAlgorithm(graph).findShortestPaths(0).pathToVertex(2) == Path(0.0, List(0, 1, 2)))
  }

  test("holes in 0 .. n-1 do not change the cost of the used vertices") {
    val graph = DirectedGraph(
      5,
      IndexedSeq(DirectedEdge(0, 2, 1.5), DirectedEdge(2, 4, 2.5))
    )
    val sp = DijkstrasAlgorithm(graph).findShortestPaths(0)
    assert(sp.pathToVertex(4) == Path(4.0, List(0, 2, 4)))
    assert(sp.pathToVertex(1).weight.isPosInfinity)
    assert(sp.pathToVertex(3).weight.isPosInfinity)
  }

  test("rejects a source outside 0 .. n-1") {
    val graph = DirectedGraph(3, IndexedSeq.empty)
    assertThrows[IllegalArgumentException] {
      DijkstrasAlgorithm(graph).findShortestPaths(3)
    }
  }
}
