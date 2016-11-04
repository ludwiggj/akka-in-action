package aia.routing.pattern

import aia.routing.ImageProcessing
import akka.actor.{Actor, ActorRef}

class SpeedCamera(minSpeed: Int, cleanUpFlow: ActorRef, getTimeFlow: ActorRef) extends Actor {

  def receive = {
    case msg: PhotoMessage => {
      val speed = ImageProcessing.getSpeed(msg.photo)
      val updatedMsg = msg.copy(speed = speed)

      if (ImageProcessing.getSpeed(msg.photo).getOrElse(0) > minSpeed)
        getTimeFlow ! updatedMsg
      else
        cleanUpFlow ! updatedMsg
    }
  }
}