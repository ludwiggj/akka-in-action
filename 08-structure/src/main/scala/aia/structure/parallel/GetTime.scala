package aia.structure.parallel

import akka.actor.{Actor, ActorRef}

class GetTime(pipe: ActorRef) extends Actor {
  def receive = {
    case msg: PhotoMessage => {
      pipe ! msg.copy(creationTime = ImageProcessing.getTime(msg.photo))
    }
  }
}
