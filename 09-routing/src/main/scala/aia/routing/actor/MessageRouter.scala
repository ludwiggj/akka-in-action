package aia.routing.actor

import aia.routing.Photo
import akka.actor._

class MessageRouter(minSpeed: Int, normalFlow: ActorRef, cleanUp: ActorRef) extends Actor {

  def receive = {
    case msg: Photo =>
      if (msg.speed > minSpeed)
        normalFlow ! msg
      else
        cleanUp ! msg
  }
}