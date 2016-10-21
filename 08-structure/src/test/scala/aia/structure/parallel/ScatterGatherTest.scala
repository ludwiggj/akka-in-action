package aia.structure.parallel

import java.util.Date

import akka.actor._
import akka.testkit._
import org.scalatest._

import scala.concurrent.duration._
import scala.language.postfixOps

class ScatterGatherTest extends TestKit(ActorSystem("ScatterGatherTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "The ScatterGather" must {
    "scatter the message and gather them again" in {
      val endProbe = TestProbe()

      val aggregateRef = system.actorOf(Props(new Aggregator(1 second, endProbe.ref)))

      val speedRef = system.actorOf(Props(new GetSpeed(aggregateRef)))
      val timeRef = system.actorOf(Props(new GetTime(aggregateRef)))

      val actorRef = system.actorOf(Props(new RecipientList(Seq(speedRef, timeRef))))

      val photoDate = new Date()
      val photoSpeed = 60
      val msg = PhotoMessage("id1", ImageProcessing.createPhotoString(photoDate, photoSpeed))

      actorRef ! msg

      val combinedMsg = PhotoMessage(msg.id, msg.photo, Some(photoDate), Some(photoSpeed))

      endProbe.expectMsg(combinedMsg)
    }
  }
}