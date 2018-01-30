package com.lynbrookrobotics.potassium.model.examples

import com.lynbrookrobotics.potassium.ClockMocking.mockedClockTicker
import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.purePursuit.MathUtilities
import com.lynbrookrobotics.potassium.commons.drivetrain.twoSided.TwoSidedDriveProperties
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.model.simulations.{SimulatedTwoSidedHardware, TwoSidedDriveContainerSimulator}
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units.{Point, _}
import org.scalatest.FunSuite
import squants.mass.{KilogramsMetersSquared, Pounds}
import squants.motion._
import squants.space._
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Length, Percent, Time, Velocity}

import scala.reflect.io.File

class SimulatePurePursuit extends FunSuite {
  val period = Milliseconds(5)

  val container = new TwoSidedDriveContainerSimulator

  implicit val propsVal: TwoSidedDriveProperties = new TwoSidedDriveProperties {
    override val maxLeftVelocity: Velocity = FeetPerSecond(17)
    override val maxRightVelocity: Velocity = FeetPerSecond(17)

    override val maxTurnVelocity: AngularVelocity = RadiansPerSecond((((maxLeftVelocity + maxRightVelocity) * Seconds(1)) / Inches(21.75)) / 2)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(16.5)
    override val defaultLookAheadDistance: Length = Feet(1)

    override val turnControlGains = PIDConfig(
      Percent(50) / DegreesPerSecond(360),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)))

    override val forwardPositionControlGains = PIDConfig(
      Percent(17.5) / Feet(1),
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1))

    override val turnPositionControlGains = PIDConfig(
      kp = Percent(60) / Degrees(90),
      ki = Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
      kd = Percent(0.5) / DegreesPerSecond(1))

    override val leftControlGains = PIDConfig(
      Percent(60) / FeetPerSecond(5),
      Percent(0) / Meters(1),
      Percent(0) / MetersPerSecondSquared(1))

    override val rightControlGains = leftControlGains
    override val track: Distance = Inches(21.75)
    override val blendExponent: Double = Double.NaN
  }

  implicit val props = Signal.constant(propsVal)

  def testPurePursuitReachesDestination(wayPoints: Seq[Point],
                                        timeOut: Time,
                                        log: Boolean = false,
                                        distanceTolerance: Length = Feet(0.5),
                                        angleTolerance: Angle = Degrees(8),
                                        driveBackwards: Boolean = false): Unit = {
    implicit val (clock, triggerClock) = mockedClockTicker
    implicit val hardware = new SimulatedTwoSidedHardware(
      Pounds(88) * MetersPerSecondSquared(1) / 2,
      Pounds(88),
      KilogramsMetersSquared(3.909),
      clock,
      period)

    val drivetrain = new container.Drivetrain

    val task = new container.unicycleTasks.FollowWayPoints(
      wayPoints,
      Inches(3),
      20,
      Percent(30),
      driveBackwards
    )(drivetrain)

    println("change id: 3")
    task.init()

    if (log) {
      val logName = s"simlog-${wayPoints.mkString.split("Value3D").mkString(",")}"
      val writer = new File(new java.io.File(logName)).printWriter()
      writer.println(s"Time\tx\ty\tvelocity\tangle\tturnSpeed")

      var i = 0
      val handle = hardware.robotStateStream.foreach{ e =>
        if(i % 10 == 0) {
          writer.println(
            s"${e.time.toSeconds}\t" +
            s"${e.position.x.toFeet}\t" +
            s"${e.position.y.toFeet}\t" +
            s"${e.forwardVelocity.toFeetPerSecond}\t" +
            s"${e.angle.toDegrees}\t",
            s"${e.turnSpeed.toDegreesPerSecond}"
          )
        }
        i = i + 1
      }
    }

    while (clock.currentTime <= timeOut/* && task.isRunning*/) {
      if (clock.currentTime >= Seconds(2.2)) {
        val bar = true
      }
      triggerClock(period)
    }

    println(s"Task finished in ${clock.currentTime}")

    implicit val implicitDistanceTolerance = distanceTolerance
    implicit val implicitAngleTolerance = angleTolerance
    val lastPosition = hardware.positionListening.apply().get
    assert(lastPosition ~= wayPoints.last, s"\nLast position was $lastPosition")

    val lastSegmentAngle = MathUtilities.swapTrigonemtricAndCompass(
      Segment(wayPoints(wayPoints.length - 2), wayPoints.last).angle
    )
    val lastAngle = hardware.angleListening.apply().get

    // this is an acceptable target angle if driving backwards is required
    val backwardTargetAngle = lastSegmentAngle - Degrees(180)
    assert(
      (lastAngle ~= lastSegmentAngle) || (lastAngle ~= backwardTargetAngle),
      s"\nLast angle was ${lastAngle.toDegrees}")
  }

