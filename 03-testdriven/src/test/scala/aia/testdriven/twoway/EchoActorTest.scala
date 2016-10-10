package aia.testdriven.twoway

import aia.testdriven.StopSystemAfterAll
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.WordSpecLike

import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class EchoActorTest extends TestKit(ActorSystem("testsystem"))
  with WordSpecLike
  // Extending ImplicitSender means that testActor is set to the sender of messages (compare to SilentActorTest)
  with ImplicitSender
  with StopSystemAfterAll {

  "An EchoActor" must {
    "Reply with the same message it receives using tell" in {

      // !, aka tell, means “fire-and-forget”, e.g. send a message asynchronously and return immediately.
      val echo = system.actorOf(Props[EchoActor], "echo1")
      echo ! "some message"
      expectMsg("some message")
    }

    "Reply with the same message it receives using ask" in {
      // ?, aka ask, means send a message asynchronously and return a Future representing a possible reply.
      import akka.pattern.ask

      import scala.concurrent.duration._
      implicit val timeout = Timeout(3 seconds)
      implicit val ec = system.dispatcher

      val echo = system.actorOf(Props[EchoActor], "echo2")

      val futureMsg: Future[String] = echo.ask("some message").mapTo[String]

      // Following block doesn't work as desired. The callback model swallows the exception

      // futureMsg.onComplete {
      //   case Failure(_) => fail("Hmm, this failed")
      //   case Success(msg) => assert("some message" == msg)
      // }

      // This does work: convert the Future[String] to an Option[Try[String]]
      val msgResult: Option[Try[String]] = Await.ready(futureMsg, timeout.duration).value

      msgResult.get match {
        case Success(msg) => assert("some message" == msg)
        case Failure(ex) => fail(s"Failed: ${ex}")
      }
    }
  }
}