package com.lynbrookrobotics.potassium.vision

import org.opencv.core.Mat
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import com.lynbrookrobotics.potassium.Signal

class CameraSignal(camera: VideoCapture, frameWidth: Int = 320, frameHeight: Int = 200) extends Signal[Mat] {
  camera.set(Videoio.CAP_PROP_FRAME_WIDTH, frameWidth)
  camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, frameHeight)

  val mat = new Mat()

  override def get() = {
    camera.read(mat)
    mat
  }
}
