package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent.*
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class SearchTraceSuite extends AnyFunSuite {

  private val triangle = DirectedGraph(
    3,
    IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(0, 2, 4.0)
    )
  )

  test("Dijkstra records frontier, extract-min, and settled vertices without changing the path") {
    val observer = SearchObserver.recording()
    val paths = new DijkstrasAlgorithm(triangle).findShortestPaths(0, observer)
    assert(paths.pathToVertex(2) == Path(3.0, List(0, 1, 2)))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1),
      Frontier(2, 3.0),
      Extracted(2), Settled(2)
    ))
  }

  test("Modified Dijkstra aimed at 2 records the same triangle trace and the same path") {
    val observer = SearchObserver.recording()
    val path = new ModifiedDijkstrasAlgorithm(triangle).getShortestPath(0, 2, observer)
    assert(path.contains(Path(3.0, List(0, 1, 2))))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1),
      Frontier(2, 3.0),
      Extracted(2), Settled(2)
    ))
  }

  test("Modified Dijkstra aimed at 1 stops when 1 is extracted") {
    val observer = SearchObserver.recording()
    val path = new ModifiedDijkstrasAlgorithm(triangle).getShortestPath(0, 1, observer)
    assert(path.contains(Path(1.0, List(0, 1))))
    assert(observer.trace.events == Vector(
      Frontier(0, 0.0),
      Extracted(0), Settled(0),
      Frontier(1, 1.0), Frontier(2, 4.0),
      Extracted(1), Settled(1)
    ))
  }

  test("the default observer leaves findShortestPaths unchanged") {
    val paths = new DijkstrasAlgorithm(triangle).findShortestPaths(0)
    assert(paths.pathToVertex(2) == Path(3.0, List(0, 1, 2)))
  }
}
