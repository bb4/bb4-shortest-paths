package com.barrybecker4.discreteoptimization.shortestpaths.solver

import _root_.com.barrybecker4.discreteoptimization.shortestpaths.solver.{BaseSolverSuite, DijkstrasPathSolver, ShortestPathsSolver}

class DijkstrasPathSolverSuite extends BaseSolverSuite {

  val update = true
  
  test("sp_4_1") {
    verify("sp_4_1", update)
  }

  test("sp_4_2") {
    verify("sp_4_2", update)
  }

  test("sp_5_1") {
    verify("sp_5_1", update)
  }

  test("network") {
    verify("network", update)
  }

  test("eclair_5_1") {
    verify("eclair_5_1", update)
  }

  test("eclair_5_2") {
    verify("eclair_5_2", update)
  }

  test("eclair_5_3") {
    verify("eclair_5_3", update)
  }

  test("eclair_6_1") {
    verify("eclair_6_1", update)
  }

  test("eclair_6_2") {
    verify("eclair_6_2", update)
  }

  test("eclair_6_3") {
    verify("eclair_6_3", update)
  }

  test("eclair_6_4") {
    verify("eclair_6_4", update)
  }

  test("eclair_6_5") {
    verify("eclair_6_5", update)
  }

  test("eclair_6_6") {
    verify("eclair_6_6", update)
  }

  test("sp_7_1") {
    verify("sp_7_1", update)
  }

  test("sp_10_1") {
    verify("sp_10_1", update)
  }

  test("sp_50_2") {
    verify("sp_50_2", update)
  }

  test("sp_100_1") {
    verify("sp_100_1", update)
  }

  test("sp_100_2") {
    verify("sp_100_2", update)
  }

  test("sp_100_3") {
    verify("sp_100_3", update)
  }

  test("sp_120") {
    verify("sp_120", update)
  }
  
  test("sp_400") {
    verify("sp_400", update)
  }

  test("sp_500") {
    verify("sp_500", update)
  }

  // run sin 0.57 s
  test("sp_700") {
    verify("sp_700", update)
  }

  test("test_5") {
    verify("test_5", update)
  }

  test("test_6") {
    verify("test_6", update)
  }

  test("test_6_1") {
    verify("test_6_1", update)
  }

  test("test_6_2") {
    verify("test_6_2", update)
  }

  test("test_6_3") {
    verify("test_6_3", update)
  }

  test("test_6_4") {
    verify("test_6_4", update)
  }

  test("test_7") {
    verify("test_7", update)
  }

  test("test_8") {
    verify("test_8", update)
  }

  test("test_15") {
    verify("test_15", update)
  }

  test("test_50") {
    verify("test_50", update)
  }

  test("test_50_2") {
    verify("test_50_2", update)
  }

  test("tiny_graph_01") {
    verify("tiny_graph_01", update)
  }

  test("tiny_graph_02") {
    verify("tiny_graph_02", update)
  }

  // This takes the lion's share of the time at 36s
  //  test("road_network_01") {
  //    verify("road_network_01", update)
  //  }


  override def createSolver(): ShortestPathsSolver = DijkstrasPathSolver()

  override def solverName(): String = DijkstrasPathSolver.BASE_NAME
}
