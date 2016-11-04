package aia.routing.pool

import aia.routing.Photo
import akka.routing._

import scala.collection.immutable

class SpeedRouterLogic(minSpeed: Int, normalFlowPath: String, cleanUpPath: String) extends RoutingLogic {

  def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee = {
    message match {
      case msg: Photo =>
        if (msg.speed > minSpeed)
          findRoutee(routees, normalFlowPath)
        else
          findRoutee(routees, cleanUpPath)
    }
  }

  def findRoutee(routees: immutable.IndexedSeq[Routee], path: String): Routee = {
    val routeeList = routees.flatMap {
      case routee: ActorRefRoutee => routees
      case SeveralRoutees(routeeSeq) => routeeSeq
    }

    val search = routeeList.find { case routee: ActorRefRoutee => routee.ref.path.toString().endsWith(path) }
    search.getOrElse(NoRoutee)
  }
}