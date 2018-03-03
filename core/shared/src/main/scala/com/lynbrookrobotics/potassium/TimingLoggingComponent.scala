package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.logging.{AsyncLogger, Histogram}
import com.lynbrookrobotics.potassium.streams.{Cancel, Periodic}

/**
  *
  * By exteding this trait, a component will print a message with histogram
  * statistics about timing data if the controller stream "misses a cycle"
  * Aka. updates with a period longer than 2 times the expected periodicity
  */
trait TimingLoggingComponent[T] extends Component[T] {
  val logger: AsyncLogger

  private var currentTimingHandle: Option[Cancel] = None

  override def setController(controller: streams.Stream[T]): Unit = {
    super.setController(controller)

    val streamPeriodicity = controller.expectedPeriodicity.asInstanceOf[Periodic].period
    val histogram = new Histogram(
      min = 0.8 * streamPeriodicity,
      max = 1.2 * streamPeriodicity,
      binCount = 10,
      asyncLogger = logger)

    currentTimingHandle.foreach(_.cancel())
    currentTimingHandle = Some(
      controller.zipWithDt.foreach { case (_, dt) =>
        histogram.apply(dt)
        if (dt >= 2 * streamPeriodicity) {
          histogram.printStatus()
        }
      }
    )
  }
}
