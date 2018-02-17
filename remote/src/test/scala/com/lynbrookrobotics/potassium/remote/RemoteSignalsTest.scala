package com.lynbrookrobotics.potassium.remote

import com.lynbrookrobotics.potassium.Signal
import com.lynbrookrobotics.potassium.remote.RemoteSignal
import com.lynbrookrobotics.potassium.remote.RemoteSignalProvider
import com.typesafe.config._
import org.scalatest.FunSuite
import scala.concurrent.duration._
import akka.actor._
import akka.remote._
import scala.concurrent._

class RemoteSignalsTest extends FunSuite {
  test("RemoteSignal successfully polls from a RemoteSignalProvider") {
    val number = scala.util.Random.nextDouble()

    class SampleSignal extends Signal[Double] {
      override def get(): Double = number
    }

    implicit val actorSystem = ActorSystem("system", ConfigFactory.load("client-test.conf"))

    val signalOnHost = new SampleSignal()
    val provider = new RemoteSignalProvider[Double]("sample-signal-host", signalOnHost)

    val clientSignal = new RemoteSignal[Double]("sample-signal-client", provider.actor)

    Thread.sleep(1000)

    assert(clientSignal.get == number)

    Await.result(actorSystem.terminate(), Duration.Inf)
  }

  test("RemoteSignal successfully polls from a RemoteSignalProvider over the network") {
    val number = scala.util.Random.nextDouble()

    class SampleSignal extends Signal[Double] {
      override def get(): Double = number
    }

    val hostActorSystem = ActorSystem("host", ConfigFactory.load("host-test.conf"))
    val signalOnHost = new SampleSignal()
    val provider = new RemoteSignalProvider[Double]("sample-signal", signalOnHost)(hostActorSystem)

    val clientActorSystem = ActorSystem("client", ConfigFactory.load("client-test.conf"))
    val hostActorSelection = clientActorSystem.actorSelection("akka.udp://host@127.0.0.1:10846/user/sample-signal")
    val selectionFuture = hostActorSelection.resolveOne(5 second)
    val hostActorRef: ActorRef = Await.result(selectionFuture, 5 second)

    val clientSignal = new RemoteSignal[Double]("sample-signal", hostActorRef)(clientActorSystem)

    Thread.sleep(1000)

    assert(clientSignal.get == number)

    clientSignal.actor ! PoisonPill
    hostActorRef ! PoisonPill

    Await.result(hostActorSystem.terminate(), Duration.Inf)
    Await.result(clientActorSystem.terminate(), Duration.Inf)
  }
}
