package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.control.OffloadedSignal.{EscPositionGains, EscVelocityGains}
import squants.motion.AngularVelocity
import squants.{Angle, Dimensionless}

sealed class OffloadedSignal

object OffloadedSignal {

  case class EscVelocityGains(p: Double, i: Double, d: Double, f: Double)

  case class EscPositionGains(p: Double, i: Double, d: Double)

}

case class VelocityControl(gains: EscVelocityGains, signal: Dimensionless) extends OffloadedSignal

case class PositionControl(gains: EscPositionGains, signal: Dimensionless) extends OffloadedSignal

case class OpenLoop(signal: Dimensionless) extends OffloadedSignal