package aia.routing.group

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.routing._

class AnotherWrongDynamicRouteeSizer(nrActors: Int, props: Props, router: ActorRef) extends Actor {
  var nrChildren = nrActors

  //restart children
  override def preStart(): Unit = {
    super.preStart()
    (0 until nrChildren).map(nr => createRoutee())
  }

  def createRoutee(): Unit = {
    val child = context.actorOf(props)
    val selection = context.actorSelection(child.path)

    router ! AddRoutee(ActorSelectionRoutee(selection))
    context.watch(child)

    println("Add routee " + child)
  }

  def receive = {

    case PreferredSize(size) => {
      val currentNumber = context.children.size
      if (size < currentNumber) {
        //remove
        println(s"Delete ${currentNumber - size} children")
        context.children.take(currentNumber - size).foreach(ref => {
          println("delete: " + ref)
          context.stop(ref)
        })
      } else {
        println(s"Adding ${size - currentNumber} children")
        (currentNumber until size).map(nr => createRoutee())
      }
      nrChildren = size
    }

    case routees: Routees => {
      println(s"routees [routees.getRoutees.size(): ${routees.getRoutees.size()}] [nrChildren: $nrChildren] " + routees)

      if (routees.getRoutees.size() < nrChildren) {
        (routees.getRoutees.size() until nrChildren).map(nr => createRoutee())
      }
    }

    // The logic here is incorrect. By removing the routee before restarting the child actor
    // the router could terminate, if the child happens to be the last one
    case Terminated(child) => {
      println("Terminated " + child)
      val selection = context.actorSelection(child.path)
      router ! RemoveRoutee(ActorSelectionRoutee(selection))
      println("About to send GetRoutees")
      router ! GetRoutees
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    println("restart %s".format(self.path.toString))
  }
}