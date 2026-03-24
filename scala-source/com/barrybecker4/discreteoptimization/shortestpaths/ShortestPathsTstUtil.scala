package com.barrybecker4.discreteoptimization.shortestpaths

import com.barrybecker4.graph.directed.{DirectedGraph, DirectedGraphParser}
import com.barrybecker4.discreteoptimization.shortestpaths.model.{ShortestPathsSolution, ShortestPathsSolutionParser}

import java.io.{File, PrintWriter}
import scala.io.Source

object ShortestPathsTstUtil {

  val PREFIX = "scala-test/com/barrybecker4/discreteoptimization/shortestpaths/solver/data/"
  val PARSER: DirectedGraphParser = DirectedGraphParser()

  def getSerializedSolution(name: String): String = {
    val source: Source = Source.fromFile(PREFIX + name)
    val s = source.getLines().mkString("\n") + "\n"
    source.close()
    s
  }

  def getSolution(name: String): ShortestPathsSolution = {
    val source: Source = Source.fromFile(PREFIX + name)
    ShortestPathsSolutionParser().parse(source.getLines().toIndexedSeq)
  }

  def writeSolution(name: String, text: String): Unit = {
    val pw = new PrintWriter(new File(PREFIX + name))
    pw.write(text)
    pw.close()
  }
}

@deprecated("Use ShortestPathsTstUtil", since = "2.0")
object ShortedPathsTstUtil {
  export ShortestPathsTstUtil.{getSerializedSolution, getSolution, writeSolution, PREFIX, PARSER}
}
