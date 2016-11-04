package aia.routing.messages

import scala.concurrent.duration.FiniteDuration

case class SetService(id: String, serviceTime: FiniteDuration)
