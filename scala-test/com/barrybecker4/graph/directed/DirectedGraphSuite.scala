package com.barrybecker4.graph.directed

import org.scalatest.funsuite.AnyFunSuite

class DirectedGraphSuite extends AnyFunSuite {

  test("keeps parallel edges that share a weight, in input order") {
    val edges = IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(0, 1, 1.0)
    )
    val graph = DirectedGraph(2, edges)

    assert(graph.outgoingNeighborsOf(0) == edges)
    assert(graph.incomingNeighborsOf(1) == edges)
  }

  test("stores neighbors when vertex ids are outside 0 .. n-1") {
    val edges = IndexedSeq(DirectedEdge(10, 20, 1.5))
    val graph = DirectedGraph(2, edges)
    assert(graph.outgoingNeighborsOf(10) == edges)
    assert(graph.incomingNeighborsOf(20) == edges)
  }

  test("findMinWeightEdge returns the lighter parallel edge") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )

    assert(graph.findMinWeightEdge(0, 1) == DirectedEdge(0, 1, 1.0))
  }
}
