package com.lynbrookrobotics.potassium.tasks
import com.lynbrookrobotics.potassium.Component

sealed trait WrapperTaskState
case object WrapperStopped extends WrapperTaskState
case object WaitingForReady extends WrapperTaskState
case object RunningInner extends WrapperTaskState

abstract class WrapperTask { self =>
  private var onReadyToRunInner: Option[() => Unit] = None

  protected def readyToRunInner(): Unit = {
    onReadyToRunInner.foreach(_.apply())
  }

  def onStart(): Unit
  def onEnd(): Unit
  val dependencies: Set[Component[_]]

  def apply(inner: FiniteTask): FiniteTask = {
    new FiniteTask with FiniteTaskFinishedListener {
      private var state: WrapperTaskState = WrapperStopped

      def onStart(): Unit = {
        state = WaitingForReady

        self.onReadyToRunInner = Some(() => {
          if (state == WaitingForReady) {
            state = RunningInner
            inner.setFinishedListener(this)
            inner.init()
          }
        })

        self.onStart()
      }

      def onEnd(): Unit = {
        self.onEnd()

        if (state == RunningInner) {
          inner.abort()
        }

        self.onReadyToRunInner = None

        state = WrapperStopped
      }

      override def onFinished(task: FiniteTask): Unit = {
        if (task == inner && state == RunningInner) {
          finished()
        }
      }

      override val dependencies: Set[Component[_]] = inner.dependencies ++ self.dependencies
    }
  }

  def apply(inner: ContinuousTask): ContinuousTask = {
    new ContinuousTask {
      private var state: WrapperTaskState = WrapperStopped

      def onStart(): Unit = {
        state = WaitingForReady

        self.onReadyToRunInner = Some(() => {
          if (state == WaitingForReady) {
            state = RunningInner
            inner.init()
          }
        })

        self.onStart()
      }

      def onEnd(): Unit = {
        if (state == RunningInner) {
          inner.abort()
        }

        self.onEnd()

        self.onReadyToRunInner = None

        state = WrapperStopped
      }

      override val dependencies: Set[Component[_]] = inner.dependencies ++ self.dependencies
    }
  }

  def toContinuous: ContinuousTask = apply(ContinuousTask.empty)
  def toFinite: FiniteTask = apply(FiniteTask.empty)
}
