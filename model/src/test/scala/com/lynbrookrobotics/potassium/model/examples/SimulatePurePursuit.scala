package com.lynbrookrobotics.potassium.model.examples

import com.lynbrookrobotics.potassium.ClockMocking.mockedClockTicker
import com.lynbrookrobotics.potassium.{Signal, commons}
import com.lynbrookrobotics.potassium.model.simulations.SimulatedTwoSidedHardware
import com.lynbrookrobotics.potassium.model.simulations.TwoSidedDriveContainerSimulator
import com.lynbrookrobotics.potassium.commons.drivetrain.{MathUtilities, PurePursuitTasks, TwoSidedDriveProperties}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.model.examples.SimulateDrivetrain.log
import com.lynbrookrobotics.potassium.units.Point
import org.scalatest.FunSuite
import squants.{Acceleration, Length, Percent, Time, Velocity}
import squants.time.Milliseconds
import squants.time.Seconds
import squants.mass.{KilogramsMetersSquared, Pounds}
import squants.motion._
import squants.space._

import scala.collection.immutable.{NumericRange, Range}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.units.GenericValue._

import scala.reflect.io.File

class SimulatePurePursuit extends FunSuite {
  val period = Milliseconds(5)

  // TODO: Kunal pls arrange a meeting to fix this
  val container = new TwoSidedDriveContainerSimulator

  implicit val propsVal: TwoSidedDriveProperties = new TwoSidedDriveProperties {
    override val maxLeftVelocity: Velocity = FeetPerSecond(15)
    override val maxRightVelocity: Velocity = FeetPerSecond(15)

    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(16.5)
    override val defaultLookAheadDistance: Length = Feet(2)

    override val turnControlGains = PIDConfig(
      Percent(100) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)))

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / Feet(4),
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1))

    override val turnPositionControlGains = PIDConfig(
      Percent(5) / Degrees(1),
      Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
      Percent(0) / DegreesPerSecond(1))

    override val leftControlGains = PIDConfig(
      Percent(100) / FeetPerSecond(1),
      Percent(0) / Meters(1),
      Percent(0) / MetersPerSecondSquared(1))

    override val rightControlGains = leftControlGains
  }

  implicit val props = Signal.constant(propsVal)

  def testPurePursuitReachesDestination(wayPoints: Seq[Point],
                                        timeOut: Time,
                                        log: Boolean = false,
                                        distanceTolerance: Length = Feet(0.5),
                                        angleTolerance: Angle = Degrees(8)): Unit = {
    implicit val (clock, triggerClocK) = mockedClockTicker
    implicit val hardware = new SimulatedTwoSidedHardware(
      Pounds(88) * MetersPerSecondSquared(1) / 2,
      Inches(21.75),
      Pounds(88),
      KilogramsMetersSquared(3.909),
      clock,
      period)

    val drivetrain = new container.Drivetrain

    val task = new container.unicycleTasks.FollowWayPoints(
      wayPoints,
      Feet(0.5),
      Percent(30)
    )(drivetrain)

    task.init()

    if (log) {
      val logName = s"simlog-${wayPoints.mkString.split("Value3D").mkString(",")}"
      val writer = new File(new java.io.File(logName)).printWriter()
      writer.println(s"Time\tx\ty\tvelocity\tangle")

      var i = 0
      val handle = hardware.historyStream.foreach{ e =>
        if(i % 10 == 0) {
          writer.println(s"${e.time.toSeconds}\t," +
            s"${e.position.x.toFeet}\t," +
            s"${e.position.y.toFeet}\t," +
            s"${e.forwardVelocity.toFeetPerSecond}\t," +
            s"${e.angle.toDegrees}")
        }
        i = i + 1
      }
    }

    for (tick <- 1 to (timeOut / period).toInt) {
      // TODO: This is a real issue we need to fix
      // TODO: Basically we are ignoring the cases when
      // TODO: the task finishes early. In reality, if
      // TODO: we drift to a stop after the task ends
      // TODO: that is a real problem

      // TODO: Ok, why does this break things?
      if (/*task.isRunning*/true) {
        triggerClocK(period)
      }
    }

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

  test("Reach destination with path from (0,0) to (-5, 5)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(-5), Feet(5))),
      Seconds(8)
    )
  }

  test("Reach destination with path from (0,0) to (-5, -5)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(-5), Feet(-5))),
      Seconds(8)
    )
  }

  test("Reach destination with path from (0,0) to (-15, -15)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(-15), Feet(-15))),
      Seconds(24),
      distanceTolerance = Feet(1)
    )
  }

  test("Reach destination with path from (0,0) to (5, 5)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(5), Feet(5))),
      Seconds(8)
    )
  }

  test("Reach destination with path from (0,0) to (0, 5)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(0), Feet(5))),
      Seconds(8)
    )
  }


  test("Reach destination with path from (0,0) to (0, -5)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(0), Feet(-5))),
      Seconds(8)
    )
  }

  test("Reach destination with path from (0,0) to (0, 5) to (5, 10)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(0), Feet(5)), Point(Feet(5), Feet(10))),
      timeOut = Seconds(10),
      distanceTolerance = Feet(1),
      log = true
    )
  }

  test("Reach destination with path from (0,0) to (0, 5) to (-5, 10)") {
    testPurePursuitReachesDestination(
      Seq(Point.origin, Point(Feet(0), Feet(5)), Point(Feet(-5), Feet(10))),
      Seconds(10),
      distanceTolerance = Feet(1)
    )
  }
}
