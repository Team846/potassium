package com.lynbrookrobotics.potassium.control.offload

import com.lynbrookrobotics.potassium.control.offload.EscConfig.{NativePositionGains, NativeVelocityGains}
import squants.Dimensionless

sealed class OffloadedSignal

object OffloadedSignal {

  case class VelocityControl(gains: NativeVelocityGains, signal: Dimensionless) extends OffloadedSignal

  case class PositionControl(gains: NativePositionGains, signal: Dimensionless) extends OffloadedSignal

  case class OpenLoop(signal: Dimensionless) extends OffloadedSignal

}