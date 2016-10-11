package aia.faulttolerance.dbstrategy_alt2

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, OneForOneStrategy, Props}

class LogProcSupervisor(dbSupervisorProps: Props) extends Actor {
  override def supervisorStrategy = OneForOneStrategy() {
    case _: CorruptedFileException => Resume
  }

  val dbSupervisor = context.actorOf(dbSupervisorProps)

  val logProcessor = context.actorOf(LogProcessor.props(dbSupervisor))

  def receive = {
    case m => logProcessor forward (m)
  }
}

object LogProcSupervisor {
  def props(dbSuperProps: Props) = Props(new LogProcSupervisor(dbSuperProps))
}
