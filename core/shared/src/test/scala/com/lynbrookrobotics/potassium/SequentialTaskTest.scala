package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks._
import org.scalatest.{BeforeAndAfter, FunSuite}

class SequentialTaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("Sequential task goes through correct flow") {
    var task1FinishTrigger: Option[() => Unit] = None
    var task2FinishTrigger: Option[() => Unit] = None

    var task1Started = false
    var task1Ended = false

    var task2Started = false
    var task2Ended = false

    val task1 = new FiniteTask {
      override def onStart(): Unit = {
        task1Started = true
      }

      override def onEnd(): Unit = {
        task1Ended = true
      }

      task1FinishTrigger = Some(() => finished())
    }

    val task2 = new FiniteTask {
      override def onStart(): Unit = {
        task2Started = true
      }

      override def onEnd(): Unit = {
        task2Ended = true
      }

      task2FinishTrigger = Some(() => finished())
    }

    val sequential = task1 then task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(sequential)

    assert(task1Started && !task1Ended && !task2Started && !task2Ended)

    task1FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && !task2Ended)

    task2FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!sequential.isRunning)
  }

  test("Sequential task with second task continuous goes through correct flow") {
    var task1FinishTrigger: Option[() => Unit] = None

    var task1Started = false
    var task1Ended = false

    var task2Started = false
    var task2Ended = false

    val task1 = new FiniteTask {
      override def onStart(): Unit = {
        task1Started = true
      }

      override def onEnd(): Unit = {
        task1Ended = true
      }

      task1FinishTrigger = Some(() => finished())
    }

    val task2 = new ContinuousTask {
      override def onStart(): Unit = {
        task2Started = true
      }

      override def onEnd(): Unit = {
        task2Ended = true
      }
    }

    val sequential = task1 then task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(sequential)

    assert(task1Started && !task1Ended && !task2Started && !task2Ended)

    task1FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && !task2Ended)

    sequential.abort()

    assert(task1Started && task1Ended && task2Started && task2Ended)
  }
}
