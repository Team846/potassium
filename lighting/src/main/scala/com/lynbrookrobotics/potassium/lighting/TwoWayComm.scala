package com.lynbrookrobotics.potassium.lighting

abstract class TwoWayComm {
  def isConnected: Boolean
  def newData(int: Int): Unit
  def connect(): Unit
  def pullLog(): String
}
