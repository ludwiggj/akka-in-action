package aia.routing.group

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.routing.{ActorRefRoutee, AddRoutee, RemoveRoutee, RoundRobinGroup}

class PingActor extends Actor with ActorLogging {
  def receive = {
    case msg: String => log.info(msg)
  }

  def onStop() = ApplicationMain.router ! RemoveRoutee(ActorRefRoutee(self))
}

// Note, as of this version of Akka, issue 17040 is fixed
// See https://github.com/akka/akka/issues/17040
object ApplicationMain extends App {
  val props = Props[PingActor]
  val system = ActorSystem("MyActorSystem")

  val actor1 = system.actorOf(props, "routee1")
  val actor2 = system.actorOf(props, "routee2")

  val router = system.actorOf(RoundRobinGroup(List("/user/routee1")).props(), "router")

  router ! AddRoutee(ActorRefRoutee(actor2))
  Thread.sleep(500)

  router ! "ping"

  system.stop(actor2)
  Thread.sleep(500)

  router ! "test"

  Thread.sleep(500)
  System.exit(0)
}