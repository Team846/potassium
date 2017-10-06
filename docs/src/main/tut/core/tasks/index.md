---
layout: docs
title: Tasks
section: core
---

# Tasks
Tasks have the role of orchestrating different components to perform specific actions by creating streams, attaching them to components, and watching for when the action is complete.

In Potassium, tasks are broken down into two types -- finite and continuous. Finite tasks are used for actions that have a clearly defined end, such as driving forward a distance or moving an arm to a position, and can be chained together to perform a series of actions. Continuous tasks do not have a defined end and represent actions such as spinning a flywheel at a given speed, and are stopped by an external force such as a button being released or a timeout.

One important difference between tasks in Potassium and similar concepts such as "commands" in WPILib is that Potassium tasks are **immutable**. This means that any operations to combine tasks **does not** affect the original tasks, which still behave exactly the same as if they had never been used in a task combination.
