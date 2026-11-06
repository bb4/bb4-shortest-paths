package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.discreteoptimization.benchmark.PathCosts
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.YensKPathsSolver
import com.barrybecker4.graph.GraphTstUtil
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.directed.{DirectedEdge, DirectedGraph}
import org.scalatest.funsuite.AnyFunSuite

/**
 * Hand-written checks that golden-file tests miss: non-zero sources, cross-solver
 * costs, unreachable vertices, and small edge cases.
 */
class SolverCorrectnessSuite extends AnyFunSuite {

  /** 0→1 (10), 1→2 (10), 2→3 (1), 0→3 (100). Source 2 reaches only 2 and 3. */
  private val chain = DirectedGraph(
    4,
    IndexedSeq(
      DirectedEdge(0, 1, 10.0),
      DirectedEdge(1, 2, 10.0),
      DirectedEdge(2, 3, 1.0),
      DirectedEdge(0, 3, 100.0)
    )
  )

  private val fixtures = Seq("tiny_graph_01", "tiny_graph_02", "sp_4_1", "sp_4_2", "sp_10_1")

  test("DijkstrasPathSolver.findPaths starts at the requested source") {
    val solution = DijkstrasPathSolver().findPaths(chain, 2)
    assert(solution.paths(2) == Path(0.0, List(2)))
    assert(solution.paths(3) == Path(1.0, List(2, 3)))
    assert(solution.paths.head.weight.isPosInfinity)
    assert(solution.paths(1).weight.isPosInfinity)
  }

  test("ModifiedDijkstrasPathSolver.findPaths starts at the requested source") {
    val solution = ModifiedDijkstrasPathSolver().findPaths(chain, 2)
    assert(solution.paths(2) == Path(0.0, List(2)))
    assert(solution.paths(3) == Path(1.0, List(2, 3)))
    assert(solution.paths.head.weight.isPosInfinity)
    assert(solution.paths(1).weight.isPosInfinity)
  }

  fixtures.foreach { name =>
    test(s"Dijkstra and Modified Dijkstra agree on path costs for $name from source 0") {
      assertCostsAgree(GraphTstUtil.getGraph(name), source = 0)
    }

    test(s"Dijkstra and Modified Dijkstra agree on path costs for $name from source 1") {
      assertCostsAgree(GraphTstUtil.getGraph(name), source = 1)
    }
  }

  test("Yen's first path cost matches Dijkstra for the same source and destination") {
    val graph = GraphTstUtil.getGraph("sp_10_1")
    val destination = 5
    val yen = YensKPathsSolver().findPaths(graph, 0, destination, 3)
    val dijkstra = DijkstrasPathSolver().findPaths(graph)
    assert(yen.shortestPaths.nonEmpty)
    assert(PathCosts.sameCost(yen.shortestPaths.head.weight, dijkstra.paths(destination).weight))
  }

  test("Yen's first path from a non-zero source matches Dijkstra") {
    val destination = 3
    val yen = YensKPathsSolver().findPaths(chain, 2, destination, 2)
    val dijkstra = DijkstrasPathSolver().findPaths(chain, 2)
    assert(yen.shortestPaths.size == 1)
    assert(PathCosts.sameCost(yen.shortestPaths.head.weight, dijkstra.paths(destination).weight))
    assert(yen.shortestPaths.head.weight == 1.0)
  }

  test("Yen's solver with k = 1 returns only the shortest path") {
    val solution = YensKPathsSolver().findPaths(chain, 2, 3, 1)
    assert(solution.shortestPaths.size == 1)
    assert(solution.shortestPaths.head == Path(1.0, List(2, 3)))
    assert(solution.k == 1)
  }

  test("Yen's solver returns fewer paths than k when not enough loopless paths exist") {
    val line = DirectedGraph(2, IndexedSeq(DirectedEdge(0, 1, 1.0)))
    val solution = YensKPathsSolver().findPaths(line, 0, 1, 5)
    assert(solution.shortestPaths.size == 1)
    assert(solution.shortestPaths.head == Path(1.0, List(0, 1)))
    assert(solution.k == 5)
  }

