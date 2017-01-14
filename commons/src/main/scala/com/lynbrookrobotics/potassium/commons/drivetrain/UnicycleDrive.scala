package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import com.lynbrookrobotics.potassium.units._
import squants.motion.AngularVelocity
import squants.{Acceleration, Dimensionless, Length, Percent, Velocity}
import GenericValue._

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive {
  case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless)
  case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity)

  protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal

  protected val maxForwardVelocity: Velocity
  protected val maxTurnVelocity: AngularVelocity

  protected val forwardControlGains: PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]

  protected val turnControlGains: PIDFConfig[AngularVelocity,
                                             GenericValue[AngularVelocity],
                                             GenericDerivative[AngularVelocity],
                                             GenericIntegral[AngularVelocity],
                                             Dimensionless]

  protected val forwardVelocity: PeriodicSignal[Velocity]
  protected val turnVelocity: PeriodicSignal[AngularVelocity]

  private def toOpenDrive(unicycle: Signal[UnicycleSignal]): Signal[DriveSignal] = {
    unicycle.map(convertUnicycleToDrive)
  }

  private def toClosedDrive(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal] = {
    driveClosedLoop(unicycle.map(convertUnicycleToDrive))
  }

  object UnicycleControllers {
    def forwardOpen(forwardSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
      toClosedDrive(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
    }

    def turnOpen(turnSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
      toClosedDrive(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
    }

    def velocity(target: SignalLike[UnicycleVelocity]): PeriodicSignal[UnicycleSignal] = {
      val forwardControl = PIDF.pidf(forwardVelocity, target.map(_.forward).toPeriodic, forwardControlGains)
      val turnControl = PIDF.pidf(turnVelocity, target.map(_.turn).toPeriodic, turnControlGains)

      forwardControl.zip(turnControl).map(s => UnicycleSignal(s._1, s._2))
    }

    def expectedVelocity(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[UnicycleSignal] = {
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
        drive.setController(toClosedDrive(combined))
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }

    class ContinuousVelocityDrive(forward: Signal[Velocity], turn: Signal[AngularVelocity])
                                 (implicit drive: Drivetrain) extends ContinuousTask {
      override def onStart(): Unit = {
        val combined = forward.zip(turn).map(t => UnicycleVelocity(t._1, t._2))
        drive.setController(toClosedDrive(velocity(combined)))
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }
  }

  protected def controlMode: UnicycleControlMode

  override protected def defaultController: PeriodicSignal[DriveSignal] = controlMode match {
    case NoOperation =>
      toOpenDrive(Signal.constant(UnicycleSignal(Percent(0), Percent(0)))).toPeriodic

    case ArcadeControls(forward, turn) =>
      toClosedDrive(expectedVelocity(forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))))
  }
}
