package com.lynbrookrobotics.potassium.remote

import com.lynbrookrobotics.potassium.Signal

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

sealed trait RemoteSignalMessage
case object Subscribe extends RemoteSignalMessage
case class Result[T] (data: T) extends RemoteSignalMessage

case class RemoteHost(actorSystemName: String, host: String)

class RemoteSignalProviderActor[T] (source: Signal[T]) extends Actor {
  case object SendLatestValue

  def receive = {
    case Subscribe =>
      context.become(sendingMessages(Set(sender())))
      self ! SendLatestValue
  }

  def sendingMessages(recipients: Set[ActorRef]): PartialFunction[Any, Unit] = {
    case SendLatestValue =>
      val value = source.get
      recipients.foreach(_ ! Result[T](value))
      self ! SendLatestValue
    case Subscribe =>
      context.become(sendingMessages(recipients + sender()))
  }
}

class RemoteSignalProvider[T] (name: String, source: Signal[T])(implicit actorSystem: ActorSystem) {
  val actor = actorSystem.actorOf(Props(new RemoteSignalProviderActor[T](source)), name)
  println(actor.path)
}

class RemoteSignalActor[T] (update: T => Unit, hostRef: ActorRef) extends Actor {
  def receive = {
    case Result(data: T) =>
      update(data)
  }

  hostRef ! Subscribe
  Unit
}

class RemoteSignal[T] (name: String, hostRef: ActorRef)(implicit actorSystem: ActorSystem) extends Signal[T] {
  private var signalValue: Option[T] = None
  override def get(): T = signalValue.get
  def getOptional(): Option[T] = signalValue

  val actor = actorSystem.actorOf(Props(new RemoteSignalActor(updateSignal, hostRef)), name)

  def updateSignal(newValue: T) = {
    signalValue = Some(newValue)
  }
}
