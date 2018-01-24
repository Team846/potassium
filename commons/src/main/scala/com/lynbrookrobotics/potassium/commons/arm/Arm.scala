package com.lynbrookrobotics.potassium.commons.arm

import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.units.{GenericDerivative, GenericIntegral, GenericValue}
import com.lynbrookrobotics.potassium.{Component, Signal}
import squants.Quantity

abstract class Arm {
  type ArmSignal
  type Feedback <: Quantity[Feedback]
  type Properties <: ArmProperties
  type Hardware <: ArmHardware

  trait ArmProperties {
    def positionGains: PIDConfig[Feedback,
      GenericValue[Feedback],
      GenericValue[Feedback],
      GenericDerivative[Feedback],
      GenericIntegral[Feedback],
      ArmSignal]
  }

  trait ArmHardware {
    def position: Stream[Feedback]
  }

  def positionControl(target: Stream[Feedback])
                     (implicit properties: Signal[Properties],
                      hardware: Hardware): (Stream[Feedback], Stream[ArmSignal])

  type Comp <: Component[ArmSignal]
}
