package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.events.{ContinuousEvent, ImpulseEvent}
import squants.Time

/**
  * Represents a continuous stream of values, where the current value
  * can be calculated on demand.
  *
  * @tparam T the type of value generated by the signal
  */
abstract class Signal[+T] { self =>
  /**
    * Calculates the latest value from the signal
    * @return the latest value of the signal
    */
  def get: T

  /**
    * Transforms the signal by applying a function to all values
    * @param f the function to transform values with
    * @tparam U the type of the resulting signal
    * @return a new signal with values transformed by the function
    */
  def map[U](f: T => U): Signal[U] = new Signal[U] {
    def get: U = f(self.get)
  }

  /**
    * Combines the signal with another signal into a signal of tuples
    * @param other the signal to combine with
    * @tparam O the type of values of the other signal
    * @return a new signal that returns tuples with one value from each signal
    */
  def zip[O](other: Signal[O]): Signal[(T, O)] = new Signal[(T, O)] {
    def get: (T, O) = (self.get, other.get)
  }

  /**
    * Generates an event from the signal by checking a condition
    * @param condition the condition to trigger the event when true
    * @param polling the logic for polling the signal for new data
    * @return a continuous event that is active when the condition is true
    */
  def filter(condition: T => Boolean)(implicit polling: ImpulseEvent): ContinuousEvent = {
    new ContinuousEvent(condition(get))
  }
}

class ConstantSignal[T](v: T) extends Signal[T] {
  def get: T = v

  override def map[U](f: (T) => U): Signal[U] = {
    Signal.constant(f(v))
  }

  override def zip[O](other: Signal[O]): Signal[(T, O)] = {
    if (other.isInstanceOf[ConstantSignal[O]]) {
      Signal.constant((v, other.get))
    } else {
      super.zip(other)
    }
  }

  override def filter(condition: (T) => Boolean)(implicit polling: ImpulseEvent): ContinuousEvent = {
    val conditionResult = condition(v)
    new ContinuousEvent(conditionResult)
  }
}

object Signal {
  /**
    * Creates a new signal given the way to calculate values
    * @param f a call-by-name value that returns the latest value
    * @tparam T the type of the signal
    * @return a signal that produces values with f
    */
  def apply[T](f: => T): Signal[T] = new Signal[T] {
    def get: T = f
  }

  /**
    * Creates a signal that returns a constant value
    * @param v the value to return
    * @tparam T the type of the signal
    * @return a signal with a constant value
    */
  def constant[T](v: T): Signal[T] = new ConstantSignal[T](v)
}