//  test("Reach destination with path from (0,0) to (-5, 5)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(-5), Feet(5))),
//      timeOut = Seconds(8),
//      log = false
//    )
//  }

  test("two cube auto") {
    testPurePursuitReachesDestination(
      Seq(
        Point.origin,
        Point(
          Inches(0),
          Feet(-3)
        ),
        Point(
          Feet(-3),
          Feet(-3)
        ),
        Point(
          Feet(-3),
          Feet(5)
        )
      ),
      timeOut = Seconds(15),
      log = true,
      driveBackwards = true
    )
  }

//  test("Reach destination with path from (0,0) to (-5, -5)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(-5), Feet(-5))),
//      timeOut = Seconds(8),
//      log = /*true*/false
//    )
//  }
//
//  test("Reach destination with path from (0,0) to (-15, -15)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(-15), Feet(-15))),
//      timeOut = Seconds(20),
//      log = false
//    )
//  }
//
//  test("Reach destination with path from (0,0) to (5, 5)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(5), Feet(5))),
//      timeOut = Seconds(8),
//      log = false
//    )
//  }
//
//  test("Reach destination with path from (0,0) to (-.5, -.5)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(-0.5), Feet(-0.5))),
//      timeOut = Seconds(8),
//      log = false
//    )
//  }
//
//
//
//  test("Reach destination with path from (0,0) to (0, 5) to (5, 5)") {
//    testPurePursuitReachesDestination(
//      Seq(
//        Point.origin,
//        Point(Feet(0), Feet(5)),
//        Point(Feet(5), Feet(5))
//      ),
//      timeOut = Seconds(8),
//      log = false
//    )
//  }
//
//  test("Center switch turned right scenario") {
//    testPurePursuitReachesDestination(
//      Seq(
//        Point.origin,
//        Point(Inches(0), Inches(56.6)),
//        Point(Inches(-55.393), Inches(111.993)),
//        Point(Inches(-55.393), Inches(143.993))
//      ),
//      timeOut = Seconds(30),
//      log = false
//    )
//  }
//
//  test("Go backwards five feeeeeetttttt") {
//    testPurePursuitReachesDestination(
//      Seq(
//        Point.origin,
//        Point(Feet(0), Feet(-10))
//      ),
//      timeOut = Seconds(10),
//      log = false/*true*/,
//      driveBackwards = true
//    )
//  }
//
//  test("Reach destination (-5, -5)") {
//    testPurePursuitReachesDestination(
//      Seq(
//        Point.origin,
//        Point(Feet(-10), Feet(-10))
//      ),
//      timeOut = Seconds(10),
//      log = false/*true*/,
//      driveBackwards = true
//    )
//  }
////
//  test("Reach destination with path from (0,0) to (0, 5)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(0), Feet(5))),
//      timeOut = Seconds(10)
//    )
//  }
//
//
//  test("Reach destination with path from (0,0) to (0, -10)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(0), Feet(-10))),
//      timeOut = Seconds(10)
//    )
//  }
////
//  test("Reach destination with path from (0,0) to (0, 5) to (5, 10)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(0), Feet(5)), Point(Feet(5), Feet(10))),
//      timeOut = Seconds(10)
//    )
//  }
////
//  test("Reach destination with path from (0,0) to (0, 5) to (-5, 10)") {
//    testPurePursuitReachesDestination(
//      Seq(Point.origin, Point(Feet(0), Feet(5)), Point(Feet(-5), Feet(10))),
//      timeOut = Seconds(10)
//    )
//  }
}
