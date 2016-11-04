package aia.routing.actor

import akka.actor._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest._

import scala.concurrent.duration._

import aia.routing.actor.SwitchRouterTests._

class SwitchRouterTests extends TestKit(testSystem) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Router" must {
    "routes depending on state" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val router = system.actorOf(Props(new SwitchRouter(normalFlow = normalFlowProbe.ref, cleanUp = cleanupProbe.ref)))

      val msg = "message"
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      router ! RouteStateOn

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff

      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

    }

    "routes2 depending on state" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val router = system.actorOf(Props(new SwitchRouter2(normalFlow = normalFlowProbe.ref, cleanUp = cleanupProbe.ref)))

      val msg = "message"
      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)

      router ! RouteStateOn

      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      router ! RouteStateOff

      router ! msg

      cleanupProbe.expectMsg(msg)
      normalFlowProbe.expectNoMsg(1 second)
    }

    "log wrong statechange requests" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val router = system.actorOf(Props(new SwitchRouter(normalFlow = normalFlowProbe.ref, cleanUp = cleanupProbe.ref)))

      EventFilter.warning(message = "Received off while already in off state",
        occurrences = 1).intercept {
        router ! RouteStateOff
      }

      router ! RouteStateOn

      EventFilter.warning(message = "Received on while already in on state",
        occurrences = 1).intercept {
        router ! RouteStateOn
      }
    }
  }
}

object SwitchRouterTests {
  val testSystem = {
    val config = ConfigFactory.parseString(
      """
         akka.loggers = [akka.testkit.TestEventListener]
      """)
    ActorSystem("testsystem", config)
  }
}