package aia.routing.hashing

import aia.routing.hashing.gather_via_function.SimpleGather
import aia.routing.hashing.gather_via_traits.{KeyedOnMessageIdLength, KeyedOnMessageIdValue, SimpleGatherAbstract}
import aia.routing.hashing.messages.{GatherMessage, GatherMessageNormalImpl, GatherMessageWithHash}
import akka.actor._
import akka.routing.ConsistentHashingRouter._
import akka.routing._
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

class HashRoutingTest extends TestKit(ActorSystem("PerfRoutingTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The HashRouting" must {
    val id1_msg1 = GatherMessageNormalImpl("1", Seq("msg1"))
    val id1_msg2 = GatherMessageNormalImpl("1", Seq("msg2"))
    val id10_msg1 = GatherMessageNormalImpl("10", Seq("msg1"))
    val id10_msg2 = GatherMessageNormalImpl("10", Seq("msg2"))
    val id101_msg1 = GatherMessageNormalImpl("101", Seq("msg1"))
    val id111_msg2 = GatherMessageNormalImpl("111", Seq("msg2"))

    def hashMappingOnIdValue: ConsistentHashMapping = {
      case msg: GatherMessage => msg.id
    }

    def hashMappingOnIdLength: ConsistentHashMapping = {
      case msg: GatherMessage => msg.id.length
    }

    def routeOnIdWorkout(router: ActorRef, endProbe: TestProbe): Unit = {
      router ! id1_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id10_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id1_msg2
      endProbe.expectMsg(GatherMessageNormalImpl("1", Seq("msg1", "msg2")))

      router ! id10_msg2
      endProbe.expectMsg(GatherMessageNormalImpl("10", Seq("msg1", "msg2")))

      router ! id101_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id111_msg2
      endProbe.expectNoMsg(100.millis)

      system.stop(router)
    }

    def routeOnIdLengthWorkout(router: ActorRef, endProbe: TestProbe): Unit = {
      router ! id1_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id10_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id1_msg2
      endProbe.expectMsg(GatherMessageNormalImpl("1", Seq("msg1", "msg2")))

      router ! id10_msg2
      endProbe.expectMsg(GatherMessageNormalImpl("2", Seq("msg1", "msg2")))

      router ! id101_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id111_msg2
      endProbe.expectMsg(GatherMessageNormalImpl("3", Seq("msg1", "msg2")))

      system.stop(router)
    }

    "works using router partial function route on id value via trait" in {

      val endProbe = TestProbe()

      val router = system.actorOf(
        ConsistentHashingPool(10, virtualNodesFactor = 10, hashMapping = hashMappingOnIdValue)
          .props(Props(new SimpleGatherAbstract(endProbe.ref) with KeyedOnMessageIdValue)),
        name = "routerMappingOnIdValue"
      )

      routeOnIdWorkout(router, endProbe)
    }

    "works using router partial function route on id value via passed function" in {

      val endProbe = TestProbe()

      val router = system.actorOf(
        ConsistentHashingPool(10, virtualNodesFactor = 10, hashMapping = hashMappingOnIdValue)
          .props(Props(new SimpleGather(endProbe.ref)(msg => msg.id))),
        name = "routerMappingOnIdValue"
      )

      routeOnIdWorkout(router, endProbe)
    }

    "works using router partial function route on id length via trait" in {
      val endProbe = TestProbe()

      val router = system.actorOf(
        ConsistentHashingPool(10, virtualNodesFactor = 10, hashMapping = hashMappingOnIdLength)
          .props(Props(new SimpleGatherAbstract(endProbe.ref) with KeyedOnMessageIdLength)),
        name = "routerMappingOnIdLength"
      )

      routeOnIdLengthWorkout(router, endProbe)
    }

    "works using router partial function route on id length via passed function" in {
      val endProbe = TestProbe()

      val router = system.actorOf(
        ConsistentHashingPool(10, virtualNodesFactor = 10, hashMapping = hashMappingOnIdLength)
          .props(Props(new SimpleGather(endProbe.ref)(msg => msg.id.length.toString))),
        name = "routerMappingOnIdLength"
      )

      routeOnIdLengthWorkout(router, endProbe)
    }

    "does not work using router partial function if do not supply hash method" in {
      val endProbe = TestProbe()

      val router = system.actorOf(ConsistentHashingPool(10, virtualNodesFactor = 10)
        .props(Props(new SimpleGatherAbstract(endProbe.ref) with KeyedOnMessageIdValue)), name = "routerMessage")

      router ! id1_msg1
      endProbe.expectNoMsg(100.millis)

      router ! id1_msg2
      endProbe.expectNoMsg(1000.millis)

      system.stop(router)
    }

    "works using message hash" in {
      val endProbe = TestProbe()

      val router = system.actorOf(ConsistentHashingPool(10, virtualNodesFactor = 10)
        .props(Props(new SimpleGatherAbstract(endProbe.ref) with KeyedOnMessageIdValue)), name = "routerMessage")

      router ! GatherMessageWithHash("1", Seq("msg1"))
      endProbe.expectNoMsg(100.millis)

      router ! GatherMessageWithHash("1", Seq("msg2"))
      endProbe.expectMsg(GatherMessageNormalImpl("1", Seq("msg1", "msg2")))

      router ! GatherMessageWithHash("10", Seq("msg1"))
      endProbe.expectNoMsg(100.millis)

      router ! GatherMessageWithHash("10", Seq("msg2"))
      endProbe.expectMsg(GatherMessageNormalImpl("10", Seq("msg1", "msg2")))

      system.stop(router)
    }

    "works using envelope" in {
      val endProbe = TestProbe()

      val router = system.actorOf(ConsistentHashingPool(10, virtualNodesFactor = 10)
        .props(Props(new SimpleGatherAbstract(endProbe.ref) with KeyedOnMessageIdValue)), name = "routerMessage")

      router ! ConsistentHashableEnvelope(message = id1_msg1, hashKey = "someHash")
      endProbe.expectNoMsg(100.millis)

      router ! ConsistentHashableEnvelope(message = id1_msg2, hashKey = "someHash")
      endProbe.expectMsg(GatherMessageNormalImpl("1", Seq("msg1", "msg2")))

      router ! ConsistentHashableEnvelope(message = id10_msg1, hashKey = "10")
      endProbe.expectNoMsg(100.millis)

      router ! ConsistentHashableEnvelope(message = id10_msg2, hashKey = "10")
      endProbe.expectMsg(GatherMessageNormalImpl("10", Seq("msg1", "msg2")))

      system.stop(router)
    }
  }
}