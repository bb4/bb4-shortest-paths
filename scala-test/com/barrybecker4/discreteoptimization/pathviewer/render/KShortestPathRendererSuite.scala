package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import com.barrybecker4.graph.visualization.GraphStreamAdapter
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import org.scalatest.funsuite.AnyFunSuite

class KShortestPathRendererSuite extends AnyFunSuite {

  test("selects non-contiguous path indices instead of a contiguous slice") {
    val paths = Seq(
      Path(1, List(0, 1)),
      Path(2, List(0, 2)),
      Path(3, List(0, 3)),
      Path(4, List(0, 4)),
      Path(5, List(0, 5)),
      Path(6, List(0, 6))
    )
    val selected = KShortestPathRenderer.pathsAt(paths, Seq(0, 2, 5))
    assert(selected.map(_._2) == Seq(0, 2, 5))
    assert(selected.map(_._1) == Seq(paths.head, paths(2), paths(5)))
  }

  test("renders paths with non-contiguous vertex IDs") {
    val edges = IndexedSeq(
      DirectedEdge(10, 20, 1.0),
      DirectedEdge(20, 30, 2.0),
      DirectedEdge(30, 50, 3.0)
    )
    val digraph = DirectedGraph(4, edges)
    val adapter = GraphStreamAdapter(digraph)
    val graph = adapter.createGraph()

    val paths = Seq(
      Path(1.0, List(10, 20)),
      Path(3.0, List(10, 20, 30)),
      Path(6.0, List(10, 20, 30, 50))
    )
    val solution = KShortestPathsSolution(6.0, 50, 3, paths)

    assert(graph.getNode("10") != null, "Node 10 should exist in graph")
    assert(graph.getNode("20") != null, "Node 20 should exist in graph")
    assert(graph.getNode("30") != null, "Node 30 should exist in graph")
    assert(graph.getNode("50") != null, "Node 50 should exist in graph")

    assert(paths.head.nodes == List(10, 20))
    assert(paths(1).nodes == List(10, 20, 30))
    assert(paths(2).nodes == List(10, 20, 30, 50))
  }
}
