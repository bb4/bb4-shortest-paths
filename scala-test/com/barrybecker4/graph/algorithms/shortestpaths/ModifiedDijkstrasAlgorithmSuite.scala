package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{ChangeableDirectedGraph, DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class ModifiedDijkstrasAlgorithmSuite extends AnyFunSuite {

  /** 0 -> 1 (1), 1 -> 2 (2), 0 -> 2 (5); shortest 0->2 = 3 */
  private val simple = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1),
      DirectedEdge(1, 2, 2),
      DirectedEdge(0, 2, 5)
    )
  )

  test("getShortestPath returns shortest route") {
    val alg = ModifiedDijkstrasAlgorithm(simple)
    assert(alg.getShortestPath(0, 2).contains(Path(3.0, List(0, 1, 2))))
  }

  test("getShortestPath returns None when unreachable") {
    val g = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    val alg = ModifiedDijkstrasAlgorithm(g)
    assert(alg.getShortestPath(0, 1).isEmpty)
  }

  test("getShortestPath honors a non-zero source") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(1, 2, 1.0))
    )
    val alg = ModifiedDijkstrasAlgorithm(graph)
    assert(alg.getShortestPath(1, 2).contains(Path(1.0, List(1, 2))))
    assert(alg.getShortestPath(1, 0).isEmpty)
  }

  test("single-vertex graph costs 0") {
    val alg = ModifiedDijkstrasAlgorithm(DirectedGraph(1, IndexedSeq.empty))
    assert(alg.getShortestPath(0, 0).contains(Path(0.0, List(0))))
  }

  test("picks the cheaper of two parallel edges") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )
    assert(ModifiedDijkstrasAlgorithm(graph).getShortestPath(0, 1).contains(Path(1.0, List(0, 1))))
  }

  test("zero-weight edges are allowed") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(DirectedEdge(0, 1, 0.0), DirectedEdge(1, 2, 0.0))
    )
    assert(ModifiedDijkstrasAlgorithm(graph).getShortestPath(0, 2).contains(Path(0.0, List(0, 1, 2))))
  }

  test("flower rooted at target records distances toward root") {
    val alg = ModifiedDijkstrasAlgorithm(simple)
    alg.findShortestPathFlowerRootAt(2)
    assert(alg.getStartVertexDistance(2) == 0.0)
    assert(alg.getStartVertexDistance(0) == 3.0)
  }

  test("correctCostBackward relaxes incoming chain") {
    val g = DirectedGraph(3, IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(1, 2, 4.0)))
    val alg = ModifiedDijkstrasAlgorithm(g)
    alg.findShortestPathFlowerRootAt(2)
    alg.setStartVertexDistance(2, 0.0)
    alg.correctCostBackward(2)
    assert(alg.getStartVertexDistance(1) == 4.0)
    alg.correctCostBackward(1)
    assert(alg.getStartVertexDistance(0) == 5.0)
  }
}
