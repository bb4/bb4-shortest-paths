package com.barrybecker4.graph.directed

import com.barrybecker4.common.geometry.FloatLocation
import com.barrybecker4.graph.Parser

import scala.collection.mutable.ArrayBuffer


case class DirectedGraphParser() extends Parser[DirectedGraph] {
  
  override protected def parse(lines: IndexedSeq[String], problemName: String): DirectedGraph = {
    val firstLine = lines(0).split("\\s+")
    val numVertices = firstLine(0).toInt
    val numEdges = firstLine(1).toInt
    val hasLocations = firstLine(2).toBoolean

    val locations =
      if (hasLocations) Some(parseLocations(numVertices, lines)) else None
    val start = if (hasLocations) 1 + numVertices else 1
    val edges = parseEdges(start, numEdges, lines)

    DirectedGraph(numVertices, edges, locations)
  }

  private def parseLocations(numVertices: Int, lines: IndexedSeq[String]): Array[FloatLocation] =
    Array.tabulate(numVertices) { i =>
      val parts = lines(i + 1).split("\\s+")
      FloatLocation(parts(0).toFloat, parts(1).toFloat)
    }

  /**
   * Warn if there are any duplicate edges. Normally, there should not be.
   * Just warn the first time to avoid too many warnings.
   */
  private def parseEdges(start: Int, numEdges: Int, lines: IndexedSeq[String]): IndexedSeq[DirectedEdge] = {
    val edges = ArrayBuffer[DirectedEdge]()
    var edgeSet: Set[(Int, Int)] = Set()
    for (i <- 0 until numEdges) {
      val line = lines(i + start)
      val parts = line.split("\\s+")
      // if no weight specified, use a deterministic default (stable for tests and reproducibility)
      val weight = if (parts.length > 2) parts(2).toDouble else 1.0
      val source = parts(0).toInt
      val dest = parts(1).toInt
      val e = (source, dest)

      if (edgeSet.contains(e)) {
        println(s"More than one edge from ${e._1} to ${e._2}. This is allowed, but may not be what you want.")
      } else edgeSet = edgeSet + e

      edges += DirectedEdge(source, dest, weight)
    }
    edges.toIndexedSeq
  }
}
