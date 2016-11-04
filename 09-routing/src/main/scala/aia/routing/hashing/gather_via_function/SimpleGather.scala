package aia.routing.hashing.gather_via_function

import aia.routing.hashing.messages.{GatherMessage, GatherMessageNormalImpl}
import akka.actor.{Actor, ActorRef}

class SimpleGather(nextStep: ActorRef)(messageKey: GatherMessage => String) extends Actor {
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
}