package aia.testdriven.sendingactor

import aia.testdriven.StopSystemAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}

class FilteringActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {
  "A Filtering Actor" must {
    "filter out particular messages" in {
      import FilteringActor._
      val props = FilteringActor.props(testActor, 5)
      val filter = system.actorOf(props, "filter-1")

      filter ! Event(1)
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(1)
      filter ! Event(6)

      val eventIds = receiveWhile() {
        case Event(id) if id <= 5 => id
      }

      eventIds must be(List(1, 2, 3, 4, 5))

      expectMsg(Event(6))
    }

    "filter out particular messages result depends on buffer size" in {
      import FilteringActor._
      val props = FilteringActor.props(testActor, 4)
      val filter = system.actorOf(props, "filter-2")

      filter ! Event(1)
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(1)
      filter ! Event(6)

      val eventIds = receiveWhile() {
        case Event(id) if id <= 5 => id
      }

      eventIds must be(List(1, 2, 3, 4, 5, 1))

      expectMsg(Event(6))
    }

    "filter out particular messages with ignoreMsg" in {
      import FilteringActor._
      val props = FilteringActor.props(testActor, 5)
      val filter = system.actorOf(props, "filter-3")

      // Ignore the following messages, need to state before the messages start flying!
      ignoreMsg({
        case Event(5) => true
      })

      filter ! Event(1)
      filter ! Event(2)
      filter ! Event(1)
      filter ! Event(3)
      filter ! Event(1)
      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      filter ! Event(1)
      filter ! Event(6)

      val eventIds = receiveWhile() {
        case Event(id) if id <= 4 => id
      }

      eventIds must be(List(1, 2, 3, 4))

      expectMsg(Event(6))

      // Also need to undo the ignore at the end, as testActor retains state between tests
      ignoreNoMsg()
    }

    "filter out particular messages using expectNoMsg" in {

      import FilteringActor._
      val props = FilteringActor.props(testActor, 5)
      val filter = system.actorOf(props, "filter-4")
      filter ! Event(1)
      filter ! Event(2)
      expectMsg(Event(1))
      expectMsg(Event(2))

      filter ! Event(1)
      expectNoMsg

      filter ! Event(3)
      expectMsg(Event(3))

      filter ! Event(1)
      expectNoMsg

      filter ! Event(4)
      filter ! Event(5)
      filter ! Event(5)
      expectMsg(Event(4))
      expectMsg(Event(5))

      expectNoMsg()
    }
  }
}