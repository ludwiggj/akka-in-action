package aia.routing.group

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import akka.routing._

class DynamicRouteeSizer(nrActors: Int, props: Props, router: ActorRef) extends Actor {
  var nrChildren = nrActors
  var childInstanceNr = 0

  // Create number of requested routees
  override def preStart(): Unit = {
    super.preStart()
    (0 until nrChildren).map(nr => createRoutee())
  }

  def createRoutee(): Unit = {
    childInstanceNr += 1
    val child = context.actorOf(props, "routee" + childInstanceNr)
    val selection = context.actorSelection(child.path)

    // After creating a new child, add it to the router using the ActorSelectionRoutee
    router ! AddRoutee(ActorSelectionRoutee(selection))
    context.watch(child)
  }

  def actorPathWithoutAddress(ref: ActorRef): String = ref.path.toStringWithoutAddress

  def receive = {

    // Changes number of routees
    case PreferredSize(size) => {
      if (size < nrChildren) {
        // Too many routees; remove the excess Routees from the router
        context.children.take(nrChildren - size).foreach(ref => {
          val selection = context.actorSelection(ref.path)
          router ! RemoveRoutee(ActorSelectionRoutee(selection))
        })
        router ! GetRoutees
      } else {
        // Creates new routees (the easy case)
        (nrChildren until size).map(nr => createRoutee())
      }
      nrChildren = size
    }

    case routees: Routees => {
      // Check if we can terminate children or need to re-create them after a termination

      // Translates routee list into a list of actor paths
      import collection.JavaConversions._
      val active: List[String] = routees.getRoutees.map {
        case x: ActorSelectionRoutee => x.selection.pathString

        // Less likely to be an ActorRefRoutee, but just in case...
        case x: ActorRefRoutee => x.ref.path.toString
      }.toList

      println(s"Routees [children: ${context.children.map(actorPathWithoutAddress _)}]")
      println(s"Routees [active: $active]")

      val (activeChildren, inactiveChildren) = context.children.partition(child => {
        active.indexOf(actorPathWithoutAddress(child)) >= 0
      })

      println(s"Routees [children, of whom active: $activeChildren]")
      println(s"Routees KILL [children, of whom inactive: $inactiveChildren]")

      for (inactiveChild <- inactiveChildren) {
        println(s"Killing child $inactiveChild")

        // Use PoisonPill so that any outstanding messages are processed
        inactiveChild ! PoisonPill
      }

      // Identify active routees that have been terminated: need to restart them
      val activeButTerminated = active.filterNot(activeChildren.map(actorPathWithoutAddress _).toSet)

      println(s"Routees RESTART [active, but not an active child: $activeButTerminated]")

      for (terminated <- activeButTerminated) {
        val name = terminated.substring(terminated.lastIndexOf("/") + 1)
        println(s"Restarting $name")
        val child = context.actorOf(props, name)
        context.watch(child)
      }
    }

    case Terminated(child) => {
      router ! GetRoutees
    }
  }
}