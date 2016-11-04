package aia.routing.group

import akka.actor._
import akka.routing._

class WrongDynamicRouteeSizer(nrActors: Int, props: Props, router: ActorRef) extends Actor {
  var nrChildren = nrActors

  // restart children
  override def preStart(): Unit = {
    super.preStart()
    (0 until nrChildren).map(nr => createRoutee())
  }

  def createRoutee(): Unit = {
    val child = context.actorOf(props)

    // Book text:

    // Add a routee as an ActorRefRoutee. The router will create a watch on the new routee.
    // When the router receives a Terminated message and it isn’t the supervisor of the Routee,
    // it will throw an akka.actor.DeathPactException, which will terminate the router.

    // But not that this has now been fixed, see PingActor
    router ! AddRoutee(ActorRefRoutee(child))

    println("Add routee " + child)
  }

  def receive = {
    case PreferredSize(size) => {
      if (size < nrChildren) {
        // remove
        println("Delete %d children".format(nrChildren - size))
        context.children.take(nrChildren - size).foreach(ref => {
          println("delete: "+ ref)
          router ! RemoveRoutee(ActorRefRoutee(ref))
        })
        router ! GetRoutees
      } else {
        println(s"Adding ${size - nrChildren} children")
        (nrChildren until size).map(nr => createRoutee())
      }
      nrChildren = size
    }

    case routees: Routees => {
      import collection.JavaConversions._

      val active = routees.getRoutees.map {
        case x: ActorRefRoutee => x.ref.path.toString
        case x: ActorSelectionRoutee => x.selection.pathString
      }

      println("Active: "+ active)

      val notUsed = context.children.filterNot(routee => active.contains(routee.path.toString))

      println("Not used: "+ notUsed)

      notUsed.foreach(context.stop(_))
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    println("restart %s".format(self.path.toString))
  }
}