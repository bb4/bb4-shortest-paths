package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.discreteoptimization.pathviewer.render.PathRenderer
import com.barrybecker4.graph.visualization.GraphStreamAdapter.LARGE_GRAPH_THRESH
import com.barrybecker4.graph.visualization.render.UiClass.*
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.ui.view.{ViewerListener, ViewerPipe}


case class GraphViewerListener(viewerPipe: ViewerPipe, graph: MultiGraph, pathRenderer: PathRenderer) extends ViewerListener  {

  private val isLarge: Boolean = graph.edges().count() > LARGE_GRAPH_THRESH

  override def viewClosed(viewName: String): Unit =
    pathRenderer.stop()

  override def buttonPushed(id: String): Unit =
    println("button" + id + " pushed")

  override def buttonReleased(id: String): Unit =
    println("button" + id + " released")

  override def mouseOver(id: String): Unit = {
    val edge = graph.getEdge(id)
    if (edge != null) {
      pathRenderer.colorPaths(edge.getNode0.getId.toInt, edge.getNode1.getId.toInt, HIGHLIGHTED)
      val left = pathRenderer.ranksText(edge.getNode0.getId.toInt)
      val right = pathRenderer.ranksText(edge.getNode1.getId.toInt)
      val ranks = (left.toList ++ right.toList).distinct.mkString(", ")
      val detail = if (ranks.isEmpty) "" else s"  ranks $ranks"
      pathRenderer.statusListener(s"edge ${edge.getId}$detail")
    } else {
      pathRenderer.colorPaths(id.toInt, HIGHLIGHTED)
      val nodeIdx = id.toInt
      val distance = pathRenderer.distanceText(nodeIdx).map(text => s"dist $text")
      val ranks = pathRenderer.ranksText(nodeIdx).map(text => s"ranks $text")
      val detail = (distance.toList ++ ranks.toList).mkString("  ")
      val message = if (detail.isEmpty) s"node $nodeIdx" else s"node $nodeIdx  $detail"
      pathRenderer.statusListener(message)
    }
  }

  override def mouseLeft(id: String): Unit = {
    val edge = graph.getEdge(id)
    val uiClass = if (isLarge) LARGE else PLAIN
    if (edge != null) pathRenderer.colorPaths(edge.getNode0.getId.toInt, edge.getNode1.getId.toInt, uiClass)
    else pathRenderer.colorPaths(id.toInt, uiClass)
    pathRenderer.statusListener("")
  }
}
