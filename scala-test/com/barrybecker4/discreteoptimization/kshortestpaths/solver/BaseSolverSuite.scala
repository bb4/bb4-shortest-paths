package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import _root_.com.barrybecker4.graph.GraphTstUtil
import _root_.com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import _root_.com.barrybecker4.discreteoptimization.kshortestpaths.solver.KShortestPathsSolver
import _root_.com.barrybecker4.discreteoptimization.kshortestpaths.KShortedPathsTstUtil
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.ShortedPathsTstUtil
import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source
import scala.util.Random


abstract class BaseSolverSuite extends AnyFunSuite {

  def createSolver(): KShortestPathsSolver

  def solverName(): String

  def verify(problemName: String, destination: Int, k: Int, update: Boolean): Unit = {
    print(s"running $problemName ...")
    val graph = GraphTstUtil.getGraph(problemName)
 
    val actual: KShortestPathsSolution = createSolver().findPaths(graph, 0, destination, k)
    val fileName = getFileName(problemName)

    if (update) {
      KShortedPathsTstUtil.writeSolution(fileName, actual.toString)
    }
    else {
      val expSolution = KShortedPathsTstUtil.getSerializedSolution(fileName)
      assertResult(expSolution, "actual:\n" + actual) {
        actual.toString() + "\n"
      }
    }
  }

  private def getFileName(problemName: String): String = problemName + "_" + solverName() + "_solution.txt"

}
