package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.motion.AngularVelocity
import squants.{Angle, Dimensionless, Length, Percent, Velocity}

trait UnicycleProperties {
  val maxForwardVelocity: Velocity
  val maxTurnVelocity: AngularVelocity
  val defaultLookAheadDistance: Length

  val forwardControlGains: ForwardVelocityGains

  lazy val forwardControlGainsFull: ForwardVelocityGains#Full = {
    forwardControlGains.withF(Percent(100) / maxForwardVelocity)
  }

  val turnControlGains: TurnVelocityGains

  lazy val turnControlGainsFull: TurnVelocityGains#Full = {
    turnControlGains.withF(Percent(100) / maxTurnVelocity)
  }

  val forwardPositionControlGains: ForwardPositionGains

  val turnPositionControlGains: TurnPositionGains
}

trait UnicycleHardware {
  val forwardVelocity: Signal[Velocity]
  val turnVelocity: Signal[AngularVelocity]

  val forwardPosition: Signal[Length]
  val turnPosition: Signal[Angle]
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
  type Hardware <: UnicycleHardware
  type Properties <: UnicycleProperties

  /**
    * Converts a unicycle signal value to the parent's signal type
    * @param uni the unicycle value to convert
    * @return the parent's equivalent signal
    */
  protected def convertUnicycleToDrive(uni: UnicycleSignal): DriveSignal
  
  object UnicycleControllers extends UnicycleCoreControllers {
    type DriveSignal = self.DriveSignal
    type DrivetrainHardware = self.Hardware
    type DrivetrainProperties = self.Properties

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
                                                               props: Signal[DrivetrainProperties]): PeriodicSignal[DriveSignal] = {
      driveClosedLoop(unicycle.map(convertUnicycleToDrive))
    }
  }

  import UnicycleControllers._

  object unicycleTasks extends UnicycleCoreTasks {
    type Drivetrain = self.Drivetrain
    override val controllers = UnicycleControllers
  }

  protected def controlMode(implicit hardware: Hardware,
                            props: Properties): UnicycleControlMode

  override protected def defaultController(implicit hardware: Hardware,
                                           props: Signal[Properties]): PeriodicSignal[DriveSignal] = {
    controlMode(hardware, props.get) match {
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
