package com.lynbrookrobotics.potassium.vision.limelight

import squants.{Angle, Length}

case class CameraProperties(cameraHorizontalOffset: Angle, cameraVerticalOffset: Angle,
                            cameraHeight: Length, targetHeight: Length)
