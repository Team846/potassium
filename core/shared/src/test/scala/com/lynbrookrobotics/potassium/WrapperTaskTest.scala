package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{ContinuousTask, FiniteTask, WrapperTask}
import org.scalatest.FunSuite

class WrapperTaskTest extends FunSuite {
  test("Wrapper task goes through correct flow for finite inner task") {
    var finiteTaskFinishTrigger: Option[() => Unit] = None

    var finiteTaskStarted = false
    var finiteTaskEnded = false

    val finiteTask = new FiniteTask {
      override def onStart(): Unit = {
        finiteTaskStarted = true
      }

      override def onEnd(): Unit = {
        finiteTaskEnded = true
      }

      finiteTaskFinishTrigger = Some(() => finished())
    }

    var wrapperTaskFinishTrigger: Option[() => Unit] = None

    var wrapperTaskStarted = false
    var wrapperTaskEnded = false

    val wrapper = new WrapperTask {
      override def onStart() = {
        wrapperTaskStarted = true
      }

      override def onEnd() = {
        wrapperTaskEnded = true
      }

      wrapperTaskFinishTrigger = Some(() => readyToRunInner())
    }

    val wrappedTask = wrapper(finiteTask)

    assert(!finiteTaskStarted && !finiteTaskEnded && !wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.init()

    assert(!finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrapperTaskFinishTrigger.get.apply()

    assert(finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    finiteTaskFinishTrigger.get.apply()

    assert(finiteTaskStarted && finiteTaskEnded && wrapperTaskStarted && wrapperTaskEnded)
  }

  test("Wrapper task aborts correctly when finite inner task has not started") {
    var finiteTaskFinishTrigger: Option[() => Unit] = None

    var finiteTaskStarted = false
    var finiteTaskEnded = false

    val finiteTask = new FiniteTask {
      override def onStart(): Unit = {
        finiteTaskStarted = true
      }

      override def onEnd(): Unit = {
        finiteTaskEnded = true
      }

      finiteTaskFinishTrigger = Some(() => finished())
    }

    var wrapperTaskFinishTrigger: Option[() => Unit] = None

    var wrapperTaskStarted = false
    var wrapperTaskEnded = false

    val wrapper = new WrapperTask {
      override def onStart() = {
        wrapperTaskStarted = true
      }

      override def onEnd() = {
        wrapperTaskEnded = true
      }

      wrapperTaskFinishTrigger = Some(() => readyToRunInner())
    }

    val wrappedTask = wrapper(finiteTask)

    assert(!finiteTaskStarted && !finiteTaskEnded && !wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.init()

    assert(!finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.abort()

    assert(!finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && wrapperTaskEnded)
  }

  test("Wrapper task aborts correctly when finite inner task has already started") {
    var finiteTaskFinishTrigger: Option[() => Unit] = None

    var finiteTaskStarted = false
    var finiteTaskEnded = false

    val finiteTask = new FiniteTask {
      override def onStart(): Unit = {
        finiteTaskStarted = true
      }

      override def onEnd(): Unit = {
        finiteTaskEnded = true
      }

      finiteTaskFinishTrigger = Some(() => finished())
    }

    var wrapperTaskFinishTrigger: Option[() => Unit] = None

    var wrapperTaskStarted = false
    var wrapperTaskEnded = false

    val wrapper = new WrapperTask {
      override def onStart() = {
        wrapperTaskStarted = true
      }

      override def onEnd() = {
        wrapperTaskEnded = true
      }

      wrapperTaskFinishTrigger = Some(() => readyToRunInner())
    }

    val wrappedTask = wrapper(finiteTask)

    assert(!finiteTaskStarted && !finiteTaskEnded && !wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.init()

    assert(!finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrapperTaskFinishTrigger.get.apply()

    assert(finiteTaskStarted && !finiteTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.abort()

    assert(finiteTaskStarted && finiteTaskEnded && wrapperTaskStarted && wrapperTaskEnded)
  }

  test("Wrapper task goes through correct flow for continuous inner task") {
    var innerTaskStarted = false
    var innerTaskEnded = false

    val innerTask = new ContinuousTask {
      override def onStart(): Unit = {
        innerTaskStarted = true
      }

      override def onEnd(): Unit = {
        innerTaskEnded = true
      }
    }

    var wrapperTaskFinishTrigger: Option[() => Unit] = None

    var wrapperTaskStarted = false
    var wrapperTaskEnded = false

    val wrapper = new WrapperTask {
      override def onStart() = {
        wrapperTaskStarted = true
      }

      override def onEnd() = {
        wrapperTaskEnded = true
      }

      wrapperTaskFinishTrigger = Some(() => readyToRunInner())
    }

    val wrappedTask = wrapper(innerTask)

    assert(!innerTaskStarted && !innerTaskEnded && !wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.init()

    assert(!innerTaskStarted && !innerTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrapperTaskFinishTrigger.get.apply()

    assert(innerTaskStarted && !innerTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.abort()

    assert(innerTaskStarted && innerTaskEnded && wrapperTaskStarted && wrapperTaskEnded)
  }

  test("Wrapper task aborts correctly when continuous inner task has not started") {
    var innerTaskStarted = false
    var innerTaskEnded = false

    val innerTask = new ContinuousTask {
      override def onStart(): Unit = {
        innerTaskStarted = true
      }

      override def onEnd(): Unit = {
        innerTaskEnded = true
      }
    }

    var wrapperTaskFinishTrigger: Option[() => Unit] = None

    var wrapperTaskStarted = false
    var wrapperTaskEnded = false

    val wrapper = new WrapperTask {
      override def onStart() = {
        wrapperTaskStarted = true
      }

      override def onEnd() = {
        wrapperTaskEnded = true
      }

      wrapperTaskFinishTrigger = Some(() => readyToRunInner())
    }

    val wrappedTask = wrapper(innerTask)

    assert(!innerTaskStarted && !innerTaskEnded && !wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.init()

    assert(!innerTaskStarted && !innerTaskEnded && wrapperTaskStarted && !wrapperTaskEnded)

    wrappedTask.abort()

    assert(!innerTaskStarted && !innerTaskEnded && wrapperTaskStarted && wrapperTaskEnded)
  }
}
