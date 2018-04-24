package com.lynbrookrobotics.potassium.tasks

import com.lynbrookrobotics.potassium.Component

/**
  * A general task, which can be started and stopped.
  */
abstract class Task {
  def init(): Unit
  def abort(): Unit
  val dependencies: Set[Component[_]]
}

object Task {
  private var currentTasks: Set[Task] = Set()

  /**
    * Starts the task, aborting any other running tasks.
    * @param newTask the task to run
    */
  def executeTask(newTask: Task): Unit = {
    currentTasks
      .filter(running => (running.dependencies intersect newTask.dependencies).nonEmpty)
      .foreach(_.abort())

    currentTasks += newTask
    newTask.init()
  }

  /**
    * Aborts the task, if it is currently running.
    * @param task the task to shut down
    */
  def abortTask(task: Task): Unit = {
    val (abortable, running) = currentTasks.partition(_ == task)
    abortable.foreach(_.abort())
    currentTasks = running
  }

  /**
    * Shuts down the all tasks.
    */
  def abortCurrentTasks(): Unit = {
    currentTasks.foreach(abortTask)
  }

  /**
    * Shuts down the task currently using a component.
    */
  def abortTaskUsing(dependency:Component[_]): Unit = {
    currentTasks
      .filter(_.dependencies contains dependency)
      .foreach(abortTask)
  }
}
