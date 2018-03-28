package com.lynbrookrobotics.potassium.streams

import squants.time.Time

sealed trait ExpectedPeriodicity

case class Periodic(period: Time, rootStream: Stream[_]) extends ExpectedPeriodicity

case object NonPeriodic extends ExpectedPeriodicity
