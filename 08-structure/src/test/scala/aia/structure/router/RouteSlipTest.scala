package aia.structure.router

import akka.actor._
import akka.testkit._
import org.scalatest._

class RoutingActor(val routeSlip: Seq[ActorRef]) extends Actor with RouteSlip {
  def receive: Receive = {
    case msg => sendMessageToNextTask(routeSlip, msg.asInstanceOf[AnyRef])
  }
}

class RouteSlipTest extends TestKit(ActorSystem("RouteSlipTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Route slip" must {
    "pass the route slip and message on to the next actor in the list" in {
      val nextToLastProbe = TestProbe()
      val endProbe = TestProbe()
      val routeSlip: Seq[ActorRef] = Seq(nextToLastProbe.ref, endProbe.ref)
      val actorRef = system.actorOf(Props(new RoutingActor(routeSlip)))
      val msg = "message"

      actorRef ! msg

      nextToLastProbe.expectMsg(RouteSlipMessage(Seq(endProbe.ref), msg))
    }

    "pass the message to the last actor in the list" in {
      val endProbe = TestProbe()
      val routeSlip: Seq[ActorRef] = Seq(endProbe.ref)
      val actorRef = system.actorOf(Props(new RoutingActor(routeSlip)))
      val msg = "message"

      actorRef ! msg

      endProbe.expectMsg(msg)
    }
  }
}