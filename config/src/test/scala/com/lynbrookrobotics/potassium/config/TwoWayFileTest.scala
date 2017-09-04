package com.lynbrookrobotics.potassium.config

import java.io.{File, PrintWriter}
import java.nio.file.Files

import squants.motion.{MetersPerSecond, Velocity}
import org.scalatest.FunSuite
import SquantsPickling._
import upickle.default._

class TwoWayFileTest extends FunSuite {
  case class RobotConfig(drive: DriveConfig)
  case class DriveConfig(maxForwardSpeed: Velocity)

  test("Test writing and reading randomly generated values to config") {
    val file = new File("test.txt")
    if (!file.exists()) {
      Files.createFile(file.toPath)
    }

    // initial value is required in config file
    val initConfig = write[RobotConfig](null)
    val writer = new PrintWriter(file)
    writer.append(initConfig)
    writer.close()

    val twoWay = new TwoWayFile(file)
    val configFromFile = twoWay.map(read[RobotConfig])(
      (_, newValue) => write[RobotConfig](newValue)
    )

    for (_ <- 1 to 100) {
      val newConfigValue = RobotConfig(DriveConfig(MetersPerSecond(math.random)))
      configFromFile.value_=(newConfigValue)
      assert(configFromFile.value == newConfigValue)
    }

    twoWay.close()
    file.delete()
  }
}