package com.lynbrookrobotics.potassium.lighting

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.Component
import squants.time.Time

class LightingComponent(numLEDs: Int, comm: TwoWayComm, period: Time)(implicit clock: Clock) extends Component[Int]() {

  var debug = false

  override def defaultController: Stream[Int] = Stream.periodic(period)(0)
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
