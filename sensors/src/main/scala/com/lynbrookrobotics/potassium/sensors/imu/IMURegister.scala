package com.lynbrookrobotics.potassium.sensors.imu

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPI

/**
  * Represents a register on the ADIS16448 IMU.
  *
  * @param register is ID of register on IMU
  */
class IMURegister(register: Byte) {
  private val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)

  private val readMessage: ByteBuffer = ByteBuffer.allocateDirect(2)
  readMessage.put(Array(register, 0.toByte))

  /**
    * Reads a value from the register.
    * @param spi the interface to use for communication
    * @return a single value from the register
    */
  def read(implicit spi: SPI): Int = {
    spi.write(readMessage, 2)

//    May not be needed, needs testing
//    readBuffer.clear()
//    readBuffer.put(0.toByte)
//    readBuffer.put(0.toByte)

    spi.read(initiate = false, readBuffer, 2)

    readBuffer.getShort(0).toInt
  }

  private val lowRegister: Byte = (register | 0x80).toByte
  private val highRegister: Byte = (register | 0x81).toByte

  private val lowBuffer = ByteBuffer.allocateDirect(2)
  lowBuffer.put(0, lowRegister)

  private val highBuffer = ByteBuffer.allocateDirect(2)
  highBuffer.put(0, highRegister)

  /**
    * Writes a single value to the register.
    *
    * @param value the value to write
    * @param spi   the interface to use for communication
    */
  def write(value: Int)(implicit spi: SPI): Unit = {
    lowBuffer.put(1, value.toByte)
    spi.write(lowBuffer, 2)

    highBuffer.put((value >> 8).toByte)
    spi.write(highBuffer, 2)
  }
}
