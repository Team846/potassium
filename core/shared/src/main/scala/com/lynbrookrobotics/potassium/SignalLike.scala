package com.lynbrookrobotics.potassium

import scala.language.implicitConversions
import scala.language.higherKinds

/**
  * Represents a general continuous or periodic signal, for transformations that do not require
  * one of the types specifically.
  *
  * @tparam T the values from the signal
  * @tparam C the type (continuous or periodic) of the signal
  */
trait SignalLike[T, C[_]] {
  /**
    * Transforms the signal by applying a function to all values
    * @param f the function to transform values with
    * @tparam U the type of the resulting signal
    * @return a new signal with values transformed by the function
    */
  def map[U](f: T => U): SignalLike[U, C]

  /**
    * Converts the signal to a periodic signal
    * @return a periodic signal
    */
  def toPeriodic: PeriodicSignal[T]

  private[potassium] def toC: C[T]
}

class SignalSignalLike[T] private[potassium](v: Signal[T]) extends SignalLike[T, Signal] {
  override def map[U](f: (T) => U): SignalLike[U, Signal] =
    new SignalSignalLike[U](v.map(f))

  override def toPeriodic: PeriodicSignal[T] = v.toPeriodic

  override private[potassium]  def toC: Signal[T] = v
}

class PeriodicSignalSignalLike[T] private[potassium](v: PeriodicSignal[T]) extends SignalLike[T, PeriodicSignal] {
  override def map[U](f: (T) => U): SignalLike[U, PeriodicSignal] =
    new PeriodicSignalSignalLike[U](v.map((u, _) => f(u)))

  override def toPeriodic: PeriodicSignal[T] = v

  override private[potassium] def toC: PeriodicSignal[T] = v
}

object SignalLike {
  implicit def toC[T, C[_]](like: SignalLike[T, C]): C[T] = like.toC

  implicit def signalToSignalLike[T](i: Signal[T]): SignalSignalLike[T] =
    new SignalSignalLike[T](i)
  implicit def periodicSignalToSignalLike[T](i: PeriodicSignal[T]): PeriodicSignalSignalLike[T] =
    new PeriodicSignalSignalLike[T](i)
}
