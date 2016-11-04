package aia.routing.pool

import java.util.Date

import aia.routing._
import aia.routing.messages.{PerformanceRoutingMessage, SetService}
import akka.actor.{Actor, ActorSystem, Kill, PoisonPill, Props, SupervisorStrategy}
import akka.routing._
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class TestSuper() extends Actor {
  def receive = {
    case "OK" =>
    case _ => throw new IllegalArgumentException("not supported")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    println("restart %s".format(self.path.toString))
  }
}

class PoolRouterTests extends TestKit(ActorSystem("PoolRoutersTest"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The Router" must {
    val photoString = ImageProcessing.createPhotoString(new Date(), 60, "123xyz")
    val msg = PerformanceRoutingMessage(photoString, None, None)
    def extractId(prmSeq: Seq[PerformanceRoutingMessage]) = prmSeq.map(_.id)

    "routes using BalancingPool via config file" in {
      val endProbe = TestProbe()
      val router = system.actorOf(FromConfig.props(Props(new GetLicense(endProbe.ref))), "poolRouter-1")

      val expectedLicense = ImageProcessing.getLicense(photoString).get

      router ! msg
      router ! msg
      router ! msg

      val series = endProbe.receiveWhile(1 second) {
        case PerformanceRoutingMessage(photo, Some(license), Some(processor), _) =>
          photo must be(photoString)
          license must be(expectedLicense)
          println(s"Processed by $processor")

        case msg: AnyRef => fail(s"Error. Was not expecting message [$msg]")
      }

      series.size must be(3)

      router ! PoisonPill
    }

    "routes using BalancingPool via programmatic config" in {
      val endProbe = TestProbe()
      val router = system.actorOf(BalancingPool(5).props(Props(new GetLicense(endProbe.ref))), "poolRouter-2")

      router ! msg

      endProbe.expectMsgType[PerformanceRoutingMessage](1 second)

      router ! Kill
    }

    "routes using RoundRobin" in {
      val endProbe = TestProbe()
      val router = system.actorOf(
        RoundRobinPool(5).props(Props(new GetLicense(endProbe.ref, 250 millis))), "poolRouter-3")

      for (index <- 0 until 10) {
        router ! msg
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)

      // verify that each member of pool has processed 2 items
      grouped.values.foreach(listProcessedByOneActor =>
        listProcessedByOneActor must have size (2))
    }

    "routes using smallest mailbox" in {
      val endProbe = TestProbe()

      val router = system.actorOf(SmallestMailboxPool(2).props(Props(
        new GetLicense(endProbe.ref))), "poolRouter-4")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees

      routees.size must be(2)

      println(routees)

      routees.get(0).send(SetService("250", 250 millis), endProbe.ref)
      routees.get(1).send(SetService("500", 500 millis), endProbe.ref)

      for (index <- 0 until 10) {
        Thread.sleep(200)
        router ! msg.copy(id = s"M$index")
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)


      println(grouped.mapValues(extractId))

      val msgProcessedByActor1 = grouped.get(Some("250")).getOrElse(Seq())

      val msgProcessedByActor2 = grouped.get(Some("500")).getOrElse(Seq())

      msgProcessedByActor1 must have size (7)
      msgProcessedByActor2 must have size (3)
    }

    /*

    Logic:
    Tries to send to the non-suspended routee with fewest messages in mailbox.
    The selection is done in this order:

    * pick any idle routee (not processing message) with empty mailbox
    * pick any routee with empty mailbox
    * pick routee with fewest pending messages in mailbox
    * pick any remote routee, remote actors are consider lowest priority, since their mailbox size is unknown

                 250        500
    M0   200 ?    M0
    M1   400 =>   P          M1
                  0 (450)
    M2   600 =>   M2         P
    M3   800 ?    P          P
                  0 (850)
                  M3         0 (900)
    M4  1000 =>   P          M4
                  0 (1100)
    M5  1200 =>   M5         P
    M6  1400 ?    P          P
                  0 (1450)
                  M6
                             0 (1500)
    M7  1600 =>   P          M7
                  0 (1700)
    M8  1800 =>   M8         P
    M9  2000 ?    P          P
                  0 (2050)
                  M9
                             0 (2100)
                  0 (2300)
     */

    "routes using smallest mailbox with addRoutee" in {
      val endProbe = TestProbe()

      val router = system.actorOf(SmallestMailboxPool(0).props(Props(
        new GetLicense(endProbe.ref, 250 millis))), "poolRouter-5")

      val actor1 = system.actorOf(Props(new GetLicense(endProbe.ref, 250 millis)), "250")
      val actor2 = system.actorOf(Props(new GetLicense(endProbe.ref, 500 millis)), "500")
      router ! AddRoutee(ActorRefRoutee(actor1))
      router ! AddRoutee(ActorRefRoutee(actor2))

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees

      routees.size must be(2)

      for (index <- 0 until 10) {
        Thread.sleep(200)
        router ! msg.copy(id = s"M$index")
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)
      println(grouped.mapValues(extractId))

      val msgProcessedByActor1 = grouped.get(Some("250")).getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500")).getOrElse(Seq())

      msgProcessedByActor1 must have size (7)
      msgProcessedByActor2 must have size (3)
    }

    "routes using BalancingPool" in {
      val testSystem = ActorSystem("poolRouter-6")
      val endProbe = TestProbe()(testSystem)

      val router = system.actorOf(BalancingPool(2).props(Props(new GetLicense(endProbe.ref))), "poolRouter-6")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees

      routees.size must be(2)

      routees.get(0).send(SetService("250", 250 millis), endProbe.ref)
      routees.get(1).send(SetService("500", 500 millis), endProbe.ref)

      for (index <- 0 until 10) {
        router ! msg.copy(id = s"M$index")
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)
      println(grouped.mapValues(extractId))

      val msgProcessedByActor1 = grouped.get(Some("250")).getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500")).getOrElse(Seq())

      msgProcessedByActor1.size must be(7 +- 1)
      msgProcessedByActor2.size must be(3 +- 1)
      testSystem.terminate()
    }

    "create routes using BalancingPool and using direct" in {
      val load: Config = ConfigFactory.load("balance")

      val testSystem = ActorSystem("balance", load)

      val endProbe = TestProbe()(testSystem)
      val router = system.actorOf(BalancingPool(2).props(Props(new GetLicense(endProbe.ref))), "poolRouter-7")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees

      routees.size must be(2)

      val routee0 = routees.get(0)
      println(s"routee0 [$routee0]")

      val routee1 = routees.get(1)
      println(s"routee1 [$routee1]")

      routee1.send(SetService("250", 250 millis), endProbe.ref)
      routee0.send(SetService("500", 500 millis), endProbe.ref)

      for (index <- 0 until 5) {
        val routee0 = routees.get(0)
        println(s"routee0 [$routee0]")
        routee0.send(msg.copy(id = s"M${2 * index}"), endProbe.ref)

        val routee1 = routees.get(1)
        println(s"routee1 [$routee1]")
        routee1.send(msg.copy(id = s"M${2 * index + 1}"), endProbe.ref)
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)
      println(grouped.mapValues(extractId))

      val msgProcessedByActor1 = grouped.get(Some("250")).getOrElse(Seq())
      val msgProcessedByActor2 = grouped.get(Some("500")).getOrElse(Seq())

      msgProcessedByActor1.size must be(7 +- 1)
      msgProcessedByActor2.size must be(3 +- 1)

      testSystem.terminate()
    }

    "routes using smallest mailbox opposite order" in {
      val endProbe = TestProbe()

      val router = system.actorOf(SmallestMailboxPool(2).props(Props(
        new GetLicense(endProbe.ref))), "poolRouter-8")

      val future = router.ask(GetRoutees)(1 second)
      val routeesMsg = Await.result(future, 1.second).asInstanceOf[Routees]
      val routees = routeesMsg.getRoutees

      routees.size must be(2)

      println(routees)

      routees.get(0).send(SetService("500", 500 millis), endProbe.ref)
      routees.get(1).send(SetService("250", 250 millis), endProbe.ref)

      for (index <- 0 until 10) {
        Thread.sleep(200)
        router ! msg.copy(id = s"M$index")
      }

      val processedMessages = endProbe.receiveN(10, 5 seconds).collect {
        case m: PerformanceRoutingMessage => m
      }

      processedMessages.size must be(10)

      val grouped = processedMessages.groupBy(_.processedBy)

      println(grouped.mapValues(extractId))

      val msgProcessedByActor1 = grouped.get(Some("500")).getOrElse(Seq())

      val msgProcessedByActor2 = grouped.get(Some("250")).getOrElse(Seq())

      msgProcessedByActor1 must have size (5)
      msgProcessedByActor2 must have size (5)
    }

    /*
                 500        250
     M0   200 ?   M0
     M1   400 =>  P          M1
     M2   600 ?   P          P
                             0 (650)
                  0 (700)
                  M2
     M3   800 =>  P          M3

     M4  1000 =>  P          P
                             0 (1050)
                  0 (1200)
                  M4
     M5  1200 =>  P          M5
     M6  1400 ?   P          P
                             0 (1450)
     M7  1600 =>  P          M7
                  0 (1700)
                  M6
     M8  1800 ?   P          P
                             0 (1850)
     M9  2000 =>  P          M9
                  0 (2200)   0 (2250)
                  M8
                  0 (2700)
      */

    "restart all routees" in {
      val router = system.actorOf(RoundRobinPool(5).props(Props[TestSuper]), "poolRouter-9")
      router ! "exception"
      Thread.sleep(1000)
    }

    "restart one routee" in {
      val router = system.actorOf(
        RoundRobinPool(3, supervisorStrategy = SupervisorStrategy.defaultStrategy).props(
          Props[TestSuper]), "poolRouter-10")
      router ! "exception"
      Thread.sleep(1000)
    }
  }
}