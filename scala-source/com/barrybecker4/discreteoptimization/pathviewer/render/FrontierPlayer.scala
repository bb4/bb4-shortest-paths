package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.algorithms.shortestpaths.SearchEvent
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph

class FrontierPlayer(graph: MultiGraph) {
  private var current: Option[Node] = None

  def apply(event: SearchEvent): Unit = event match {
    case SearchEvent.Frontier(vertex, _) =>
      setClass(vertex, "frontier")
    case SearchEvent.Extracted(vertex) =>
      clearCurrent()
      setClass(vertex, "current")
      current = Option(graph.getNode(vertex.toString))
    case SearchEvent.Settled(vertex) =>
      setClass(vertex, "visited")
      if (current.exists(_.getId == vertex.toString)) current = None
  }

  private def clearCurrent(): Unit =
    current.foreach { node =>
      if (node.hasAttribute("ui.class")) {
        val kept = node.getAttribute("ui.class", classOf[String])
          .split(',').iterator.map(_.trim).filter(_ != "current").mkString(", ")
        if (kept.isEmpty) node.removeAttribute("ui.class")
        else node.setAttribute("ui.class", kept)
      }
    }

  private def setClass(vertex: Int, uiClass: String): Unit = {
    val node = graph.getNode(vertex.toString)
    if (node != null) {
      val existing =
        if (node.hasAttribute("ui.class")) Some(node.getAttribute("ui.class", classOf[String]))
        else None
      node.setAttribute("ui.class", PathRenderer.mergeUiClass(existing, uiClass))
    }
  }
}
