package com.barrybecker4.discreteoptimization.traffic.vehicles

import org.graphstream.graph.{Edge, Graph}
import org.graphstream.ui.spriteManager.SpriteManager

import scala.collection.mutable


class VehicleSpriteManager(graph: Graph) extends SpriteManager(graph) {
  
  def getVehiclesOnEdge(edgeId: String): mutable.Set[VehicleSprite] = {
    val edge: Edge = getEdge(edgeId)
    var vehicleSprites: mutable.Set[VehicleSprite] =
      edge.getAttribute[mutable.Set[VehicleSprite]]("vehicles", classOf[mutable.Set[VehicleSprite]])
      
    if (vehicleSprites == null) {
      vehicleSprites = mutable.Set() 
      edge.setAttribute("vehicles", vehicleSprites)
    }
    vehicleSprites
  }

  def addVehicleToEdge(edgeId: String, vehicleSprite: VehicleSprite): Unit =
    getVehiclesOnEdge(edgeId).add(vehicleSprite)

  def getEdge(edgeId: String): Edge = graph.getEdge(edgeId)

  def removeVehicleFromEdge(edgeId: String, vehicleSprite: VehicleSprite): Unit = {
    val v = getVehiclesOnEdge(edgeId).remove(vehicleSprite)
    assert(v)
  }

}
