package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class SequentialTaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("Sequential task goes through correct flow") {
    var task1FinishTrigger: () => Unit = null
    var task2FinishTrigger: () => Unit = null

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

      task1FinishTrigger = () => finished()
    }

    val task2 = new FiniteTask {
      override def onStart(): Unit = {
        task2Started = true
      }

      override def onEnd(): Unit = {
        task2Ended = true
      }

      task2FinishTrigger = () => finished()
    }

    val sequential = task1 then task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(sequential)

    assert(task1Started && !task1Ended && !task2Started && !task2Ended)

    task1FinishTrigger()

    assert(task1Started && task1Ended && task2Started && !task2Ended)

    task2FinishTrigger()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!sequential.isRunning)
  }
}
