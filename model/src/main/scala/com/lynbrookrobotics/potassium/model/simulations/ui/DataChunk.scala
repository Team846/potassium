package com.lynbrookrobotics.potassium.model.simulations.ui

import com.lynbrookrobotics.potassium.units.Point
import squants.{Angle, Time}

case class DataChunk(val time: Time, val point: Point, val angle: Angle)
