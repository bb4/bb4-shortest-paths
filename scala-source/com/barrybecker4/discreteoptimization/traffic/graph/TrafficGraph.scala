package com.barrybecker4.discreteoptimization.traffic.graph

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.discreteoptimization.traffic.graph.model.{Intersection, Street}


/**
 * An immutable directed graph with optional node locations
 */
case class TrafficGraph(numVehicles: Int,
                        intersections: IndexedSeq[Intersection], 
                        streets: IndexedSeq[Street]) {
  
  private val neighborMap: NeighborMap = NeighborMap()
  computeNeighborsMap()

  def numIntersections: Int = intersections.size
  def getLocation(intersectionIdx: Int): FloatLocation = intersections(intersectionIdx).location
  def getIntersection(intersectionIdx: Int): Intersection = intersections(intersectionIdx)
  def neighborsOf(v: Int): Set[Intersection] = neighborMap(v).map(i => intersections(i))
  
  private def computeNeighborsMap(): Unit = {
    for (street <- streets) {
      neighborMap.addNeighbor(street.intersectionIdx1, street.intersectionIdx2)
    }
  }

}
