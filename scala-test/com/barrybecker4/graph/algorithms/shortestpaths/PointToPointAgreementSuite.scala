package com.barrybecker4.graph.algorithms.shortestpaths

import com.barrybecker4.discreteoptimization.SlowTest
import com.barrybecker4.discreteoptimization.benchmark.PathCosts
import com.barrybecker4.discreteoptimization.shortestpaths.solver.DijkstrasPathSolver
import com.barrybecker4.graph.GraphTstUtil
import org.scalatest.funsuite.AnyFunSuite

class PointToPointAgreementSuite extends AnyFunSuite {

  private val fixtures = Seq("tiny_graph_01", "tiny_graph_02", "sp_4_1", "sp_4_2", "sp_10_1")

  private def pairs(last: Int): Seq[(Int, Int)] = {
    val base = Seq((0, 1), (0, last))
    if (last > 1) base :+ (1, last) else base
  }

  private def pathCost(path: Option[com.barrybecker4.graph.Path]): Double =
    path.map(_.weight).getOrElse(Double.PositiveInfinity)

  fixtures.foreach { name =>
    val graph = GraphTstUtil.getGraph(name)
    val last = graph.numVertices - 1
    pairs(last).foreach { case (source, destination) =>
      test(s"$name bidirectional and ALT match Dijkstra $source->$destination") {
        val expected = DijkstrasPathSolver().findPaths(graph, source).paths(destination).weight
        val bi = pathCost(new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(source, destination))
        val alt = pathCost(new AltAlgorithm(graph).getShortestPath(source, destination))
        assert(PathCosts.sameCost(bi, expected), s"bidirectional $bi vs $expected")
        assert(PathCosts.sameCost(alt, expected), s"ALT $alt vs $expected")
        if (AStarAlgorithm.available(graph)) {
          val star = pathCost(new AStarAlgorithm(graph).getShortestPath(source, destination))
          assert(PathCosts.sameCost(star, expected), s"A* $star vs $expected")
        }
      }
    }
  }

  test("road_network_01 bidirectional and ALT match Modified Dijkstra 0->11000", SlowTest) {
    assume(SlowTest.include, "skipped unless -Dshortestpaths.includeSlow=true")
    val graph = GraphTstUtil.getGraph("road_network_01")
    val expected = pathCost(new ModifiedDijkstrasAlgorithm(graph).getShortestPath(0, 11000))
    val bi = pathCost(new BidirectionalDijkstrasAlgorithm(graph).getShortestPath(0, 11000))
    val alt = pathCost(new AltAlgorithm(graph).getShortestPath(0, 11000))
    assert(PathCosts.sameCost(bi, expected), s"bidirectional $bi vs $expected")
    assert(PathCosts.sameCost(alt, expected), s"ALT $alt vs $expected")
  }
}
