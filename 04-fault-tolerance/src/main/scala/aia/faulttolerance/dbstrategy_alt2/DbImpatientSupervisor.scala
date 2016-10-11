package aia.faulttolerance.dbstrategy_alt2

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}
import concurrent.duration._

class DbImpatientSupervisor(writerProps: Props) extends Actor {
  override def supervisorStrategy = OneForOneStrategy(
    maxNrOfRetries = 5,
    withinTimeRange = 60 seconds) {
    case _: DbBrokenConnectionException => Restart
  }

  val writer = context.actorOf(writerProps)

  def receive = {
    case m => writer forward (m)
  }
}