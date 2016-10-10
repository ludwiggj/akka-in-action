package aia.testdriven.sendingactor

import aia.testdriven.StopSystemAfterAll
import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}

import scala.util.Random

class MutatingCopyActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {

  "A Mutating Copy Actor" must {
    "send a message to another actor when it has finished processing" in {
      import MutatingCopyActor._
      val props = MutatingCopyActor.props(testActor)
      val sendingActor = system.actorOf(props, "sendingActor")

      val size = 1000
      val maxInclusive = 100000

      def randomEvents() = (0 until size).map{ _ =>
        Event(Random.nextInt(maxInclusive))
      }.toVector

      val unsorted = randomEvents()
      val sortEvents = SortEvents(unsorted)
      sendingActor ! sortEvents

      expectMsg(SortedEvents(unsorted.sortBy(_.id)))

//      expectMsgPF() {
//        case SortedEvents(events) =>
//          events.size must be(size)
//          unsorted.sortBy(_.id) must be(events)
//      }
    }
  }
}