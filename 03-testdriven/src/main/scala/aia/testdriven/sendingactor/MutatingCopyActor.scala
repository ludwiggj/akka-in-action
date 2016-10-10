package aia.testdriven.sendingactor

import akka.actor.{Actor, ActorRef, Props}

class MutatingCopyActor(receiver: ActorRef) extends Actor {

  import MutatingCopyActor._

  def receive = {
    case SortEvents(unsorted) =>
      receiver ! SortedEvents(unsorted.sortBy(_.id))
  }
}

object MutatingCopyActor {
  def props(receiver: ActorRef) =
    Props(new MutatingCopyActor(receiver))

  case class Event(id: Long)

  case class SortEvents(unsorted: Vector[Event])

  case class SortedEvents(sorted: Vector[Event])

}