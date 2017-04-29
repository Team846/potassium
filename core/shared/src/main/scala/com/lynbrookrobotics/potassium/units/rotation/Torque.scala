package com.lynbrookrobotics.potassium.units.rotation

import squants.{Dimension, PrimaryUnit, Quantity, SiBaseUnit, UnitConverter, UnitOfMeasure}

class Torque(val value: Double, val unit: TorqueUnit) extends Quantity[Torque] {
  override def dimension: Dimension[Torque] = ???

  def toNewtonMeters: Double = to(NewtonMeters)

  def / (that: MomentOfInertia): AngularAcceleration = {
    val kilogramsMetersSquared = that.toKilogramsMetersSquared
    RadiansPerSecondSquared(toNewtonMeters / kilogramsMetersSquared)
  }
}

trait TorqueUnit extends UnitOfMeasure[Torque] with UnitConverter {
  def apply[A](n: A)(implicit num: Numeric[A]) = {
    new Torque(num.toDouble(n), this)
  }

  /**
    * conversion factor from this to primary unit
    */
  val conversionFactor: Double
}

object NewtonMeters extends TorqueUnit with PrimaryUnit with  SiBaseUnit {
  override val symbol: String = "N‧m"
}
