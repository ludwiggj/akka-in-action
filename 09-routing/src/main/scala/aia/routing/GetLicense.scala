package aia.routing

import aia.routing.messages.{PerformanceRoutingMessage, SetService}
import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

class GetLicense(pipe: ActorRef, initialServiceTime: FiniteDuration = 0 millis) extends Actor {
  var id = self.path.name
  var serviceTime = initialServiceTime

  def receive = {
    case init: SetService => {
      println(s"[$id] got message [$init]")
      id = init.id
      serviceTime = init.serviceTime
      Thread.sleep(100)
    }

    case msg: PerformanceRoutingMessage => {
      println(s"[$id] got message [$msg]")
      Thread.sleep(serviceTime.toMillis)
      pipe ! msg.copy(
        license = ImageProcessing.getLicense(msg.photo),
        processedBy = Some(id)
      )
    }

    case msg: AnyRef => {
      println(s"[$id] got unrecognised [$msg]. Right back atcha!")
      pipe ! msg
    }
  }

  override def postStop(): Unit = {
    println(s"Stopped [$id]")
  }
}