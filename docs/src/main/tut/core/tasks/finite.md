---
layout: docs
title: Finite Tasks
section: core
---

# Finite Tasks
Finite tasks are actions that have a defined starting, running, and ending period. These tasks are primarily used for autonomous routines, where a robot must perform a series of actions one after the other. Through the task API, finite tasks can be easily composed to form complex routines based on elementary pieces.

## Creating a Finite Task
To create a finite task, simply extend the `FiniteTask` class and implement the `onStart` and `onEnd` methods:
```scala
class DriveForward(distance: Double) extends FiniteTask {
  def onStart(): Unit = { ... }
  def onEnd(): Unit = { ... }
}
```

One important thing to remember is that **finite tasks must decide when they are done**. To do this, call the `finished()` method from inside the finite task class when the task is complete. The logic for checking if a task is done will usually come from a `withCheck` on a stream, where information such as distances to targets can be inspected to see if the task is done.

As soon as the `finished()` method is called, the `onEnd` method of the task will be called as well. So there is **no need** to reset component controllers in the piece of code that calls `finished()`, as the `onEnd` method will be called and all resetting logic can go there.

## Sequencing Finite Tasks
To run one finite task after another, use the `then` method on finite tasks, which takes a task to be run after the task that `then` was called on. With `then`, both continuous and finite tasks can be passed to run after the task. If a finite task is passed, the `then` method will return a new finite task that runs both tasks in order and then terminates. If a continuous task is passed, the `then` method will return a continuous task that runs the finite task first and then starts the continuous task with no specified end.

For example:
```scala
new DriveForward(5 feet).then(new TurnByAngle(10 degrees)) // finite task
new DriveForward(5 feet).then(new ContinuouslyShootBalls()) // continuous task
```

## Parallel Finite Tasks
Finite tasks can be run in parallel by using the `and` method, which returns a new finite task that runs both subtasks at the same time and only terminates when both subtasks have finished.

```scala
new DriveForward(5 feet).and(new LiftArm(2 feet)) // only finishes when we have driven forward and lifted the arm
```

## Combining with Continuous Tasks
Sometimes, finite tasks need to be combined with continuous tasks to have a continuous task run in parallel while a finite task is running. For example, you may want to display a different lighting pattern while the robot is driving to a new position. This behavior can be achieved with the `andUntilDone` method, which takes a continuous task to run in parallel while the finite task is running.

```scala
new DriveForward(5 feet).andUntilDone(new DisplayLightingColor("green"))
```

## Timeouts
Especially when running automated routines in a time-constrained environment (such as the FIRST Robotics Competition autonomous period), it is often necessary to place timeouts to force finite tasks to terminate after a set duration of running. This can be achieved through the `withTimeout` method, which takes a parameter of a duration to terminate the task after. Following the principle of immutability, this **does not** modify the existing task to have a timeout, but returns a new task with the timeout applied.

```scala
new DriveForward(5 feet).withTimeout(10 seconds) // if we get stuck, this will stop after 10 seconds
```

## `WaitTask`
Autonomous routines often contain pauses, which can be introduced into a series of actions by using the `WaitTask` class. `WaitTask` is a finite task that waits for a given duration of time before completing. `WaitTask`s can be used just the same as any other `FiniteTask`, and can be combined with other tasks to produce more complex routines.

```scala
new DriveForward(5 feet).then(new WaitTask(5 seconds)).then(new DriveForward(5 feet)) // drives, pauses, and then drives again
```
