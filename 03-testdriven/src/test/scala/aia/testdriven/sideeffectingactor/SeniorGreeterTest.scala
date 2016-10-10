package aia.testdriven.sideeffectingactor

import aia.testdriven.StopSystemAfterAll
import akka.actor._
import akka.testkit.TestKit
import org.scalatest.WordSpecLike

class SeniorGreeterTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with StopSystemAfterAll {

  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val props = SeniorGreeter.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter02-1")
      greeter ! Greeting("World")
      expectMsg("Hello World!")
    }

    "say something else and see what happens" in {
      val props = SeniorGreeter.props(Some(testActor))
      val greeter = system.actorOf(props, "greeter02-2")

      system.eventStream.subscribe(testActor, classOf[UnhandledMessage])

      greeter ! "World"

      expectMsg(UnhandledMessage("World", system.deadLetters, greeter))
    }
  }
}