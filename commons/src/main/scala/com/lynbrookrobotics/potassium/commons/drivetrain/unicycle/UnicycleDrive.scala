package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.commons.drivetrain.purePursuit.{PurePursuitControllers, PurePursuitTasks}
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Percent

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
