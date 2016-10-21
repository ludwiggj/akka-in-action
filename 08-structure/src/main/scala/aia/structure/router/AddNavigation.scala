package aia.structure.router

import akka.actor.Actor

class AddNavigation() extends Actor with RouteSlip {
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip, car.copy(hasNavigation = true))
    }
  }
}