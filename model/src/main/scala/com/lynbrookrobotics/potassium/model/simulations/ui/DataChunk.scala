package com.lynbrookrobotics.potassium.model.simulations.ui

import com.lynbrookrobotics.potassium.units.Point
import squants.{Angle, Time}

case class DataChunk(time: Time, point: Point, angle: Angle)
