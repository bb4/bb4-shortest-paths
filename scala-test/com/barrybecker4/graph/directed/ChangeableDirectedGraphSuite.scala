package com.barrybecker4.graph.directed

import org.scalatest.funsuite.AnyFunSuite

class ChangeableDirectedGraphSuite extends AnyFunSuite {

  private val base = DirectedGraph(
    4,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 1.0),
      DirectedEdge(0, 2, 10.0)
    )
  )

  test("outgoing neighbors hide removed edge") {
    val g = ChangeableDirectedGraph(base)
    g.remove(0, 1)
    assert(!g.outgoingNeighborsOf(0).exists(_.destination == 1))
    assert(g.outgoingNeighborsOf(0).exists(_.destination == 2))
  }

  test("recover edge restores neighbors") {
    val g = ChangeableDirectedGraph(base)
    g.remove(0, 1)
    g.recover(0, 1)
    assert(g.outgoingNeighborsOf(0).exists(e => e.destination == 1 && e.weight == 1.0))
  }

  test("recover() clears all removals") {
    val g = ChangeableDirectedGraph(base)
    g.remove(1)
    g.remove(0, 2)
    g.recover()
    assert(g.outgoingNeighborsOf(1) == base.outgoingNeighborsOf(1))
  }

  test("findOrigEdge throws when edge missing") {
    val g = ChangeableDirectedGraph(base)
    intercept[IllegalStateException] {
      g.findOrigEdge(2, 0)
    }
  }

  test("remove drops only the lighter parallel edge") {
    val g = ChangeableDirectedGraph(
      DirectedGraph(
        2,
        IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
      )
    )
    g.remove(0, 1)
    assert(g.outgoingNeighborsOf(0) == IndexedSeq(DirectedEdge(0, 1, 5.0)))
  }

  test("one remove hides every equal parallel edge and recover restores them") {
    val edges = IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(0, 1, 1.0))
    val g = ChangeableDirectedGraph(DirectedGraph(2, edges))
    g.remove(0, 1)
    assert(g.outgoingNeighborsOf(0).isEmpty)
    g.recover(0, 1)
    assert(g.outgoingNeighborsOf(0) == edges)
  }

  test("findOrigEdge returns the lighter parallel edge") {
    val g = ChangeableDirectedGraph(
      DirectedGraph(
        2,
        IndexedSeq(
          DirectedEdge(0, 1, 9.0),
          DirectedEdge(0, 1, 2.0),
          DirectedEdge(0, 1, 4.0)
        )
      )
    )

    assert(g.findOrigEdge(0, 1) == DirectedEdge(0, 1, 2.0))
  }
}
