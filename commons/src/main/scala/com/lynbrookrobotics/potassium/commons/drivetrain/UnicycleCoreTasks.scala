package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.{Component, Signal}
import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask}
import squants.{Angle, Dimensionless, Length, Velocity}
import squants.motion.AngularVelocity

trait UnicycleCoreTasks {
  val controllers: UnicycleCoreControllers with UnicycleMotionProfileControllers

  type Drivetrain <: Component[controllers.DriveSignal]

  import controllers._

  class ContinuousClosedDrive(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
                             (implicit drive: Drivetrain, hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends ContinuousTask {
    override def onStart(): Unit = {
      val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
      drive.setController(parentClosedLoop(combined))
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
      drive.setController(parentClosedLoop(velocityControl(combined)))
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

      drive.setController(parentClosedLoop(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class DriveDistanceStraight(distance: Length, toleranceForward: Length, toleranceAngle: Angle)
                             (implicit drive: Drivetrain,
                              hardware: DrivetrainHardware,
                              props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteDistance = hardware.forwardPosition.get + distance
      val (forwardController, forwardError) = forwardPositionControl(absoluteDistance)

      val absoluteAngle = hardware.turnPosition.get
      val (turnController, turnError) = turnPositionControl(absoluteAngle)

      val combinedController = forwardController.zip(turnController).map(t => t._1 + t._2)

      val checkedController = combinedController.withCheck { _ =>
        if (forwardError.get.abs < toleranceForward && turnError.get.abs < toleranceAngle) {
          finished()
        }
      }

      drive.setController(parentClosedLoop(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }

  class RotateByAngle(relativeAngle: Angle, tolerance: Angle)
                     (implicit drive: Drivetrain,
                      hardware: DrivetrainHardware,
                      props: Signal[DrivetrainProperties]) extends FiniteTask {
    override def onStart(): Unit = {
      val absoluteAngle = hardware.turnPosition.get + relativeAngle
      val (controller, error) = turnPositionControl(absoluteAngle)
      val checkedController = controller.withCheck { _ =>
        if (error.get.abs < tolerance) {
          finished()
        }
      }

      drive.setController(parentClosedLoop(speedControl(checkedController)))
    }

    override def onEnd(): Unit = {
      drive.resetToDefault()
    }
  }
}
