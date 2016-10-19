package com.goticks

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import com.typesafe.config.ConfigFactory

object FrontendRemoteDeployMain extends App with Startup {
  val config = ConfigFactory.load("frontend-remote-deploy")

  implicit val system = ActorSystem("frontend", config)

  val api = new RestApi() {
    val log = Logging(system.eventStream, "frontend-remote")
    implicit val requestTimeout = configuredRequestTimeout(config)
    implicit def executionContext = system.dispatcher
    def createBoxOffice: ActorRef = {
      val ref = system.actorOf(BoxOffice.props, BoxOffice.name)
      log.info(s"!!! Created Box Office with ref [$ref] !!!")
      ref
    }
  }

  startup(api.routes)
}