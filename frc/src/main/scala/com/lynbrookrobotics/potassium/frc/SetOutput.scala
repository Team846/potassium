package com.lynbrookrobotics.potassium.frc

import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import edu.wpi.first.wpilibj.Joystick

/**
  * Created by davintjong on 1/17/17.
  */
class SetOutput(joystick: Joystick, port: Int, power: Boolean) extends ContinuousTask {
  override def onStart(): Unit = {
    joystick.setOutput(port, power)
  }

  override def onEnd(): Unit = {
    joystick.setOutput(port, false)
  }
}
