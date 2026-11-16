package com.barrybecker4.graph.visualization.render

import org.graphstream.ui.geom.Point3
import org.graphstream.ui.graphicGraph.{GraphicEdge, GraphicElement, GraphicGraph}
import org.graphstream.ui.view.camera.DefaultCamera2D

class GraphCamera2D(graphicGraph: GraphicGraph) extends DefaultCamera2D(graphicGraph) {

  /**
   * Override the edgeContains method to customize its behavior.
   * We would like to see if the x, y passed in are on the line connecting the nodes
   */
  override def edgeContains(elt: GraphicElement, x: Double, y: Double): Boolean = {

    val edge = elt.asInstanceOf[GraphicEdge]
    val node1 = edge.from
    val node2 = edge.to
    val pt1 = bck.transform(node1.getX, node1.getY, 0)
    val pt2 = bck.transform(node2.getX, node2.getY, 0)

    val distThresh = Math.max(2.0, elt.getStyle.getSize.size().toDouble)

    isPointNearLineSegment(pt1, pt2, Point3(x, y, 0), distThresh)
  }

  private def isPointNearLineSegment(pt1: Point3, pt2: Point3, testPoint: Point3, distance: Double): Boolean = {

    val dotProd = dotProduct(pt1, testPoint, pt2)
    if (dotProd.isPosInfinity) {
      // Points are the same, check if the test point coincides with them
      return pt1.distance(testPoint) <= distance
    }

    // Check if the projection falls within the segment
    if (dotProd < 0.0 || dotProd > 1.0) {
      return false
    }

    // Calculate the coordinates of the projection on the line
    val projection = calcProjection(pt1, pt2, dotProd)

    // Check if the distance from the test point to the projection is within the specified threshold
    testPoint.distance(projection) <= distance
  }

  private def calcProjection(pt1: Point3, pt2: Point3, dotProd: Double): Point3 =
    Point3(pt1.x + dotProd * (pt2.x - pt1.x), pt1.y + dotProd * (pt2.y - pt1.y), 0)

  private def dotProduct(vectorsStart: Point3, vector1End: Point3, vector2End: Point3): Double = {
    val length = vectorsStart.distance(vector2End)
    val lengthSquared = length * length
    if (lengthSquared == 0.0) {
      Double.PositiveInfinity
    } else {
      val deltaX = vector2End.x - vectorsStart.x
      val deltaY = vector2End.y - vectorsStart.y
      ((vector1End.x - vectorsStart.x) * deltaX + (vector1End.y - vectorsStart.y) * deltaY) / lengthSquared
    }
  }
}