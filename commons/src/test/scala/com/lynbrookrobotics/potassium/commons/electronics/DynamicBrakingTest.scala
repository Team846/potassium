package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.Stream
import org.scalatest.FunSuite
import squants.Percent

import scala.collection.mutable
import org.scalatest.prop.Checkers
import org.scalacheck.Gen
import org.scalacheck.Prop._

class DynamicBrakingTest extends FunSuite with Checkers {
  // TODO: better name and move into testing utilities
  private def evaluateStreamForList[I, O](values: List[I])(getOutputStream: Stream[I] => Stream[O]): List[O] = {
    val outputQueue = mutable.Queue.empty[O]
    val (inputStream, push) = Stream.manual[I]
    val outputStream = getOutputStream(inputStream)
    val cancelOutputStream = outputStream.foreach(outputQueue.enqueue(_))

    values.foreach(push)

    cancelOutputStream.apply()

    outputQueue.toList
  }

  def patternToList(pattern: String): List[Boolean] = {
    pattern.toList.map(_ == '|')
  }

  test("Dither pattern - 0%") {
    assert(evaluateStreamForList(
      List.fill(8)(Percent(0))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("........"))
  }

  test("Dither pattern - 20%") {
    assert(evaluateStreamForList(
      List.fill(10)(Percent(20))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("|...|....|"))
  }

  test("Dither pattern - 25%") {
    assert(evaluateStreamForList(
      List.fill(8)(Percent(25))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("|...|..."))
  }

  test("Dither pattern - 50%") {
    assert(evaluateStreamForList(
      List.fill(8)(Percent(50))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("|.|.|.|."))
  }

  test("Dither pattern - 75%") {
    assert(evaluateStreamForList(
      List.fill(8)(Percent(75))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("|||.|||."))
  }

  test("Dither pattern - 100%") {
    assert(evaluateStreamForList(
      List.fill(8)(Percent(100))
    ) { inputStream =>
      DynamicBraking.ditherPattern(inputStream)
    } == patternToList("||||||||"))
  }

  test("Dither patterns over lots of values have close to the expected target percent") {
    val arbitraryBrakePower = Gen.chooseNum(0, 100).map(Percent(_))

    val valuesCount = 1000

    check {
      forAll(arbitraryBrakePower) { brakePower =>
        val patternValues = evaluateStreamForList(List.fill(valuesCount)(brakePower)) { inputStream =>
          DynamicBraking.ditherPattern(inputStream)
        }

        val percentBrake = patternValues.count(identity).toDouble / patternValues.size

        math.abs(percentBrake * 100 - brakePower.toPercent) <= 0.25
      }
    }
  }

  test("Dither patterns with changed brake power over lots of values have close to the expected target percent") {
    val arbitraryBrakePower = Gen.chooseNum(0, 100).map(Percent(_))
    val arbitraryBrakePowerPair = for {
      a <- arbitraryBrakePower
      b <- arbitraryBrakePower
    } yield (a, b)

    val valuesCount = 1000

    check {
      forAll(arbitraryBrakePowerPair) { case (brakeA, brakeB) =>
        val patternValues = evaluateStreamForList(List.fill(valuesCount)(brakeA) ++ List.fill(valuesCount)(brakeB)) { inputStream =>
          DynamicBraking.ditherPattern(inputStream)
        }

        val percentBrakeA = patternValues.take(valuesCount).count(identity).toDouble / valuesCount
        val percentBrakeB = patternValues.drop(valuesCount).count(identity).toDouble / valuesCount

        // slightly increased thresholds because takes some ticks to adjust
        (math.abs(percentBrakeA * 100 - brakeA.toPercent) <= 0.25) && (math.abs(percentBrakeB * 100 - brakeB.toPercent) <= 0.75)
      }
    }
  }

  test("End-to-end dynamic braking when slowing from 100% to 50%") {
    assert(evaluateStreamForList(
      List.fill(8)((Percent(50), Percent(100)))
    ) { stream =>
      val targetStream = stream.map(_._1)
      val speedStream = stream.map(_._2)

      DynamicBraking.dynamicBrakingOutput(targetStream, speedStream).map(_.isEmpty) // true when option is None (braking)
    } == patternToList("|.|.|.|."))
  }

  test("End-to-end dynamic braking when accelerating from 50% to 100%") {
    assert(evaluateStreamForList(
      List.fill(8)((Percent(100), Percent(50)))
    ) { stream =>
      val targetStream = stream.map(_._1)
      val speedStream = stream.map(_._2)

      DynamicBraking.dynamicBrakingOutput(targetStream, speedStream)
    } == List.fill(8)(Some(Percent(100))))
  }

  test("End-to-end dynamic braking when reversing direction from 50% to -25%") {
    assert(evaluateStreamForList(
      List.fill(8)((Percent(-25), Percent(50)))
    ) { stream =>
      val targetStream = stream.map(_._1)
      val speedStream = stream.map(_._2)

      DynamicBraking.dynamicBrakingOutput(targetStream, speedStream)
    } == List.fill(8)(Some(Percent(-50))))
  }
}
