package com.barrybecker4.graph.visualization.render

import org.graphstream.ui.graphicGraph.GraphicGraph
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.view.Viewer

/** Customized view */
class CustomView(viewer: Viewer, name: String, graphicGraph: GraphicGraph)
  extends DefaultView(viewer, name, CustomSwingGraphRenderer())
