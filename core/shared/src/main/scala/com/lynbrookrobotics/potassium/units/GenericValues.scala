package com.lynbrookrobotics.potassium.units

import com.lynbrookrobotics.potassium.{PeriodicSignal, SignalLike}
import squants.{Dimension, Quantity, UnitOfMeasure}
import squants.time.{Seconds, Time, TimeDerivative, TimeIntegral}

import scala.language.implicitConversions

class GenericIntegral[T <: Quantity[T]](val value: Double, val uom: UnitOfMeasure[T])
  extends Quantity[GenericIntegral[T]] with TimeIntegral[GenericValue[T]] {
  override def unit: UnitOfMeasure[GenericIntegral[T]] = new UnitOfMeasure[GenericIntegral[T]] {
    override protected def converterFrom = identity

    override def symbol = uom.symbol + " * s"

    override protected def converterTo = identity

    override def apply[N](n: N)(implicit num: Numeric[N]) = new GenericIntegral[T](num.toDouble(n), uom)
  }

  override def dimension: Dimension[GenericIntegral[T]] = null

  override protected def timeDerived: GenericValue[T] = new GenericValue[T](value, uom)

  override protected def time: Time = Seconds(1)
}

class GenericDerivative[T <: Quantity[T]](val value: Double, val uom: UnitOfMeasure[T])
  extends Quantity[GenericDerivative[T]] with TimeDerivative[GenericValue[T]] {
  override def unit: UnitOfMeasure[GenericDerivative[T]] = new UnitOfMeasure[GenericDerivative[T]] {
    override protected def converterFrom = identity

    override def symbol = uom.symbol + " / s"

    override protected def converterTo = identity

    override def apply[N](n: N)(implicit num: Numeric[N]) = new GenericDerivative[T](num.toDouble(n), uom)
  }

  override def dimension: Dimension[GenericDerivative[T]] = null

  override def timeIntegrated: GenericValue[T] = new GenericValue[T](value, uom)

  override def time: Time = Seconds(1)
}

class GenericValue[T <: Quantity[T]](val value: Double, val uom: UnitOfMeasure[T])
  extends Quantity[GenericValue[T]] with TimeIntegral[GenericDerivative[T]] with TimeDerivative[GenericIntegral[T]] {
  override def unit: UnitOfMeasure[GenericValue[T]] = new UnitOfMeasure[GenericValue[T]] {
    override protected def converterFrom = identity

    override def symbol = uom.symbol

    override protected def converterTo = identity

    override def apply[N](n: N)(implicit num: Numeric[N]) = new GenericValue[T](num.toDouble(n), uom)
  }

  override def dimension: Dimension[GenericValue[T]] = null

  override protected def timeDerived: GenericDerivative[T] = new GenericDerivative[T](value, uom)

  override def timeIntegrated: GenericIntegral[T] = new GenericIntegral[T](value, uom)

  override def time: Time = Seconds(1)
}

object GenericValue {
  implicit def toGenericValue[T <: Quantity[T]](l: T): GenericValue[T] = new GenericValue[T](l.value, l.unit)
  implicit def fromGenericValue[T <: Quantity[T]](v: GenericValue[T]): T = v.uom.apply(v.value)

  implicit class ToGeneric[T <: Quantity[T]](l: T) {
    def toGeneric: GenericValue[T] = toGenericValue(l)
  }
}
