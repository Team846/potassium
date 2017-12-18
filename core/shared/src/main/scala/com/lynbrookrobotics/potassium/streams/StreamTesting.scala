package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import squants.Time

import scala.collection.mutable

object StreamTesting {
  def evaluateStreamForList[I, O](values: List[I])(getOutputStream: Stream[I] => Stream[O]): List[O] = {
    val outputQueue = mutable.Queue.empty[O]
    val (inputStream, push) = Stream.manual[I]
    val outputStream = getOutputStream(inputStream)
    val cancelOutputStream = outputStream.foreach(outputQueue.enqueue(_))

    values.foreach(push)

    cancelOutputStream.apply()

    outputQueue.toList
  }

  def evaluateStreamForList[I, O](values: List[I], dt: Time)(getOutputStream: Stream[I] => Stream[O]): List[O] = {
    implicit val (clock, tick) = ClockMocking.mockedClockTicker

    var valueToPush: Option[I] = None

    val outputQueue = mutable.Queue.empty[O]
    val inputStream = Stream.periodic[I](dt)(valueToPush.get)
    val outputStream = getOutputStream(inputStream)
    val cancelOutputStream = outputStream.foreach(outputQueue.enqueue(_))

    values.foreach { v =>
      valueToPush = Some(v)
      tick(dt)
    }

    cancelOutputStream.apply()

    outputQueue.toList
  }
}
