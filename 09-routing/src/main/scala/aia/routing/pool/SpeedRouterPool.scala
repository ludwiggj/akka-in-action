package aia.routing.pool

import akka.actor.{ActorContext, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.dispatch.Dispatchers
import akka.routing._

import scala.collection.immutable

case class SpeedRouterPool(minSpeed: Int, normalFlow: Props, cleanUp: Props) extends Pool {

  def nrOfInstances(sys: ActorSystem): Int = 1

  def resizer: Option[Resizer] = None

  def supervisorStrategy: SupervisorStrategy = OneForOneStrategy()(SupervisorStrategy.defaultDecider)

  override def createRouter(system: ActorSystem): Router = {
    new Router(new SpeedRouterLogic(minSpeed, "normalFlow", "cleanup"))
  }

  override val routerDispatcher: String = Dispatchers.DefaultDispatcherId

  override def newRoutee(routeeProps: Props, context: ActorContext): Routee = {
    val normal = context.actorOf(normalFlow, "normalFlow")
    val clean = context.actorOf(cleanUp, "cleanup")

    SeveralRoutees(immutable.IndexedSeq[Routee](ActorRefRoutee(normal), ActorRefRoutee(clean)))
  }
}