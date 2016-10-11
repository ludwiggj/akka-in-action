package aia.faulttolerance

import akka.actor._
import akka.testkit.TestKit
import org.scalatest.{MustMatchers, WordSpecLike}

class Watched extends Actor {
  def receive: Receive = {
    case _ =>
  }
}

class WatcherTest extends TestKit(ActorSystem("testsystem"))
  with MustMatchers
  with WordSpecLike {

  "The Watcher" must {
    "act on termination of watched actor" in {

      val watched = system.actorOf(Props[Watched], "lemming")
      val props = Watcher.props(watched, Some(testActor))
      val watcher = system.actorOf(props, "watcher-1")

      watched ! PoisonPill

      expectMsgPF() {
        case s: String =>
          s must (startWith("Dead actor!") and include("lemming"))
      }

      system.terminate()
    }
  }
}