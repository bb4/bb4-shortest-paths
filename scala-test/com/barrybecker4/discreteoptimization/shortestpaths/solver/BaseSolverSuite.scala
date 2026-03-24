package com.barrybecker4.discreteoptimization.shortestpaths.solver

import _root_.com.barrybecker4.discreteoptimization.FixtureUpdateMode
import _root_.com.barrybecker4.graph.GraphTstUtil
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.ShortestPathsTstUtil
import _root_.com.barrybecker4.discreteoptimization.shortestpaths.solver.ShortestPathsSolver
import org.scalatest.funsuite.AnyFunSuite


abstract class BaseSolverSuite extends AnyFunSuite {

  def createSolver(): ShortestPathsSolver

  def solverName(): String

  /** Set JVM `-Dshortestpaths.updateFixtures=true` or env `SHORTEST_PATHS_UPDATE_FIXTURES=true` to rewrite golden files. */
  protected def updateFixtures: Boolean = FixtureUpdateMode.updateFixtures

  def verify(problemName: String): Unit = {
    print(s"running $problemName ...")
    val graph = GraphTstUtil.getGraph(problemName)

    val actual = createSolver().findPaths(graph)
    val fileName = getFileName(problemName)

    if (updateFixtures) {
      ShortestPathsTstUtil.writeSolution(fileName, actual.toString)
    } else {
      val expSolution = ShortestPathsTstUtil.getSerializedSolution(fileName)
      assertResult(expSolution, "actual:\n" + actual) {
        actual.toString() + "\n"
      }
    }
  }

  private def getFileName(problemName: String): String = problemName + "_" + solverName() + "_solution.txt"

}
