package com.barrybecker4.discreteoptimization.traffic.vehicles.placement

import com.barrybecker4.discreteoptimization.traffic.vehicles.VehicleSpriteManager
import com.barrybecker4.discreteoptimization.traffic.vehicles.placement.VehiclePlacer.RND
import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficGraphUtil
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.ui.spriteManager.Sprite
import org.graphstream.ui.spriteManager.SpriteManager

import java.util.concurrent.atomic.AtomicInteger
import scala.util.Random


/**
 * Determines vehicle placements along all the edges of a provided graph.
 * We don't want the vehicles placed perfectly uniformly because that would not be natural.
 * Instead, they should be placed randomly, but with no overlap.
 * Throw an error if there are so many vehicles that they cannot be placed without overlap.
 * Each edge will have a maximum number of vehicles that it can hold. Do not exceed that.
 *
 * Do not place vehicles in intersections initially.
 */
object VehiclePlacer {
  /** The minimum gap between vehicles */
  private val MIN_GAP = 8.0
  /**
   * This will not be exact because the sprites size is relative to the window size.
   * Sprites get proportionally larger as the window size shrinks.
   * See traffic.css - size: 12px, 6px;
   */
  private val VEHICLE_LENGTH = 20.0
  // Colors can be #rrggbbaa; where aa is opacity (optional)
  private val VEHICLE_COLORS = Array[String](
    "#77225599;",
    "#77663399;",
    "#33775599;",
    "#33227799;"
  )
  private val RND: Random = new Random(0)
}

class VehiclePlacer(private val sprites: VehicleSpriteManager, private val graph: Graph) {
  private val streetEdges = graph.edges.filter(e => TrafficGraphUtil.isStreet(e)).toList

  private var totalAllocation: Integer = 0

  def placeVehicleSprites(): Unit = {
    val edgeIdToNumVehicles = determineNumVehiclesOnEdges
    allocateVehicles(edgeIdToNumVehicles)
  }

  /**
   * Each edge can have an expected allocation assuming uniform distribution.
   * expectedAllocation = edgeLen / totalLen * numVehicles.
   * Each edge can support a maximum number of vehicles
   * maxAllocation = floor(edgeLen / (MIN_GAP + vehicleLen))
   * If expectedAllocation > maxAllocation for any edge, then throw error.
   *
   * The actual number of vehicles to put on an edge can be
   * delta = maxAllocation - expectedAllocation
   * random( expectedAllocation - delta, expectedAllocation + delta + 1)
   * Then if there are too few allocated, randomly add them from edges until numVehicles reached.
   * If too many allocation, randomly remove them from edges until numVehicles reached.
   */
  private def determineNumVehiclesOnEdges = {
    val numVehicles = sprites.getSpriteCount
    val totalLen = findTotalLengthOfAllEdges(graph)
    var edgeIdToNumVehicles: Map[String, Integer] = createInitialEdgeAllocations(numVehicles, totalLen)
    edgeIdToNumVehicles = fineTuneEdgeAllocations(edgeIdToNumVehicles, numVehicles)

    val sumAllocatedVehicles = getSumAllocatedVehicles(edgeIdToNumVehicles)
    assert(numVehicles == sumAllocatedVehicles)
    edgeIdToNumVehicles
  }

  private def createInitialEdgeAllocations(numVehicles: Int, totalLen: Double): Map[String, Integer] = {
    var edgeIdToNumVehicles: Map[String, Integer] = Map()
    totalAllocation = 0
    streetEdges.forEach((edge: Edge) => {
      val edgeId = edge.getId
      val edgeLen = getEdgeLen(edge)
      val expectedAllocation = (numVehicles * edgeLen / totalLen).toInt
      val maxAllocation = (edgeLen / (VehiclePlacer.MIN_GAP + VehiclePlacer.VEHICLE_LENGTH)).toInt
      if (expectedAllocation > maxAllocation)
        throw new IllegalArgumentException("Trying to allocate more vehicles (" + expectedAllocation + ") than the street will hold (" + maxAllocation + ")!")
      edge.setAttribute("maxAllocation", maxAllocation)
      val delta = expectedAllocation
      assert(delta >= 0)
      val min = Math.max(0, expectedAllocation - delta)
      val max = Math.min(maxAllocation, min + 2 * delta)
      val randomAllocation = min + RND.nextInt(max - min + 1)
      assert(randomAllocation <= maxAllocation)
      totalAllocation += randomAllocation
      edgeIdToNumVehicles += (edgeId, randomAllocation)
    })
    edgeIdToNumVehicles
  }

