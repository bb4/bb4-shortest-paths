package com.barrybecker4.discreteoptimization.traffic.vehicles

case class VehicleStatistics(vehicles: Set[VehicleSprite]) {

  private val totalDistance: Double = vehicles.iterator.map(_.getTotalDistance).sum
  private val incrementalDistance: Double = vehicles.iterator.map(_.getIncrementalDistance).sum

  def getTotalDistance: Double = totalDistance
  def getIncrementalDistance: Double = incrementalDistance

  def resetIncrementalDistance(): Unit =
    vehicles.foreach(_.resetIncrementalDistance())
}
