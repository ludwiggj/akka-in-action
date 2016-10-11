package aia.faulttolerance.dbstrategy_alt2

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, AllForOneStrategy, PoisonPill, Props, Terminated}

class FileWatcherSupervisor(sources: Vector[String], logProcSuperProps: Props) extends Actor {

  var fileWatchers: Vector[ActorRef] = sources.map { source =>
    val logProcSupervisor = context.actorOf(logProcSuperProps)
    val fileWatcher = context.actorOf(FileWatcher.props(source, logProcSupervisor))
    context.watch(fileWatcher)
    fileWatcher
  }

  override def supervisorStrategy = AllForOneStrategy() {
    case _: DiskError => Stop
  }

  def receive = {
    case Terminated(fileWatcher) =>
      fileWatchers = fileWatchers.filterNot(w => w == fileWatcher)
      if (fileWatchers.isEmpty) self ! PoisonPill
  }
}

object FileWatcherSupervisor {
  def props(sources: Vector[String], logProcSuperProps: Props) =
    Props(new FileWatcherSupervisor(sources, logProcSuperProps))
}
