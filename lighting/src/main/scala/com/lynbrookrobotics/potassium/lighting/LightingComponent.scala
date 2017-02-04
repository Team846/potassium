package com.lynbrookrobotics.potassium.lighting

import java.awt.Color

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import squants.time.Milliseconds

class LightingComponent(numLEDs: Int, comm: TwoWayComm)(implicit clock: Clock) extends Component[List[Color]](Milliseconds(20)) {
  override def defaultController: PeriodicSignal[List[Color]] = staticColor(Color.BLACK).toPeriodic

  /**
    * Applies the latest control signal value.
    *
    * @param signal the signal value to act on
    */
  override def applySignal(signal: List[Color]): Unit = {
    def helper(i: Int, color: Color): String = {
      val r = color.getRed
      val g = color.getGreen
      val b = color.getBlue
      f"$i%03d$r%03d$g%03d$b%03d"
    }

    if (comm.isConnected) {
      signal.zipWithIndex.foreach { case (c, i) =>
        comm.pushDataToQueue(helper(i, c))
      }
    } else {
      println("Error: Serial device not connected")
    }
  }

  def staticColor(color: Color): Signal[List[Color]] = {
    Signal(List.fill(numLEDs)(color))
  }
}
