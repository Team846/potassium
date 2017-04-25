package com.lynbrookrobotics.potassium.commons.drivetrain.Simulations

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain.{ForwardVelocityGains, TwoSidedDriveProperties, UnicycleProperties}
import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import org.scalatest.FunSuite
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared, _}
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Length, Percent, Velocity}

class SimulateDrivetrainDynamics extends FunSuite {
  implicit val props = Signal.constant(new TwoSidedDriveProperties {
    override val maxLeftVelocity = FeetPerSecond(15.5)
    override val maxRightVelocity = FeetPerSecond(15.5)

    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(16.5)
    override val defaultLookAheadDistance: Length = null

    override val turnControlGains = PIDConfig(
      Percent(10) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1)))

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / Feet(2),
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
  })

  implicit val propsVal: TwoSidedDriveProperties = props.get


  test("Test drive forward controller oscilates about end point") {
    implicit val (clock, ticker) = ClockMocking.mockedClockTicker

    implicit val drivetrainContainer = new TwoSidedDriveContainerSimulator(Milliseconds(5))
    implicit val hardware = new drivetrainContainer.Hardware(
      ???,
      Feet(2),
      ???,
      ???)

    implicit val simulatedComponent = new drivetrainContainer.Drivetrain
    val task = new drivetrainContainer.unicycleTasks.DriveDistanceStraight(
      Meters(1),
      Meters(0.0),
      Degrees(5),
      Percent(100))
    task.init()

    for (_ <- 1 to 3000) {
      ticker(Milliseconds(5))
    }

    hardware.history.foreach(e =>
      println(s"${e.time.toSeconds}\t${e.forwardVelocity.toFeetPerSecond}\t${e.forwardPosition.toFeet}"))

    assert(task.isRunning, "finished with this history " + hardware.history.mkString)
  }

  test("Drive distance smooth doesn't terminate") {
//    implicit val drivetrainContainer = new TwoSidedDrivePackageSimulator
//    implicit val (clock, ticker) = ClockMocking.mockedClockTicker
//
//    implicit val hardware = new SimulatedUnicycleHardware(props.get, Feet(2), MetersPerSecondSquared(1))
//    implicit val simulatedDrivetrainComp = new SimulatedUnicycleDriveComponent(Milliseconds(5))
//
//    val task = new drivetrainContainer.unicycleTasks.DriveDistanceWithTrapazoidalProfile(
//      props.get.maxForwardVelocity,
//      FeetPerSecond(0),
//      props.get.maxAcceleration,
//      Meters(1),
//      hardware.forwardPosition,
//      Feet(0),
//      Degrees(0)
//    )
//    task.init()
//
//    for (_ <- 1 to 3000) {
//      ticker(Milliseconds(5))
//    }
//
////    hardware.history.foreach(e =>
////      println(s"${e._1.toSeconds}\t${e._4.toFeetPerSecond}\t${e._2.toFeet}"))
//
//    assert(task.isRunning)
  }
}
