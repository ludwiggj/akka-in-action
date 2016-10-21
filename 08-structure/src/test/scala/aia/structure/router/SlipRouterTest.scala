package aia.structure.router

package aia.structure.router

import akka.actor._
import akka.testkit._
import org.scalatest._

class SlipRouterTest extends TestKit(ActorSystem("SlipRouterTest")) with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    system.terminate()
  }

  "Slip router" must {
    "build a black car by default" in {
      val probe = TestProbe()
      val router = system.actorOf(Props(new SlipRouter(probe.ref)), "SlipRouter1")
      val minimalOrder = new Order(Seq())

      router ! minimalOrder

      val defaultCar = new Car(
        color = "black",
        hasNavigation = false,
        hasParkingSensors = false
      )

      probe.expectMsg(defaultCar)
    }

    "build the full monty" in {
      val probe = TestProbe()
      val router = system.actorOf(Props(new SlipRouter(probe.ref)), "SlipRouter2")

      val fullOrder = new Order(Seq(
        CarOptions.CAR_COLOR_GRAY,
        CarOptions.NAVIGATION,
        CarOptions.PARKING_SENSORS))

      router ! fullOrder

      val carWithAllOptions = new Car(
        color = "gray",
        hasNavigation = true,
        hasParkingSensors = true)

      probe.expectMsg(carWithAllOptions)
    }
  }
}