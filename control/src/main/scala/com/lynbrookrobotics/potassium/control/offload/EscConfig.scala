package com.lynbrookrobotics.potassium.control.offload

import com.lynbrookrobotics.potassium.control.{PIDConfig, PIDProperUnitsConfig}
import com.lynbrookrobotics.potassium.units.{GenericIntegral, GenericValue, Ratio}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Dimensionless, Length, Percent, Quantity, Time, Velocity}

case class EscConfig[Base <: Quantity[Base]](maxNativeOutput: Int = 1023,
                                             nativeTimeUnit: Time = Milliseconds(100),
                                             loopTime: Time = Milliseconds(1),
                                             ticksPerUnit: Ratio[Dimensionless, Base])

object EscConfig {

  case class NativeVelocityGains(p: Double, i: Double, d: Double, f: Double)

  case class NativePositionGains(p: Double, i: Double, d: Double)

  def forwardToAngularVelocityGains(g: PIDProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]#Full)
                                   (implicit c: EscConfig[Length]): NativeVelocityGains = {
    val outOvrPercent = c.maxNativeOutput / Percent(100).toEach
    val ftOvrTicks = c.ticksPerUnit.den.toFeet / c.ticksPerUnit.num.toEach
    val escTimeOvrSec = 1 /*native time unit*/ / c.nativeTimeUnit.toSeconds
    val loopOvrSec = 1 /*loop time*/ / c.loopTime.toSeconds

    import g.{kd, kf, ki, kp}
    NativeVelocityGains(
      p = (kp.num.toEach / kp.den.toFeetPerSecond) * ftOvrTicks * escTimeOvrSec * outOvrPercent,
      i = (ki.num.toEach / ki.den.toFeet) * ftOvrTicks * outOvrPercent,
      d = (kd.num.toEach / kd.den.toFeetPerSecondSquared) * ftOvrTicks * escTimeOvrSec * loopOvrSec * outOvrPercent,
      f = (kf.num.toEach / kf.den.toFeetPerSecond) * ftOvrTicks * escTimeOvrSec * outOvrPercent
    )
  }

  def forwardToAngularPositionGains(g: PIDConfig[Length, Length, GenericValue[Length], Velocity, GenericIntegral[Length], Dimensionless])
                                   (implicit c: EscConfig[Length]): NativePositionGains = {
    val outOvrPercent = c.maxNativeOutput / Percent(100).toEach
    val ftOvrTicks = c.ticksPerUnit.den.toFeet / c.ticksPerUnit.num.toEach
    val escTimeOvrSec = 1 /*native time unit*/ / c.nativeTimeUnit.toSeconds
    val loopOvrSec = 1 /*loop time*/ / c.loopTime.toSeconds
    /*seconds*/
    val t = Seconds(1)

    import g.{kd, ki, kp}
    NativePositionGains(
      p = (kp.num.toEach / kp.den.toFeet) * ftOvrTicks * outOvrPercent,
      i = ((ki.num.toEach / t).toHertz / (ki.den / t).toFeet) * ftOvrTicks * outOvrPercent / loopOvrSec,
      d = (kd.num.toEach / kd.den.toFeetPerSecond) * ftOvrTicks * outOvrPercent * escTimeOvrSec
    )
  }

  def ticks(x: GenericIntegral[Length])(implicit c: EscConfig[Length]): Dimensionless = ticks(x / c.loopTime)

  def ticks(x: Length)(implicit c: EscConfig[Length]): Dimensionless = c.ticksPerUnit * x

  def ticks(x: Velocity)(implicit c: EscConfig[Length]): Dimensionless = ticks(x * c.nativeTimeUnit)

  def ticks(x: Acceleration)(implicit c: EscConfig[Length]): Dimensionless = ticks(x * c.loopTime)
}