  test("DijkstrasPathSolver records unreachable vertices as infinite cost") {
    val graph = DirectedGraph(2, IndexedSeq(DirectedEdge(1, 0, 1.0)))
    val solution = DijkstrasPathSolver().findPaths(graph)
    assert(solution.paths.head == Path(0.0, List(0)))
    assert(solution.paths(1).weight.isPosInfinity)
    assert(solution.paths(1).nodes.isEmpty)
  }

  test("single-vertex graph costs 0 at the source") {
    val graph = DirectedGraph(1, IndexedSeq.empty)
    val dijkstra = DijkstrasPathSolver().findPaths(graph)
    val modified = ModifiedDijkstrasPathSolver().findPaths(graph)
    assert(dijkstra.paths == List(Path(0.0, List(0))))
    assert(dijkstra.totalCost == 0.0)
    assert(modified.paths == List(Path(0.0, List(0))))
    assert(modified.totalCost == 0.0)
  }

  test("graph with no edges costs 0 at a non-zero source and infinity elsewhere") {
    val graph = DirectedGraph(3, IndexedSeq.empty)
    val solution = DijkstrasPathSolver().findPaths(graph, 1)
    assert(solution.paths(1) == Path(0.0, List(1)))
    assert(solution.paths.head.weight.isPosInfinity)
    assert(solution.paths(2).weight.isPosInfinity)
    assert(solution.paths.head.nodes.isEmpty)
  }

  test("parallel edges: shortest path uses the cheaper edge") {
    val graph = DirectedGraph(
      2,
      IndexedSeq(DirectedEdge(0, 1, 5.0), DirectedEdge(0, 1, 1.0))
    )
    val dijkstra = DijkstrasPathSolver().findPaths(graph)
    val modified = ModifiedDijkstrasPathSolver().findPaths(graph)
    assert(dijkstra.paths(1) == Path(1.0, List(0, 1)))
    assert(modified.paths(1) == Path(1.0, List(0, 1)))
  }

  test("zero-weight edges are allowed") {
    val graph = DirectedGraph(
      3,
      IndexedSeq(
        DirectedEdge(0, 1, 0.0),
        DirectedEdge(1, 2, 0.0),
        DirectedEdge(0, 2, 5.0)
      )
    )
    val solution = DijkstrasPathSolver().findPaths(graph)
    assert(solution.paths(2) == Path(0.0, List(0, 1, 2)))
  }

  test("unused vertex ids inside 0 .. n-1 stay unreachable and do not change other costs") {
    val graph = DirectedGraph(
      5,
      IndexedSeq(DirectedEdge(0, 2, 1.5), DirectedEdge(2, 4, 2.5))
    )
    val solution = DijkstrasPathSolver().findPaths(graph)
    assert(solution.paths(4) == Path(4.0, List(0, 2, 4)))
    assert(solution.paths(1).weight.isPosInfinity)
    assert(solution.paths(3).weight.isPosInfinity)
    assert(solution.paths(1).nodes.isEmpty)
  }

  private def assertCostsAgree(graph: DirectedGraph, source: Int): Unit = {
    val dijkstra = DijkstrasPathSolver().findPaths(graph, source)
    val modified = ModifiedDijkstrasPathSolver().findPaths(graph, source)
    assert(dijkstra.paths(source) == Path(0.0, List(source)))
    assert(modified.paths(source) == Path(0.0, List(source)))
    assert(dijkstra.paths.size == modified.paths.size)
    dijkstra.paths.zip(modified.paths).zipWithIndex.foreach { case ((left, right), vertex) =>
      assert(
        PathCosts.sameCost(left.weight, right.weight),
        s"source $source vertex $vertex dijkstra=${left.weight} modified=${right.weight}"
      )
    }
  }
}
