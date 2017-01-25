package com.lynbrookrobotics.potassium.config

trait TwoWaySignal[T] { self =>
  def value: T

  def reversePropagate(newValue: T): Unit

  def map[U](transform: T => U)(reverseTransform: (T, U) => T): TwoWaySignal[U] = {
    new TwoWaySignal[U] {
      override def reversePropagate(newValue: U): Unit = {
        self.reversePropagate(reverseTransform(self.value, newValue))
      }

      override def value: U = transform(self.value)
    }
  }

  def value_=(newValue: T): Unit = {
    reversePropagate(newValue)
  }
}

object TwoWaySignal{
  def apply[T](initialValue: T): TwoWaySignal[T] = new TwoWaySignal[T] {
    private var currentValue = initialValue

    override def value: T = currentValue

    override def reversePropagate(newValue: T): Unit = {
      currentValue = newValue
    }
  }
}
