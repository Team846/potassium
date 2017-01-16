package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.{PIDF, PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import com.lynbrookrobotics.potassium.units.GenericValue._
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}

import squants.motion.AngularVelocity
import squants.{Acceleration, Dimensionless, Length, Percent, Velocity}

trait UnicycleProperties {
  val maxForwardVelocity: Velocity
  val maxTurnVelocity: AngularVelocity

  val forwardControlGains: PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]
  val turnControlGains: PIDFConfig[AngularVelocity,
                                   GenericValue[AngularVelocity],
                                   GenericDerivative[AngularVelocity],
                                   GenericIntegral[AngularVelocity],
                                   Dimensionless]
}

trait UnicycleHardware {
  val forwardVelocity: Signal[Velocity]
  val turnVelocity: Signal[AngularVelocity]
}

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive {
  case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless)
  case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity)

  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  /**
    * Converts a unicycle signal value to the parent's signal type
    * @param uni the unicycle value to convert
    * @return the parent's equivalent signal
    */
  protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal

  /**
    * Uses the parent's open loop control for the equivalent drive signal for the unicycle signal
    * @param unicycle the unicycle signal to drive with
    * @return a signal controlled with open-loop on the parent
    */
  private def parentOpenLoop(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal] = {
    unicycle.map(convertUnicycleToDrive).toPeriodic
  }

  /**
    * Uses the parent's closed loop control for the drive signal for the unicycle signal
    * @param unicycle the unicycle signal to closed-loop drive with
    * @return a signal controlled with closed-loop on the parent
    */
  private def parentClosedLoop(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal] = {
    driveClosedLoop(unicycle.map(convertUnicycleToDrive))
  }

  object UnicycleControllers {
    def openForwardClosedDrive(forwardSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
      parentClosedLoop(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
    }

    def openTurnClosedDrive(turnSpeed: Signal[Dimensionless]): PeriodicSignal[DriveSignal] = {
      parentClosedLoop(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
    }

    def velocityControl(target: SignalLike[UnicycleVelocity])
                       (implicit hardware: DrivetrainHardware,
                        props: DrivetrainProperties): PeriodicSignal[UnicycleSignal] = {
      import hardware._
      import props._

      val forwardControl = PIDF.pidf(
        forwardVelocity.toPeriodic,
        target.map(_.forward).toPeriodic,
        forwardControlGains
      )

      val turnControl = PIDF.pidf(
        turnVelocity.toPeriodic,
        target.map(_.turn).toPeriodic,
        turnControlGains
      )

      forwardControl.zip(turnControl).map(s => UnicycleSignal(s._1, s._2))
    }

    def closedLoopControl(unicycle: SignalLike[UnicycleSignal])
                         (implicit hardware: DrivetrainHardware,
                          props: DrivetrainProperties): PeriodicSignal[UnicycleSignal] = {
      import props._

      velocityControl(unicycle.map(s => UnicycleVelocity(
        maxForwardVelocity * s.forward, maxTurnVelocity * s.turn
      )))
    }
  }

  import UnicycleControllers._

  object unicycleTasks {
    class ContinuousClosedDrive(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
                               (implicit drive: Drivetrain) extends ContinuousTask {
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
                                  props: DrivetrainProperties) extends ContinuousTask {
      override def onStart(): Unit = {
        val combined = forward.zip(turn).map(t => UnicycleVelocity(t._1, t._2))
        drive.setController(parentClosedLoop(velocityControl(combined)))
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }
  }

  protected def controlMode: UnicycleControlMode

  override protected def defaultController(implicit hardware: DrivetrainHardware,
                                           props: DrivetrainProperties): PeriodicSignal[DriveSignal] = {
    controlMode match {
      case NoOperation =>
        parentOpenLoop(Signal.constant(UnicycleSignal(Percent(0), Percent(0))))

      case ArcadeControls(forward, turn) =>
        val combinedSignal = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        parentClosedLoop(closedLoopControl(combinedSignal))
    }
  }
}
