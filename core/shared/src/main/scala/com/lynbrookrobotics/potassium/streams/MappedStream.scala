package com.lynbrookrobotics.potassium.streams

abstract class MappedStream[T, U](parent: Stream[T]) extends Stream[U] {
  def applyTransform(in: T): U

  override private[potassium] val parents = Seq(parent)
  override private[potassium] val streamType = "map"

  var unsubscribe: Cancel = null

  def receiveValue(value: T): Unit = {
    publishValue(applyTransform(value))
  }

  override def subscribeToParents(): Unit = {
    unsubscribe = parent.foreach(v => this.receiveValue(v))
  }

  override def unsubscribeFromParents(): Unit = {
    unsubscribe.cancel(); unsubscribe = null
  }

  override val expectedPeriodicity = parent.expectedPeriodicity
  override val originTimeStream = parent.originTimeStream
}
