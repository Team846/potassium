package com.lynbrookrobotics.potassium.tasks

/**
  * A general task, which can be started and stopped.
  */
abstract class Task {
  def init(): Unit
  def abort(): Unit
}

object Task {
  private var currentTask: Option[Task] = None

  /**
    * Starts the task, aborting any other running tasks.
    * @param task the task to run
    */
  def executeTask(task: Task): Unit = {
    currentTask.foreach(_.abort())
    currentTask = Some(task)
    task.init()
  }

  /**
    * Aborts the task, if it is currently running.
    * @param task the task to shut down
    */
  def abortTask(task: Task): Unit = {
    currentTask.foreach { t =>
      if (task == t) {
        t.abort()
        currentTask = None
      }
    }
  }

  /**
    * Shuts down the current task.
    */
  def abortCurrentTask(): Unit = {
    currentTask.foreach(abortTask)
  }
}
