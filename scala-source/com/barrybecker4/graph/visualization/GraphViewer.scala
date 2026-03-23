package com.barrybecker4.graph.visualization

import com.barrybecker4.graph.visualization.render.CustomView
import org.graphstream.graph.Graph
import org.graphstream.ui.layout.springbox.implementations.{LinLog, SpringBox}
import org.graphstream.ui.swing_viewer.{SwingViewer, ViewPanel}
import org.graphstream.ui.view.{GraphRenderer, View, Viewer}

import javax.swing.{JFrame, JLabel, WindowConstants}


/**
 * Renders a graph in another thread (separate from UI thread) to avoid janky performance.
 */
class GraphViewer(thisGraph: Graph) extends SwingViewer(thisGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD) {

  def getViewPanel: ViewPanel =
    addDefaultView(false).asInstanceOf[ViewPanel]


  override def addDefaultView(openInAFrame: Boolean): View = views.synchronized {
    val view = CustomView(this, getDefaultID, graph)
    addView(view)
    if (openInAFrame) view.openInAFrame(true)
    return view
  }

  override def addDefaultView(openInAFrame: Boolean, renderer: GraphRenderer[?, ?]): View =
    addDefaultView(openInAFrame)
  
}


