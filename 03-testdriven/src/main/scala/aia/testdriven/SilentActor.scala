package aia.testdriven

import aia.testdriven.SilentActor.{GetState, SilentMessage}
import akka.actor.{Actor, ActorRef}

class SilentActor extends Actor {
  var internalState = Vector[String]()

  def receive = {
    case SilentMessage(data) =>
      internalState = internalState :+ data

    case GetState(receiver) =>
      receiver ! internalState
  }

  def state = internalState
}

object SilentActor {

  case class SilentMessage(data: String)

  case class GetState(receiver: ActorRef)

}