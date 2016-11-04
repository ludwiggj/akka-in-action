package aia.routing.group

import aia.routing.GetLicense
import akka.actor.{Actor, ActorRef, Kill, Props}

class GetLicenseCreator(nrActors: Int, nextStep: ActorRef) extends Actor {
  var createdActors = Seq[ActorRef]()

  override def preStart(): Unit = {
    super.preStart()
    createdActors = (0 until nrActors).map(nr => {
      context.actorOf(Props(new GetLicense(nextStep)), s"GetLicense$nr")
    })
  }

  def receive = {
    case "KillFirst" => {
      createdActors.headOption.foreach(_ ! Kill)
      createdActors = createdActors.tail
    }
    case _ => throw new IllegalArgumentException("not supported")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    println(s"restart ${self.path.toString}")
  }
}
