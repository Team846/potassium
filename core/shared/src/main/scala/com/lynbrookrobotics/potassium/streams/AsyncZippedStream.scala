package com.lynbrookrobotics.potassium.streams

class AsyncZippedStream[A, B](master: ExpectedPeriodicity) extends Stream[(A, B)] {
  override val expectedPeriodicity: ExpectedPeriodicity = master

  private[this] var followerSlot: Option[B] = None

  def receiveMaster(masterValue: A): Unit = {
    if (followerSlot.isDefined) {
      publishValue((masterValue, followerSlot.get))
    }
  }

  def receiveFollower(followerValue: B): Unit = {
    followerSlot = Some(followerValue)
  }
}
