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
