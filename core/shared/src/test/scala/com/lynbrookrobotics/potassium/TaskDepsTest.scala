package com.lynbrookrobotics.potassium

import com.lynbrookrobotics.potassium.tasks.{FiniteTask, Task}
import org.scalatest.{BeforeAndAfter, FunSuite}
import squants.radio.Irradiance
import squants.thermal.ThermalCapacity

class TaskDepsTest extends FunSuite with BeforeAndAfter {
  after {
    Task.abortCurrentTasks()
  }

  private val dummyComps = List(
    new Component[Int] {
      override def defaultController: streams.Stream[Int] = ???

      override def applySignal(signal: Int): Unit = ???
    },
    new Component[Int] {
      override def defaultController: streams.Stream[Int] = ???

      override def applySignal(signal: Int): Unit = ???
    },
    new Component[Irradiance] {
      override def defaultController: streams.Stream[Irradiance] = ???

      override def applySignal(signal: Irradiance): Unit = ???
    },
    new Component[ThermalCapacity] {
      override def defaultController: streams.Stream[ThermalCapacity] = ???

      override def applySignal(signal: ThermalCapacity): Unit = ???
    }
  )

  test("Tasks with overlapping dependencies abort each other") {

    def genTask(a: Int*) = new FiniteTask {
      override def onStart() = Unit
      override def onEnd() = Unit
      override val dependencies: Set[Component[_]] = a.map(dummyComps(_)).toSet
    }

    val a = genTask(0)
    assert(!a.isRunning)
    Task.executeTask(a)
    assert(a.isRunning)

    val b = genTask(0)
    assert(a.isRunning)
    assert(!b.isRunning)
    Task.executeTask(b)
    assert(!a.isRunning)
    assert(b.isRunning)

    Task.abortTaskUsing(dummyComps(0))
    assert(!a.isRunning)
    assert(!b.isRunning)

    val c = genTask(0, 1)
    assert(!c.isRunning)
    Task.executeTask(c)
    assert(c.isRunning)

    val d = genTask(0, 2)
    assert(c.isRunning)
    assert(!d.isRunning)
    Task.executeTask(d)
    assert(!c.isRunning)
    assert(d.isRunning)

    Task.abortTaskUsing(dummyComps(0))
    assert(!c.isRunning)
    assert(!d.isRunning)

    val e = genTask(0)
    val f = genTask(1)
    val g = genTask(2)
    val h = genTask(3)

    assert(!e.isRunning)
    assert(!f.isRunning)
    assert(!g.isRunning)
    assert(!h.isRunning)
    Task.executeTask(e)
    assert(e.isRunning)
    assert(!f.isRunning)
    assert(!g.isRunning)
    assert(!h.isRunning)
    Task.executeTask(f)
    assert(e.isRunning)
    assert(f.isRunning)
    assert(!g.isRunning)
    assert(!h.isRunning)
    Task.executeTask(g)
    assert(e.isRunning)
    assert(f.isRunning)
    assert(g.isRunning)
    assert(!h.isRunning)
    Task.abortTask(e)
    assert(!e.isRunning)
    assert(f.isRunning)
    assert(g.isRunning)
    assert(!h.isRunning)
    Task.abortTask(g)
    assert(!e.isRunning)
    assert(f.isRunning)
    assert(!g.isRunning)
    assert(!h.isRunning)
    Task.executeTask(h)
    assert(!e.isRunning)
    assert(f.isRunning)
    assert(!g.isRunning)
    assert(h.isRunning)
    Task.abortCurrentTasks()
    assert(!e.isRunning)
    assert(!f.isRunning)
    assert(!g.isRunning)
    assert(!h.isRunning)
  }
}
