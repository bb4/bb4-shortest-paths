package com.barrybecker4.discreteoptimization.benchmark

import org.scalatest.funsuite.AnyFunSuite

class BaselineBenchmarkSuite extends AnyFunSuite {

  test("covers smoke fixtures, medium graphs, and road_network_01") {
    assert(BaselineBenchmark.cases.map(_.graph) == Seq(
      "tiny_graph_01",
      "tiny_graph_02",
      "sp_4_1",
      "sp_4_2",
      "sp_10_1",
      "sp_400",
      "sp_500",
      "sp_700",
      "road_network_01"
    ))
    assert(BaselineBenchmark.cases.map(_.role).distinct == Seq("smoke", "medium", "large"))
  }

  test("sameCost treats positive infinities as equal and ignores tiny noise") {
    assert(PathCosts.sameCost(Double.PositiveInfinity, Double.PositiveInfinity))
    assert(!PathCosts.sameCost(Double.PositiveInfinity, 1.0))
    assert(!PathCosts.sameCost(1.0, Double.PositiveInfinity))
    assert(PathCosts.sameCost(1.0, 1.0 + 1e-9))
    assert(!PathCosts.sameCost(1.0, 2.0))
    assert(PathCosts.sameCosts(Seq(1.0, Double.PositiveInfinity), Seq(1.0, Double.PositiveInfinity)))
    assert(!PathCosts.sameCosts(Seq(1.0), Seq(1.0, 2.0)))
  }
}
