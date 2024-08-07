package com.barrybecker4.graph

import com.barrybecker4.graph.directed.{DirectedGraph, DirectedGraphParser}
import com.barrybecker4.discreteoptimization.shortestpaths.model.{ShortestPathsSolution, ShortestPathsSolutionParser}

import java.io.{File, PrintWriter}
import scala.io.Source

object GraphTstUtil {

  val PREFIX = "scala-test/com/barrybecker4/graph/data/"
  val PARSER: DirectedGraphParser = DirectedGraphParser()

  def getGraph(name: String): DirectedGraph = {
    val source: Source = Source.fromFile(PREFIX + name)
    PARSER.parse(source, name)
  }

}
