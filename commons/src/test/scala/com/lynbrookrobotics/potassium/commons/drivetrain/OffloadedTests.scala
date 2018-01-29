package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.commons.drivetrain.offloaded.{OffloadedDrive, OffloadedProperties}
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.TwoSided
import com.lynbrookrobotics.potassium.control.{OffloadedSignal, PIDConfig, PIDFConfig}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units.{Ratio, _}
import org.scalatest.{Assertion, FunSuite}
import squants.motion.AngularVelocity
import squants.space.{Inches, Length, Turns}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Dimensionless, Each, Percent, Time, Velocity}

class OffloadedTests extends FunSuite {

  private val nums = Set(0, 0.01, 0.5, 1, 25, 100)
    .flatMap(x => Set(-x, +x))

  private def asserteq(a: Double, b: Double): Assertion = assert(Math.abs(a - b) < 0.0001)

  /**
    * 5pi inches / 1 wheel rotation
    * 1 wheel rotation / 2 encoder rotations
    * 1 encoder rotation / 100 encoder ticks
    * 1 second / 1000 millis
    * => 5pi in-sec / 200000 tick-period
    **/
  test("Floor to encoder absement conversion")(nums.foreach(it => asserteq(
    drive.ticks(
      Inches(5 * Math.PI * it) * Seconds(1)
    ).toEach, 200000 * it
  )))

  /**
    * 5pi inches / 1 wheel rotation
    * 1 wheel rotation / 2 encoder rotations
    * 1 encoder rotation / 100 encoder ticks
    * => 5pi inches forward / 200 encoder ticks
    **/
  test("Floor to encoder position conversion")(nums.foreach(it => asserteq(
    drive.ticks(
      Inches(5 * Math.PI * it)
    ).toEach, 200 * it
  )))

  /**
    * 5pi inches / 1 wheel rotation
    * 1 wheel rotation / 2 encoder rotations
    * 1 encoder rotation / 100 encoder ticks
    * 1 second / 10 native period
    * => 5pi in/sec / 20 ticks/period
    **/
  test("Floor to encoder velocity conversion")(nums.foreach(it => asserteq(
    drive.ticks(
      Inches(5 * Math.PI * it) / Seconds(1)
    ).toEach, 20 * it
  )))

  /**
    * 5pi inches / 1 wheel rotation
    * 1 wheel rotation / 2 encoder rotations
    * 1 encoder rotation / 100 encoder ticks
    * 1 second / 10 native period
    * 1 second / 1000 milliseconds
    * => 5pi in/sec/sec / 0.02 ticks/period/millis
    **/
  test("Floor to encoder acceleration conversion")(nums.foreach(it => asserteq(
    drive.ticks(
      Inches(5 * Math.PI * it) / Seconds(1) / Seconds(1)
    ).toEach, 0.02 * it
  )))

  test("Forward to angular velocity gains conversion") {
    val p = Percent(100) / (Inches(5) / Seconds(1))
    val i = Percent(100) / Inches(5)
    val d = Percent(100) / (Inches(5) / Seconds(1) / Seconds(1))
    val f = p

    val g = drive.forwardToAngularVelocityGains(PIDFConfig(p, i, d, f))

    nums.foreach { it =>
      asserteq(g.p * drive.ticks(p.den * it).toEach, 1000 * it)
      asserteq(g.i * drive.ticks(i.den * it).toEach, 1000 * it)
      asserteq(g.d * drive.ticks(d.den * it).toEach, 1000 * it)
      asserteq(g.f * drive.ticks(f.den * it).toEach, 1000 * it)
    }
  }

  test("Forward to angular position gains conversion") {
    val p = Percent(100) / Inches(5)
    val i = Percent(100) / (Inches(5) * Seconds(1))
    val d = Percent(100) / (Inches(5) / Seconds(1))

    val g = drive.forwardToAngularPositionGains(PIDConfig(p, i, d))

    nums.foreach { it =>
      asserteq(g.p * drive.ticks(p.den * it).toEach, 1000 * it)
      asserteq(g.i * drive.ticks(i.den * it).toEach, 1000 * it)
      asserteq(g.d * drive.ticks(d.den * it).toEach, 1000 * it)
    }
  }

  val drive: OffloadedDrive = new OffloadedDrive {
    override type Properties = OffloadedProperties

    override protected def output(hardware: Hardware, signal: TwoSided[OffloadedSignal]): Unit = ???

    override protected def controlMode(implicit hardware: Hardware, props: OffloadedProperties) = NoOperation

    override type Drivetrain = Nothing
    override type Hardware = Nothing
  }

  implicit val props: drive.type#Properties = new OffloadedProperties {
    override val escTimeConst: Time = Milliseconds(100)
    override val wheelDiameter: Length = Inches(5)
    override val wheelOverEncoderGears: Ratio[Angle, Angle] = Ratio(Turns(1), Turns(2))
    override val encoderAngleOverTicks: Ratio[Angle, Dimensionless] = Ratio(Turns(1), Each(100))
    override val escNativeOutputOverPercent: Ratio[Dimensionless, Dimensionless] = Ratio(Each(1000), Percent(100))

    override val rightVelocityGains: ForwardVelocityGains = null
    override val maxRightVelocity: Velocity = null
    override val maxLeftVelocity: Velocity = null
    override val leftVelocityGains: ForwardVelocityGains = null
    override val defaultLookAheadDistance: Length = null
    override val forwardPositionGains: ForwardPositionGains = null
    override val turnPositionGains: TurnPositionGains = null
    override val maxTurnVelocity: AngularVelocity = null
    override val turnVelocityGains: TurnVelocityGains = null
    override val maxAcceleration: Acceleration = null
    override val blendExponent: Double = 0
    override val track: Length = null
  }.asInstanceOf[drive.type#Properties]
}