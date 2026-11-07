package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class BidirectionalDijkstrasAlgorithmSuite extends AnyFunSuite {

  /** 0 -> 1 (1), 1 -> 2 (2), 0 -> 2 (5); shortest 0->2 = 3 */
  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 5.0)
    )
  )

  test("finds the triangle shortest path") {
    assert(new BidirectionalDijkstrasAlgorithm(triangle).getShortestPath(0, 2)
      .contains(Path(3.0, List(0, 1, 2))))
  }

  test("unreachable destination returns None") {
    val g = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    assert(new BidirectionalDijkstrasAlgorithm(g).getShortestPath(0, 1).isEmpty)
  }

  test("source equals destination costs 0") {
    assert(new BidirectionalDijkstrasAlgorithm(DirectedGraph(1, IndexedSeq.empty))
      .getShortestPath(0, 0).contains(Path(0.0, List(0))))
  }

  test("picks the cheaper of two parallel edges") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )
    assert(new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(0, 1)
      .contains(Path(1.0, List(0, 1))))
  }

  test("zero-weight edges are allowed") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(DirectedEdge(0, 1, 0.0), DirectedEdge(1, 2, 0.0))
    )
    assert(new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(0, 2)
      .contains(Path(0.0, List(0, 1, 2))))
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
    val alg = new BidirectionalDijkstrasAlgorithm(graph)
    assert(alg.getShortestPath(0, 1).contains(Path(14.0, List(0, 2, 3, 1))))
    assert(alg.getShortestPath(0, 2).contains(Path(1.0, List(0, 2))))
    assert(alg.getShortestPath(0, 3).contains(Path(8.0, List(0, 2, 3))))
  }

  test("stops before settling cheap dead-end branches once μ is known") {
    val deadEnds = (0 until 10).map(i => DirectedEdge(0, 2 + i, 0.1))
    val graph = DirectedGraph(
      12,
      (DirectedEdge(0, 1, 1.0) +: deadEnds)
    )
    val observer = SearchObserver.recording()
    val path = new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(0, 1, observer)
    assert(path.contains(Path(1.0, List(0, 1))))
    val settled = observer.trace.events.collect {
      case SearchEvent.Settled(v, _) => v
    }.toSet
    assert(!settled.contains(2))
    assert(observer.trace.events.exists {
      case SearchEvent.Settled(_, SearchDirection.Backward) => true
      case _ => false
    })
  }

  test("optimal meet is not the first vertex reached by both searches") {
    val graph = DirectedGraph(
      4,
      IndexedSeq(
        DirectedEdge(0, 2, 1.0),
        DirectedEdge(2, 1, 100.0),
        DirectedEdge(0, 3, 10.0),
        DirectedEdge(3, 1, 10.0)
      )
    )
    assert(new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(0, 1)
      .contains(Path(20.0, List(0, 3, 1))))
  }
}
