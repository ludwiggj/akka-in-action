package aia.structure.router

import akka.actor.ActorRef

case class RouteSlipMessage(routeSlip: Seq[ActorRef], message: AnyRef)