package com.barrybecker4.graph.visualization.render

import org.graphstream.stream.ProxyPipe
import org.graphstream.ui.view.ViewerPipe

import scala.jdk.CollectionConverters.*


/**
 * Shell around a proxy pipe coming from the viewer allowing us to put viewer
 * listeners on a viewer that runs in a distinct thread.
 *
 * <p>
 * This pipe is a probe that you can place in the event loop between the viewer
 * and the graph. It will transmit all events coming from the viewer to the
 * graph (or any sink you connect to it). But in addition it will monitor
 * standard attribute changes to redistribute them to specify "viewer
 * listeners".
 * </p>
 *
 * <p>
 * As any proxy pipe, a viewer pipe must be "pumped" to receive events coming
 * from other threads.
 * </p>
 */
class GraphViewerPipe(id: String, pipeIn: ProxyPipe) extends ViewerPipe(id, pipeIn) {

  override def edgeAttributeAdded(sourceId: String, timeId: Long, edgeId: String, attribute: String, value: AnyRef): Unit = {
    super.sendEdgeAttributeAdded(sourceId, timeId, edgeId, attribute, value)
    
    if (attribute.equals("ui.clicked")) {
      for (listener <- viewerListeners.asScala) {
        listener.buttonPushed(edgeId)
      }
    }

    else if (attribute.equals("ui.mouseOver")) {
      for (listener <- viewerListeners.asScala) {
        listener.mouseOver(edgeId)
      }
    }
  }


  override def edgeAttributeRemoved(sourceId: String, timeId: Long, edgeId: String, attribute: String): Unit = {
    super.sendEdgeAttributeRemoved(sourceId, timeId, edgeId, attribute)

    if (attribute.equals("ui.clicked")) {
      for (listener <- viewerListeners.asScala) {
        listener.buttonReleased(edgeId)
      }
    }

    else if (attribute.equals("ui.mouseOver")) {
      for (listener <- viewerListeners.asScala) {
        listener.mouseLeft(edgeId)
      }
    }
  }

}