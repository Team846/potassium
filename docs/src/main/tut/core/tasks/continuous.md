---
layout: docs
title: Continuous Tasks
section: core
---

## Continuous Tasks
Continuous tasks are used to represent operations that can continue indefinitely but do not have a defined condition for when they should stop. Actions such as spinning a wheel at a given velocity or continuously shooting balls can be represented by a continuous event. The largest difference between continuous and finite tasks is that continuous tasks cannot be sequenced, since there is no definite point where a continuous task ends.

## Creating a Continuous Task
To create a continuous task, extend the `ContinuousTask` abstract class and implement the `onStart` and `onEnd` methods.

```scala
class DriveAtVelocity(velocity: Double) extends ContinuousTask {
  def onStart(): Unit = { ... }
  def onEnd(): Unit = { ... }
}
```
