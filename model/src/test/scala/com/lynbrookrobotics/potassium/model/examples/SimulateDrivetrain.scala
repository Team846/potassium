package com.lynbrookrobotics.potassium.model.examples

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.model.simulations.TwoSidedDriveContainerSimulator
import com.lynbrookrobotics.potassium.commons.drivetrain.TwoSidedDriveProperties
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import squants.{Acceleration, Length, Percent, Velocity}
import squants.motion._
import squants.space.{Degrees, Feet, Inches, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.Time
import com.lynbrookrobotics.potassium.ClockMocking._
import squants.mass.{KilogramsMetersSquared, Pounds}

import scala.reflect.io.File

object SimulateDrivetrain extends App {
  implicit val propsVal: TwoSidedDriveProperties = new TwoSidedDriveProperties {
    override val maxLeftVelocity: Velocity = FeetPerSecond(5)
    override val maxRightVelocity: Velocity = FeetPerSecond(5)

    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(16.5)
    override val defaultLookAheadDistance: Length = Feet(1)

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

  implicit val (clock, ticker) = mockedClockTicker

  val period = Milliseconds(10)
  val drivetrainContainer = new TwoSidedDriveContainerSimulator
  implicit val hardware = new drivetrainContainer.Hardware(
    Pounds(88) * MetersPerSecondSquared(1) / 2,
    Inches(21.75),
    Pounds(88),
    KilogramsMetersSquared(3.909),
    clock,
    period)

  val simulatedComponent = new drivetrainContainer.Drivetrain

  var itr = 0l
  val log = new File(new java.io.File("simlog")).printWriter()
  val streamPrintingCancel = hardware.robotStateStream.foreach { e =>
    if (itr == 0) {
      log.println(s"Time\tx\ty\tvelocity\tangle")
    }
    if (itr % 10 == 0) {
      log.println(s"${e.time.toSeconds}\t${e.position.x.toFeet}\t ${e.position.y.toFeet}\t${e.forwardVelocity.toFeetPerSecond}\t${e.angle.toDegrees}")
    }

    itr += 1
  }

  val wayPoints = Seq(Point.origin, Point(Feet(-1), Feet(5)), Point(Feet(-5), Feet(10)))

  val task = new drivetrainContainer.unicycleTasks.FollowWayPoints(
    wayPoints,
    Inches(5),
    Percent(70),
    Percent(30)
  )(simulatedComponent)
//  val task = new drivetrainContainer.unicycleTasks.RotateToAngle(
//    Degrees(1e200),
//    Degrees(0)
//  )(simulatedComponent)


  task.init()


  for (i <- 1 to (20D / period.toSeconds).round.toInt) {
//    val startTime = System.nanoTime()
    ticker(period)
    if (i == (1.64D / period.toSeconds).round.toInt) {
      val atTime = true
    }
  }
}
