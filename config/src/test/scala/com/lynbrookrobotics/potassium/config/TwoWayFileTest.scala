package com.lynbrookrobotics.potassium.config

import java.io.{File, PrintWriter}
import java.nio.file.Files

import squants.motion.{FeetPerSecond, MetersPerSecond, Velocity}
import org.scalatest.FunSuite
import SquantsPickling._
import argonaut.Argonaut._
import argonaut._
import ArgonautShapeless._

case class RobotConfig(drive: DriveConfig)
case class DriveConfig(maxForwardSpeed: Velocity)

class TwoWayFileTest extends FunSuite {
  test("Test writing and reading randomly generated values to config") {
    quantityReader[Velocity]
    val file = new File("test.txt")
    if (!file.exists()) {
      Files.createFile(file.toPath)
    }

    // initial value is required in config file
    val initConfig = RobotConfig(DriveConfig(FeetPerSecond(1))).jencode.toString()
    val writer = new PrintWriter(file)
    writer.append(initConfig)
    writer.close()

    val twoWay = new TwoWayFile(file)
    val configFromFile = twoWay.map(_.decodeOption[RobotConfig].get)(
      (_, newValue) => newValue.jencode.toString()
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