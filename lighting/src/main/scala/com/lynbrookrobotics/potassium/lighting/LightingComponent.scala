package com.lynbrookrobotics.potassium.lighting

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import squants.time.Milliseconds

class LightingComponent(numLEDs: Int, comm: TwoWayComm)(implicit clock: Clock) extends Component[Int](Milliseconds(20)) {

  val debug = false

  override def defaultController: PeriodicSignal[Int] = Signal[Int](0).toPeriodic
  /**
    * Applies the latest control signal value.
    *
    * @param signal the signal value to act on
    */
  override def applySignal(signal: Int): Unit = {
    if (comm.isConnected) {
      comm.newData(signal)
      if(debug){
        println(comm.pullLog)
      }
    } else {
      if(debug) {
        println("Error: Serial device not connected")
      }
    }
  }
}
