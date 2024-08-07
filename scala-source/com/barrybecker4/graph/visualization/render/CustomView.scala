package com.barrybecker4.graph.visualization.render

import org.graphstream.ui.graphicGraph.GraphicGraph
import org.graphstream.ui.swing.SwingGraphRenderer
import org.graphstream.ui.swing_viewer.DefaultView
import org.graphstream.ui.view.camera.Camera
import org.graphstream.ui.view.{GraphRenderer, Viewer}

/** Customized view */
class CustomView(viewer: Viewer, name: String, graphicGraph: GraphicGraph)
  extends DefaultView(viewer, name, CustomSwingGraphRenderer())
