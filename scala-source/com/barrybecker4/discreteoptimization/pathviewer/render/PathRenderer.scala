package com.barrybecker4.discreteoptimization.pathviewer.render

import com.barrybecker4.graph.Path
import com.barrybecker4.graph.visualization.render.{GraphViewerPipe, UiClass}
import org.graphstream.graph.implementations.MultiGraph
import org.graphstream.graph.{Edge, Node}
import org.graphstream.ui.swing_viewer.util.MouseOverMouseManager
import org.graphstream.ui.view.util.InteractiveElement
import org.graphstream.ui.view.{Viewer, ViewerPipe}

import java.util
import java.util.concurrent.atomic.AtomicBoolean


object PathRenderer {
  val ANIMATION_DELAY = 20
  val PAUSE = 100

  /**
   * Keep `source` after a render class is applied. The render class comes first
   * so it overrides shared source properties; `source` still fills the rest.
   */
  def mergeUiClass(existing: Option[String], uiClass: String): String = {
    val keepSource = existing.exists(value => value.split(',').iterator.map(_.trim).contains("source"))
    val classes =
      (if (uiClass.nonEmpty) List(uiClass) else Nil) ++
        (if (keepSource) List("source") else Nil)
    classes.mkString(", ")
  }
}

trait PathRenderer(graph: MultiGraph, viewer: Viewer) {

  // The viewer pipe sends events from the UI thread to the render thread
  viewer.getDefaultView.setMouseManager(MouseOverMouseManager(util.EnumSet.of(InteractiveElement.EDGE, InteractiveElement.NODE)))

  protected val viewerPipe: ViewerPipe = GraphViewerPipe("my_custom_pipe", viewer.newViewerPipe())

  private val acceptingEvents = new AtomicBoolean(true)
  private var pumpThread: Option[Thread] = None

  def render(): Unit = {
    val viewerListener = GraphViewerListener(viewerPipe, graph, this)
    viewerPipe.addViewerListener(viewerListener)

    // simulation and interaction happens in a separate thread
    val thread = new Thread(() => pumpEvents(), "path-renderer-pump")
    pumpThread = Some(thread)
    thread.start()
  }

  /**
   * End the pump. Safe from the pump thread (viewClosed) and from the UI thread.
   * Interrupts a pump blocked in `blockingPump` or in the opening animation,
   * since a flag alone is not observed until the next event.
   */
  def stop(): Unit = {
    acceptingEvents.set(false)
    pumpThread.foreach { thread =>
      if (thread != Thread.currentThread()) thread.interrupt()
    }
  }

  protected def initialAnimation(): Unit = {}

  private def pumpEvents(): Unit = {
    try {
      if (acceptingEvents.get()) initialAnimation()
      listenForMouseEvents()
    } catch {
      case _: InterruptedException =>
        acceptingEvents.set(false)
    }
  }

  private def listenForMouseEvents(): Unit = {
    while (acceptingEvents.get()) {
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
      val nextNode = graph.getNode(nodeIdx.toString)
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
  
  protected def setNodeClass(node: Node, uiClass: String): Unit = {
    val existing =
      if (node.hasAttribute("ui.class")) Some(node.getAttribute("ui.class", classOf[String]))
      else None
    node.setAttribute("ui.class", PathRenderer.mergeUiClass(existing, uiClass))
  }

  /** color paths containing nodeIdx */
  def colorPaths(nodeIdx: Int, uiClass: UiClass): Unit

  /** color paths containing both nodeIdx1 and nodeIdx2 */
  def colorPaths(nodeIdx1: Int, nodeIdx: Int, uiClass: UiClass): Unit

}
