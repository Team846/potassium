package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit.PurePursuitControllers
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.{UnicycleSignal, UnicycleVelocity}
import com.lynbrookrobotics.potassium.streams.Stream
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask}
import com.lynbrookrobotics.potassium.{Component, Signal}
import squants.motion.{AngularVelocity, DegreesPerSecond}
import squants.space.{Degrees, Feet}
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Quantity, Time, Velocity}

import scala.collection.immutable.Queue

trait UnicycleCoreTasks {
  val controllers: UnicycleCoreControllers with UnicycleMotionProfileControllers with PurePursuitControllers

  type Drivetrain <: Component[controllers.DriveSignal]

  import controllers._

  class DriveOpenLoop(forward: Stream[Dimensionless], turn: Stream[Dimensionless])
                     (drive: Drivetrain)
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
      drive.setController(childOpenLoop(combined))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }


  class ContinuousClosedDrive(forward: Stream[Dimensionless], turn: Stream[Dimensionless])
                             (drive: Drivetrain)
                             (implicit hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
      drive.setController(childVelocityControl(combined))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class ContinuousVelocityDrive(forward: Stream[Velocity], turn: Stream[AngularVelocity])
                               (drive: Drivetrain)
                               (implicit hardware: DrivetrainHardware,
                                props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleVelocity(t._1, t._2))
      drive.setController(childVelocityControl(velocityControl(combined)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveDistance(distance: Length, tolerance: Length)
                     (drive: Drivetrain)
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.currentValue.map(_ + distance)
      val (controller, error) = forwardPositionControl(absoluteDistance)

      val checkedController = controller.withCheckZipped(error) { error =>
        if (error.abs < tolerance) {
          finished()
        }
      }

      drive.setController(childVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveDistanceWithTrapezoidalProfile(cruisingVelocity: Velocity,
                                            finalVelocity: Velocity,
                                            acceleration: Acceleration,
                                            deceleration: Acceleration,
                                            targetDistance: Length,
                                            tolerance: Length,
                                            toleranceAngle: Angle)
                                           (drive: Drivetrain)
                                           (implicit hardware: DrivetrainHardware,
                                            properties: Signal[DrivetrainProperties]) extends FiniteTask {
    if (cruisingVelocity.abs > properties.get.maxForwardVelocity) {
      throw new IllegalArgumentException("Input speed: " +
        cruisingVelocity.abs.toFeetPerSecond +
        " ft/s is greater than max speed")
    }

    override final def onStart(): Unit = {
      val (idealVelocity, forwardError) = trapezoidalDriveControl(
          cruisingVelocity,
          finalVelocity,
          acceleration,
          deceleration,
          hardware.forwardPosition,
          hardware.forwardPosition.currentValue.map(_ + targetDistance),
          hardware.forwardVelocity)

      val absoluteAngleTarget = hardware.turnPosition.currentValue
      val (turnController, turnError) = turnPositionControl(absoluteAngleTarget)
      val forwardOutput = idealVelocity.map(UnicycleVelocity(_, DegreesPerSecond(0)).toUnicycleSignal)
      val combinedController = forwardOutput.zip(turnController).map(t => t._1 + t._2)

      val uncheckedController = childVelocityControl(speedControl(combinedController))
      val zippedError = forwardError.zip(turnError)
      drive.setController(uncheckedController.withCheckZipped(zippedError) {
        case (forwardError, turnError) =>
          if (forwardError.abs < tolerance && turnError.abs < toleranceAngle) {
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
                           (drive: Drivetrain)
                           (implicit hardware: DrivetrainHardware,
                            properties: Signal[DrivetrainProperties])
    extends DriveDistanceWithTrapezoidalProfile(
      0.5 * properties.get.maxForwardVelocity,
      finalVelocity,
      properties.get.maxAcceleration,
      properties.get.maxDeceleration,
      targetForwardDistance,
      Feet(.1),
      Degrees(5)
    )(drive)

  class DriveDistanceStraight(distance: Length,
                              toleranceForward: Length,
                              toleranceAngle: Angle,
                              maxSpeed: Dimensionless,
                              minStableTicks: Int = 10)
                             (drive: Drivetrain)
                             (implicit hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends FiniteTask {
    var stableTicks = 0

    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.currentValue.map(_ + distance)
      val (forwardController, forwardError) = forwardPositionControl(absoluteDistance)

      val limitedForward = forwardController.map { u =>
        UnicycleSignal(u.forward max (-maxSpeed) min maxSpeed, u.turn)
      }

      val targetAngleAbsolute = hardware.turnPosition.currentValue
      val (turnController, turnError) = turnPositionControl(targetAngleAbsolute)

      val combinedController = limitedForward.zip(turnController).map(t => t._1 + t._2)

      val zippedError = forwardError.zip(turnError)
      val checkedController = combinedController.withCheckZipped(zippedError) {
        case  (forwardError, turnError) =>
          if (forwardError.abs < toleranceForward && turnError.abs < toleranceAngle) {
            stableTicks += 1
            if (stableTicks >= minStableTicks) {
              finished()
            }
          } else {
            stableTicks = 0
          }
      }

      drive.setController(
        childVelocityControl(speedControl(checkedController))
      )
    }

    override def onEnd(): Unit = {
      stableTicks = 0
      drive.resetToDefault()
    }
  }

  class DriveDistanceAtAngle(distance: Length,
                             toleranceForward: Length,
                             targetAngle: Angle,
                             toleranceAngle: Angle,
                             maxSpeed: Dimensionless)
                            (drive: Drivetrain)
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.currentValue.map(_ + distance)
      val (forwardController, forwardError) = forwardPositionControl(absoluteDistance)

      val limitedForward = forwardController.map { u =>
        UnicycleSignal(u.forward max (-maxSpeed) min maxSpeed, u.turn)
      }

      val (turnController, turnError) = turnPositionControl(targetAngle)

      val combinedController = limitedForward.zip(turnController).map(t => t._1 + t._2)

      val zippedError = forwardError.zip(turnError)
      val checkedController = combinedController.withCheckZipped(zippedError) {
        case  (forwardError, turnError) =>
          if (forwardError.abs < toleranceForward && turnError.abs < toleranceAngle) {
            finished()
          }
      }

      drive.setController(
        childVelocityControl(speedControl(checkedController))
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
                           (drive: Drivetrain)
                           (implicit hardware: DrivetrainHardware,
                            props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val (forwardController, forwardError) = (
        if (distance.value > 0) {
          hardware.forwardPosition.mapToConstant(maxSpeed)
        } else {
          hardware.forwardPosition.mapToConstant(-maxSpeed)
        },

        hardware.forwardPosition.relativize((initial, current) => {
          val absoluteTarget = initial + distance
          absoluteTarget - current
        })
      )

      val limitedForward = forwardController.map { u =>
        UnicycleSignal(u, Percent(0))
      }

      val absoluteAngle = hardware.turnPosition.currentValue
      val (turnController, turnError) = turnPositionControl(absoluteAngle)

      val combinedController = limitedForward.zip(turnController).map(t => t._1 + t._2)

      val zippedError = forwardError.zip(turnError)
      val checkedController = combinedController.withCheckZipped(zippedError) {
        case (forwardError, turnError) =>
          val beyond = if (distance.value > 0) {
            forwardError.value < 0
          } else {
            forwardError.value > 0
          }

          if (beyond) {
            finished()
          }
      }

      drive.setController(
        childVelocityControl(speedControl(checkedController))
      )
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }



  class RotateByAngle(relativeAngle: Angle, tolerance: Angle, timeWithinTolerance: Int)
                     (drive: Drivetrain)
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteAngle = hardware.turnPosition.currentValue.map(_ + relativeAngle)

//      val absoluteAngle = hardware.turnPosition.get + relativeAngle
      val (controller, error) = turnPositionControl(absoluteAngle)

      var ticksWithinTolerance = 0

      val checkedController = controller.withCheckZipped(error) { error =>
        if (error.abs < tolerance) {
          ticksWithinTolerance += 1
        } else {
          ticksWithinTolerance = 0
        }

        if (ticksWithinTolerance >= timeWithinTolerance) {
          finished()
        }
      }

      drive.setController(childVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class RotateToAngle(absoluteAngle: Angle, tolerance: Angle)
                     (drive: Drivetrain)
                     (implicit hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val (controller, error) = turnPositionControl(absoluteAngle)
      val checkedController = controller.withCheckZipped(error) { error =>
        if (error.abs < tolerance) {
          finished()
        }
      }

      drive.setController(childVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class CorrectOffsetWithLatency(timestampedOffset: Stream[(Angle, Time)], tolerance: Angle)
                                (drive: Drivetrain)
                                (implicit hardware: DrivetrainHardware,
                                 props: Signal[DrivetrainProperties],
                                 clock: Clock) extends FiniteTask {

    val positionSlide: Stream[Queue[(Angle, Time)]] = hardware.turnPosition.zipWithTime.sliding(20)

    private def calculateTargetFromOffsetWithLatency[T <: Quantity[T]]
    (timestampedOffset: Stream[(T, Time)],
     positionSlide: Stream[Queue[(T, Time)]]) = {
      positionSlide.zip(timestampedOffset).map { t =>
        val (positionHistory, (offset, offsetTime)) = t
        val closestTimeSoFar = positionHistory.minBy{ case (position, positionTime) =>
          Math.abs(positionTime.value - offsetTime.value)
        }

        closestTimeSoFar._1 + offset
      }
    }

    override def onStart(): Unit = {
      val targetAbsolute = calculateTargetFromOffsetWithLatency(timestampedOffset, positionSlide)

      val (controller, error) = continuousTurnPositionControl(targetAbsolute)
      val checkedController = controller.zip(error).withCheck { t =>
        val (_, e) = t
        if (e.abs < tolerance) {
          finished()
        }
      }.map(_._1)

      drive.setController(childVelocityControl(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveToTargetWithConstantSpeed(drivetrainComponent: Drivetrain,
                                       distanceToTarget: Stream[Option[Length]],
                                       angleToTarget: Stream[Angle],
                                       forwardVelocity: Dimensionless,
                                       maxTurnVelocity: Dimensionless,
                                       minDistance: Length)
                                      (implicit drivetrainHardware: DrivetrainHardware,
                                       props: Signal[DrivetrainProperties]) extends FiniteTask {

    override def onStart(): Unit = {
      val absoluteTargetAngle = drivetrainHardware.turnPosition.zipAsync(angleToTarget).map {t =>
        t._1 + t._2
      }

      val turnController = turnPositionControl(absoluteTargetAngle)._1

      val out = childVelocityControl(
        turnController.map { p =>
          UnicycleSignal(forwardVelocity, p.turn min maxTurnVelocity max -maxTurnVelocity)
        }
      )

      drivetrainComponent.setController(out.withCheckZipped(distanceToTarget){
        distanceToTarget => {
          if (distanceToTarget.exists(_ <= minDistance)) {
            finished()
          }
        }
      })
    }

    override def onEnd(): Unit = {
      drivetrainComponent.resetToDefault()
    }
  }
}
