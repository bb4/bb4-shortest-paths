package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution

/**
 * See https://en.wikipedia.org/wiki/K_shortest_path_routing
 * Implemented variants:
 *  - Yen's algorithm — k shortest loopless (simple) paths
 *  - Eppstein's algorithm — k shortest walks; vertices may repeat
 *
 *  For visualization look at
 *   - JGraphT - https://jgrapht.org/ - generate viz and save as images
 *   - GraphStream - https://graphstream-project.org/
 */
trait KShortestPathsSolver {

    def findPaths(graph: DirectedGraph, source: Int, destination: Int, k: Int): KShortestPathsSolution

}
