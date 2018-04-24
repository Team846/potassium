package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.CANifier
import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.streams.Stream
import edu.wpi.first.wpilibj.DriverStation
import edu.wpi.first.wpilibj.DriverStation.Alliance

class LEDController(val coreTicks: Stream[Unit], val alliance: Signal[DriverStation.Alliance])(
  implicit hardware: LEDControllerHardware
) extends Component[Color] {
  override def defaultController: Stream[Color] = coreTicks.mapToConstant {
    if (alliance.get == Alliance.Red) {
      Color.rgb(255, 0, 0)
    } else {
      Color.rgb(0, 0, 255)
    }
  }

  /**
   * Applies the latest control signal value.
   *
   * @param signal the signal value to act on
   */
  override def applySignal(signal: Color): Unit = {
    hardware.canifier.setLEDOutput(signal.green / 255.0, CANifier.LEDChannel.LEDChannelA)
    hardware.canifier.setLEDOutput(signal.red / 255.0, CANifier.LEDChannel.LEDChannelB)
    hardware.canifier.setLEDOutput(signal.blue / 255.0, CANifier.LEDChannel.LEDChannelC)
  }
}
