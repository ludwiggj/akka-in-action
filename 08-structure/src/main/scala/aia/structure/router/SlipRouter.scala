package aia.structure.router

import akka.actor.{Actor, ActorRef, Props}

import scala.collection.mutable.ListBuffer

class SlipRouter(endStep: ActorRef) extends Actor with RouteSlip {

  // The actors we might need
  val paintBlack = context.actorOf(Props(new PaintCar("black")), "paintBlack")

  val paintGray = context.actorOf(Props(new PaintCar("gray")), "paintGray")

  val addNavigation = context.actorOf(Props[AddNavigation], "navigation")

  val addParkingSensor = context.actorOf(Props[AddParkingSensors], "parkingSensors")

  def receive = {
    case order: Order => {
      // Create the routing slip with the correct list of actors
      val routeSlip = createRouteSlip(order.options)

      // start the routing process
      sendMessageToNextTask(routeSlip, new Car)
    }
  }

  private def createRouteSlip(options: Seq[CarOptions.Value]): Seq[ActorRef] = {
    val routeSlip = new ListBuffer[ActorRef]

    // car needs a color
    if (!options.contains(CarOptions.CAR_COLOR_GRAY)) {
      routeSlip += paintBlack
    }

    options.foreach {
      // This order dictates the order of the actors
      case CarOptions.CAR_COLOR_GRAY => routeSlip += paintGray
      case CarOptions.NAVIGATION => routeSlip += addNavigation
      case CarOptions.PARKING_SENSORS => routeSlip += addParkingSensor
      case other => //do nothing
    }

    routeSlip += endStep

    routeSlip
  }
}