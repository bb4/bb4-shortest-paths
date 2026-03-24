package com.barrybecker4.discreteoptimization.kshortestpaths.solver

import com.barrybecker4.graph.directed.DirectedGraph
import com.barrybecker4.discreteoptimization.kshortestpaths.model.KShortestPathsSolution

/**
 * See https://en.wikipedia.org/wiki/K_shortest_path_routing
 * There are 2 main variants
 *  - Yen's algorithm - built on Dijkstra's shortest path algorithm. Find's k shortest loopless paths
 *  - Yen's algorithm (implemented) — k shortest simple paths.
 *      see https://codeforces.com/blog/entry/102085
 *  - Hershbergers algorthm - faster than Yen's - see https://archive.siam.org/meetings/alenex03/Abstracts/jhershberger.pdf
 *
 *  For visualization look at
 *   - JGraphT - https://jgrapht.org/ - generate viz and save as images
 *   - GraphStream - https://graphstream-project.org/
 */
trait KShortestPathsSolver {

    def findPaths(graph: DirectedGraph, source: Int, destination: Int, k: Int): KShortestPathsSolution

}
