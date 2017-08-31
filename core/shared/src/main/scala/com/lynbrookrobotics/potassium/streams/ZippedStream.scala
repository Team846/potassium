package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.clock.Clock

class ZippedStream[A, B](private val parentA: Stream[A], private val parentB: Stream[B]) extends Stream[(A, B)] {
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
