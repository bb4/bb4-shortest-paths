package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.render.{GraphViewerPipe, UiClass}
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.{Edge, Node}
import org.graphstream.ui.swing_viewer.util.MouseOverMouseManager
import org.graphstream.ui.view.util.InteractiveElement
import org.graphstream.ui.view.{Viewer, ViewerPipe}

import java.util


object PathRenderer {
  val ANIMATION_DELAY = 20
  val PAUSE = 100
}

trait PathRenderer(graph: MultiGraph, viewer: Viewer) {

  // The viewer pipe sends events from the UI thread to the render thread
  viewer.getDefaultView.setMouseManager(MouseOverMouseManager(util.EnumSet.of(InteractiveElement.EDGE, InteractiveElement.NODE)))

  protected val viewerPipe: ViewerPipe = GraphViewerPipe("my_custom_pipe", viewer.newViewerPipe())

  def render(): Unit = {
    val viewerListener = GraphViewerListener(viewerPipe, graph, this)
    viewerPipe.addViewerListener(viewerListener)

    // simulation and interaction happens in a separate thread
    new Thread(() => {
      initialAnimation()
      listenForMouseEvents()
    }).start()
  }

  protected def initialAnimation(): Unit = {}

  private def listenForMouseEvents(): Unit = {
    while (true) {
      // use blockingPump to avoid 100% CPU usage
      viewerPipe.blockingPump()
    }
  }

  /**
   * Walk consecutive nodes of a path, invoking callbacks for each node and
   * for each leaving edge between consecutive nodes. Optionally animates.
   */
  protected def walkPath(
      path: Path,
      animationDelay: Int
  )(onNode: (Node, Int, Boolean) => Unit)(onEdge: Edge => Unit): Unit = {
    if (path.nodes.size <= 1) return

    val lastNodeIdx = path.lastNode
    var prevNode: Option[Node] = None

    for (nodeIdx <- path.nodes) {
      val nextNode = graph.getNode(nodeIdx)
      prevNode.foreach { prev =>
        val leavingEdge = prev.leavingEdges().filter(e => e.getNode1 == nextNode).findFirst()
        if (leavingEdge.isPresent) onEdge(leavingEdge.get)
      }
      onNode(nextNode, nodeIdx, nodeIdx == lastNodeIdx)
      prevNode = Some(nextNode)
      if (animationDelay > 0) {
        viewerPipe.pump()
        Thread.sleep(animationDelay)
      }
    }
    if (animationDelay == 0) viewerPipe.pump()
  }
  
  /** color paths containing nodeIdx */
  def colorPaths(nodeIdx: Int, uiClass: UiClass): Unit

  /** color paths containing both nodeIdx1 and nodeIdx2 */
  def colorPaths(nodeIdx1: Int, nodeIdx: Int, uiClass: UiClass): Unit

}
