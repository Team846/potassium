package com.lynbrookrobotics.potassium.config

import java.io.File

import upickle.default._

object TwoWayFileJSON {
  def apply[T](file: File)(implicit writer: Writer[T], reader: Reader[T]): TwoWaySignal[T] = {
    new TwoWayFile(file).map[T](string => read[T](string))(
      (_, newValue) => write(newValue)
    )
  }
}
