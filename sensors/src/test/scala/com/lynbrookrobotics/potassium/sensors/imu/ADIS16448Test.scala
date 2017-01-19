package com.lynbrookrobotics.potassium.sensors.imu

import java.nio.ByteBuffer

import com.lynbrookrobotics.potassium.sensors.SPI
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import squants.motion.DegreesPerSecond
import squants.time.Milliseconds

class ADIS16448Test extends FunSuite with BeforeAndAfterEach with MockitoSugar {
  private val spi = mock[SPI]

  when(spi.read(same(false), any[ByteBuffer], same(2))).thenAnswer((inv) => {
    val byteBuffer = inv.getArguments.apply(1).asInstanceOf[ByteBuffer]
    byteBuffer.putShort(0, 16448)
  })

  val aDIS16448: ADIS16448 = new ADIS16448(spi, Milliseconds(5))

  def configureSPIResponse(out: Short): Unit = {
    when(spi.read(same(false), any[ByteBuffer], same(2))).thenAnswer((inv) => {
      val byteBuffer = inv.getArguments.apply(1).asInstanceOf[ByteBuffer]
      byteBuffer.putShort(0, out)
    })
  }

  test("Gyro data parsing and conversion produces correct velocities") {
    when(spi.write(any[ByteBuffer], any[Int])).thenAnswer((inv) => {
      val byteBuffer = inv.getArguments.head.asInstanceOf[ByteBuffer]
      byteBuffer.get(0) match {
        case 0x04 => // X_GYRO
          configureSPIResponse(50)
        case 0x06 => // Y_GYRO
          configureSPIResponse(75)
        case 0x08 =>
          configureSPIResponse(100)
      }
    })

    val velocity = aDIS16448.retrieveVelocity

    assert(velocity.x == DegreesPerSecond(2) &&
      velocity.y == DegreesPerSecond(3) &&
      velocity.z == DegreesPerSecond(4))
  }
}
