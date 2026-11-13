package com.barrybecker4.discreteoptimization.traffic.viewer.adapter

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.discreteoptimization.traffic.graph.model.Intersection
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.IntersectionSubGraphBuilder.{INTERSECTION_RADIUS, INTERSECTION_TYPE, LANE_SEP_ANGLE, VECTOR_SCALE}
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.TrafficStreamAdapter.COMPUTE_CURVES
import org.graphstream.graph.{Edge, Node}
import org.graphstream.graph.implementations.MultiGraph


object IntersectionSubGraphBuilder {
  val INTERSECTION_RADIUS = 90.0
  val INTERSECTION_TYPE = "intersection"
  // How much to separate the incoming lanes on the intersection by (in radians).
  private val LANE_SEP_ANGLE = 0.2
  // proportion of radial length to use for the vector. 0-1, where 1 is the full length of the radius
  private val VECTOR_SCALE = 0.25
}

/**
 * Populates the graph with all the nodes and edges needed to represent the N-way intersection.
 * Provides convenient accessors for the streets that enter and exit the intersection.
 */
case class IntersectionSubGraphBuilder(intersection: Intersection, graph: MultiGraph) {
  
  val incomingNodes: Array[Node] = Array.ofDim[Node](intersection.ports.size)
  val outgoingNodes: Array[Node] = Array.ofDim[Node](intersection.ports.size)

  populate()

  /**
   * For each port, create 2 nodes in a radial fashion around the center of the node.
   * The intersection nodes will then be connected up in the appropriate way.
   * Each incoming node can go to every outgoing node using a directed edge
   */
  private def populate(): Unit = {
    addPortNodes()
    addPortEdges()
  }

  private def addPortNodes(): Unit = {
    for (port <- intersection.ports) {
      val portId = port.id
      val inNode = graph.addNode(getNodeName(portId, "incoming"))
      val outNode = graph.addNode(getNodeName(portId, "outgoing"))
      val loc = intersection.location
      val ang = port.angleRad

      inNode.setAttribute("xyz",
        loc.x + INTERSECTION_RADIUS * Math.cos(ang + LANE_SEP_ANGLE),
        loc.y + INTERSECTION_RADIUS * Math.sin(ang + LANE_SEP_ANGLE),
        0.0)
      outNode.setAttribute("xyz",
        loc.x + INTERSECTION_RADIUS * Math.cos(ang - LANE_SEP_ANGLE),
        loc.y + INTERSECTION_RADIUS * Math.sin(ang - LANE_SEP_ANGLE),
        0.0)

      incomingNodes(portId) = inNode
      outgoingNodes(portId) = outNode
    }
  }

  // For each incoming port, connect it to all the other outgoing port nodes
  private def addPortEdges(): Unit = {
    for (fromPortId <- intersection.ports.indices) {
      val fromNode: Node = incomingNodes(fromPortId)
      for (toPortId <- intersection.ports.indices) {
        if (toPortId != fromPortId) {
          val toNode = outgoingNodes(toPortId)
          val edge: Edge = graph.addEdge(getEdgeName(fromPortId, toPortId), fromNode, toNode, true)

          val src = fromNode.getAttribute("xyz", classOf[Array[AnyRef]])
          val dst = toNode.getAttribute("xyz", classOf[Array[AnyRef]])
          if (COMPUTE_CURVES) {
            val srcVec = partway(src, intersection.location)
            val dstVec = partway(dst, intersection.location)
            edge.setAttribute("ui.points",
              src(0), src(1), 0.0,
              srcVec(0), srcVec(1), 0,
              dstVec(0), dstVec(1), 0,
              dst(0), dst(1), 0.0)
          }
          edge.setAttribute("type", INTERSECTION_TYPE)
          edge.setAttribute("lastVehicle", None)
        }
      }
    }
  }

  private def partway(src: Array[AnyRef], dest: FloatLocation): Array[Double] = {
    val srcX = src(0).toString.toDouble
    val srcY = src(1).toString.toDouble
    Array(srcX + (dest.x - srcX) * VECTOR_SCALE, srcY + (dest.y - srcY) * VECTOR_SCALE)
  }

  private def getNodeName(portId: Int, direction:String): String =
    s"i${intersection.id}:p${portId}_dir:$direction"

  private def getEdgeName(fromPortId: Int, toPortId: Int) =
    s"i${intersection.id}:from$fromPortId-to$toPortId"
}
