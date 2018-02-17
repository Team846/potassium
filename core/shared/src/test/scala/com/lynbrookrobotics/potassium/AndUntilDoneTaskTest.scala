package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class AndUntilDoneTaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("AndUntilDone task goes through correct flow") {
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

    val andUntilDone = task1 andUntilDone task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(andUntilDone)

    assert(task1Started && !task1Ended && task2Started && !task2Ended)

    task1FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!andUntilDone.isRunning)
  }

  test("AndUntilDone task aborts both finite and continuous task") {
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
    }

    val task2 = new ContinuousTask {
      override def onStart(): Unit = {
        task2Started = true
      }

      override def onEnd(): Unit = {
        task2Ended = true
      }
    }

    val andUntilDone = task1 andUntilDone task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(andUntilDone)

    assert(task1Started && !task1Ended && task2Started && !task2Ended)

    andUntilDone.abort()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!andUntilDone.isRunning)
  }
}
