package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.{Component, PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask}
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Time, Velocity}
import squants.motion.{AngularVelocity, DegreesPerSecond, Distance, FeetPerSecond}
import squants.space.{Degrees, Feet}
import squants.time.Milliseconds

import scala.collection.immutable.Queue

trait UnicycleCoreTasks {
  val controllers: UnicycleCoreControllers with UnicycleMotionProfileControllers with PurePursuitControllers

  type Drivetrain <: Component[controllers.DriveSignal]

  import controllers._

  class DriveOpenLoop(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
    (implicit drive: Drivetrain, hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
      drive.setController(lowerLevelOpenLoop(combined))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }


  class ContinuousClosedDrive(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
                             (implicit drive: Drivetrain, hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
      drive.setController(lowerLevelVelocityControl(combined))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class ContinuousVelocityDrive(forward: Signal[Velocity], turn: Signal[AngularVelocity])
                               (implicit drive: Drivetrain,
                                hardware: DrivetrainHardware,
                                props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleVelocity(t._1, t._2))
      drive.setController(lowerLevelVelocityControl(velocityControl(combined)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveDistance(distance: Length, tolerance: Length)
                     (implicit drive: Drivetrain,
                      hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.get + distance
      val (controller, error) = forwardPositionControl(absoluteDistance)

      val checkedController = controller.withCheck { _ =>
        if (error.get.abs < tolerance) {
          finished()
        }
      }

      drive.setController(lowerLevelVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveDistanceWithTrapazoidalProfile(cruisingVelocity: Velocity,
                                            finalVelocity: Velocity,
                                            acceleration: Acceleration,
                                            targetDistance: Length,
                                            position: Signal[Length],
                                            tolerance: Length,
                                            toleranceAngle: Angle)
                                           (implicit drive: Drivetrain,
                                            hardware: DrivetrainHardware,
                                            properties: Signal[DrivetrainProperties]) extends FiniteTask {
    if (cruisingVelocity.abs > properties.get.maxForwardVelocity) {
      throw new IllegalArgumentException("Input speed: " +
        cruisingVelocity.abs.toFeetPerSecond +
        " ft/s is greater than max speed")
    }

    override final def onStart(): Unit = {
      val (velocity, forwardError) = trapezoidalDriveControl(
        hardware.forwardVelocity.get, // not map because we need position at this time
        cruisingVelocity,
        finalVelocity,
        acceleration,
        hardware.forwardPosition.get, // not map because we need position at this time
        targetDistance,
        position,
        tolerance
      )

      val absoluteAngle = hardware.turnPosition.get
      val (turnController, turnError) = turnPositionControl(absoluteAngle)
      val forwardOutput = velocity.map(UnicycleVelocity(_, DegreesPerSecond(0)).toUnicycleSignal)
      val combinedController = forwardOutput.zip(turnController).map(t => t._1 + t._2)

      drive.setController(lowerLevelVelocityControl(speedControl(combinedController)).withCheck { _ =>
          if (forwardError.get.abs < tolerance && turnError.get.abs < toleranceAngle) {
            finished()
          }
        })
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  /**
    * drives the target distance with default values for acceleration and cruising velocity
    * TODO: finish adding docs
    * @param targetForwardDistance
    * @param finalVelocity
    * @param drive
    * @param hardware
    * @param properties
    */
  class DriveDistanceSmooth(targetForwardDistance: Length, finalVelocity: Velocity)
                           (implicit drive: Drivetrain,
                            hardware: DrivetrainHardware,
                            properties: Signal[DrivetrainProperties]) extends DriveDistanceWithTrapazoidalProfile(
                              0.5 * properties.get.maxForwardVelocity,
                              finalVelocity,
                              properties.get.maxAcceleration,
                              targetForwardDistance,
                              hardware.forwardPosition,
                              Feet(.1),
                              Degrees(5))

  class DriveDistanceStraight(distance: Length,
                              toleranceForward: Length,
                              toleranceAngle: Angle,
                              maxSpeed: Dimensionless)
                             (implicit drive: Drivetrain,
                              hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.get + distance
      val (forwardController, forwardError) = forwardPositionControl(absoluteDistance)

      val limitedForward = forwardController.map { u =>
        UnicycleSignal(u.forward max (-maxSpeed) min maxSpeed, u.turn)
      }

      val absoluteAngle = hardware.turnPosition.get
      val (turnController, turnError) = turnPositionControl(absoluteAngle)

      val combinedController = limitedForward.zip(turnController).map(t => t._1 + t._2)

      val checkedController = combinedController.withCheck { _ =>
        if (forwardError.get.abs < toleranceForward && turnError.get.abs < toleranceAngle) {
          finished()
        }
      }

      drive.setController(
        lowerLevelVelocityControl(speedControl(checkedController))
      )
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveBeyondStraight(distance: Length,
                              toleranceForward: Length,
                              toleranceAngle: Angle,
                              maxSpeed: Dimensionless)
                             (implicit drive: Drivetrain,
                              hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.get + distance
      val (forwardController, forwardError) = (
        if (distance.value > 0) {
          Signal.constant(maxSpeed).toPeriodic
        } else {
          Signal.constant(-maxSpeed).toPeriodic
        },
        hardware.forwardPosition.map(absoluteDistance - _)
      )

      val limitedForward = forwardController.map { u =>
        UnicycleSignal(u, Percent(0))
      }

      val absoluteAngle = hardware.turnPosition.get
      val (turnController, turnError) = turnPositionControl(absoluteAngle)

      val combinedController = limitedForward.zip(turnController).map(t => t._1 + t._2)

      val checkedController = combinedController.withCheck { _ =>
        val beyond = if (distance.value > 0) {
          forwardError.get.value < 0
        } else {
          forwardError.get.value > 0
        }

        if (beyond && turnError.get.abs < toleranceAngle) {
          finished()
        }
      }

      drive.setController(
        lowerLevelVelocityControl(speedControl(checkedController))
      )
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class RotateByAngle(relativeAngle: Angle, tolerance: Angle, timeWithinTolerance: Int)
                     (implicit drive: Drivetrain,
                      hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteAngle = hardware.turnPosition.get + relativeAngle
      val (controller, error) = turnPositionControl(absoluteAngle)

      var ticksWithinTolerance = 0

      val checkedController = controller.withCheck { _ =>
        if (error.get.abs < tolerance) {
          ticksWithinTolerance += 1
        } else {
          ticksWithinTolerance = 0
        }

        if (ticksWithinTolerance >= timeWithinTolerance) {
          finished()
        }
      }

      drive.setController(lowerLevelVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class RotateToAngle(absoluteAngle: Angle, tolerance: Angle)
                     (implicit drive: Drivetrain,
                      hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val (controller, error) = turnPositionControl(absoluteAngle)
      val checkedController = controller.withCheck { _ =>
        if (error.get.abs < tolerance) {
          finished()
        }
      }

      drive.setController(lowerLevelVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class CorrectOffsetWithLatency(timestampedOffset: Signal[(Angle, Time)], tolerance: Angle)
    (implicit drive: Drivetrain,
      hardware: DrivetrainHardware,
      props: Signal[DrivetrainProperties]) extends FiniteTask {

    val positionSlide: PeriodicSignal[Queue[(Angle, Time)]] = hardware.turnPosition.toPeriodic.zipWithTime.sliding(
      20, (hardware.turnPosition.get, Milliseconds(System.currentTimeMillis()))
    )

    override def onStart(): Unit = {
      val targetAbsolute = calculateTargetFromOffsetWithLatency(timestampedOffset, positionSlide)

      val (controller, error) = continuousTurnPositionControl(targetAbsolute)
      val checkedController = controller.zip(error).withCheck { t =>
        val (_, e) = t
        if (e.abs < tolerance) {
          finished()
        }
      }.map(_._1)

      drive.setController(lowerLevelVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

}
