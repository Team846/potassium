package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}

class TaskTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTasks()
  }

  test("Single task execute and abort") {
    var started = false
    var aborted = false

    val task = new Task {
      override def init(): Unit = {
        started = true
      }

      override def abort(): Unit = {
        aborted = true
      }

      override val dependencies: Set[Component[_]] = Set()
    }

    assert(!started && !aborted)

    task.init()

    assert(started && !aborted)

    task.abort()

    assert(started && aborted)
  }

  test("FiniteTask.empty immediately finishes"){
    val task = FiniteTask.empty

    task.init()

    assert(!task.isRunning)
  }
}
