package com.barrybecker4.discreteoptimization.pathviewer

import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution
import com.barrybecker4.discreteoptimization.kshortestpaths.solver.{EppsteinsKPathsSolver, YensKPathsSolver}
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution
import com.barrybecker4.discreteoptimization.shortestpaths.solver.{DijkstrasPathSolver, ModifiedDijkstrasPathSolver}
import com.barrybecker4.graph.Path
import com.barrybecker4.graph.algorithms.shortestpaths.{
  AStarAlgorithm,
  AltAlgorithm,
  BidirectionalDijkstrasAlgorithm,
  SearchObserver,
  SearchTrace
}
import com.barrybecker4.graph.directed.DirectedGraph

enum AlgorithmKind {
  case Dijkstra, ModifiedDijkstra, BidirectionalDijkstra, AStar, Alt, Yen, Eppstein
}

case class SolveRequest(algorithm: AlgorithmKind, source: Int, target: Int, k: Int)

enum SolveResult {
  case ToAll(solution: ShortestPathsSolution, trace: SearchTrace)
  case OnePath(path: Path, trace: SearchTrace)
  case KPaths(solution: KShortestPathsSolution)
}

case class ComparedSolutions(first: SolveResult, second: SolveResult, sameTargetCost: Boolean)

object LiveSolver {

  def maxVertexId(graph: DirectedGraph): Int = {
    val edgeMax = graph.edges.foldLeft(-1) { (maxId, edge) =>
      maxId.max(edge.source).max(edge.destination)
    }
    edgeMax.max(graph.numVertices - 1)
  }

  def validate(graph: DirectedGraph, request: SolveRequest): Either[String, SolveRequest] = {
    val maxId = maxVertexId(graph)
    if (request.source < 0 || request.source > maxId)
      Left(s"Source must be from 0 to $maxId")
    else if (request.target < 0 || request.target > maxId)
      Left(s"Target must be from 0 to $maxId")
    else if (request.algorithm == AlgorithmKind.Yen && request.source == request.target)
      Left("Yen needs different source and target")
    else if (request.algorithm == AlgorithmKind.AStar && graph.locations.isEmpty)
      Left("A* needs node locations")
    else if (usesK(request.algorithm) && (request.k < 1 || request.k > 10))
      Left("k must be from 1 to 10")
    else Right(request)
  }

  def usesK(algorithm: AlgorithmKind): Boolean =
    algorithm == AlgorithmKind.Yen || algorithm == AlgorithmKind.Eppstein

  /** Caller must pass a request that `validate` accepts. */
  def solve(
      graph: DirectedGraph,
      request: SolveRequest,
      alt: DirectedGraph => AltAlgorithm = g => new AltAlgorithm(g)
  ): SolveResult =
    request.algorithm match {
      case AlgorithmKind.Dijkstra =>
        val observer = SearchObserver.recording()
        val solution = new DijkstrasPathSolver().findPaths(graph, request.source, observer)
        SolveResult.ToAll(solution, observer.trace)
      case AlgorithmKind.ModifiedDijkstra =>
        val observer = SearchObserver.recording()
        val path = new ModifiedDijkstrasPathSolver()
          .findPath(graph, request.source, request.target, observer)
          .getOrElse(Path.EMPTY_PATH)
        SolveResult.OnePath(path, observer.trace)
      case AlgorithmKind.BidirectionalDijkstra =>
        onePath(graph, request, (g, s, t, o) => new BidirectionalDijkstrasAlgorithm(g).getShortestPath(s, t, o))
      case AlgorithmKind.AStar =>
        onePath(graph, request, (g, s, t, o) => new AStarAlgorithm(g).getShortestPath(s, t, o))
      case AlgorithmKind.Alt =>
        onePath(graph, request, (g, s, t, o) => alt(g).getShortestPath(s, t, o))
      case AlgorithmKind.Yen =>
        val solution = new YensKPathsSolver(request.k)
          .findPaths(graph, request.source, request.target, request.k)
        SolveResult.KPaths(solution)
      case AlgorithmKind.Eppstein =>
        val solution = new EppsteinsKPathsSolver(request.k)
          .findPaths(graph, request.source, request.target, request.k)
        SolveResult.KPaths(solution)
    }

  private def onePath(
      graph: DirectedGraph,
      request: SolveRequest,
      find: (DirectedGraph, Int, Int, SearchObserver) => Option[Path]
  ): SolveResult = {
    val observer = SearchObserver.recording()
    val path = find(graph, request.source, request.target, observer).getOrElse(Path.EMPTY_PATH)
    SolveResult.OnePath(path, observer.trace)
  }

  /** Finite cost of the path to `target`, if the result has one. */
  def targetCost(result: SolveResult, target: Int): Option[Double] = result match {
    case SolveResult.ToAll(solution, _) =>
      solution.paths
        .find(path => path.nodes.nonEmpty && path.lastNode == target && !path.weight.isPosInfinity)
        .map(_.weight)
    case SolveResult.OnePath(path, _) =>
      if (path.nodes.nonEmpty && !path.weight.isPosInfinity) Some(path.weight) else None
    case SolveResult.KPaths(solution) =>
      solution.shortestPaths.headOption
        .filter(path => path.nodes.nonEmpty && !path.weight.isPosInfinity)
        .map(_.weight)
  }

  /** Caller must pass requests that `validate` accepts. */
  def compare(
      graph: DirectedGraph,
      first: SolveRequest,
      second: SolveRequest,
      alt: DirectedGraph => AltAlgorithm = g => new AltAlgorithm(g)
  ): ComparedSolutions = {
    val a = solve(graph, first, alt)
    val b = solve(graph, second, alt)
    val same = (targetCost(a, first.target), targetCost(b, second.target)) match {
      case (Some(left), Some(right)) => left == right
      case _ => false
    }
    ComparedSolutions(a, b, same)
  }
}
