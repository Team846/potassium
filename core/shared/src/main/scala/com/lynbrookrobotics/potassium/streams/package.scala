package com.lynbrookrobotics.potassium

package object streams {
  trait Cancel {
    def cancel(): Unit
  }
}
