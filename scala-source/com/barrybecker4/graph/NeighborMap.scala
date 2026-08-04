package com.barrybecker4.graph

import scala.collection.mutable

class NeighborMap {

  private val map = mutable.Map.empty[Int, Set[Int]]

  def apply(v: Int): Set[Int] = map(v)

  def addNeighbor(v1: Int, v2: Int): Unit = {
    map(v1) = map.getOrElse(v1, Set.empty) + v2
  }
}
