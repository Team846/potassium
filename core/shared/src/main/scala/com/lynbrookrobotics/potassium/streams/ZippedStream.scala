package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock

class ZippedStream[A, B](parentA: Stream[A], parentB: Stream[B]) extends Stream[(A, B)] {
  var parentAUnsubscribe: Cancel = null
  var parentBUnsubscribe: Cancel = null

  override def subscribeToParents(): Unit = {
    parentAUnsubscribe = parentA.foreach(this.receiveA)
    parentBUnsubscribe = parentB.foreach(this.receiveB)
  }

  override def unsubscribeFromParents(): Unit = {
    parentAUnsubscribe.cancel(); parentAUnsubscribe = null
    parentBUnsubscribe.cancel(); parentBUnsubscribe = null
  }

  override val expectedPeriodicity: ExpectedPeriodicity = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a eq b) {
        Periodic(a)
      } else {
        NonPeriodic
      }

    case _ => NonPeriodic
  }

  override val originTimeStream = (parentA.expectedPeriodicity, parentB.expectedPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a eq b) {
        parentA.originTimeStream
      } else {
        None // TODO: alternatives?
      }

    case _ => None
  }

  private[this] var aSlot: Option[A] = None
  private[this] var bSlot: Option[B] = None

  def attemptPublish(): Unit = {
    if (aSlot.isDefined && bSlot.isDefined) {
      publishValue((aSlot.get, bSlot.get))
      aSlot = None
      bSlot = None
    }
  }

  def receiveA(aValue: A): Unit = {
    aSlot = Some(aValue)
    attemptPublish()
  }

  def receiveB(bValue: B): Unit = {
    bSlot = Some(bValue)
    attemptPublish()
  }
}
