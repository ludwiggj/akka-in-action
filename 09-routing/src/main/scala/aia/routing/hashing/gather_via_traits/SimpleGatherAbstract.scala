package aia.routing.hashing.gather_via_traits

import aia.routing.hashing.messages.{GatherMessage, GatherMessageNormalImpl}
import akka.actor.{Actor, ActorRef}

abstract class SimpleGatherAbstract(nextStep: ActorRef) extends Actor {
  var messages = Map[String, GatherMessage]()

  def receive = {
    case msg: GatherMessage => {
      messages.get(messageKey(msg)) match {
        case Some(previous) => {
          //join
          val msgKey = messageKey(msg)

          nextStep ! GatherMessageNormalImpl(msgKey, previous.values ++ msg.values)
          println(s"Joined: $msgKey by ${self.path.name}")
          messages -= msgKey
        }

        case None => messages += messageKey(msg) -> msg
      }
    }
  }

  def messageKey(msg: GatherMessage): String
}