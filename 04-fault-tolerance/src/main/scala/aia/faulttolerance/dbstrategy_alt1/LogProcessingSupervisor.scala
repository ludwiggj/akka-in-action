package aia.faulttolerance.dbstrategy_alt1

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, Terminated}

class LogProcessingSupervisor(sources: Vector[String], databaseUrl: String) extends Actor with ActorLogging {

  override def supervisorStrategy = OneForOneStrategy() {
    case _: CorruptedFileException => Resume
    case _: DbBrokenConnectionException => Restart
    case _: DiskError => Stop
  }

  var fileWatchers = sources.map { source =>
    val dbWriter = context.actorOf(DbWriter.props(databaseUrl), DbWriter.name(databaseUrl))

    val logProcessor = context.actorOf(LogProcessor.props(dbWriter), LogProcessor.name)

    val fileWatcher = context.actorOf(FileWatcher.props(source, logProcessor), FileWatcher.name)

    context.watch(fileWatcher)

    fileWatcher
  }


  def receive = {
    case Terminated(actorRef) =>
      if (fileWatchers.contains(actorRef)) {
        fileWatchers = fileWatchers.filterNot(_ == actorRef)
        if (fileWatchers.isEmpty) {
          log.info("Shutting down, all file watchers have failed.")
          context.system.terminate()
        }
      }
  }
}

object LogProcessingSupervisor {
  def props(sources: Vector[String], databaseUrl: String) =
    Props(new LogProcessingSupervisor(sources, databaseUrl))

  def name = "file-watcher-supervisor"
}