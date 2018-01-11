package com.lynbrookrobotics.potassium.commons.drivetrain.unicycle

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.commons.drivetrain._
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.purePursuit.{PurePursuitControllers, PurePursuitTasks}
import com.lynbrookrobotics.potassium.commons.drivetrain.unicycle.control.{UnicycleCoreControllers, UnicycleCoreTasks, UnicycleMotionProfileControllers}
import com.lynbrookrobotics.potassium.streams.Stream
import squants.Percent

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive {
  self =>
  type Hardware <: UnicycleHardware
  type Properties <: UnicycleProperties

  /**
    * Converts a unicycle signal value to the parent's signal type
    *
    * @param uni the unicycle value to convert
    * @return the parent's equivalent signal
    */
  protected def convertUnicycleToOpenLoopInput(uni: UnicycleSignal): OpenLoopInput

  object UnicycleControllers extends PurePursuitControllers with UnicycleMotionProfileControllers with UnicycleCoreControllers {
    type DriveSignal = self.DriveSignal
    type DrivetrainHardware = self.Hardware
    type DrivetrainProperties = self.Properties
    type OpenLoopInput = self.OpenLoopInput

    override def openLoopToDriveSignal(openLoopInput: OpenLoopInput): DriveSignal = self.openLoopToDriveSignal(openLoopInput)

    /**
      * Uses the child's open loop control for the equivalent drive signal for the unicycle signal
      *
      * @param unicycle the unicycle signal to drive with
      * @return a signal controlled with open-loop on the child
      */
    def childOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[OpenLoopInput] =
      unicycle.map(convertUnicycleToOpenLoopInput)

    /**
      * Uses the child's closed loop control for the drive signal for the unicycle signal
      *
      * @param unicycle the unicycle signal to closed-loop drive with
      * @return a signal controlled with closed-loop on the child
      */
    def childVelocityControl(unicycle: Stream[UnicycleSignal])
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): Stream[DriveSignal] =
      driveClosedLoop(unicycle.map(convertUnicycleToOpenLoopInput))
  }

  object unicycleTasks extends UnicycleCoreTasks with PurePursuitTasks {
    type Drivetrain = self.Drivetrain
    override val controllers = UnicycleControllers
  }

  protected def getControlMode(implicit hardware: Hardware,
                               props: Properties): UnicycleControlMode

  override protected def defaultController(implicit hardware: Hardware,
                                           props: Signal[Properties]): Stream[DriveSignal] = {
    getControlMode(hardware, props.get) match {
      case NoOperation =>
        UnicycleControllers.childOpenLoop(
          hardware.forwardPosition.mapToConstant(
            UnicycleSignal(Percent(0), Percent(0))
          )
        ).map(openLoopToDriveSignal)

      case ArcadeControlsOpen(forward, turn) =>
        UnicycleControllers.childOpenLoop(
          forward.zip(turn).map(
            t => UnicycleSignal(t._1, t._2)
          )
        ).map(openLoopToDriveSignal)

      case ArcadeControlsClosed(forward, turn) =>
        UnicycleControllers.childVelocityControl(UnicycleControllers.speedControl(
          forward.zip(turn).map(
            t => UnicycleSignal(t._1, t._2)
          )
        ))
    }
  }
}
