package com.lynbrookrobotics.potassium.commons.drivetrain


import com.lynbrookrobotics.potassium.control.PIDConfig
import com.lynbrookrobotics.potassium.testing.ClockMocking
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal, SignalLike}
import squants.motion.{AngularVelocity, DegreesPerSecond, MetersPerSecond, MetersPerSecondSquared}
import squants.motion._
import squants.space.{Degrees, Feet, Meters}
import squants.time.{Milliseconds, Seconds}
import squants.{Acceleration, Angle, Length, Percent, Velocity}
import org.scalatest.FunSuite
/**
  * Created by Philip on 4/9/2017.
  */
class SimulateDrivetrainDynamics extends FunSuite {
  implicit val drivetrainPackage = new UnicyclePackageSimulator
  val props = Signal.constant(new UnicycleProperties {
    override val maxForwardVelocity: Velocity = MetersPerSecond(10)
    override val maxTurnVelocity: AngularVelocity = DegreesPerSecond(10)
    override val maxAcceleration: Acceleration = FeetPerSecondSquared(15)
    override val defaultLookAheadDistance: Length = Meters(3)

    override val forwardControlGains = PIDConfig(
      Percent(10) / MetersPerSecond(1),
      Percent(0) / Meters(1),
      Percent(0) / MetersPerSecondSquared(1)
    )

    override val turnControlGains = PIDConfig(
      Percent(10) / DegreesPerSecond(1),
      Percent(0) / Degrees(1),
      Percent(0) / (DegreesPerSecond(1).toGeneric / Seconds(1))
    )

    override val forwardPositionControlGains = PIDConfig(
      Percent(100) / Meters(1),
      Percent(0) / (Meters(1).toGeneric * Seconds(1)),
      Percent(0) / MetersPerSecond(1)
    )

    override val turnPositionControlGains = PIDConfig(
      Percent(100) / Degrees(10),
      Percent(0) / (Degrees(1).toGeneric * Seconds(1)),
      Percent(0) / DegreesPerSecond(1)
    )
  })

  implicit val hardware = new SimulatedUnicycleHardware(props.get, Feet(2), MetersPerSecondSquared(0))

  test("Test drive forward controller oscilates about end point") {
    implicit val (clock, ticker) = ClockMocking.mockedClockTicker

    implicit val simulatedComponent = new SimulatedUnicycleDriveComponent(Milliseconds(5))(hardware, clock)
    val task = new drivetrainPackage.unicycleTasks.DriveDistanceStraight(
      Meters(1),
      Meters(0.0),
      Degrees(5),
      Percent(100))(simulatedComponent, hardware, props)
    task.init()

    for (_ <- 1 to 3000) {
      ticker(Milliseconds(5))
    }
    println(hardware.history.last)
    for (i <- 1 to 3000 by 10) {
      println(s"${hardware.history(i)._1.toSeconds}\t ${hardware.history(i)._2.toFeet}")
    }


    assert(task.isRunning, "finished with this history " + hardware.history.mkString)
  }
}
