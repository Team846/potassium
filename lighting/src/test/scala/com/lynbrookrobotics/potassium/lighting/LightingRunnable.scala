package com.lynbrookrobotics.potassium.lighting

import java.util.Scanner

object LightingRunnable extends App{
  val scanner = new Scanner(System.in)
  val comms = new RXTXTwoWayComms
  comms.connect(comms.systemPort)
  while(true){
    comms.newData(scanner.nextInt())
    while(comms.hasLog){
      print(comms.pullLog)
    }
  }
}
