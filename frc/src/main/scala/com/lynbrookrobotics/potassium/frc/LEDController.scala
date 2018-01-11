package com.lynbrookrobotics.potassium.frc

import java.awt.Color

import com.ctre.phoenix.CANifier
import com.lynbrookrobotics.potassium.Component
import com.lynbrookrobotics.potassium.streams.Stream
import edu.wpi.first.wpilibj.DriverStation.Alliance

class LEDController(val coreTicks: Stream[Unit])(implicit hardware: LEDControllerHardware) extends Component[Color]{
  override def defaultController: Stream[Color] = coreTicks.mapToConstant{
    if(hardware.driverStation.getAlliance == Alliance.Red){
      Color.RED
    } else {
      Color.BLUE
    }
  }

  /**
    * Applies the latest control signal value.
    *
    * @param signal the signal value to act on
    */
  override def applySignal(signal: Color): Unit = {
    hardware.canifier.setLEDOutput(signal.getRed / 255.0, CANifier.LEDChannel.LEDChannelA)
    hardware.canifier.setLEDOutput(signal.getGreen / 255.0, CANifier.LEDChannel.LEDChannelB)
    hardware.canifier.setLEDOutput(signal.getBlue / 255.0, CANifier.LEDChannel.LEDChannelC)
  }
}
