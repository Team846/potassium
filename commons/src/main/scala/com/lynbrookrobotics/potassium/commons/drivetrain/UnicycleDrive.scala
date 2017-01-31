package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.control.{PIDFConfig, PIDFProperUnitsConfig}
import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.motion.AngularVelocity
import squants.{Acceleration, Angle, Dimensionless, Length, Percent, Velocity}

trait UnicycleProperties {
  def maxForwardVelocity: Velocity
  def maxTurnVelocity: AngularVelocity

  def forwardControlGains: PIDFProperUnitsConfig[Velocity, Acceleration, Length, Dimensionless]
  def turnControlGains: PIDFConfig[AngularVelocity,
                                   GenericValue[AngularVelocity],
                                   GenericValue[AngularVelocity],
                                   GenericDerivative[AngularVelocity],
                                   GenericIntegral[AngularVelocity],
                                   Dimensionless]

  def forwardPositionControlGains: PIDFConfig[Length,
                                              Length,
                                              GenericValue[Length],
                                              Velocity,
                                              GenericIntegral[Length],
                                              Dimensionless]

  def turnPositionControlGains: PIDFConfig[Angle,
                                           Angle,
                                           GenericValue[Angle],
                                           AngularVelocity,
                                           GenericIntegral[Angle],
                                           Dimensionless]
}

trait UnicycleHardware {
  def forwardVelocity: Signal[Velocity]
  def turnVelocity: Signal[AngularVelocity]

  def forwardPosition: Signal[Length]
  def turnPosition: Signal[Angle]
}

case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless) {
  def +(that: UnicycleSignal): UnicycleSignal =
    UnicycleSignal(this.forward + that.forward, this.turn + that.turn)
}

case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity)

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive { self =>
  type DrivetrainHardware <: UnicycleHardware
  type DrivetrainProperties <: UnicycleProperties

  /**
    * Converts a unicycle signal value to the parent's signal type
    * @param uni the unicycle value to convert
    * @return the parent's equivalent signal
    */
  protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal
  
  object UnicycleControllers extends UnicycleCoreControllers
    with UnicycleMotionProfileControllers {
    type DriveSignal = self.DriveSignal
    type DrivetrainHardware = self.DrivetrainHardware
    type DrivetrainProperties = self.DrivetrainProperties

    /**
      * Uses the parent's open loop control for the equivalent drive signal for the unicycle signal
      * @param unicycle the unicycle signal to drive with
      * @return a signal controlled with open-loop on the parent
      */
    def parentOpenLoop(unicycle: SignalLike[UnicycleSignal]): PeriodicSignal[DriveSignal] = {
      unicycle.map(convertUnicycleToDrive).toPeriodic
    }

    /**
      * Uses the parent's closed loop control for the drive signal for the unicycle signal
      * @param unicycle the unicycle signal to closed-loop drive with
      * @return a signal controlled with closed-loop on the parent
      */
    def parentClosedLoop(unicycle: SignalLike[UnicycleSignal])(implicit hardware: DrivetrainHardware,
                                                               props: DrivetrainProperties): PeriodicSignal[DriveSignal] = {
      driveClosedLoop(unicycle.map(convertUnicycleToDrive))
    }
  }

  import UnicycleControllers._

  object unicycleTasks extends UnicycleCoreTasks {
    type Drivetrain = self.Drivetrain
    override val controllers = UnicycleControllers
  }

  protected def controlMode(implicit hardware: DrivetrainHardware,
                            props: DrivetrainProperties): UnicycleControlMode

  override protected def defaultController(implicit hardware: DrivetrainHardware,
                                           props: DrivetrainProperties): PeriodicSignal[DriveSignal] = {
    controlMode match {
      case NoOperation =>
        parentOpenLoop(Signal.constant(UnicycleSignal(Percent(0), Percent(0))))

      case ArcadeControlsOpen(forward, turn) =>
        val combinedSignal = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        parentOpenLoop(combinedSignal)

      case ArcadeControlsClosed(forward, turn) =>
        val combinedSignal = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        parentClosedLoop(speedControl(combinedSignal))
    }
  }
}
