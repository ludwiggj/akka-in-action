package aia.routing.pattern

import aia.routing.ImageProcessing
import akka.actor.{Actor, ActorRef}

class GetSpeed(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: PhotoMessage => {
      pipe ! msg.copy(speed = ImageProcessing.getSpeed(msg.photo))
    }
  }
}
