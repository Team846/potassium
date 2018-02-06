package com.lynbrookrobotics.potassium.control.offload

import com.lynbrookrobotics.potassium.control.offload.EscConfig.{NativePositionGains, NativeVelocityGains}
import squants.Dimensionless

sealed class OffloadedSignal

object OffloadedSignal {

  case class VelocityPIDF(gains: NativeVelocityGains, signal: Dimensionless) extends OffloadedSignal

  case class PositionPID(gains: NativePositionGains, signal: Dimensionless) extends OffloadedSignal

  case class VelocityBangBang(forwardWhenBelow: Boolean, reverseWhenAbove: Boolean, signal: Dimensionless) extends OffloadedSignal

  case class PositionBangBang(forwardWhenBelow: Boolean, reverseWhenAbove: Boolean, signal: Dimensionless) extends OffloadedSignal

  case class OpenLoop(signal: Dimensionless) extends OffloadedSignal

}