package aia.routing.group

import aia.routing.GetLicense
import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}

class WatchfulGetLicenseCreator(nrActors: Int, nextStep: ActorRef) extends Actor {
  var currentNrActors = nrActors

  //restart children
  override def preStart(): Unit = {
    super.preStart()

    // No longer using a private sequence to store the actors,
    // each one is being watched separately (and stored in context.children)
    (0 until nrActors).map(nr => {
      val child = context.actorOf(Props(new GetLicense(nextStep)), s"GetLicense$nr")
      context.watch(child)
    })
  }

  def receive = {

    case "KillFirst" => {
      if (!context.children.isEmpty) {
        currentNrActors = currentNrActors - 1
        println(s">>> ${context.children.head} about to die!")
        context.children.head ! PoisonPill
      }
    }

    case "ZeroActorCount" => {
      currentNrActors = 0
    }

    // Handled a terminated child
    case Terminated(child) => {
      println(s">>> ${child.path.name} about to live!")
      val newChild = context.actorOf(Props(new GetLicense(nextStep)), child.path.name)
      context.watch(newChild)

      currentNrActors = currentNrActors + 1

      if (currentNrActors == nrActors) nextStep ! "Ready"
    }
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    println(s"restart ${self.path.toString}")
  }
}