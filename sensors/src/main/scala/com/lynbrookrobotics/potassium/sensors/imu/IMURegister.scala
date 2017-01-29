package com.lynbrookrobotics.potassium.sensors.imu

>>>>>>> bc8452c... Uses WPILIB with SPI trait
import java.nio.ByteBuffer
import com.lynbrookrobotics.potassium.sensors.SPITrait

/**
  * Represents a register on the ADIS16448 IMU.
  *
  * @param register is ID of register on IMU
  */
class IMURegister(register: Int) {
  private val readBuffer: ByteBuffer = ByteBuffer.allocateDirect(2)
  private val readMessage: Array[Byte] = Array((register & 0x7f).toByte, 0.toByte)
  private val writeMessage1: Byte = (register | 0x80).toByte
  private val writeMessage2: Byte = (register | 0x81).toByte

  /**
    * Reads a value from the register.
    * @param spi the interface to use for communication
    * @return a single value from the register
    */
  def read(spi: SPITrait): Int = {
    readBuffer.clear()
    spi.write(ByteBuffer.wrap(readMessage), 2)
    spi.read(false, readBuffer, 2)

    readBuffer.getShort(0).asInstanceOf[Int] & 0xffff
  }

  /**
    * Writes a single value to the register.
    *
    * @param value the value to write
    * @param spi   the interface to use for communication
    */
  def write(value: Int, spi: SPITrait): Unit = {
    val valueWriter1: Array[Byte] = Array(writeMessage1, value.asInstanceOf[Byte])
    val valueWriter2: Array[Byte] = Array(writeMessage2, (value >> 8).asInstanceOf[Byte])
    spi.write(ByteBuffer.wrap(valueWriter1), 2)
    spi.write(ByteBuffer.wrap(valueWriter2), 2)
  }
}
