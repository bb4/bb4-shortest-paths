package com.barrybecker4.discreteoptimization.traffic.signals

import com.barrybecker4.discreteoptimization.traffic.signals.SignalState.{GREEN, RED, YELLOW}
import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSprite
import org.graphstream.graph.Node

import java.util.concurrent.{Executors, ScheduledExecutorService}


trait TrafficSignal(numStreets: Int) {

  protected val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  protected var yellowStartTime = 0L

  def getOptimalDistance: Double = 30.0
  def getFarDistance: Double = 200.0
  def getYellowDurationSecs: Int = 2
  def getGreenDurationSecs: Int = 4
  def getLightState(port: Int): SignalState

  def handleTraffic(sortedVehicles: IndexedSeq[VehicleSprite], portId: Int,
                    edgeLen: Double, deltaTime: Double): Unit

  def showLight(node: Node, portId: Int): Unit = {
    val lightState = getLightState(portId)
    node.setAttribute("ui.style", "size: 30px; z-index:0; fill-color: " + lightState.color)
  }

  def printLightStates(): Unit = {
    val states = Range(0, numStreets).map(i => getLightState(i))
    println(states)
  }

  protected def handleTrafficBasedOnLightState(sortedVehicles: IndexedSeq[VehicleSprite],
                                               portId: Int, edgeLen: Double, deltaTime: Double): Unit = {
    val lightState = getLightState(portId)
    if (sortedVehicles.isEmpty) return
    val vehicleClosestToLight = sortedVehicles.last

    lightState match {
      case RED =>
        // if the light is red, then first car should already be stopped if it is close to the light
        if (vehicleClosestToLight.getSpeed > 0.0 && vehicleClosestToLight.getPosition > 0.97) {
          //println("vehicleClosestToLight.getSpeed=" + vehicleClosestToLight.getSpeed + " should have been 0")
          vehicleClosestToLight.stop()
        } else if (vehicleClosestToLight.getPosition > 0.9) {
          vehicleClosestToLight.setSpeed(vehicleClosestToLight.getSpeed * .9)
        }
      case YELLOW =>
        val yellowElapsedTime = (System.currentTimeMillis() - yellowStartTime) / 1000.0
        val yellowRemainingTime = getYellowDurationSecs.toDouble - yellowElapsedTime
        val vehicleToBrake = sortedVehicles.reverseIterator.find { vehicle =>
          val distanceToLight = (1.0 - vehicle.getPosition) * edgeLen
          val distAtCurrentSpeed = yellowRemainingTime * vehicle.getSpeed
          val farDist = (yellowRemainingTime + getYellowDurationSecs) * vehicle.getSpeed
          distAtCurrentSpeed > distanceToLight && distAtCurrentSpeed < farDist
        }
        vehicleToBrake.foreach(v => v.brake(yellowRemainingTime * v.getSpeed, deltaTime))
      case GREEN =>
        vehicleClosestToLight.accelerate(0.1)
    }
  }
}
