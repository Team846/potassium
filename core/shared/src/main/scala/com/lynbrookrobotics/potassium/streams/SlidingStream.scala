package com.lynbrookrobotics.potassium.streams

import scala.collection.mutable

class SlidingStream[T](parent: Stream[T], size: Int) extends Stream[Seq[T]] {
  override private[potassium] val parents = Seq(parent)
  override private[potassium] val streamType = "sliding"

  private var unsubscribe: Cancel = null
  private var currentData = mutable.Queue.empty[T]
  private var valuesLeft = size // start from 1

  override def tryFix(): Unit = {
    currentData = mutable.Queue.empty[T]
    valuesLeft = size
    val origUnsubscribe = unsubscribe
    subscribeToParents()
    origUnsubscribe.cancel()
  }

  override def subscribeToParents(): Unit = {
    unsubscribe = parent.foreach { v =>
      currentData.enqueue(v)

      if (valuesLeft > 0) {
        valuesLeft -= 1
      }

      if (valuesLeft == 0) {
        this.publishValue(currentData)
        currentData.dequeue()
      }
    }
  }

  override def unsubscribeFromParents(): Unit = {
    unsubscribe.cancel(); unsubscribe = null
  }

  override def checkRelaunch(): Unit = {
    throw new IllegalStateException("Sliding streams cannot be relaunched")
  }

  override val expectedPeriodicity = parent.expectedPeriodicity
  override val originTimeStream = parent.originTimeStream.map { o =>
    o.sliding(size).map(_.last)
  }
}