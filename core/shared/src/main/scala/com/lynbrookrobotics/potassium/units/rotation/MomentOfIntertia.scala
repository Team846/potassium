package com.lynbrookrobotics.potassium.units.rotation

import com.lynbrookrobotics.potassium.units.{GenericDerivative, ScalarQuantity}
import squants.{PrimaryUnit, Quantity, SiBaseUnit, UnitConverter, UnitOfMeasure}
import squants.motion.AngularVelocity

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
  override def symbol: String = "kg‧m²"
}

class MomentOfInertia(val value: Double,
                      val unit: MomentOfInertiaUnit) extends ScalarQuantity[MomentOfInertia](value){
  override def dimension = ???

  def toKilogramsMetersSquared: Double = to(KilogramsMetersSquared)

  def *(angularAcceleration: AngularAcceleration): Torque = {
    val kilogramsMetersSquared = toKilogramsMetersSquared
    val radiansPerSecondSquared = angularAcceleration.toRadiansPerSecondSquared

    NewtonMeters(kilogramsMetersSquared * radiansPerSecondSquared)
  }
}