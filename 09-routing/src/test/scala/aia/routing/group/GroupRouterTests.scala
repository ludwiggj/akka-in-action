package aia.routing.group

import java.util.Date

import aia.routing._
import aia.routing.messages.PerformanceRoutingMessage
import akka.actor.{Props, _}
import akka.pattern.ask
import akka.routing._
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class GroupRouterTests extends TestKit(ActorSystem("GroupRouterTests"))
  with MustMatchers with WordSpecLike with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  "The routerGroup" must {
    val msg = PerformanceRoutingMessage(ImageProcessing.createPhotoString(new Date(), 60, "123xyz"), None, None)

    def sendMessageAndVerify(router: ActorRef, endProbe: TestProbe, deadProbe: TestProbe) {
      router ! msg
      val procMsg = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: " + procMsg)
      endProbe.expectNoMsg()
      deadProbe.expectNoMsg()
    }

    def routeToRecreatedRouteesKilledIndividually(endProbe: TestProbe, waitForActorRecovery: () => Unit) {
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val creator = system.actorOf(Props(
        new WatchfulGetLicenseCreator(2, endProbe.ref)), "WatchfulGetLicenseCreator-test1")

      val router = system.actorOf(FromConfig.props(), "groupRouter-3")

      1 to 3 foreach {
        _ =>
          creator ! "KillFirst"
          waitForActorRecovery()
          sendMessageAndVerify(router, endProbe, deadProbe)
      }

      system.stop(router)
      system.stop(creator)
    }

    def getRoutees(router: ActorRef) = {
      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      routeesMsg.getRoutees
    }

    def dynamicRouteeWorkout(router: ActorRef, creator: ActorRef, endProbe: TestProbe, deadProbe: TestProbe,
                             completeWorkout: () => Unit) {
      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      Thread.sleep(100)

      sendMessageAndVerify(router, endProbe, deadProbe)

      var routees = getRoutees(router)
      routees.size must be(2)

      println("Expanding routes from 2 to 4")
      creator ! PreferredSize(4)
      Thread.sleep(1000)

      routees = getRoutees(router)
      routees.size must be(4)

      sendMessageAndVerify(router, endProbe, deadProbe)

      println("Contracting routes from 4 to 2")
      creator ! PreferredSize(2)
      Thread.sleep(1000)

      routees = getRoutees(router)
      routees.size must be(2)

      sendMessageAndVerify(router, endProbe, deadProbe)

      completeWorkout()

      system.stop(router)
      system.stop(creator)
    }

    "routes using RoundRobinGroup via programmatic config" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val creator = system.actorOf(Props(new GetLicenseCreator(2, endProbe.ref)), "GetLicenseCreator2-test1")

      val paths = List(
        "/user/GetLicenseCreator2-test1/GetLicense0",
        "/user/GetLicenseCreator2-test1/GetLicense1"
      )

      val router = system.actorOf(RoundRobinGroup(paths).props(), "groupRouter-1")

      creator ! "KillFirst"

      Thread.sleep(100)

      router ! msg

      println(s"DeadLetter??? ${deadProbe.expectMsgType[DeadLetter](1 second)}")

      router ! msg

      endProbe.expectMsgType[PerformanceRoutingMessage](1 second)

      router ! msg

      deadProbe.expectMsgType[DeadLetter](1 second)

      system.stop(router)
      system.stop(creator)
    }

    "routes using RoundRobinGroup via config file" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val creator = system.actorOf(Props(new GetLicenseCreator(2, endProbe.ref)), "GetLicenseCreator2-test2")

      val router = system.actorOf(FromConfig.props(), "groupRouter-2")

      creator ! "KillFirst"

      Thread.sleep(100)

      router ! msg

      println(s"DeadLetter??? ${deadProbe.expectMsgType[DeadLetter](1 second)}")

      router ! msg

      endProbe.expectMsgType[PerformanceRoutingMessage](1 second)

      router ! msg

      deadProbe.expectMsgType[DeadLetter](1 second)

      system.stop(router)
      system.stop(creator)
    }

    "routes to recreated routees killed individually with sleep" in {
      val endProbe = TestProbe()
      endProbe.ignoreMsg({ case "Ready" => true })

      routeToRecreatedRouteesKilledIndividually(endProbe, () => Thread.sleep(100))
    }

    "routes to recreated routees killed individually with ready message" in {
      val endProbe = TestProbe()

      routeToRecreatedRouteesKilledIndividually(endProbe, () => endProbe.expectMsg(1 second, "Ready"))
    }

    "routes to recreated routees killed via broadcast" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val creator = system.actorOf(Props(
        new WatchfulGetLicenseCreator(2, endProbe.ref)), "WatchfulGetLicenseCreator-test2")

      val paths = List(
        "/user/WatchfulGetLicenseCreator-test2/GetLicense0",
        "/user/WatchfulGetLicenseCreator-test2/GetLicense1"
      )
      val router = system.actorOf(RoundRobinGroup(paths).props(), "groupRouter-4")

      creator ! "ZeroActorCount"

      deadProbe.expectNoMsg()
      router ! Broadcast(PoisonPill)
      deadProbe.expectNoMsg()

      endProbe.expectMsg(1 second, "Ready")

      sendMessageAndVerify(router, endProbe, deadProbe)

      sendMessageAndVerify(router, endProbe, deadProbe)

      system.stop(router)
      system.stop(creator)
    }

    "dynamic routee shrinks and restarts all routees" in {

      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      val router = system.actorOf(RoundRobinGroup(List()).props(), "groupRouter-5")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props(new DynamicRouteeSizer(2, props, router)), "DynamicRouteeSizer-test1")

      dynamicRouteeWorkout(router, creator, endProbe, deadProbe, () => {
        // Last steps

        val termProbe = TestProbe()
        termProbe.watch(router)

        println("Sending PoisonPill - will the 2 actors recover?")

        router ! Broadcast(PoisonPill)
        Thread.sleep(1000)

        termProbe.expectNoMsg
        termProbe.unwatch(router)

        sendMessageAndVerify(router, endProbe, deadProbe)

        getRoutees(router).size must be(2)
      })
    }

    "dynamic routee shrinks but cannot handle PoisonPill correctly" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      val router = system.actorOf(RoundRobinGroup(List()).props(), "groupRouter-6")

      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props(new AnotherWrongDynamicRouteeSizer(2, props, router)), "AnotherWrongDynamicRouteeSizer")

      dynamicRouteeWorkout(router, creator, endProbe, deadProbe, () => {
        // Last steps

        val termProbe = TestProbe()
        termProbe.watch(router)

        println("Sending PoisonPill - will the 2 actors recover?")

        router ! Broadcast(PoisonPill)
        Thread.sleep(1000)

        // Router terminates due to faulty logic
        termProbe.expectTerminated(router)

        // GetRoutees message that's sent as part of actor recovery ends up as a dead letter
        deadProbe.expectMsgType[DeadLetter](1 second)
      })
    }

    "dynamic routee restarts individual routee" in {
      val termProbe = TestProbe()
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "groupRouter-7")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props(new DynamicRouteeSizer(2, props, router)), "DynamicRouteeSizer-test2")
      Thread.sleep(100)

      var routees = getRoutees(router)
      routees.size must be(2)

      println("About to kill a routee")

      // router isn't terminated due to the use of ActorSelectionRoutee
      termProbe.watch(router)

      routees.get(0).send(PoisonPill, endProbe.ref)

      termProbe.expectNoMsg
      termProbe.unwatch(router)

      routees = getRoutees(router)
      routees.size must be(2)

      import collection.JavaConversions._
      for (routee <- routees) {
        routee.send(msg, endProbe.ref)
      }
      val procMsg1 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: " + procMsg1)
      val procMsg2 = endProbe.expectMsgType[PerformanceRoutingMessage](1 second)
      println("Received: " + procMsg2)
      system.stop(router)
      system.stop(creator)
    }

    "survive killed actor ref routee" in {
      val endProbe = TestProbe()
      val deadProbe = TestProbe()

      system.eventStream.subscribe(deadProbe.ref, classOf[DeadLetter])

      val router = system.actorOf(RoundRobinGroup(List()).props(), "groupRouter-8")
      val props = Props(new GetLicense(endProbe.ref))
      val creator = system.actorOf(Props(new WrongDynamicRouteeSizer(2, props, router)), "WrongDynamicRouteeSizer")

      dynamicRouteeWorkout(router, creator, endProbe, deadProbe, () => {
        // Last steps
        val routees = getRoutees(router)
        val routee = routees.get(0).asInstanceOf[ActorRefRoutee]

        val termProbe = TestProbe()

        termProbe.watch(routee.ref)
        termProbe.watch(router)

        routee.send(PoisonPill, endProbe.ref)

        termProbe.expectTerminated(routee.ref)

        termProbe.expectNoMsg

        sendMessageAndVerify(router, endProbe, deadProbe)

        println("Sending PoisonPill - will the actors recover?")

        router ! Broadcast(PoisonPill)
        Thread.sleep(1000)

        // Router terminates due to faulty logic
        termProbe.expectTerminated(router)
      })
    }
  }
}