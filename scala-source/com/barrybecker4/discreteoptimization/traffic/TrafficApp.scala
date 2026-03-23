package com.barrybecker4.discreteoptimization.traffic

import com.barrybecker4.discreteoptimization.traffic.viewer.TrafficViewerFrame


/**
 * Ideas:
 *  - Move traffic code to separate repo.
 *  - Metrics in order to measure efficiency of the traffic system.
 *     - Total distance traveled by all cars. Cumulative
 *     - Total time that the simulation ran for
 *     - average speed: Total distance / total time
 *     - Current average speed: Distance for last 5s / 5s.
 *     - Num stopped, slow, fast cars
 *     - Each car should keep track of its distance traveled.
 *       At the end show,
 *        - the Mean, median, and b min distance (should not be too low) traveled by all cars.
 *        - the Mean, median, and min speed traveled by all cars.
 *     The value to optimize might be the mean + the median + the min speed.
 *     Which is better? Everyone traveling fast, but one car stopped for the whole time,
 *     or all cars go slow, but none stopped for any significant time.
 *
 *
 *  - Avoid gridlock. If cars are stopped in one of the outgoing intersection streets, then we need to turn red.
 *     - add a lastVehicle attribute to streets.
 *     - Use the lastVehicle in the traffic flow calculation.

 time     | <total time> |   last 2 seconds
 distance | <total dist> |   <inc distance>
 speed    | <avg speed>  |   <current avg speed>
 */
object TrafficApp {

  def main(args: Array[String]): Unit = {
    new TrafficViewerFrame()
    ()
  }

}
