package com.barrybecker4.graph.algorithms.kshortestpaths

import com.barrybecker4.discreteoptimization.shortestpaths.solver.DijkstrasPathSolver
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class EppsteinsAlgorithmSuite extends AnyFunSuite {

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

  /** Cycle that admits non-simple walks cheaper than the direct edge. */
  private val cycleGraph = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 1.0),
      DirectedEdge(0, 2, 10.0),
      DirectedEdge(1, 0, 1.0)
    )
  )

  test("findKShortestPaths returns two walks ordered by weight on a DAG") {
    val paths = EppsteinsAlgorithm(twoPathGraph).findKShortestPaths(0, 3, 2)
    assert(paths.size == 2)
    assert(paths.map(_.weight) == Seq(3.0, 4.0))
    assert(paths.map(_.nodes) == Seq(List(0, 2, 3), List(0, 1, 3)))
  }

  test("k = 1 returns only the unique shortest path") {
    val paths = EppsteinsAlgorithm(twoPathGraph).findKShortestPaths(0, 3, 1)
    assert(paths.size == 1)
    assert(paths.head.weight == 3.0)
    assert(paths.head.nodes == List(0, 2, 3))
  }

  test("findKShortestPaths returns fewer than k when not enough walks exist") {
    val line = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val paths = EppsteinsAlgorithm(line).findKShortestPaths(0, 1, 5)
    assert(paths.size == 1)
    assert(paths.head == Path(1.0, List(0, 1)))
  }

  test("cycle graph returns non-simple walks that Yen omits") {
    // Fourth walk loops again (cost 8); the direct edge (cost 10) is fifth.
    val eppstein = EppsteinsAlgorithm(cycleGraph).findKShortestPaths(0, 2, 5)
    assert(eppstein.map(_.weight) == Seq(2.0, 4.0, 6.0, 8.0, 10.0))
    assert(eppstein.map(_.nodes) == Seq(
      List(0, 1, 2),
      List(0, 1, 0, 1, 2),
      List(0, 1, 0, 1, 0, 1, 2),
      List(0, 1, 0, 1, 0, 1, 0, 1, 2),
      List(0, 2)
    ))

    val yen = YensAlgorithm(cycleGraph).findKShortestPaths(0, 2, 5)
    assert(yen.map(_.weight) == Seq(2.0, 10.0))
    assert(yen.map(_.nodes) == Seq(List(0, 1, 2), List(0, 2)))
  }

  test("parallel edges are distinct walks") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(0, 1, 3.0))
    )
    val paths = EppsteinsAlgorithm(graph).findKShortestPaths(0, 1, 2)
    assert(paths.map(_.weight) == Seq(1.0, 3.0))
    assert(paths.map(_.nodes) == Seq(List(0, 1), List(0, 1)))
  }

  test("zero-weight self-loop yields equal-cost walks and finishes") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 1.0),
        DirectedEdge(1, 2, 1.0),
        DirectedEdge(1, 1, 0.0)
      )
    )
    val paths = EppsteinsAlgorithm(graph).findKShortestPaths(0, 2, 3)
    assert(paths.map(_.weight) == Seq(2.0, 2.0, 2.0))
    assert(paths.map(_.nodes) == Seq(
      List(0, 1, 2),
      List(0, 1, 1, 2),
      List(0, 1, 1, 1, 2)
    ))
  }

  test("unreachable destination returns empty") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    val paths = EppsteinsAlgorithm(graph).findKShortestPaths(0, 1, 3)
    assert(paths.isEmpty)
  }

  test("source equals destination includes the zero walk then a closed walk") {
    val paths = EppsteinsAlgorithm(cycleGraph).findKShortestPaths(1, 1, 2)
    assert(paths.size == 2)
    assert(paths.head == Path(0.0, List(1)))
    assert(paths(1) == Path(2.0, List(1, 0, 1)))
  }

  test("negative edge weight is rejected") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, -1.0)))
    intercept[IllegalArgumentException] {
      EppsteinsAlgorithm(graph).findKShortestPaths(0, 1, 1)
    }
  }

  test("first path cost matches Dijkstra and weights are non-decreasing") {
    val paths = EppsteinsAlgorithm(cycleGraph).findKShortestPaths(0, 2, 5)
    val dijkstraCost = DijkstrasPathSolver().findPaths(cycleGraph, 0).paths(2).weight
    assert(paths.head.weight == dijkstraCost)
    assert(paths.map(_.weight) == paths.map(_.weight).sorted)
    assert(paths.map(_.weight).sliding(2).forall {
      case Seq(a, b) => a < b
      case _ => true
    })
  }
}
