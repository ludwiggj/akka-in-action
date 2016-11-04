package aia.routing.pattern

import java.text.SimpleDateFormat
import java.util.Date

import aia.routing.ImageProcessing
import akka.actor._
import akka.testkit.{TestKit, TestProbe}
import org.scalatest._

import scala.concurrent.duration._

class SpeedCameraTest extends TestKit(ActorSystem("SpeedCameraTest"))
  with WordSpecLike with MustMatchers with BeforeAndAfterAll {

  override def afterAll() = {
    system.terminate()
  }

  def routeToGetTimeIfSpeedExceedsMinimum(compositeSpeedCamera: ActorRef,
                                          cleanupProbe: TestProbe,
                                          getTimeProbe: TestProbe) {
    val photo = ImageProcessing.createPhotoString(new Date(), speed = 60)
    val msg = new PhotoMessage("id", photo, creationTime = None, speed = None)

    compositeSpeedCamera ! msg

    cleanupProbe.expectNoMsg(1 second)
    getTimeProbe.expectMsg(msg.copy(speed = Some(60)))
  }

  def routeToCleanupIfSpeedDoesNotExceedMinimum(compositeSpeedCamera: ActorRef,
                                                cleanupProbe: TestProbe,
                                                getTimeProbe: TestProbe) {
    val photo = ImageProcessing.createPhotoString(new Date(), speed = 45)
    val msg = new PhotoMessage("id", photo, creationTime = None, speed = None)

    compositeSpeedCamera ! msg

    cleanupProbe.expectMsg(msg.copy(speed = Some(45)))
    getTimeProbe.expectNoMsg(1 second)
  }

  def routeToCleanupIfSpeedIsNotAvailable(compositeSpeedCamera: ActorRef,
                                          cleanupProbe: TestProbe,
                                          getTimeProbe: TestProbe) {
    val dateFormat = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS")
    val photo = "%s|%s|%s".format(dateFormat.format(new Date), "", "")
    val msg = new PhotoMessage("id", photo, creationTime = None, speed = None)

    compositeSpeedCamera ! msg

    cleanupProbe.expectMsg(msg)
    getTimeProbe.expectNoMsg(1 second)
  }

  "The composite speed camera" must {
    "route to getTime if speed exceeds minimum" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedRouter = system.actorOf(Props(new SpeedRouter(50, cleanupProbe.ref, getTimeProbe.ref)))
      val compositeSpeedCamera = system.actorOf(Props(new GetSpeed(speedRouter)))

      routeToGetTimeIfSpeedExceedsMinimum(compositeSpeedCamera, cleanupProbe, getTimeProbe)
    }

    "route to cleanup if speed does not exceed minimum" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedRouter = system.actorOf(Props(new SpeedRouter(50, cleanupProbe.ref, getTimeProbe.ref)))
      val compositeSpeedCamera = system.actorOf(Props(new GetSpeed(speedRouter)))

      routeToCleanupIfSpeedDoesNotExceedMinimum(compositeSpeedCamera, cleanupProbe, getTimeProbe)
    }

    "route to cleanup if speed is not available" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedRouter = system.actorOf(Props(new SpeedRouter(50, cleanupProbe.ref, getTimeProbe.ref)))
      val compositeSpeedCamera = system.actorOf(Props(new GetSpeed(speedRouter)))

      routeToCleanupIfSpeedIsNotAvailable(compositeSpeedCamera, cleanupProbe, getTimeProbe)
    }
  }

  "The speed camera" must {
    "route to getTime if speed exceeds minimum" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedCamera = system.actorOf(Props(new SpeedCamera(50, cleanupProbe.ref, getTimeProbe.ref)))

      routeToGetTimeIfSpeedExceedsMinimum(speedCamera, cleanupProbe, getTimeProbe)
    }

    "route to cleanup if speed does not exceed minimum" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedCamera = system.actorOf(Props(new SpeedCamera(50, cleanupProbe.ref, getTimeProbe.ref)))

      routeToCleanupIfSpeedDoesNotExceedMinimum(speedCamera, cleanupProbe, getTimeProbe)
    }

    "route to cleanup if speed is not available" in {
      val cleanupProbe = TestProbe()
      val getTimeProbe = TestProbe()

      val speedCamera = system.actorOf(Props(new SpeedCamera(50, cleanupProbe.ref, getTimeProbe.ref)))

      routeToCleanupIfSpeedIsNotAvailable(speedCamera, cleanupProbe, getTimeProbe)
    }
  }
}