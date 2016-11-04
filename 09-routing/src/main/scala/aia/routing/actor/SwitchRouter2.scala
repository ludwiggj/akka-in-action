package aia.routing.actor

import akka.actor._

class SwitchRouter2(normalFlow: ActorRef, cleanUp: ActorRef) extends Actor with ActorLogging {

  def on: Receive = {
    case RouteStateOn => log.warning("Received on while already in on state")
    case RouteStateOff => context.unbecome()
    case msg: AnyRef => normalFlow ! msg
  }

  def off: Receive = {
    case RouteStateOn => context.become(on)
    case RouteStateOff => log.warning("Received off while already in off state")
    case msg: AnyRef => cleanUp ! msg
  }

  def receive = {
    case msg: AnyRef => off(msg)
  }
}