package aia.logging

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingReceive

// Following is more verbose version of adding logging
// import akka.event.Logging
// class MyActor extends Actor {
//   val log = Logging(context.system, this)

class MyActor extends Actor with ActorLogging {

  def receive: Receive = LoggingReceive {
    case msg: String =>
      log.info("1: Received message %s from %s".format(msg, sender()))
      log.info("2: Received message {} from {}", msg, sender())
  }
}

// This has to be commented out for the "sbt stage" to successfully generate the start script
object MyActor extends App {
  val system = ActorSystem("hello")
  val actor = system.actorOf(Props[MyActor])

  actor ! "Dolly"
  system.terminate()
}