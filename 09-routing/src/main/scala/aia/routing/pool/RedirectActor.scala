package aia.routing.pool

import akka.actor.{Actor, ActorRef}

class RedirectActor(pipe: ActorRef) extends Actor {
  println("RedirectActor instance created")

  def receive = {
    case msg: AnyRef => {
      pipe ! msg
    }
  }
}