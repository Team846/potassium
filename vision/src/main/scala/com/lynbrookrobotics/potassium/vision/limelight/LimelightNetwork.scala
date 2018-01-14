package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.networktables.{NetworkTable, NetworkTableInstance}
import squants.{Dimensionless, Percent}
import squants.space.{Angle, Degrees}
import squants.time.Milliseconds

object LimelightNetwork {

  val table: NetworkTableInstance = (new NetworkTable).getInstance()
  val yOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(table.getEntry("tx").getDouble(0)))
  val xOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(table.getEntry("ty").getDouble(0)))
  val targetArea: Stream[Dimensionless] = Stream.periodic(Milliseconds(5))(Percent(table.getEntry("ta").getDouble(0)))
  val targetSkew: Stream[Dimensionless] = Stream.periodic(Milliseconds(5))(Percent(table.getEntry("ts").getDouble(0)))
}
