package com.lynbrookrobotics.potassium.model.examples

import com.lynbrookrobotics.potassium.{ClockMocking, Signal}
import com.lynbrookrobotics.potassium.model.simulations.TwoSidedDriveContainerSimulator
import com.lynbrookrobotics.potassium.commons.drivetrain.TwoSidedDriveProperties
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import squants.{Acceleration, Length, Percent, Velocity}
import squants.motion._
import squants.space.{Degrees, Feet, Inches, Meters}
import squants.time.{Milliseconds, Seconds}
import com.lynbrookrobotics.potassium.testing.ClockMocking._
import com.lynbrookrobotics.potassium.units.rotation.KilogramsMetersSquared
import squants.mass.Pounds

object SimulateDrivetrain extends App {
  implicit val propsVal: TwoSidedDriveProperties = new TwoSidedDriveProperties {
    override val maxLeftVelocity: Velocity = FeetPerSecond(15)
    override val maxRightVelocity: Velocity = FeetPerSecond(15)

    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(16.5)
    override val defaultLookAheadDistance: Length = Feet(1)

    override val turnControlGains = PIDConfig(
      Percent(10) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)))

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / Feet(4),
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1))

    override val turnPositionControlGains = PIDConfig(
      Percent(100) / Degrees(10),
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

  val period = Milliseconds(0.001)
  implicit val drivetrainContainer = new TwoSidedDriveContainerSimulator(period)
  implicit val hardware = new drivetrainContainer.Hardware(
    Pounds(88) * MetersPerSecondSquared(1) / 2,
    Inches(21.75),
    Pounds(88),
    KilogramsMetersSquared(3.909))

  implicit val simulatedComponent = new drivetrainContainer.Drivetrain
  val task = /*new drivetrainContainer.unicycleTasks.RotateByAngle(
    Degrees(90),
    Degrees(0),
    5
  )*/ new drivetrainContainer.unicycleTasks.FollowWayPoints(
    Point.origin :: new Point(Meters(2), Meters(2)) :: Nil,
    Inches(0)
  )

  task.init()

  for (_ <- 1 to (2D / period.toSeconds).round.toInt) {
    ticker(period)
  }

  hardware.history.foreach(e =>
    println(s"${e.time.toSeconds}\t${e.position.x.toFeet}\t${e.position.y.toFeet}"))
}