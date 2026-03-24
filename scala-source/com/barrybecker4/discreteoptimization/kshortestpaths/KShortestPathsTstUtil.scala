package com.barrybecker4.discreteoptimization.kshortestpaths

import com.barrybecker4.discreteoptimization.kshortestpaths.model.{KShortestPathsSolution, KShortestPathsSolutionParser}

import java.io.{File, PrintWriter}
import scala.io.Source

object KShortestPathsTstUtil {

  val PREFIX = "scala-test/com/barrybecker4/discreteoptimization/kshortestpaths/solver/data/"

  def getSerializedSolution(name: String): String = {
    val source: Source = Source.fromFile(PREFIX + name)
    val s = source.getLines().mkString("\n") + "\n"
    source.close()
    s
  }

  def getSolution(name: String): KShortestPathsSolution = {
    val source: Source = Source.fromFile(PREFIX + name)
    KShortestPathsSolutionParser().parse(source.getLines().toIndexedSeq)
  }

  def writeSolution(name: String, text: String): Unit = {
    val pw = new PrintWriter(new File(PREFIX + name))
    pw.write(text)
    pw.close()
  }
}

@deprecated("Use KShortestPathsTstUtil", since = "2.0")
object KShortedPathsTstUtil {
  export KShortestPathsTstUtil.{getSerializedSolution, getSolution, writeSolution, PREFIX}
}
