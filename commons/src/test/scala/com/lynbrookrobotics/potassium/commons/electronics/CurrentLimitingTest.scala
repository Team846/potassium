package com.lynbrookrobotics.potassium.commons.electronics

import com.lynbrookrobotics.potassium.streams.StreamTesting._
import org.scalatest.FunSuite
import squants.Percent
import squants.electro.{Amperes, Volts}
import squants.motion.{FeetPerSecond, Velocity}
import squants.time.{Milliseconds, Seconds}

class CurrentLimitingTest extends FunSuite {
  test("Slew rate limiting correctly ramps up output") {
    val evaluated = evaluateStreamForList(List.fill(10)(Percent(100)), Milliseconds(100)) { input =>
      CurrentLimiting.slewRate(Percent(0), input, Percent(100) / Seconds(1))
    }

    assert(evaluated.zip((10 to 100 by 10).map(Percent(_)).toList).forall {
      case (actual, expected) =>
        (actual - expected).abs < Percent(0.0001)
    })
  }

  test("Slew rate limiting correctly ramps up output with nonzero previous output") {
    val evaluated = evaluateStreamForList(List.fill(10)(Percent(100)), Milliseconds(100)) { input =>
      CurrentLimiting.slewRate(Percent(50), input, Percent(100) / Seconds(2) /* half as slow, goes from 50% to 100% */ )
    }

    assert(evaluated.zip((55 to 100 by 5).map(Percent(_)).toList).forall {
      case (actual, expected) =>
        (actual - expected).abs < Percent(0.0001)
    })
  }

  test("Current Limiting - increasing positive output") {
    assert(
      CurrentLimiting.limitCurrentOutput(
        targetPower = Percent(100),
        normalizedVelocity = Percent(50),
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == Percent(75)
    )
  }

  test("Velocity Current Limiting - increasing positive output") {
    val maxSpeed = FeetPerSecond(20)
    assert(
      CurrentLimiting.limitCurrentOutput(
        target = maxSpeed,
        currentSpeed = maxSpeed / 2,
        maxSpeed = maxSpeed,
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == maxSpeed * 0.75
    )
  }

  test("Current Limiting - reducing negative output") {
    assert(
      CurrentLimiting.limitCurrentOutput(
        targetPower = Percent(-100),
        normalizedVelocity = Percent(-50),
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == Percent(-75)
    )
  }

  test("Velocity Current Limiting - reducing negative output") {
    val maxSpeed = FeetPerSecond(20)
    assert(
      CurrentLimiting.limitCurrentOutput(
        target = -maxSpeed,
        currentSpeed = -maxSpeed / 2,
        maxSpeed = maxSpeed,
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == maxSpeed * -0.75
    )
  }

  test("Current Limiting - reversing direction") {
    assert(
      CurrentLimiting.limitCurrentOutput(
        targetPower = Percent(-50),
        normalizedVelocity = Percent(25),
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == Percent(-20)
    )
  }

  test("Velocity Current Limiting - reversing direction") {
    val maxSpeed = FeetPerSecond(20)
    assert(
      CurrentLimiting.limitCurrentOutput(
        target = -maxSpeed * 0.5,
        currentSpeed = maxSpeed / 4,
        maxSpeed = maxSpeed,
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == maxSpeed * -0.2
    )
  }

  test("Current Limiting - no effect when reducing power") {
    assert(
      CurrentLimiting.limitCurrentOutput(
        targetPower = Percent(50),
        normalizedVelocity = Percent(100),
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == Percent(50)
    )

    assert(
      CurrentLimiting.limitCurrentOutput(
        targetPower = Percent(-50),
        normalizedVelocity = Percent(-100),
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == Percent(-50)
    )
  }

  test("Velocity Current Limiting - no effect when reducing power") {
    val maxSpeed = FeetPerSecond(20)
    assert(
      CurrentLimiting.limitCurrentOutput(
        target = maxSpeed / 2,
        currentSpeed = maxSpeed,
        maxSpeed = maxSpeed,
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == maxSpeed / 2
    )

    assert(
      CurrentLimiting.limitCurrentOutput(
        target = -maxSpeed / 2,
        currentSpeed = -maxSpeed,
        maxSpeed = maxSpeed,
        forwardCurrentLimit = Percent(25),
        backwardsCurrentLimit = Percent(25)
      ) == -maxSpeed / 2
    )
  }
}
