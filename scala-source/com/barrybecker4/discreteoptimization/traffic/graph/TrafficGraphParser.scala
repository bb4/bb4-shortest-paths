package com.barrybecker4.discreteoptimization.traffic.graph

import com.barrybecker4.common.geometry.FloatLocation
import com.barrybecker4.graph.Parser
import com.barrybecker4.discreteoptimization.traffic.graph.model.{Intersection, Port, Street}
import com.barrybecker4.discreteoptimization.traffic.signals.TrafficSignalType

import scala.collection.mutable.ArrayBuffer


/**
 * Parse traffic graphs. The format is as follows
 *
 * nIntersections nStreets
 * x_0 y_0 angle_0a, radialLength_0a angle_0b, radialLength_0b …
 * x_1 y_1 angle_1a, radialLength_1a angle_1b, radialLength_1b …
 * :
 * x_nIntersections y_nIntersections angle_nIntersections_a, radialLength_nIntersections_a angle_nIntersections_b, radialLength_nIntersections_b...
 * intersectionId_i1 port_i intersectionId_j1 port_j
 * intersectionId_i2 port_i intersectionId_j2 port_j
 * :
 * intersectionId_i_nEdges port_i intersectionId_j_nEdges port_j
 */
case class TrafficGraphParser() extends Parser[TrafficGraph] {
  
  override protected def parse(lines: IndexedSeq[String], trafficMapName: String): TrafficGraph = {
    val firstLine = lines(0).split("\\s+")
    val numIntersections = firstLine(0).toInt
    val numStreets = firstLine(1).toInt
    val numVehicles = firstLine(2).toInt

    val intersections = parseIntersections(numIntersections, lines)
    
    val start = 1 + numIntersections
    val streets = parseStreets(start, numStreets, lines)

    TrafficGraph(numVehicles, intersections, streets)
  }

  private def parseIntersections(numIntersections: Int, lines: IndexedSeq[String]): IndexedSeq[Intersection] = {
    val intersections = ArrayBuffer[Intersection]()
    for (i <- 0 until numIntersections) {
      val line = lines(i + 1)
      val parts = line.split("\\s+")
      val location = FloatLocation(parts(0).toFloat, parts(1).toFloat)
      val signalType = TrafficSignalType.valueOf(parts(2))
      val numPorts = (parts.length - 3) / 2
      val ports: IndexedSeq[Port] = for (j <- 0 until numPorts; idx = 3 + j * 2)
        yield Port(j, parts(idx).toDouble, parts(idx + 1).toInt)

      intersections += Intersection(i, location, ports, signalType)
    }
    intersections.toIndexedSeq
  }

  /**
   * More than one street is not allowed to connect to the same port on a node. Each street is bidirectional.
   * Format
   * intersectionId_i1 port_i intersectionId_j1 port_j
   */
  private def parseStreets(start: Int, numStreets: Int, lines: IndexedSeq[String]): IndexedSeq[Street] = {
    val streets = ArrayBuffer[Street]()
    var portSet: Set[(Int, Int)] = Set()
    for (i <- 0 until numStreets) {
      val line = lines(i + start)
      val parts = line.split("\\s+")

      val intersectionIdx1 = parts(0).toInt
      val port1 = parts(1).toInt
      val intersectionIdx2 = parts(2).toInt
      val port2 = parts(3).toInt

      portSet = addNodePortIfAvailable(intersectionIdx1, port1, portSet)
      portSet = addNodePortIfAvailable(intersectionIdx2, port2, portSet)

      streets += Street(intersectionIdx1, port1, intersectionIdx2, port2)
    }
    streets.toIndexedSeq
  }

  private def addNodePortIfAvailable(intersectionIdx: Int, portIdx: Int, nodePortSet: Set[(Int, Int)]): Set[(Int, Int)] = {
    val p = (intersectionIdx, portIdx)
    if (nodePortSet.contains(p)) {
      throw new IllegalStateException(s"Each intersection port can only have one street attached. More than one at ${p._1} to ${p._2}.")
    } else nodePortSet + p
  }
}

