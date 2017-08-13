package com.lynbrookrobotics.potassium.streams

class ZippedStream[A, B](aPeriodicity: ExpectedPeriodicity,
                         bPeriodicity: ExpectedPeriodicity) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = (aPeriodicity, bPeriodicity) match {
    case (Periodic(a), Periodic(b)) =>
      if (a == b) {
        Periodic(a)
      } else {
        NonPeriodic
      }

    case _ => NonPeriodic
  }

  private[this] var aSlot: Option[A] = None
  private[this] var bSlot: Option[B] = None

  def maybePublishPair(): Unit = {
    if (aSlot.isDefined && bSlot.isDefined) {
      publishValue((aSlot.get, bSlot.get))
      aSlot = None
      bSlot = None
    }
  }

  def receiveA(aValue: A): Unit = {
    aSlot = Some(aValue)
    maybePublishPair()
  }

  def receiveB(bValue: B): Unit = {
    bSlot = Some(bValue)
    maybePublishPair()
  }
}
