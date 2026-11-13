package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class AStarAlgorithmSuite extends AnyFunSuite {

  test("available is false without locations") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    assert(!AStarAlgorithm.available(graph))
  }

  test("scaled Euclidean stays admissible when an edge is shorter than geometry") {
    val locations = Array(
      FloatLocation(0f, 0f),
      FloatLocation(0f, 10f),
      FloatLocation(0f, 20f)
    )
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 100.0),
        DirectedEdge(1, 2, 1.0),
        DirectedEdge(0, 2, 1000.0)
      ),
      Some(locations)
    )
    assert(new AStarAlgorithm(graph).getShortestPath(0, 2).contains(Path(101.0, List(0, 1, 2))))
  }

  test("located straight line with weights equal to length returns that path") {
    val locations = Array(
      FloatLocation(0f, 0f),
      FloatLocation(1f, 0f),
      FloatLocation(2f, 0f)
    )
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 1.0),
        DirectedEdge(1, 2, 1.0)
      ),
      Some(locations)
    )
    assert(AStarAlgorithm.available(graph))
    assert(new AStarAlgorithm(graph).getShortestPath(0, 2).contains(Path(2.0, List(0, 1, 2))))
  }
}
