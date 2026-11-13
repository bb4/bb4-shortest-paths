package com.barrybecker4.discreteoptimization.traffic.viewer.adapter

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.discreteoptimization.traffic.graph.model.{Intersection, Street}
import org.graphstream.graph.{Edge, Node}
import org.graphstream.graph.implementations.MultiGraph
import com.barrybecker4.graph.visualization.render.UiClass.PLAIN
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.StreetSubGraph.STREET_TYPE
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.IntersectionSubGraphBuilder.INTERSECTION_RADIUS
import com.barrybecker4.discreteoptimization.traffic.viewer.adapter.TrafficStreamAdapter.COMPUTE_CURVES


object StreetSubGraph {
  val STREET_TYPE: String = "street"
}

case class StreetSubGraph(street: Street,
                     intersectionSubGraph1: IntersectionSubGraph,
                     intersectionSubGraph2: IntersectionSubGraph,
                     graph: MultiGraph) {

  private val forwardEdge = createEdge(
    street, isForward = true,
    intersectionSubGraph1.getOutgoingNode(street.portIdx1),
    intersectionSubGraph2.getIncomingNode(street.portIdx2)
  )

  private val backwardEdge = createEdge(
    street, isForward = false,
    intersectionSubGraph2.getOutgoingNode(street.portIdx2),
    intersectionSubGraph1.getIncomingNode(street.portIdx1)
  )

  private def createEdge(street: Street, isForward: Boolean, srcNode: Node, dstNode: Node): Edge = {
    val edgeId = getStreetEdgeId(street, isForward)
    val edge = graph.addEdge(edgeId, srcNode, dstNode, true)
    edge.setAttribute("ui.class", PLAIN.name)
    edge.setAttribute("type", STREET_TYPE)
    edge.setAttribute("lastVehicle", None)
    if (COMPUTE_CURVES) addCurvePoints(edge, street, isForward)
    edge
  }

  private def addCurvePoints(edge: Edge, street: Street, forward: Boolean): Unit = {
    val src = edge.getSourceNode.getAttribute("xyz", classOf[Array[AnyRef]])
    val dst = edge.getTargetNode.getAttribute("xyz", classOf[Array[AnyRef]])

    val intersection1 = if (forward) intersectionSubGraph1.intersection else intersectionSubGraph2.intersection
    val intersection2 = if (forward) intersectionSubGraph2.intersection else intersectionSubGraph1.intersection
    val portIdx1 = if (forward) street.portIdx1 else street.portIdx2
    val portIdx2 = if (forward) street.portIdx2 else street.portIdx1
    val srcCtrlPt = getPortSpokePoint(src, intersection1, portIdx1)
    val dstCtrlPt = getPortSpokePoint(dst, intersection2, portIdx2)
    edge.setAttribute("ui.points",
      src(0), src(1), 0.0,
      srcCtrlPt.x, srcCtrlPt.y, 0,
      dstCtrlPt.x, dstCtrlPt.y, 0,
      dst(0), dst(1), 0.0)
  }

  private def halfway(src: Array[AnyRef], dst: Array[AnyRef]): FloatLocation = {
    FloatLocation((src(0).toString.toFloat + dst(0).toString.toFloat) / 2.0f, (src(1).toString.toFloat + dst(1).toString.toFloat) / 2.0f)
  }

  private def getPortSpokePoint(pt: Array[AnyRef], intersection: Intersection, portId: Int): FloatLocation = {
    val radialPos = getRadialPosition(intersection, portId)
    FloatLocation(pt(0).toString.toFloat + radialPos.x, pt(1).toString.toFloat + radialPos.y)
  }

  private def getRadialPosition(intersection: Intersection, portId: Int): FloatLocation = {
    val port = intersection.ports(portId)
    //val len = IntersectionSubGraph.INTERSECTION_RADIUS + port.radialLength
    // Instead of having each port have a radial length, just use half the intersection
    // radius to keep the vectors consistent with those within the intersection.
    val rad = INTERSECTION_RADIUS
    val vecX = (Math.cos(port.angleRad) * rad).toFloat //port.radialLength).toFloat
    val vecY = (Math.sin(port.angleRad) * rad).toFloat //port.radialLength).toFloat
    FloatLocation(vecX, vecY)
  }

  private def getStreetEdgeId(street: Street, isForward: Boolean): String = {
    if (isForward)
      s"i${street.intersectionIdx1}:p${street.portIdx1}-i${street.intersectionIdx2}:p${street.portIdx2}"
    else
      s"i${street.intersectionIdx2}:p${street.portIdx2}-i${street.intersectionIdx1}:p${street.portIdx1}"
  }

  private def ptToString(array: Array[AnyRef]): String =
    s"${array(0).toString}, ${array(1).toString}"
}
