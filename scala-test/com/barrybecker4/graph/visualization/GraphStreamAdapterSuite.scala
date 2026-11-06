package com.barrybecker4.graph.visualization

import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

class GraphStreamAdapterSuite extends AnyFunSuite {

  private val line = DirectedGraph(
    3,
    IndexedSeq(DirectedEdge(0, 1, 1.0), DirectedEdge(1, 2, 1.0))
  )

  test("stylesheet highlights a source class instead of hardcoding node 0") {
    val graph = GraphStreamAdapter(line).createGraph(2)
    val css = graph.getAttribute("ui.stylesheet", classOf[String])
    assert(css.contains("node.source"))
    assert(!css.contains("node#0"))
  }

  test("source highlight follows the requested vertex") {
    val graph = GraphStreamAdapter(line).createGraph(2)
    assert(graph.getNode("2").getAttribute("ui.class", classOf[String]) == "source")
    assert(!graph.getNode("0").hasAttribute("ui.class"))
    assert(!graph.getNode("1").hasAttribute("ui.class"))
  }

  test("source highlight defaults to vertex 0") {
    val graph = GraphStreamAdapter(line).createGraph()
    assert(graph.getNode("0").getAttribute("ui.class", classOf[String]) == "source")
    assert(!graph.getNode("1").hasAttribute("ui.class"))
  }

  test("creates graph with non-contiguous vertex IDs") {
    val edges = IndexedSeq(
      DirectedEdge(10, 20, 1.0),
      DirectedEdge(20, 30, 2.0),
      DirectedEdge(30, 50, 3.0)
    )
    val digraph = DirectedGraph(4, edges)
    val adapter = GraphStreamAdapter(digraph)
    val graph = adapter.createGraph()

    assert(graph.getNodeCount == 4)
    assert(graph.getNode("10") != null, "Node with ID 10 should exist")
    assert(graph.getNode("20") != null, "Node with ID 20 should exist")
    assert(graph.getNode("30") != null, "Node with ID 30 should exist")
    assert(graph.getNode("50") != null, "Node with ID 50 should exist")

    assert(graph.getNode("0") == null, "Node with ID 0 should not exist")
    assert(graph.getNode("1") == null, "Node with ID 1 should not exist")

    assert(graph.getEdgeCount == 3)
    assert(graph.getEdge("10-20") != null, "Edge 10-20 should exist")
    assert(graph.getEdge("20-30") != null, "Edge 20-30 should exist")
    assert(graph.getEdge("30-50") != null, "Edge 30-50 should exist")
  }

  test("creates graph with contiguous vertex IDs starting at 0") {
    val edges = IndexedSeq(
      DirectedEdge(0, 1, 1.0),
      DirectedEdge(1, 2, 2.0),
      DirectedEdge(2, 3, 3.0)
    )
    val digraph = DirectedGraph(4, edges)
    val adapter = GraphStreamAdapter(digraph)
    val graph = adapter.createGraph()

    assert(graph.getNodeCount == 4)
    assert(graph.getNode("0") != null, "Node with ID 0 should exist")
    assert(graph.getNode("1") != null, "Node with ID 1 should exist")
    assert(graph.getNode("2") != null, "Node with ID 2 should exist")
    assert(graph.getNode("3") != null, "Node with ID 3 should exist")

    assert(graph.getEdgeCount == 3)
  }

  test("creates graph with sparse vertex IDs") {
    val edges = IndexedSeq(
      DirectedEdge(5, 15, 1.0),
      DirectedEdge(100, 200, 2.0)
    )
    val digraph = DirectedGraph(4, edges)
    val adapter = GraphStreamAdapter(digraph)
    val graph = adapter.createGraph()

    assert(graph.getNodeCount == 4)
    assert(graph.getNode("5") != null, "Node with ID 5 should exist")
    assert(graph.getNode("15") != null, "Node with ID 15 should exist")
    assert(graph.getNode("100") != null, "Node with ID 100 should exist")
    assert(graph.getNode("200") != null, "Node with ID 200 should exist")
  }
}
