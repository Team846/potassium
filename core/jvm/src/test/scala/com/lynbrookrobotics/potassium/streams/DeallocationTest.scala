package com.lynbrookrobotics.potassium.streams

import com.lynbrookrobotics.potassium.ClockMocking
import org.scalatest.FunSuite

import scala.ref.WeakReference

class DeallocationTest extends FunSuite {
  private def testDeallocates[T <: AnyRef](value: WeakReference[T]): Unit = {
    var count = 0

    while (value.get.isDefined) {
      if (count >= 10) {
        assert(false, "Did not deallocate")
      }

      System.gc()
      count += 1
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

    val parent = Stream.manual[Int]
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
}