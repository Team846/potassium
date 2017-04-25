package com.lynbrookrobotics.potassium.units

import squants.motion.{AngularVelocity, RadiansPerSecond}
import squants.time.{Seconds, TimeDerivative}
import squants.{Dimension, Energy, Force, Length, PrimaryUnit, Quantity, SiBaseUnit, UnitConverter, UnitOfMeasure}

trait MomentOfInertiaUnit extends UnitOfMeasure[MomentOfInertia] with UnitConverter {
  def apply[A](n: A)(implicit num: Numeric[A]) = {
    new MomentOfInertia(num.toDouble(n), this)
  }

  /**
    * conversion fact from this to primary unit
    */
  val conversionFactor: Double
}

object KilogramsMetersSquared extends MomentOfInertiaUnit
                              with PrimaryUnit
                              with SiBaseUnit {
  override def symbol: String = "ms²"
}

class MomentOfInertia(val value: Double,
                      val unit: MomentOfInertiaUnit) extends Quantity[MomentOfInertia]{
  if (value < 0) {
    throw new IllegalArgumentException("moment of inertia may not be negative")
  }

  override def dimension = ???

  def toKilogramsMetersSquared: Double = {
    value * this.unit.conversionFactor
  }

  def *(acceleration: GenericDerivative[AngularVelocity]): Torque = {
    val kilogramsMetersSquared = toKilogramsMetersSquared
    val radiansPerSecondSquared = acceleration.timeIntegrated.toRadiansPerSecond / acceleration.time.toSeconds

    NewtonMeters(kilogramsMetersSquared * radiansPerSecondSquared)
  }
}

trait TorqueUnit extends UnitOfMeasure[Torque] with UnitConverter {
  def apply[A](n: A)(implicit num: Numeric[A]) = {
    new Torque(num.toDouble(n), this)
  }

  /**
    * conversion fact from this to primary unit
    */
  val conversionFactor: Double
}

object NewtonMeters extends TorqueUnit with PrimaryUnit with  SiBaseUnit {
  override val symbol: String = "Nm"
}

object Conversions {
  implicit def fromEnergyToTorque(energy: Energy): Torque = {
    NewtonMeters(energy.toJoules)
  }
}

class Torque(val value: Double, val unit: TorqueUnit) extends Quantity[Torque] {
  override def dimension: Dimension[Torque] = ???

  def toNewtonMeters: Double = {
    value * this.unit.conversionFactor
  }

  def / (moment: MomentOfInertia): GenericDerivative[AngularVelocity] = {
    val newtonMeters = this.toNewtonMeters
    val kilogramsMetersSquared = moment.toKilogramsMetersSquared
    val radiansPerSecondSquared = newtonMeters / kilogramsMetersSquared

    new GenericValue(
      radiansPerSecondSquared,
      RadiansPerSecond).timeDerived
  }
}

