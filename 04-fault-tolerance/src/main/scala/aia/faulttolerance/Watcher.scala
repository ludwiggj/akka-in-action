package aia.faulttolerance

import akka.actor._
import akka.actor.Terminated

class Watcher(watched: ActorRef, listener: Option[ActorRef]) extends Actor with ActorLogging {
  context.watch(watched)

  def receive = {
    case Terminated(actorRef) =>
      log.warning("Actor {} terminated", actorRef)
      listener.foreach(_ ! s"Dead actor! $actorRef")
  }
}

object Watcher {
  def props(watched: ActorRef, listener: Option[ActorRef] = None) =
    Props(new Watcher(watched, listener))
}