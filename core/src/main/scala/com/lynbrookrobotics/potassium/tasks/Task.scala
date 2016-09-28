package com.lynbrookrobotics.potassium.tasks

abstract class Task {
  def init(): Unit
  def abort(): Unit
}

object Task {
  private var currentTask: Option[Task] = None

  def executeTask(task: Task): Unit = {
    currentTask.foreach(_.abort())
    currentTask = Some(task)
    task.init()
  }

  def abortTask(task: Task): Unit = {
    currentTask.foreach { t =>
      if (task == t) {
        t.abort()
        currentTask = None
      }
    }
  }

  def abortCurrentTask(): Unit = {
    currentTask.foreach(abortTask)
  }
}
