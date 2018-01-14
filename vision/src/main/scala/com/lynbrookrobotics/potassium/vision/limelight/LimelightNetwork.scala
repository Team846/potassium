package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.networktables.{NetworkTable, NetworkTableInstance}
import squants.{Dimensionless, Percent}
import squants.space.{Angle, Degrees}
import squants.time.Milliseconds

class LimelightNetwork(table: NetworkTable)(implicit clock: Clock) {
  val percentArea: Stream[Dimensionless] = Stream.periodic(Milliseconds(5))(Percent(table.getEntry("ta").getDouble(0)))
  val yOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(table.getEntry("tx").getDouble(0)))
}

object LimelightNetwork {
  def apply(implicit clock: Clock): LimelightNetwork = {
    new LimelightNetwork(NetworkTableInstance.getDefault.getTable("/limelight"))
  }
}
