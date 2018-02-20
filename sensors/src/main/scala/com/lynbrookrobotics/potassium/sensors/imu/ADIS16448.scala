package com.lynbrookrobotics.potassium.sensors.imu

import java.nio.ByteBuffer

import com.lynbrookrobotics.potassium.sensors.SPITrait
import com.lynbrookrobotics.potassium.units.Value3D
import squants.Time
import squants.motion.{AngularVelocity, DegreesPerSecond}
/**
  * An interface for communicating with the ADIS16448 IMU.
  */
class ADIS16448(spi: SPITrait, updatePeriod: Time) extends DigitalGyro(updatePeriod) {
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
  }

  // Private object used to store private variables
  private object ADIS16448Protocol {
    val X_GYRO_REG: Byte = 0x04
    val Y_GYRO_REG: Byte = 0x06
    val Z_GYRO_REG: Byte = 0x08
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
  // Saves the com.lynbrookrobotics.potassium.sensors.SPI being used (16448) to the various registers
  Registers.SMPL_PRD.write(1, spi) // 1 Means use default period
  Registers.MSC_CTRL.write(4, spi) // 4 is reset command
  Registers.SENS_AVG.write(Integer.parseInt("10000000000", 2), spi) // Creates an empty queue of size 1024 bits

  // Creates ByteBuffer of sie 2 for inputs and outputs
  private val outBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private val inBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private var firstRun = true

  /**
    * Returns data from register as short (16 bit integer)
    * @param register register- hex
    * @return short
    */
  private def readGyroRegister(register: Byte): Short = {
    outBuffer.put(0, register) // Request data from register
    outBuffer.put(1, 0.asInstanceOf[Byte]) // Second byte must be 0
    spi.write(outBuffer, 2) // Outputs 2 elements to spi

    inBuffer.clear()
    // inBuffer already defined so it does not need to be created
    // Reads 2 bytes and puts them in inBuffer
    spi.read(firstRun, inBuffer, 2)

    if(firstRun) firstRun = false

    inBuffer.getShort
  }

  /**
    * Gets the current gyro data from the IMU.
    * 2nd and 3rd parameters are null because accelerometer and magneto data not used.
    * @return IMUValue
    */
  def currentData: IMUValue = {
    val gyro: Value3D[AngularVelocity] = Value3D[AngularVelocity](
      DegreesPerSecond(readGyroRegister(ADIS16448Protocol.X_GYRO_REG)),
      DegreesPerSecond(readGyroRegister(ADIS16448Protocol.Y_GYRO_REG)),
      DegreesPerSecond(readGyroRegister(ADIS16448Protocol.Z_GYRO_REG))
    ).times(Constants.DegreePerSecondPerLSB)
    IMUValue(gyro, null, null)
  }

  /**
    * Retrieves 3-dimensional data from the gyro
    */
  override def retrieveVelocity: Value3D[AngularVelocity] = {
    currentData.gyro
  }
}
