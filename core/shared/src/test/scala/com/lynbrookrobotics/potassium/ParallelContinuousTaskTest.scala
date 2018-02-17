package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class ParallelContinuousTaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTask()
  }

  test("Parallel continuous task goes through correct flow") {
    var task1Started = false
    var task1Ended = false

    var task2Started = false
    var task2Ended = false

    val task1 = new ContinuousTask {
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

    val parallel = task1 and task2

    assert(!task1Started && !task1Ended && !task2Started && !task2Ended)

    Task.executeTask(parallel)

    parallel.abort()

    assert(task1Started && task1Ended && task2Started && task2Ended)
  }
}
