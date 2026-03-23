package com.barrybecker4.discreteoptimization.traffic.vehicles

import scala.collection.immutable.Set
import scala.compiletime.uninitialized

case class VehicleStatistics(vehicles: Set[VehicleSprite]) {

  private var totalDistance: Double = uninitialized
  private var incrementalDistance: Double = uninitialized
  initialize()

  def getTotalDistance: Double = totalDistance
  def getIncrementalDistance: Double = incrementalDistance
  def resetIncrementalDistance(): Unit = {
    for (vehicle <- vehicles) {
      vehicle.resetIncrementalDistance()
    }
  }

  private def initialize(): Unit = {
    for (vehicle <- vehicles) {
      totalDistance += vehicle.getTotalDistance
      incrementalDistance += vehicle.getIncrementalDistance
    }
  }
}
