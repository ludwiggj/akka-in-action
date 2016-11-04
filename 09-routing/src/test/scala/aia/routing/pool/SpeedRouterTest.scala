package aia.routing.pool

import aia.routing.Photo
import akka.actor._
import akka.routing.{ActorRefRoutee, Routee}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest._

import scala.collection.immutable
import scala.concurrent.duration._

class SpeedRouterTest extends TestKit(ActorSystem("MsgRoutingTest"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The Router" must {
    "routes depending on speed" in {
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val router = system.actorOf(Props.empty.withRouter(
        new SpeedRouterPool(50,
          Props(new RedirectActor(normalFlowProbe.ref)),
          Props(new RedirectActor(cleanupProbe.ref))
        )
      ))

      val msg = new Photo(license = "123xyz", speed = 60)
      router ! msg

      cleanupProbe.expectNoMsg(1 second)
      normalFlowProbe.expectMsg(msg)

      val msg2 = new Photo(license = "123xyz", speed = 45)
      router ! msg2

      cleanupProbe.expectMsg(msg2)
      normalFlowProbe.expectNoMsg(1 second)
    }

    "routes direct test" in {
      /*
      Used to be able to test routing directly using akka.testkit.ExtractRoute i.e.

      val route = ExtractRoute(router)

      val r = Await.result(router.ask(CurrentRoutees)(1 second).mapTo[RouterRoutees], 1 second)
      r.routees.size must be(2)
      val normal = r.routees.head
      val clean = r.routees.last

      val msg = new Photo(license = "123xyz", speed = 60)
      route(testActor -> msg) must be(Seq(Destination(testActor, normal)))

      val msg2 = new Photo(license = "123xyz", speed = 45)
      route(testActor -> msg2) must be(Seq(Destination(testActor, clean)))
      */

      // Now have to test routing logic directly - there's probably a better way than the following...
      val normalFlowProbe = TestProbe()
      val cleanupProbe = TestProbe()

      val routerLogic = new SpeedRouterLogic(50, "normalFlow", "cleanup")

      val normal = system.actorOf(Props(new RedirectActor(normalFlowProbe.ref)), "normalFlow")
      val clean = system.actorOf(Props(new RedirectActor(cleanupProbe.ref)), "cleanup")

      val routees = immutable.IndexedSeq[Routee](ActorRefRoutee(normal), ActorRefRoutee(clean))

      val msg = new Photo(license = "123xyz", speed = 60)
      routerLogic.select(msg, routees).asInstanceOf[ActorRefRoutee].ref must be(normal)

      val msg2 = new Photo(license = "123xyz", speed = 45)
      routerLogic.select(msg2, routees).asInstanceOf[ActorRefRoutee].ref must be(clean)
    }
  }
}