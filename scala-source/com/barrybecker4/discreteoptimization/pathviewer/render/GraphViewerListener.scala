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
    }
    else pathRenderer.colorPaths(id.toInt, HIGHLIGHTED)
  }

  override def mouseLeft(id: String): Unit = {
    val edge = graph.getEdge(id)
    val uiClass = if (isLarge) LARGE else PLAIN
    if (edge != null) {
      pathRenderer.colorPaths(edge.getNode0.getId.toInt, edge.getNode1.getId.toInt, uiClass)
    }
    else pathRenderer.colorPaths(id.toInt, uiClass)
  }
}
