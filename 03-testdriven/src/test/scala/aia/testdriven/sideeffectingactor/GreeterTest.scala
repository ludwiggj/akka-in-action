package aia.testdriven.sideeffectingactor

import aia.testdriven.StopSystemAfterAll
import akka.actor.{ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, EventFilter, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike

import aia.testdriven.sideeffectingactor.GreeterTest._

class GreeterTest extends TestKit(testSystem)
  with WordSpecLike
  with StopSystemAfterAll {

  "The Greeter" must {
    "say Hello World! when a Greeting(\"World\") is sent to it" in {
      val dispatcherId = CallingThreadDispatcher.Id

      // Run test in a single-threaded environment
      val props = Props[Greeter].withDispatcher(dispatcherId)
      val greeter = system.actorOf(props)

      EventFilter.info(message = "Hello World!",
        occurrences = 1).intercept {
        greeter ! Greeting("World")
      }
    }
  }
}

object GreeterTest {
  val testSystem = {
    val config = ConfigFactory.parseString(
      """
         akka.loggers = [akka.testkit.TestEventListener]
      """)
    ActorSystem("testsystem", config)
  }
}