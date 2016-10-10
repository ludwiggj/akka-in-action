package aia.testdriven.sideeffectingactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class SeniorGreeter(listener: Option[ActorRef]) extends Actor with ActorLogging {
  def receive = {
    case Greeting(who) =>
      val message = "Hello " + who + "!"
      log.info(message)
      listener.foreach(_ ! message)
  }
}

object SeniorGreeter {
  def props(listener: Option[ActorRef] = None) =
    Props(new SeniorGreeter(listener))
}