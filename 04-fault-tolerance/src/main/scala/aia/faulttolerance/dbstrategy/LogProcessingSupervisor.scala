package aia.faulttolerance.dbstrategy

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorRef, AllForOneStrategy, Props, Terminated}

class LogProcessingSupervisor(sources: Vector[String], databaseUrls: Vector[String]) extends Actor with ActorLogging {

  var fileWatchers: Vector[ActorRef] = sources.map { source =>
    val fileWatcher = context.actorOf(FileWatcher.props(source, databaseUrls))
    context.watch(fileWatcher)
    fileWatcher
  }

  override def supervisorStrategy = AllForOneStrategy() {
    // Stop all FileWatcher's on disk error
    // The LogProcessor and DbWriter child actors will also all be stopped
    case _: DiskError => Stop
  }

  def receive = {
    // Remove terminated FileWatcher
    // If there are none left, terminate the whole actor system
    case Terminated(fileWatcher) =>
      fileWatchers = fileWatchers.filterNot(_ == fileWatcher)
      if (fileWatchers.isEmpty) {
        log.info("Shutting down, all file watchers have failed.")
        context.system.terminate()
      }
  }
}

object LogProcessingSupervisor {
  def props(sources: Vector[String], databaseUrls: Vector[String]) =
    Props(new LogProcessingSupervisor(sources, databaseUrls))

  def name = "file-watcher-supervisor"
}