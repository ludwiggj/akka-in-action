package aia.structure.parallel

import akka.actor.{Actor, ActorRef}

class RecipientList(recipientList: Seq[ActorRef]) extends Actor {
  def receive = {
    case msg: AnyRef => recipientList.foreach(_ ! msg)
  }
}