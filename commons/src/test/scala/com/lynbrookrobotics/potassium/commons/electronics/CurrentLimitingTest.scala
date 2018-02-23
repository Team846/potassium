package com.lynbrookrobotics.potassium.commons.electronics

import org.scalatest.FunSuite
import com.lynbrookrobotics.potassium.streams.StreamTesting._
import squants.Percent
import squants.electro.{Amperes, ElectricCurrent, ElectricPotential, Volts}
import squants.motion.{FeetPerSecond, Velocity}
import squants.time.{Milliseconds, Seconds}

class CurrentLimitingTest extends FunSuite {
  test("Slew rate limiting correctly ramps up output") {
    val evaluated = evaluateStreamForList(List.fill(10)(Percent(100)), Milliseconds(100)) { input =>
      CurrentLimiting.slewRate(Percent(0), input, Percent(100) / Seconds(1))
    }

    assert(evaluated.zip((10 to 100 by 10).map(Percent(_)).toList).forall { case (actual, expected) =>
      (actual - expected).abs < Percent(0.0001)
    })
  }

  test("Slew rate limiting correctly ramps up output with nonzero previous output") {
    val evaluated = evaluateStreamForList(List.fill(10)(Percent(100)), Milliseconds(100)) { input =>
      CurrentLimiting.slewRate(Percent(50), input, Percent(100) / Seconds(2) /* half as slow, goes from 50% to 100% */)
    }

    assert(evaluated.zip((55 to 100 by 5).map(Percent(_)).toList).forall { case (actual, expected) =>
      (actual - expected).abs < Percent(0.0001)
    })
  }

  test("Current Limiting - increasing positive output") {
    assert(CurrentLimiting.limitCurrentOutput(
      targetPower = Percent(100),
      normalizedVelocity = Percent(50),
      forwardCurrentLimit = Percent(25),
      backwardsCurrentLimit = Percent(25)
    ) == Percent(75))
  }

  test("Current Limiting - reducing negative output") {
    assert(CurrentLimiting.limitCurrentOutput(
      targetPower = Percent(-100),
      normalizedVelocity = Percent(-50),
      forwardCurrentLimit = Percent(25),
      backwardsCurrentLimit = Percent(25)
    ) == Percent(-75))
  }

  test("Current Limiting - reversing direction") {
    assert(CurrentLimiting.limitCurrentOutput(
      targetPower = Percent(-50),
      normalizedVelocity = Percent(25),
      forwardCurrentLimit = Percent(25),
      backwardsCurrentLimit = Percent(25)
    ) == Percent(-20))
  }

  test("Current Limiting - no effect when reducing power") {
    assert(CurrentLimiting.limitCurrentOutput(
      targetPower = Percent(50),
      normalizedVelocity = Percent(100),
      forwardCurrentLimit = Percent(25),
      backwardsCurrentLimit = Percent(25)
    ) == Percent(50))

    assert(CurrentLimiting.limitCurrentOutput(
      targetPower = Percent(-50),
      normalizedVelocity = Percent(-100),
      forwardCurrentLimit = Percent(25),
      backwardsCurrentLimit = Percent(25)
    ) == Percent(-50))
  }
}
