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
    * Converts a unicycle signal value to the child's signal type
    *
    * @param uni the unicycle value to convert
    * @return the child's equivalent signal
    */
  protected def unicycleToOpenLoopSignal(uni: UnicycleSignal): OpenLoopSignal

  object UnicycleControllers extends PurePursuitControllers with UnicycleMotionProfileControllers with UnicycleCoreControllers {
    type DriveSignal = self.DriveSignal
    type OpenLoopSignal = self.OpenLoopSignal
    type DrivetrainHardware = self.Hardware
    type DrivetrainProperties = self.Properties

    /**
      * Uses the childs's open loop control for the equivalent drive signal for the unicycle signal
      *
      * @param unicycle the unicycle signal to drive with
      * @return a signal controlled with open-loop on the child
      */
    def childOpenLoop(unicycle: Stream[UnicycleSignal]): Stream[DriveSignal] =
      unicycle.map(unicycleToOpenLoopSignal).map(self.openLoopToDriveSignal)

    /**
      * Uses the child's closed loop control for the drive signal for the unicycle signal
      *
      * @param unicycle the unicycle signal to closed-loop drive with
      * @return a signal controlled with closed-loop on the child
      */
    def childVelocityControl(unicycle: Stream[UnicycleSignal])
                            (implicit hardware: DrivetrainHardware,
                             props: Signal[DrivetrainProperties]): Stream[DriveSignal] =
      driveClosedLoop(unicycle.map(unicycleToOpenLoopSignal))
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
        childOpenLoop(
          hardware.forwardPosition
            .mapToConstant(
              UnicycleSignal(Percent(0), Percent(0))
            )
        )

      case ArcadeControlsOpen(forward, turn) =>
        childOpenLoop(
          forward
            .zip(turn)
            .map(t => UnicycleSignal(t._1, t._2))
        )

      case ArcadeControlsClosed(forward, turn) =>
        childVelocityControl(
          speedControl(
            forward
              .zip(turn)
              .map(t => UnicycleSignal(t._1, t._2))
          )
        )
    }
  }
}
