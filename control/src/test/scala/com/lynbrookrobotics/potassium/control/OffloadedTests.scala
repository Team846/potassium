package com.lynbrookrobotics.potassium.control

import com.lynbrookrobotics.potassium.control.offload.EscConfig
import com.lynbrookrobotics.potassium.control.offload.EscConfig._
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units.{Ratio, _}
import org.scalatest.{Assertion, FunSuite}
import squants.space.{Inches, Length}
import squants.time.{Milliseconds, Seconds}
import squants.{Each, Percent}

class OffloadedTests extends FunSuite {

  private val nums = Set(0, 0.01, 0.5, 1, 25, 100)
    .flatMap(x => Set(-x, +x))

  private def asserteq(a: Double, b: Double): Assertion = assert(Math.abs(a - b) < 0.0001)

  /**
   * 5 inches / 100 encoder ticks
   * 1 second / 1000 loop times
   * => 5 in-sec / 100000 tick-period
    **/
  test("Floor to encoder absement conversion")(
    nums.foreach(
      it =>
        asserteq(
          ticks(
            Inches(5 * it) * Seconds(1)
          ).toEach,
          100000 * it
      )
    )
  )

  /**
   * 5 inches / 100 encoder ticks
   * => 5 inches / 100 encoder ticks
    **/
  test("Floor to encoder position conversion")(
    nums.foreach(
      it =>
        asserteq(
          ticks(
            Inches(5 * it)
          ).toEach,
          100 * it
      )
    )
  )

  /**
   * 5 inches / 100 encoder ticks
   * 1 second / 10 native period
   * => 5pi in/sec / 10 ticks/period
    **/
  test("Floor to encoder velocity conversion")(
    nums.foreach(
      it =>
        asserteq(
          ticks(
            Inches(5 * it) / Seconds(1)
          ).toEach,
          10 * it
      )
    )
  )

  /**
   * 5 inches / 100 encoder ticks
   * 1 second / 10 native periods
   * 1 second / 1000 loop times
   * => 5pi in/sec/sec / 0.01 ticks/period/millis
    **/
  test("Floor to encoder acceleration conversion")(
    nums.foreach(
      it =>
        asserteq(
          ticks(
            Inches(5 * it) / Seconds(1) / Seconds(1)
          ).toEach,
          0.01 * it
      )
    )
  )

  test("Forward to angular velocity gains conversion") {
    val p = Percent(100) / (Inches(5) / Seconds(1))
    val i = Percent(100) / Inches(5)
    val d = Percent(100) / (Inches(5) / Seconds(1) / Seconds(1))
    val f = p

    val g = forwardToAngularVelocityGains(PIDFConfig(p, i, d, f))

    nums.foreach { it =>
      asserteq(g.p * ticks(p.den * it).toEach, 1000 * it)
      asserteq(g.i * ticks(i.den * it).toEach, 1000 * it)
      asserteq(g.d * ticks(d.den * it).toEach, 1000 * it)
      asserteq(g.f * ticks(f.den * it).toEach, 1000 * it)
    }
  }

  test("Forward to angular position gains conversion") {
    val p = Percent(100) / Inches(5)
    val i = Percent(100) / (Inches(5) * Seconds(1))
    val d = Percent(100) / (Inches(5) / Seconds(1))

    val g = forwardToAngularPositionGains(PIDConfig(p, i, d))

    nums.foreach { it =>
      asserteq(g.p * ticks(p.den * it).toEach, 1000 * it)
      asserteq(g.i * ticks(i.den * it).toEach, 1000 * it)
      asserteq(g.d * ticks(d.den * it).toEach, 1000 * it)
    }
  }

  implicit val c: EscConfig[Length] = EscConfig(
    maxNativeOutput = 1000,
    nativeTimeUnit = Milliseconds(100),
    loopTime = Milliseconds(1),
    ticksPerUnit = Ratio(Each(100), Inches(5))
  )
}
