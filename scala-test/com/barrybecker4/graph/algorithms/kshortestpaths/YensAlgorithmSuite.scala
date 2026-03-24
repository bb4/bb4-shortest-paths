package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class YensAlgorithmSuite extends AnyFunSuite {

  /** Two distinct paths 0->3: 0-2-3 (cost 3) and 0-1-3 (cost 4). */
  private val twoPathGraph = DirectedGraph(
    4,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 3, 3.0),
      DirectedEdge(0, 2, 2.0),
      DirectedEdge(2, 3, 1.0)
    )
  )

  test("findKShortestPaths returns two loopless paths ordered by weight") {
    val paths = YensAlgorithm(twoPathGraph).findKShortestPaths(0, 3, 2)
    assert(paths.size == 2)
    assert(paths.map(_.weight) == Seq(3.0, 4.0))
    assert(paths.map(_.nodes) == Seq(List(0, 2, 3), List(0, 1, 3)))
  }

  test("findKShortestPaths returns fewer than k when not enough paths exist") {
    val line = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val paths = YensAlgorithm(line).findKShortestPaths(0, 1, 5)
    assert(paths.size == 1)
  }
}
