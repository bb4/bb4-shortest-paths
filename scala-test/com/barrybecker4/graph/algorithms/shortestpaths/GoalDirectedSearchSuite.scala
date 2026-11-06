package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class GoalDirectedSearchSuite extends AnyFunSuite {

  private val zero: Int => Double = _ => 0.0

  /** 0 -> 1 (1), 1 -> 2 (2), 0 -> 2 (5); shortest 0->2 = 3 */
  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 5.0)
    )
  )

  test("zero potential finds the triangle shortest path") {
    assert(GoalDirectedSearch.search(triangle, 0, 2, zero).contains(Path(3.0, List(0, 1, 2))))
  }

  test("unreachable destination returns None") {
    val g = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    assert(GoalDirectedSearch.search(g, 0, 1, zero).isEmpty)
  }

  test("source equals destination costs 0") {
    assert(GoalDirectedSearch.search(DirectedGraph(1, IndexedSeq.empty), 0, 0, zero)
      .contains(Path(0.0, List(0))))
  }

  test("picks the cheaper of two parallel edges") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )
    assert(GoalDirectedSearch.search(graph, 0, 1, zero).contains(Path(1.0, List(0, 1))))
  }

  test("zero-weight edges are allowed") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(DirectedEdge(0, 1, 0.0), DirectedEdge(1, 2, 0.0))
    )
    assert(GoalDirectedSearch.search(graph, 0, 2, zero).contains(Path(0.0, List(0, 1, 2))))
  }

  test("decrease-key keeps the shorter path when the same vertex is enqueued twice") {
    val graph = DirectedGraph(
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
    assert(GoalDirectedSearch.search(graph, 0, 1, zero).contains(Path(14.0, List(0, 2, 3, 1))))
    assert(GoalDirectedSearch.search(graph, 0, 2, zero).contains(Path(1.0, List(0, 2))))
    assert(GoalDirectedSearch.search(graph, 0, 3, zero).contains(Path(8.0, List(0, 2, 3))))
  }
}
