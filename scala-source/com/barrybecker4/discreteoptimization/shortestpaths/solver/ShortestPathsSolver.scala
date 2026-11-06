package com.barrybecker4.discreteoptimization.shortestpaths.solver

import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.shortestpaths.model.ShortestPathsSolution

/**
 * Single-source shortest paths. Implementations include [[DijkstrasPathSolver]]
 * (non-negative weights) and [[BellmanFordPathSolver]] (negative weights, with
 * explicit negative-cycle detection).
 *
 *  For visualization look at
 *   - JGraphT - https://jgrapht.org/ - generate viz and save as images
 *   - GraphStream - https://graphstream-project.org/
 */
trait ShortestPathsSolver {

    def findPaths(graph: DirectedGraph, sourceVertex: Int = 0): ShortestPathsSolution

}
