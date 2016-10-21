package aia.structure.router

import akka.actor.ActorRef

trait RouteSlip {
  def sendMessageToNextTask(routeSlip: Seq[ActorRef], message: AnyRef) = {
    val nextTask = routeSlip.head
    val newSlip = routeSlip.tail
    if (newSlip.isEmpty) {
      nextTask ! message
    } else {
      nextTask ! RouteSlipMessage(routeSlip = newSlip, message = message)
    }
  }
}