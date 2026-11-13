package com.barrybecker4.discreteoptimization.traffic.graph.model

import com.barrybecker4.graph.geometry.FloatLocation
import com.barrybecker4.discreteoptimization.traffic.signals.TrafficSignalType
import com.barrybecker4.discreteoptimization.traffic.signals.TrafficSignalType.DUMB_TRAFFIC_SIGNAL


case class Intersection(id: Int,
                        location: FloatLocation,
                        ports: IndexedSeq[Port],
                        signalType: TrafficSignalType = DUMB_TRAFFIC_SIGNAL) 
