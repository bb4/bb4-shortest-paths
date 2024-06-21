package com.barrybecker4.discreteoptimization.traffic

import com.barrybecker4.discreteoptimization.common.graph.GraphTstUtil
import com.barrybecker4.discreteoptimization.common.graph.visualization.{GraphStreamAdapter, GraphViewerFrame}
import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficViewerFrame


/**
 * Ideas:
 * - Within an intersection, examine the sprites on intersection edges and the edges leading into the intersection.
 * - Sprites should be aware of how distant the next sprite in front is, if any.
 *     - There should be an optimal distance to it
 *     - If >= distantThreshold, don't try to catch up
 *     - If < distanceThreshold, and > optimalDistance, then try to speed up a little to get closer to optimal
 *     - If < optimalDistance, then break until >= optimalDistance
 *     - If Signal says to slow down, then brake to slow speed
 *     - If upcoming Signal is red, then start to smoothly slow so that we can be stopped by the time we get there
 * - For green lights, we can travel into an intersection, but there are still 2 cases where we may have to stop
 *     - when turning right, if there is a competing car or cars, then wait. Only go if you can make it through before next car
 *     - when turning left, consider oncoming traffic. It has the right of way. You can only go when there is a break sufficient to go.
 *
 *  - Metrics in order to measure efficiency of the traffic system
 *     - Total distance traveled by all cars.
 *     - Mean, median distance
 *     - Min distance (should not be too low)
 *
 *  - Add semaphore signal
 *    - If a car is within double yellow distance, then try to take semaphore and stay green.
 *      Others go yellow, then red.
 *    - A light stays green until double yellow empty or time exceeded.
 *      If time exceeded, then turn yellow, else straight to red.
 *    - The next street(i.e. intersection node port) with cars waiting gets the semaphore and turns green.
 *    - If no cars coming, all can be green, and semaphore up for grabs.
 *
 * Done
 * - Sprite attachment and detachment to/from edges will also make the Edge aware of which sprites are currently attached to it.
 * - Each Intersection should have a SignalStrategy / TrafficSignal
 *    - it can be configurable per node in the traffic config file
 *    - Enum like DumbTrafficLight, SmartTrafficLight, CollisionAvoidance
 */
object TrafficApp extends App {

  val frame = new TrafficViewerFrame()

}
