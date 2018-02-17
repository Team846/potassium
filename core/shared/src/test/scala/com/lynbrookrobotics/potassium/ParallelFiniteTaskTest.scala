package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class ParallelFiniteTaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("Parallel task goes through correct flow") {
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

    val parallel = task1 and task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(parallel)

    assert(task1Started && !task1Ended && task2Started && !task2Ended)

    task1FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && !task2Ended)

    task2FinishTrigger.get.apply()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!parallel.isRunning)
  }

  test("Parallel task aborts both tasks when they are still running") {
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

    val task2 = new FiniteTask {
      override def onStart(): Unit = {
        task2Started = true
      }

      override def onEnd(): Unit = {
        task2Ended = true
      }
    }

    val parallel = task1 and task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(parallel)

    assert(task1Started && !task1Ended && task2Started && !task2Ended)

    parallel.abort()

    assert(task1Started && task1Ended && task2Started && task2Ended)
    assert(!parallel.isRunning)
  }
}
