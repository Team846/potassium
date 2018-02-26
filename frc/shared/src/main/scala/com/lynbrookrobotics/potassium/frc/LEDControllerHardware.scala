package com.lynbrookrobotics.potassium.frc

import com.ctre.phoenix.CANifier
import edu.wpi.first.wpilibj.DriverStation

case class LEDControllerHardware(canifier: CANifier)

object LEDControllerHardware {
  def apply(config: LEDControllerConfig): LEDControllerHardware = {
    LEDControllerHardware(
      new CANifier(config.port)
    )
  }
}
