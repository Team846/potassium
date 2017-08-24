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
    override def singleExecution(delay: Time)(thunk: => Unit): Unit = ???

    override def currentTime: Time = Seconds(0)
  }

  private def testDeallocates[T <: AnyRef](value: WeakReference[T]): Unit = {
    var count = 0

    while (value.get.isDefined) {
      if (count >= 20) {
        assert(false, "Did not deallocate")
      }

      System.gc()
      count += 1
    }
  }

  private def testDoesNotDeallocate[T <: AnyRef](value: WeakReference[T]): Unit = {
    (1 to 20).foreach(_ => System.gc())

    if (value.get.isEmpty) {
      assert(false, "Did deallocate")
    }
  }

  test("Mapped streams can be deallocated") {
    val parent = Stream.manual[Int]
    val ptr = WeakReference(parent._1.map(_ + 1))
    parent._2(0)


    testDeallocates(ptr)
  }

  test("Zipped streams can be deallocated") {
    val parent1 = Stream.manual[Int]
    val parent2 = Stream.manual[Int]
    val ptr = WeakReference(parent1._1.zip(parent2._1))
    parent1._2(0)
    parent2._2(0)

    testDeallocates(ptr)
  }

  test("Async zipped streams can be deallocated") {
    val parent1 = Stream.manual[Int]
    val parent2 = Stream.manual[Int]
    val ptr = WeakReference(parent1._1.zipAsync(parent2._1))
    parent1._2(0)
    parent2._2(0)

    testDeallocates(ptr)
  }

  test("Eager streams can be deallocated") {
    val parent1 = Stream.manual[Int]
    val parent2 = Stream.manual[Int]
    val ptr = WeakReference(parent1._1.zipEager(parent2._1))
    parent1._2(0)
    parent2._2(0)

    testDeallocates(ptr)
  }

  test("Zipped with time streams can be deallocated") {
    implicit val (clock, update) = ClockMocking.mockedClockTicker

    val parent = Stream.manualWithTime[Int]
    val ptr = WeakReference(parent._1.zipWithTime)
    parent._2(0)

    testDeallocates(ptr)
  }

  test("Sliding streams can be deallocated") {
    val parent = Stream.manual[Int]
    val ptr = WeakReference(parent._1.sliding(2))
    parent._2(0)

    testDeallocates(ptr)
  }

  test("Synced streams can be deallocated") {
    val ref = Stream.manual[Int]
    val str = Stream.manual[Int]
    val ptr = WeakReference(str._1.syncTo(ref._1))
    ref._2(0)

    testDeallocates(ptr)
  }

  test("Mapped intermediate streams are not deallocated") {
    val (parent, pub) = Stream.manual[Int]
    var middle = parent.map(_ + 1)
    val ptr = WeakReference(middle)
    val last = middle.map(_ + 1)
    middle = null

    testDoesNotDeallocate(ptr)
  }
}
