package com.barrybecker4.graph.visualization

import com.barrybecker4.common.geometry.FloatLocation
import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.graph.visualization.GraphStreamAdapter.LARGE_GRAPH_THRESH
import com.barrybecker4.graph.visualization.render.UiClass.*
import org.graphstream.graph.implementations.MultiGraph

import scala.io.Source
import scala.util.Using


object GraphStreamAdapter {
  val LARGE_GRAPH_THRESH = 300
  private val STYLE_SHEET_PATH = "scala-source/com/barrybecker4/graph/visualization/graph.css"

  private def loadStyleSheet(): String = {
    Using(Source.fromFile(STYLE_SHEET_PATH)) { source => source.mkString }
      .getOrElse(throw new RuntimeException(s"Failed to read the style sheet from $STYLE_SHEET_PATH"))
  }
}

/**
 * Creates a stream graph from DirectedGraph
 */
case class GraphStreamAdapter(digraph: DirectedGraph) {

  private val isLarge = digraph.edges.size > LARGE_GRAPH_THRESH

  def createGraph(): MultiGraph = {
    val graph = new MultiGraph("Some Graph")

    addNodesToGraph(graph)
    addEdgesToGraph(graph)
    graph.setAttribute("ui.stylesheet", GraphStreamAdapter.loadStyleSheet())
    graph.setAttribute("ui.antialias", true)
    graph
  }

  private def addNodesToGraph(graph: MultiGraph): Unit = {
    for (nodeId <- Range(0, digraph.numVertices)) {
      val node = graph.addNode(nodeId.toString)
      node.setAttribute("ui.label", node.getId)
      if (digraph.locations.isDefined) {
        val location: FloatLocation = digraph.locations.get(nodeId)
        node.setAttribute("xy", location.x, location.y)
      }
    }
  }

  private def addEdgesToGraph(graph: MultiGraph): Unit = {
    val edgeCount = scala.collection.mutable.Map.empty[(Int, Int), Int]
    val uiClass = if (isLarge) LARGE.name else PLAIN.name
    for (edge <- digraph.edges) {
      val edgeTuple = (edge.source, edge.destination)
      val reverseTuple = (edge.destination, edge.source)
      val baseId = s"${edge.source}-${edge.destination}"
      val edgeId =
        if (edgeCount.contains(edgeTuple)) baseId + "_" + edgeCount(edgeTuple)
        else baseId
      val graphEdge = graph.addEdge(edgeId, edge.source.toString, edge.destination.toString, true)
      edgeCount(edgeTuple) = edgeCount.getOrElse(edgeTuple, 0) + 1
      val weightText =
        if (edgeCount.contains(reverseTuple)) s"          ${edge.weight}"
        else s"${edge.weight}           "
      graphEdge.setAttribute("ui.label", weightText)
      graphEdge.setAttribute("ui.class", uiClass)
    }
  }
}