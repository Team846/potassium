---
layout: docs
title: Continuous Events
section: core
---

# Continuous Events
Events that have a start, running, and end phase. For example, an autonomous
mode, holding down of a button, or when a vision target is in sight

## Creating Continuous Events
The main way to create a Continuous Event is to use the `eventWhen` method in 
a `Stream`. Every new value of the `Stream` instance triggers an update of the 
event, which checks if `condition` function of the state of the stream returns true, and marks 
the `ContinuousEvent` as running appropriately.
```scala
val buttonPressedStream: Stream[Boolean] = ...
val buttonPressedEvent: ContinuousEvent = buttonPressedStream.eventWhen(identity)

val robotSpeedStream: Stream[Velocity] =  ...
val robotTooFast: ContinuousEvent = robotSpeedStream.eventWhen(v > FeetPerSecond(20))
``` 

## Using Continuous Events
You can use Continuous Events in 3 different ways.
1. Run a callback for every update of the Continuous event when it is true
```scala

val robotSpeedStream: Stream[Velocity] =  ...
val robotTooFast: ContinuousEvent = robotSpeedStream.eventWhen(v > FeetPerSecond(20))

// Will print the message with every update of robotSpeedStream while the robot
// is going too fast
robotTooFast.foreach { () =>
  println("The robot is going to fast")
}
```

2. Run a `ContinuousTask` when the `ContinuousEvent` is running.
```scala
class BlinkAlarmLEDs extends ContinuousTask {
  override def onStart = ...
  override def onEnd = ...
}

robotTooFast.foreach(BlinkAlarmLEDs)
```

3. Run a Signal of `ContinuousTask`. 
For use when the `ContinuousTask` is variable. When the `ContinuousEvent` starts, it aborts the current Task, retrieves the current `ContinuousTask` from the Signal, and runs the retrieved Task.  