  /** fine-tune in the event that we have too many or too few vehicles allocated */
  private def fineTuneEdgeAllocations(edgeIdToNumVehicles: Map[String, Integer], numVehicles: Int): Map[String, Integer] = {
    var edgeIdToNum = edgeIdToNumVehicles
    val edgeIds = edgeIdToNum.keySet.toArray
    while (totalAllocation > numVehicles) {
      val rndId = edgeIds(RND.nextInt(edgeIds.length))
      if (edgeIdToNum(rndId) > 0) {
        edgeIdToNum += (rndId, edgeIdToNum(rndId) - 1)
        totalAllocation -= 1
      }
    }

    while (totalAllocation < numVehicles) {
      val rndId = edgeIds(RND.nextInt(edgeIds.length))
      if (edgeIdToNum(rndId) < getMaxAllocation(graph.getEdge(rndId))) {
        edgeIdToNum += (rndId, edgeIdToNum(rndId) + 1)
        totalAllocation += 1
      }
    }
    edgeIdToNum
  }

  private def getSumAllocatedVehicles(edgeIdToNumVehicles: Map[String, Integer]) =
    edgeIdToNumVehicles.values.map(_.toInt).sum

  /**
   * Allocation algorithm
   *  - Divide the edge into maxAllocation equal slots.
   *  - Create an array of that same length
   *  - Using vehicle hash mod maxVehicles for that edge,
   *    place each into the array using array hashing algorithm,
   *    which resolves conflicts by moving to the next available slot.
   *  - Place the sprites in the array into the edge at the corresponding position.
   */
  private def allocateVehicles(edgeIdToNumVehicles: Map[String, Integer]): Unit = {
    val spriteCt = new AtomicInteger
    streetEdges.forEach((edge: Edge) => {
      val numVehiclesToAdd = edgeIdToNumVehicles(edge.getId)
      placeVehiclesForEdge(edge, numVehiclesToAdd, spriteCt)
    })
    println("done allocating vehicles on edge")
  }

  private def placeVehiclesForEdge(edge: Edge, numVehiclesToAdd: Int, spriteCount: AtomicInteger): Unit = {
    if (numVehiclesToAdd == 0) return
    val maxAllocation = getMaxAllocation(edge)
    val spriteSlots = new Array[Sprite](maxAllocation)
    assert(numVehiclesToAdd <= spriteSlots.length)
    System.out.println("now adding " + numVehiclesToAdd + " vehicles to edge " + edge.getId + " total avail slots = " + spriteSlots.length + " with maxAllocation=" + maxAllocation);
    for (i <- 0 until numVehiclesToAdd) {
      val positionIdx = chooseOpenSlotIndex(spriteSlots)
      val sprite = sprites.addSprite(s"${spriteCount.get()}")
      val color = VehiclePlacer.VEHICLE_COLORS(RND.nextInt(VehiclePlacer.VEHICLE_COLORS.length))
      sprite.setAttribute("ui.style", s"fill-color: $color")
      spriteCount.incrementAndGet()
      // for some reason there seems to be a problem with sprite orientation a the ver ends
      val pos = 0.01 + 0.98 * positionIdx.toDouble / spriteSlots.length
      //println("setting sprite " + spriteCount.get() + " pos to " + pos + " and putting it in slot " + positionIdx)
      sprite.setPosition(pos)
      spriteSlots(positionIdx) = sprite
      sprite.attachToEdge(edge.getId)
    }
  }

  private def chooseOpenSlotIndex(spriteSlots: Array[Sprite]): Int = {
    var positionIdx = RND.nextInt(spriteSlots.length)
    while (spriteSlots(positionIdx) != null)
      positionIdx = (positionIdx + 1) % spriteSlots.length
    positionIdx
  }

  private def findTotalLengthOfAllEdges(graph: Graph) = streetEdges.stream().mapToDouble(e => getEdgeLen(e)).toArray.sum

  private def getMaxAllocation(edge: Edge) = edge.getAttribute("maxAllocation", classOf[Object]).asInstanceOf[Integer]

  private def getEdgeLen(edge: Edge) =
    edge.getAttribute("length", classOf[Object]).asInstanceOf[Double]
}
