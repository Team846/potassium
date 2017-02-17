package com.lynbrookrobotics.potassium.sensors.imu

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPI
import squants.Time
import squants.motion.{AngularVelocity, DegreesPerSecond}

/**
  * An interface for communicating with the ADIS16448 IMU.
  */
class ADIS16448(spi: SPI, updatePeriod: Time) extends DigitalGyro(updatePeriod) {
  implicit private val spiInterface = spi

  // List of register addresses on the IMU
  private object Registers {
    // Sample period
    val SMPL_PRD: IMURegister = new IMURegister(0x36)
    // Sensor data
    val SENS_AVG: IMURegister = new IMURegister(0x38)
    // Misc. Controll (resetting)
    val MSC_CTRL: IMURegister = new IMURegister(0x34)
    // Product ID
    val PROD_ID: IMURegister = new IMURegister(0x56)

    val X_GYRO_REG = new IMURegister(0x04)
    val Y_GYRO_REG = new IMURegister(0x06)
    val Z_GYRO_REG = new IMURegister(0x08)
  }

  // Private object used to substitute for private values in Java
  // Contains gyro- specific constants, should be unchanged for new robot
  // LSB: Least Significant Bits (smallest)
  private object Constants {
    // Specifies imu sensitivity to raw sensor data
    val DegreePerSecondPerLSB: Double = 1.0 / 25.0
    val GPerLSB: Double = 1.0 / 1200.0
    // Gauss: unit of magnetic flux
    val MilligaussPerLSB: Double = 1.0 / 7.0
  }

  spi.setClockRate(3000000)
  spi.setMSBFirst()
  spi.setSampleDataOnFalling()
  spi.setClockActiveLow()
  spi.setChipSelectActiveLow()

  // Checks whether or not the IMU connected is the ADIS16448
  if (Registers.PROD_ID.read(spi) != 16448) {
    throw new IllegalStateException("The device in the MXP port is not an ADIS16448 IMU")
  }

  Registers.SMPL_PRD.write(1) // use internal sampling clock (819.2 sps)
  Registers.MSC_CTRL.write(4) // 4 is reset command
  Registers.SENS_AVG.write(1024) // Creates an empty queue of size 1024 bits

  // Creates ByteBuffer of sie 2 for inputs and outputs
  private val outBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private val inBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private var firstRun = true

  /**
    * Gets the current gyro data from the IMU.
    * 2nd and 3rd parameters are null because accelerometer and magneto data not used.
    * @return IMUValue
    */
  def currentData: IMUValue = {
    val gyro = Value3D(
      DegreesPerSecond(Registers.X_GYRO_REG.read),
      DegreesPerSecond(Registers.Y_GYRO_REG.read),
      DegreesPerSecond(Registers.Z_GYRO_REG.read)
    ) * Constants.DegreePerSecondPerLSB

    IMUValue(gyro, null, null)
  }

  /**
    * Retrieves 3-dimensional data from the gyro
    */
  override def retrieveVelocity: Value3D[AngularVelocity] = {
    currentData.gyro
  }
}
