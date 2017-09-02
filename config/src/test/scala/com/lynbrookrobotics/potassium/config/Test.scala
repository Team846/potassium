package com.lynbrookrobotics.potassium.config

import com.lynbrookrobotics.potassium.config.TwoWayFile
import java.io.{File, PrintWriter}
import java.nio.file.Files

import squants.motion.{MetersPerSecond, Velocity}
import SquantsPickling._
import org.scalatest.FunSuite
import upickle.default._


class Test extends FunSuite {
  case class RobotConfig(drive: DriveConfig)
  case class DriveConfig(maxForwardSpeed: Velocity)

  test("Test writing and reading randomly generated values to config") {
    val file = new File("test.txt")
    file.createNewFile()

    // initial value is required in config file
    val initConfig = write[RobotConfig](null)
    val writer = new PrintWriter(file)
    writer.append(initConfig)
    writer.flush()

    val configFromFile = new TwoWayFile(file).map(read[RobotConfig])(
      (_, newValue) => write[RobotConfig](newValue)
    )

    for (_ <- 1 to 100) {
      val newConfigValue = RobotConfig(DriveConfig(MetersPerSecond(math.random)))
      configFromFile.value_=(newConfigValue)
      assert(configFromFile.value == newConfigValue)
    }
  }
}