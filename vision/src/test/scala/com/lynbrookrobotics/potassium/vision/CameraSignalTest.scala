package com.lynbrookrobotics.potassium.vision

import org.scalatest.FunSuite
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture

class CameraSignalTest extends FunSuite with MockitoSugar {
  test("CameraSignal outputs Mat from OpenCV VideoCapture") {
    val mockedVideoCapture = mock[VideoCapture]
    val cameraSignal = new CameraSignal(mockedVideoCapture, 600, 600)

    when(mockedVideoCapture.read(cameraSignal.mat)).thenReturn(true)
    assert(cameraSignal.get == cameraSignal.mat)
  }
}
