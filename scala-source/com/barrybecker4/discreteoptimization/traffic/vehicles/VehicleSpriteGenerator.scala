package com.barrybecker4.discreteoptimization.traffic.vehicles

import com.barrybecker4.discreteoptimization.traffic.vehicles.placement.VehiclePlacer
import org.graphstream.graph.Graph
import org.graphstream.ui.spriteManager.Sprite

class VehicleSpriteGenerator(private val numSprites: Int, initialSpeed: Double) {

  /** The set of sprites. */
  private var spriteManager: Option[VehicleSpriteManager] = None

  def getSpriteManager: VehicleSpriteManager =
    spriteManager.getOrElse(
      throw new IllegalStateException("Sprites have not been added yet; call addSprites first")
    )

  def addSprites(graph: Graph): Unit = {
    val manager = new VehicleSpriteManager(graph)
    manager.setSpriteFactory(new VehicleSpriteFactory(initialSpeed))
    for (i <- 0 until numSprites) {
      manager.addSprite(s"$i")
    }
    new VehiclePlacer(manager, graph).placeVehicleSprites()
    spriteManager = Some(manager)
  }

  def moveSprites(deltaTime: Double): Unit = {
    getSpriteManager.forEach((s: Sprite) => s.asInstanceOf[VehicleSprite].move(deltaTime))
  }
}
