package aia.faulttolerance.dbstrategy_alt2

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, OneForOneStrategy, Props}

class DbSupervisor(writerProps: Props) extends Actor {
  override def supervisorStrategy = OneForOneStrategy() {
    case _: DbBrokenConnectionException => Restart
  }

  val writer = context.actorOf(writerProps)

  def receive = {
    case m => writer forward (m)
  }
}

object DbSupervisor {
  def props(writerProps: Props) = Props(new DbSupervisor(writerProps))
}