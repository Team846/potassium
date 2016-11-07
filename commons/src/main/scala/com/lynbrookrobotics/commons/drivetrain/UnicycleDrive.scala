package com.lynbrookrobotics.commons.drivetrain

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.tasks.ContinuousTask
import squants.{Dimensionless, Percent}

/**
  * A drivetrain that has forward-backward and turning control in the unicycle model
  */
trait UnicycleDrive extends Drive {
  case class UnicycleSignal(forward: Dimensionless, turn: Dimensionless)

  protected def convertToDrive(uni: UnicycleSignal): DriveSignal

  object UnicycleControllers {
    def unicycleControl(unicycle: Signal[UnicycleSignal]): Signal[DriveSignal] = {
      unicycle.map(convertToDrive)
    }

    def forwardControl(forwardSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
      unicycleControl(forwardSpeed.map(f => UnicycleSignal(f, Percent(0))))
    }

    def turnControl(turnSpeed: Signal[Dimensionless]): Signal[DriveSignal] = {
      unicycleControl(turnSpeed.map(t => UnicycleSignal(Percent(0), t)))
    }
  }

  object unicycleTasks {
    class ContinuousDrive(forward: Signal[Dimensionless], turn: Signal[Dimensionless])
                         (implicit drive: Drivetrain) extends ContinuousTask {
      override def onStart(): Unit = {
        val combined = forward.zip(turn).map(t => UnicycleSignal(t._1, t._2))
        drive.setController(UnicycleControllers.unicycleControl(combined).toPeriodic)
      }

      override def onEnd(): Unit = {
        drive.resetToDefault()
      }
    }
  }

  import UnicycleControllers._

  protected val controlMode: UnicycleControlMode

  override protected val defaultController = controlMode match {
    case NoOperation =>
      unicycleControl(Signal.constant(UnicycleSignal(Percent(0), Percent(0))))

    case ArcadeControls(forward, turn) =>
      unicycleControl(forward.zip(turn).map(t => UnicycleSignal(t._1, t._2)))
  }
}
