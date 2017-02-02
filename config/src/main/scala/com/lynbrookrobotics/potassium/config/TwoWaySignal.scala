package com.lynbrookrobotics.potassium.config

import scala.ref.WeakReference

case class ChildReference[T, U](child: WeakReference[TwoWaySignal[U]], transform: T => U) {
  type TargetType = U

  val tt: WeakReference[TwoWaySignal[TargetType]] = child.asInstanceOf[WeakReference[TwoWaySignal[TargetType]]]
}

trait TwoWaySignal[T] { self =>
  private var currentValue: T = null.asInstanceOf[T]

  private var children: List[ChildReference[T, _]] = List.empty

  def value: T = currentValue

  def handlePropogate(newValue: T): Unit

  def reversePropagate(newValue: T): Unit = {
    currentValue = newValue
    handlePropogate(newValue)
  }

  def map[U](transform: T => U)(reverseTransform: (T, U) => T): TwoWaySignal[U] = {
    val mappedSignal = new TwoWaySignal[U] {
      override def handlePropogate(newValue: U): Unit = {
        self.reversePropagate(reverseTransform(self.value, newValue))
      }
    }

    children = ChildReference(new WeakReference(mappedSignal), transform) :: children

    mappedSignal.updateNoReverse(transform(currentValue))

    mappedSignal
  }

  private[TwoWaySignal] def updateNoReverse(newValue: T): Unit = {
    if (currentValue != newValue) {
      currentValue = newValue

      children = children.flatMap { cr =>
        cr.tt.get.map { child =>
          child.updateNoReverse(cr.transform(currentValue).asInstanceOf[cr.TargetType])
          cr
        }
      }
    }
  }

  def value_=(newValue: T): Unit = {
    if (currentValue != newValue) {
      updateNoReverse(newValue)
      reversePropagate(newValue)
    }
  }
}

object TwoWaySignal{
  def apply[T](initialValue: T): TwoWaySignal[T] = new TwoWaySignal[T] {
    override def handlePropogate(newValue: T): Unit = {}
  }
}
