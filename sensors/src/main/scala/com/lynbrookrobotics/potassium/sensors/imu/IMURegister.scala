package com.lynbrookrobotics.potassium.sensors.imu

import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPITrait

/**
 * Represents a register on the ADIS16448 IMU.
 *
 * @param register is ID of register on IMU
 */
class IMURegister(register: Byte) {
  private val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private val readMessage: ByteBuffer = ByteBuffer.allocateDirect(2)
  readMessage.put(0, register)
  readMessage.put(1, 0.toByte)
  private val writeMessage1: Byte = (register | 0x80).toByte
  private val writeMessage2: Byte = (register | 0x81).toByte

  /**
   * Reads a value from the register.
   * @param spi the interface to use for communication
   * @return a single value from the register
   */
  def read(spi: SPITrait): Int = {
    readBuffer.clear()
    readBuffer.put(0.toByte)
    readBuffer.put(0.toByte)
    spi.write(readMessage, 2)

    spi.read(false, readBuffer, 2)

    readBuffer.getShort(0).toInt
  }

  val valueWriter1 = ByteBuffer.allocateDirect(2)
  val valueWriter2 = ByteBuffer.allocateDirect(2)

  /**
   * Writes a single value to the register.
   *
   * @param value the value to write
   * @param spi   the interface to use for communication
   */
  def write(value: Int, spi: SPITrait): Unit = {
    valueWriter1.put(0, writeMessage1)
    valueWriter1.put(1, value.toByte)

    valueWriter2.put(0, writeMessage2)
    valueWriter2.put(1, (value >> 8).toByte)

    spi.write(valueWriter1, 2)
    spi.write(valueWriter2, 2)
  }
}
