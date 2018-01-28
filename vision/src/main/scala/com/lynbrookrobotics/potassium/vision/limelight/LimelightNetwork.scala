package com.lynbrookrobotics.potassium.vision.limelight

import com.lynbrookrobotics.potassium.clock.Clock
import com.lynbrookrobotics.potassium.streams._
import edu.wpi.first.networktables.{NetworkTable, NetworkTableInstance}
import squants.{Dimensionless, Percent}
import squants.space.{Angle, Degrees}
import squants.time.Milliseconds

class LimelightNetwork(val table: NetworkTable)(implicit clock: Clock) {
  private val percentEntry = table.getEntry("ta")
  val percentArea: Stream[Option[Dimensionless]] = Stream.periodic(Milliseconds(5)) {
    val value = percentEntry.getDouble(0)
    if (value == 0) {
      None
    } else {
      Some(Percent(value))
    }
  }

  private val txEntry = table.getEntry("tx")
  val xOffsetAngle: Stream[Angle] = Stream.periodic(Milliseconds(5))(Degrees(txEntry.getDouble(0)))
}

object LimelightNetwork{
  def apply(implicit clock: Clock): LimelightNetwork = {
    new LimelightNetwork(NetworkTableInstance.getDefault.getTable("/limelight"))
  }
}
