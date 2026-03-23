package com.barrybecker4.discreteoptimization.traffic.vehicles

import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSprite.{DEBUG, MAX_ACCELERATION, MAX_SPEED, PREFERRED_SPEED, RND}
import org.graphstream.graph.Edge
import org.graphstream.graph.Node
import org.graphstream.ui.spriteManager.Sprite
import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSpriteManager

import scala.compiletime.uninitialized
import scala.util.Random


object VehicleSprite {
  // Meters/second
  private val MAX_SPEED = 20.0
  private val PREFERRED_SPEED = 0.8 * MAX_SPEED
  // Meters/ second^2
  private val MAX_ACCELERATION = 4.0
  private val DEBUG = false
  private val RND = Random(0)
}

/**
 * The vehicle sprite represents a vehicle on a road.
 * It know which turn it will take at the next intersection.
 * @param identifier name of the sprite
 * @param initialSpeed initial speed of the sprite in meters per second
 */
class VehicleSprite(identifier: String, initialSpeed: Double, manager: VehicleSpriteManager, rnd: Random = RND) extends Sprite(identifier, manager) {
  private var positionPct: Double = 0.0 // 0 - 1.0
  private var speed = initialSpeed
  private var incrementalDistance: Double = 0
  private var totalDistance: Double = 0
  private var nextEdge: Edge = uninitialized

  def getSpeed: Double = speed
  def getNextEdge: Edge = nextEdge
  private def getCurrentEdge: Edge = getAttachment.asInstanceOf[Edge]

  /** @param acceleration requested amount of acceleration to change the current speed by. It has constraints.
   */
  def accelerate(acceleration: Double): Unit = {
    speed += Math.max(-MAX_ACCELERATION, Math.min(acceleration, MAX_ACCELERATION))
    speed = Math.max(0, Math.min(speed, PREFERRED_SPEED))
  }

  def setSpeed(newSpeed: Double): Unit = {
    speed = Math.max(0, Math.min(newSpeed, MAX_SPEED))
  }

  def brake(stoppingDistance: Double, deltaTime: Double): Unit = {
    if (stoppingDistance < 0.01) {
      stop()
    } else {
      val brake = Math.min(MAX_ACCELERATION, speed * deltaTime / stoppingDistance) // not sure about this
      //println("braking by " + brake + ";  stoppingDistance=" + stoppingDistance + " deltaTime=" + deltaTime + " speed=" + speed)
      accelerate(-brake)
    }
  }

  // This would be quite jarring to the driver. Avoid doing this unless going slow.
  def stop(): Unit = {
    speed = 0
  }

  /** When we attach a vehicle to an edge,
   * also select its next edge so we know how it will turn at the next intersection.
   */
  override def attachToEdge(edgeId: String): Unit = {
    val edge = manager.getEdge(edgeId)
    if (this.attachment != edge) {
      this.detach()
      this.attachment = edge
      manager.addVehicleToEdge(edgeId, this)
      nextEdge = randomEdge(edge.getTargetNode)
    }
    this.attachment.setAttribute(this.completeId, Array(0: java.lang.Double))
  }

  override def detach(): Unit = {
    if (getCurrentEdge != null) {
      manager.removeVehicleFromEdge(getCurrentEdge.getId, this)
      super.detach()
    }
  }
  
  def move(deltaTime: Double): Unit = {
    var p: Double = getPosition
    val step = calculateIncrement(getCurrentEdge, deltaTime)

    distanceAccounting(deltaTime)
    p += step
    if (p < 0 || p > 1)
      advanceToNextEdge(p, step, deltaTime)
    else setPosition(p)

    val edgeId = getCurrentEdge.getId
    if (DEBUG && (this.getId == "60" || edgeId == "i2:p0-i1:p0"))
      setAttribute("ui.label", s"id: $getId pct: ${positionPct.toFloat}        s: ${speed.toFloat} edge:$edgeId")
    else
      setAttribute("ui.label", "")
  }

  def predictNextPosition(deltaTime: Double): Double = {
    getPosition + calculateIncrement(getAttachment.asInstanceOf[Edge], deltaTime)
  }

  def advanceToNextEdge(p: Double, step: Double, deltaTime: Double): Unit = {
    val edge = getCurrentEdge
    var node = edge.getSourceNode

    if (step > 0) node = edge.getTargetNode
    val offset: Double = Math.abs(p % 1)
    val pos = if (node eq nextEdge.getSourceNode) {
      offset
    } else {
      // For the traffic sim, we never do this, because the vehicles always move forward.
      1.0 - offset
    }
    attachToEdge(nextEdge.getId)
    setPosition(pos)
  }

  override def setPosition(pct: Double): Unit = {
    if (positionPct != pct) {
      positionPct = pct
      super.setPosition(pct)
    }
  }

  def getPosition: Double = positionPct
  def getTotalDistance: Double = totalDistance
  def getIncrementalDistance: Double = incrementalDistance
  def resetIncrementalDistance(): Unit = incrementalDistance = 0

  private def distanceAccounting(deltaTime: Double): Unit = {
    val dist = deltaTime * speed
    incrementalDistance += dist
    totalDistance += dist
  }

  /** Move in larger percentage steps across shorter edges */
  private def calculateIncrement(edge: Edge, deltaTime: Double): Double = {
    val edgeLen = edge.getAttribute("length", classOf[Object]).asInstanceOf[Double]
    deltaTime * speed / edgeLen
  }

  /**
   * select an edge other than the one we came from
   */
  private def randomEdge(node: Node): Edge = {
    val rand = rnd.nextInt(node.getOutDegree)
    node.getLeavingEdge(rand)
  }
}