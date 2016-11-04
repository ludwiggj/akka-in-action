package aia.routing.pattern

import akka.actor.{Actor, ActorRef}

class SpeedRouter(minSpeed: Int, cleanUpFlow: ActorRef, getTimeFlow: ActorRef) extends Actor {

  def receive = {
    case msg: PhotoMessage => {
      if (msg.speed.getOrElse(0) > minSpeed)
        getTimeFlow ! msg
      else
        cleanUpFlow ! msg
    }
  }
}