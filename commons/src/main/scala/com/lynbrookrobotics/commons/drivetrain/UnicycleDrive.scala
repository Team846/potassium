package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal}
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import com.lynbrookrobotics.potassium.units._
import squants.motion.AngularVelocity
import squants.{Dimensionless, Percent, Velocity}

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive {
  case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless)
  case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity)

  protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal

  protected val maxForwardVelocity: Velocity
  protected val maxTurnVelocity: AngularVelocity

  // TODO: replace with configuration object from control module
  protected val forwardProportionalGain: Ratio[Dimensionless, Velocity]
  protected val turnProportionalGain: Ratio[Dimensionless, AngularVelocity]

  protected val forwardFeedForwardGain: Ratio[Dimensionless, Velocity]
  protected val turnFeedForwardGain: Ratio[Dimensionless, AngularVelocity]

  protected val forwardVelocity: Signal[Velocity]
  protected val turnVelocity: Signal[AngularVelocity]

  private def toOpenDrive(unicycle: Signal[UnicycleSignal]): Signal[DriveSignal] = {
    unicycle.map(convertUnicycleToDrive)
  }

  private def toClosedDrive(unicycle: Signal[UnicycleSignal]): Signal[DriveSignal] = {
    driveClosedLoop(unicycle.map(convertUnicycleToDrive))
  }

  object UnicycleControllers {
    def forward(forwardSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
      toClosedDrive(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
    }

    def turn(turnSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
      toClosedDrive(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
    }

    def velocity(unicycle: Signal[UnicycleVelocity]): Signal[UnicycleSignal] = {
      unicycle.zip(forwardVelocity).zip(turnVelocity).map { case ((target, fwdVel), turnVel) =>
        UnicycleSignal(
          (target.forward ** forwardFeedForwardGain) + ((target.forward - fwdVel) ** forwardProportionalGain),
          (target.turn ** turnFeedForwardGain) + ((target.turn - turnVel) ** turnProportionalGain)
        )
      }
    }

    def expectedVelocity(unicycle: Signal[UnicycleSignal]): Signal[UnicycleSignal] = {
      velocity(unicycle.map(s => UnicycleVelocity(
        maxForwardVelocity * s.forward, maxTurnVelocity * s.turn
      )))
    }
  }

  import UnicycleControllers._

  object unicycleTasks {
    class ContinuousDrive(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
                         (implicit drive: Drivetrain) extends ContinuousTask {
      override def onStart(): Unit = {
        val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        drive.setController(toClosedDrive(combined).toPeriodic)
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }

    class ContinuousVelocityDrive(forward: Signal[Velocity], turn: Signal[AngularVelocity])
                                 (implicit drive: Drivetrain) extends ContinuousTask {
      override def onStart(): Unit = {
        val combined = forward.zip(turn).map(t => UnicycleVelocity(t._1, t._2))
        drive.setController(toClosedDrive(velocity(combined)).toPeriodic)
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }
  }

  protected def controlMode: UnicycleControlMode

  override protected def defaultController: PeriodicSignal[DriveSignal] = (controlMode match {
    case NoOperation =>
      toOpenDrive(Signal.constant(UnicycleSignal(Percent(0), Percent(0))))

    case ArcadeControls(forward, turn) =>
      toClosedDrive(expectedVelocity(forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))))
  }).toPeriodic
}
