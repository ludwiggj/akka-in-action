package aia.structure.router

import akka.actor.Actor

class PaintCar(colour: String) extends Actor with RouteSlip {
  def receive = {
    case RouteSlipMessage(routeSlip, car: Car) => {
      sendMessageToNextTask(routeSlip, car.copy(color = colour))
    }
  }
}