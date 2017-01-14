package com.lynbrookrobotics.potassium

import scala.language.implicitConversions
import scala.language.higherKinds

/**
  * Represents a general continuous or periodic signal, for transformations that do not require
  * one of the types specifically.
  *
  * @tparam T the values from the signal
  */
trait SignalLike[T] {
  /**
    * Transforms the signal by applying a function to all values
    * @param f the function to transform values with
    * @tparam U the type of the resulting signal
    * @return a new signal with values transformed by the function
    */
  def map[U](f: T => U): SignalLike[U]

  /**
    * Converts the signal to a periodic signal
    * @return a periodic signal
    */
  def toPeriodic: PeriodicSignal[T]
}

class SignalSignalLike[T] private[potassium](v: Signal[T]) extends SignalLike[T] {
  override def map[U](f: (T) => U): SignalLike[U] =
    new SignalSignalLike[U](v.map(f))

  override def toPeriodic: PeriodicSignal[T] = v.toPeriodic
}

class PeriodicSignalSignalLike[T] private[potassium](v: PeriodicSignal[T]) extends SignalLike[T] {
  override def map[U](f: (T) => U): SignalLike[U] =
    new PeriodicSignalSignalLike[U](v.map(f))

  override def toPeriodic: PeriodicSignal[T] = v
}

object SignalLike {
  implicit def signalToSignalLike[T](i: Signal[T]): SignalSignalLike[T] =
    new SignalSignalLike[T](i)
  implicit def periodicSignalToSignalLike[T](i: PeriodicSignal[T]): PeriodicSignalSignalLike[T] =
    new PeriodicSignalSignalLike[T](i)
}
