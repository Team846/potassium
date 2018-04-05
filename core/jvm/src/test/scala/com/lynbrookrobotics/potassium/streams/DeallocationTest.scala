package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import com.lynbrookrobotics.potassium.clock.Clock
import org.scalatest.FunSuite
import squants.Time
import squants.time.Seconds

import scala.ref.WeakReference

class DeallocationTest extends FunSuite {
  implicit val clock = new Clock {
    /**
      * Schedules a periodic execution
      *
      * @param period the period between executions
      * @param thunk  the block of code to execute
      * @return a function to cancel the execution
      */
    override def apply(period: Time)(thunk: (Time) => Unit): Cancel = ???

    /**
      * Schedules a single execution of a function
      *
      * @param delay the initial delay before running the function
      * @param thunk the function to execute after the delay
      */
    override def singleExecution(delay: Time)(thunk: => Unit): Cancel = ???

    override def currentTime: Time = Seconds(0)
  }

  private def testDeallocates[T <: AnyRef](value: WeakReference[T]): Unit = {
    var count = 0

    while (value.get.isDefined) {
      if (count >= 30) {
        assert(false, "Did not deallocate")
      }

      System.gc()
      count += 1
    }
  }

  private def testDoesNotDeallocate[T <: AnyRef](value: WeakReference[T]): Unit = {
    (1 to 30).foreach(_ => System.gc())

    if (value.get.isEmpty) {
      assert(false, "Did deallocate")
    }
  }

  def testStreamDeallocation(getStream: (() => Stream[Int]) => Stream[Int]): Unit = {
    var inputPublishes = List[Int => Unit]()
    var returnStream = getStream(() => {
      val ret = Stream.manual[Int]
      inputPublishes = ret._2 :: inputPublishes
      ret._1
    })
    var cancel = returnStream.foreach(_ => ())
    inputPublishes.foreach(_.apply(0))
    cancel.cancel()
    cancel = null
    val ptr = WeakReference(returnStream)
    returnStream = null
    testDeallocates(ptr)
  }

  test("Mapped streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().map(_ + 1)
    })
  }

  test("Zipped streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().zip(newStream()).map(t => t._1 + t._2)
    })
  }

  test("Async zipped streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().zipAsync(newStream()).map(t => t._1 + t._2)
    })
  }

  test("Eager streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().zipEager(newStream()).map(t => t._1 + t._2)
    })
  }

  test("Zipped with time streams can be deallocated") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker

    val parent = Stream.manualWithTime[Int]

    var returnStream = parent._1.zipWithTime
    var cancel = returnStream.foreach(_ => ())
    parent._2.apply(0)
    cancel.cancel()
    cancel = null
    val ptr = WeakReference(returnStream)
    returnStream = null

    testDeallocates(ptr)
  }

  test("Sliding streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().sliding(2).map(q => q.head + q.last)
    })
  }

  test("Synced streams can be deallocated") {
    testStreamDeallocation(newStream => {
      newStream().syncTo(newStream())
    })
  }

  test("Mapped intermediate streams are not deallocated") {
    val (parent, pub) = Stream.manual[Int]
    var middle = parent.map(_ + 1)
    val ptr = WeakReference(middle)
    val last = middle.map(_ + 1)
    middle = null

    testDoesNotDeallocate(ptr)
  }

  test("Zipped intermediate streams are not deallocated") {
    val (parent, _) = Stream.manual[Int]
    val (other, _) = Stream.manual[Int]
    var middle = parent.zip(other)
    val ptr = WeakReference(middle)
    val last = middle.map(identity)
    middle = null

    testDoesNotDeallocate(ptr)
  }
}
