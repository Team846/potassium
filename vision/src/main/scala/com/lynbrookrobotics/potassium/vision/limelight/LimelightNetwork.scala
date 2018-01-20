package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.networktables.{NetworkTable, NetworkTableInstance}
import squants.space.{Angle, Degrees}
import squants.time.Milliseconds

class LimelightNetwork(table: NetworkTable)(implicit clock: Clock) {
  val hasTarget: Stream[Double] = Stream.periodic(Milliseconds(5))(table.getEntry("tv").getDouble(0))
  val yOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(table.getEntry("tx").getDouble(0)))
  val xOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(table.getEntry("ty").getDouble(0)))
  val keys = table.getKeys()
}

object LimelightNetwork{
  def apply(implicit clock: Clock): LimelightNetwork = {
    new LimelightNetwork(NetworkTableInstance.getDefault.getTable("/limelight"))
  }
}
