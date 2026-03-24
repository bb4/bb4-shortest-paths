package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import _root_.com.barrybecker4.discreteoptimization.FixtureUpdateMode
import _root_.com.barrybecker4.graph.GraphTstUtil
import _root_.com.barrybecker4.discreteoptimization.kshortestpaths.KShortestPathsTstUtil
import _root_.com.barrybecker4.discreteoptimization.kshortestpaths.solver.KShortestPathsSolver
import org.scalatest.funsuite.AnyFunSuite


abstract class BaseSolverSuite extends AnyFunSuite {

  def createSolver(): KShortestPathsSolver

  def solverName(): String

  /** Set JVM `-Dshortestpaths.updateFixtures=true` or env `SHORTEST_PATHS_UPDATE_FIXTURES=true` to rewrite golden files. */
  protected def updateFixtures: Boolean = FixtureUpdateMode.updateFixtures

  def verify(problemName: String, destination: Int, k: Int): Unit = {
    print(s"running $problemName ...")
    val graph = GraphTstUtil.getGraph(problemName)

    val actual = createSolver().findPaths(graph, 0, destination, k)
    val fileName = getFileName(problemName)

    if (updateFixtures) {
      KShortestPathsTstUtil.writeSolution(fileName, actual.toString)
    } else {
      val expSolution = KShortestPathsTstUtil.getSerializedSolution(fileName)
      assertResult(expSolution, "actual:\n" + actual) {
        actual.toString() + "\n"
      }
    }
  }

  private def getFileName(problemName: String): String = problemName + "_" + solverName() + "_solution.txt"

}
