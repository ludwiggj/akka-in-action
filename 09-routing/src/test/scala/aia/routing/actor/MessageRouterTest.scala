package aia.routing.actor

import aia.routing.Photo
import akka.actor._
import akka.testkit.{TestKit, TestProbe}
import org.scalatest._

import scala.concurrent.duration._

class MessageRouterTest extends TestKit(ActorSystem("MsgRoutingTest"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The Router" must {
    "routes depending on speed" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val router = system.actorOf(Props(new MessageRouter(50, normalFlowProbe.ref, cleanupProbe.ref)))

      val msg = new Photo(license = "123xyz", speed = 60)
      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      val msg2 = new Photo(license = "123xyz", speed = 45)
      router ! msg2

      cleanupProbe.expectMsg(msg2)
      normalFlowProbe.expectNoMsg(1 second)
    }
  }
}