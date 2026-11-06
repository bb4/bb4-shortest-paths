package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class AltAlgorithmSuite extends AnyFunSuite {

  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 5.0)
    )
  )

  private val decreaseKeyGraph = DirectedGraph(
    4,
    IndexedSeq(
      DirectedEdge(3, 2, 8),
      DirectedEdge(2, 1, 14),
      DirectedEdge(1, 0, 4),
      DirectedEdge(0, 1, 16),
      DirectedEdge(0, 3, 19),
      DirectedEdge(2, 3, 7),
      DirectedEdge(3, 1, 6),
      DirectedEdge(0, 2, 1)
    )
  )

  test("finds the triangle shortest path") {
    assert(new AltAlgorithm(triangle).getShortestPath(0, 2).contains(Path(3.0, List(0, 1, 2))))
  }

  test("decrease-key graph keeps cost 14") {
    assert(new AltAlgorithm(decreaseKeyGraph).getShortestPath(0, 1)
      .contains(Path(14.0, List(0, 2, 3, 1))))
  }

  test("unreachable destination returns None") {
    val g = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    assert(new AltAlgorithm(g).getShortestPath(0, 1).isEmpty)
  }

  test("source equals destination costs 0") {
    assert(new AltAlgorithm(DirectedGraph(1, IndexedSeq.empty)).getShortestPath(0, 0)
      .contains(Path(0.0, List(0))))
  }

  test("reuses landmark labels across queries on the same instance") {
    val alg = new AltAlgorithm(triangle)
    assert(alg.getShortestPath(0, 2).contains(Path(3.0, List(0, 1, 2))))
    assert(alg.getShortestPath(0, 1).contains(Path(1.0, List(0, 1))))
  }

  test("settled set is no larger than zero-potential search") {
    val observerAlt = SearchObserver.recording()
    val observerZero = SearchObserver.recording()
    assert(new AltAlgorithm(triangle).getShortestPath(0, 2, observerAlt)
      .contains(Path(3.0, List(0, 1, 2))))
    assert(GoalDirectedSearch.search(triangle, 0, 2, Potential.Zero, observerZero)
      .contains(Path(3.0, List(0, 1, 2))))
    val settledAlt = observerAlt.trace.events.count {
      case SearchEvent.Settled(_, _) => true
      case _ => false
    }
    val settledZero = observerZero.trace.events.count {
      case SearchEvent.Settled(_, _) => true
      case _ => false
    }
    assert(settledAlt <= settledZero)
  }
}
