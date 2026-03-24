package com.barrybecker4.graph.algorithms.shortestpaths

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
}
