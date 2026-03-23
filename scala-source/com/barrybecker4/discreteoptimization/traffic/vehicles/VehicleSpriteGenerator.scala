package com.barrybecker4.discreteoptimization.traffic.vehicles

import com.barrybecker4.discreteoptimization.traffic.vehicles.placement.VehiclePlacer
import org.graphstream.graph.Graph
import org.graphstream.ui.spriteManager.Sprite
import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSpriteManager

import scala.compiletime.uninitialized

class VehicleSpriteGenerator(private val numSprites: Int, initialSpeed: Double) {

  /** The set of sprites. */
  private var spriteManager: VehicleSpriteManager = uninitialized

  def getSpriteManager: VehicleSpriteManager = spriteManager

  def addSprites(graph: Graph): Unit = {
    spriteManager = new VehicleSpriteManager(graph)
    spriteManager.setSpriteFactory(new VehicleSpriteFactory(initialSpeed))
    for (i <- 0 until numSprites) {
      spriteManager.addSprite(s"$i")
    }
    new VehiclePlacer(spriteManager, graph).placeVehicleSprites()
  }

  def moveSprites(deltaTime: Double): Unit = {
    spriteManager.forEach((s: Sprite) => s.asInstanceOf[VehicleSprite].move(deltaTime))
  }
}
