package aia.testdriven

import aia.testdriven.SilentActor.{GetState, SilentMessage}
import org.scalatest.{MustMatchers, WordSpecLike}
import akka.testkit.{TestActorRef, TestKit}
import akka.actor._

class SilentActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  with MustMatchers
  with StopSystemAfterAll {

  "A Silent Actor" must {
    "change state when it receives a message, single threaded" in {
      // TestActorRef used for single-threaded testing
      val silentActor = TestActorRef[SilentActor]
      silentActor ! SilentMessage("whisper")
      silentActor.underlyingActor.state must (contain("whisper"))
    }

    "change state when it receives a message, multi-threaded" in {
      val silentActor = system.actorOf(Props[SilentActor], "s3")
      silentActor ! SilentMessage("whisper1")
      silentActor ! SilentMessage("whisper2")

      // testActor is an actor provided by the akka test kit

      // testActor is passed in explicitly so it can receive a message in response
      // see EchoActorTest for an improvement to this method
      silentActor ! GetState(testActor)

      expectMsg(Vector("whisper1", "whisper2"))
    }
  }
}