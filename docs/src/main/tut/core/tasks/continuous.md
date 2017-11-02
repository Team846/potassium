---
layout: docs
title: Continuous Tasks
section: core
---

# Continuous Tasks
Continuous tasks are used to represent operations that can continue indefinitely but do not have a defined condition for when they should stop. Actions such as spinning a wheel at a given velocity or continuously shooting balls can be represented by a continuous event. The largest difference between continuous and finite tasks is that continuous tasks cannot be sequenced, since there is no definite point where a continuous task ends.

## Creating a Continuous Task
To create a continuous task, extend the `ContinuousTask` abstract class and implement the `onStart` and `onEnd` methods.

```scala
class DriveAtVelocity(velocity: Double) extends ContinuousTask {
  def onStart(): Unit = { ... }
  def onEnd(): Unit = { ... }
}
```

## Parallel Continuous Tasks
As continuous tasks have no defined end, there is no way to chain continuyous tasks to run one after the other. Instead, the only continuous task combination method is `and`, which allows two continuous tasks to be run in parallel.

```scala
val collectAndShoot: ContinuousTask = new CollectItems().and(new ShootIntoGoal())
```

## Conversion to Finite Tasks
Sometimes, a continuous task needs to be integrated into a finite task sequence by having the continuous task only run for a specified duration. To convert a continuous task to a finite task that runs the action for the specified duration, `ContinuousTask` has a method `forDuration(time)` that produces a new finite task that wraps the continuous task.

```scala
val fiveSecondShoot: FiniteTask = new ShootIntoGoal().forDuration(Seconds(5))
```
