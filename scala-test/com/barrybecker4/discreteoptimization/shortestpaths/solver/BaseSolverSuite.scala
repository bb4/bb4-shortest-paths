package com.barrybecker4.discreteoptimization.shortestpaths.solver

import _root_.com.barrybecker4.graph.GraphTstUtil
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.solver.ShortestPathsSolver
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.ShortedPathsTstUtil
import org.scalatest.funsuite.AnyFunSuite

import scala.io.Source
import scala.util.Random


abstract class BaseSolverSuite extends AnyFunSuite {

  def createSolver(): ShortestPathsSolver

  def solverName(): String

  def verify(problemName: String, update: Boolean): Unit = {
    print(s"running $problemName ...")
    val graph = GraphTstUtil.getGraph(problemName)
 
    val actual: ShortestPathsSolution = createSolver().findPaths(graph)
    val fileName = getFileName(problemName)

    if (update) {
      ShortedPathsTstUtil.writeSolution(fileName, actual.toString)
    }
    else {
      val expSolution = ShortedPathsTstUtil.getSerializedSolution(fileName)
      assertResult(expSolution, "actual:\n" + actual) {
        actual.toString() + "\n"
      }
    }
  }

  private def getFileName(problemName: String): String = problemName + "_" + solverName() + "_solution.txt"

}
