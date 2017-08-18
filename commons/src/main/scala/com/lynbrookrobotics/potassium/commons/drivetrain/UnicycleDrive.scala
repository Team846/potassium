package com.lynbrookrobotics.potassium.commons.drivetrain

import com.lynbrookrobotics.potassium.units._
import com.lynbrookrobotics.potassium.{PeriodicSignal, Signal, SignalLike}
import squants.motion.AngularVelocity
import com.lynbrookrobotics.potassium.streams.Stream
import squants.{Acceleration, Angle, Dimensionless, Each, Length, Percent, Velocity}
import com.lynbrookrobotics.potassium.commons.drivetrain.UnicycleMotionProfileControllers
import squants.space.Feet
import squants.time.Milliseconds

trait UnicycleProperties {
  val maxForwardVelocity: Velocity
  val maxTurnVelocity: AngularVelocity
  val maxAcceleration: Acceleration
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
  val forwardVelocity: Stream[Velocity]
  val turnVelocity: Stream[AngularVelocity]

  val forwardPosition: Stream[Length]
  val turnPosition: Stream[Angle]
}

case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless) {
  def +(that: UnicycleSignal): UnicycleSignal =
    UnicycleSignal(this.forward + that.forward, this.turn + that.turn)
}

case class UnicycleVelocity(forward: Velocity, turn: AngularVelocity) {
  def toUnicycleSignal(implicit unicycleProperties: Signal[UnicycleProperties]) = {
    UnicycleSignal(
      Each(forward / unicycleProperties.get.maxForwardVelocity),
      Each(turn / unicycleProperties.get.maxTurnVelocity))
  }
}

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
  
  object UnicycleControllers extends PurePursuitControllers with UnicycleMotionProfileControllers with UnicycleCoreControllers{
    type DriveSignal = self.DriveSignal
    type DrivetrainHardware = self.Hardware
    type DrivetrainProperties = self.Properties

    /**
      * Uses the parent's open loop control for the equivalent drive signal for the unicycle signal
      * @param unicycle the unicycle signal to drive with
      * @return a signal controlled with open-loop on the parent
      */
    def parentOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[DriveSignal] = {
      unicycle.map(convertUnicycleToDrive)
    }

    /**
      * Uses the parent's closed loop control for the drive signal for the unicycle signal
      * @param unicycle the unicycle signal to closed-loop drive with
      * @return a signal controlled with closed-loop on the parent
      */
    def lowerLevelVelocityControl(unicycle: Stream[UnicycleSignal])(implicit hardware: DrivetrainHardware,
                                                                        props: Signal[DrivetrainProperties]): Stream[DriveSignal] = {
      driveClosedLoop(unicycle.map(convertUnicycleToDrive))
    }
  }

  import UnicycleControllers._

  object unicycleTasks extends UnicycleCoreTasks with PurePursuitTasks {
    type Drivetrain = self.Drivetrain
    override val controllers = UnicycleControllers
  }

  protected def controlMode(implicit hardware: Hardware,
                            props: Properties): UnicycleControlMode

  override protected def defaultController(implicit hardware: Hardware,
                                           props: Signal[Properties]): Stream[DriveSignal] = {
    controlMode(hardware, props.get) match {
      case NoOperation =>
        parentOpenLoop(hardware.forwardPosition.mapToConstant(UnicycleSignal(Percent(0), Percent(0))))

      case ArcadeControlsOpen(forward, turn) =>
        val combinedSignal = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        parentOpenLoop(combinedSignal)

      case ArcadeControlsClosed(forward, turn) =>
        val combinedSignal = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        lowerLevelVelocityControl(speedControl(combinedSignal))
    }
  }
}
