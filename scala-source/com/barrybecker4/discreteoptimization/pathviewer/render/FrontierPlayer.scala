package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.algorithms.shortestpaths.{SearchDirection, SearchEvent}
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.MultiGraph

class FrontierPlayer(graph: MultiGraph) {
  private var currentForward: Option[Node] = None
  private var currentBackward: Option[Node] = None

  def apply(event: SearchEvent): Unit = event match {
    case SearchEvent.Frontier(vertex, _, direction) =>
      setClass(vertex, className("frontier", direction))
    case SearchEvent.Extracted(vertex, direction) =>
      clearCurrent(direction)
      setClass(vertex, className("current", direction))
      setCurrent(direction, Option(graph.getNode(vertex.toString)))
    case SearchEvent.Settled(vertex, direction) =>
      setClass(vertex, className("visited", direction))
      if (getCurrent(direction).exists(_.getId == vertex.toString)) setCurrent(direction, None)
  }

  private def className(base: String, direction: SearchDirection): String =
    if (direction == SearchDirection.Backward) s"${base}Back" else base

  private def getCurrent(direction: SearchDirection): Option[Node] =
    if (direction == SearchDirection.Backward) currentBackward else currentForward

  private def setCurrent(direction: SearchDirection, node: Option[Node]): Unit =
    if (direction == SearchDirection.Backward) currentBackward = node
    else currentForward = node

  private def clearCurrent(direction: SearchDirection): Unit = {
    val klass = className("current", direction)
    getCurrent(direction).foreach { node =>
      if (node.hasAttribute("ui.class")) {
        val kept = node.getAttribute("ui.class", classOf[String])
          .split(',').iterator.map(_.trim).filter(_ != klass).mkString(", ")
        if (kept.isEmpty) node.removeAttribute("ui.class")
        else node.setAttribute("ui.class", kept)
      }
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
