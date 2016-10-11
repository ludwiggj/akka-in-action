package aia.faulttolerance.dbstrategy_alt2

import java.io.File

import akka.actor.{Actor, ActorRef, PoisonPill, Props}

object FileWatcherProtocol {

  case class NewFile(file: File, timeAdded: Long)

  case class SourceAbandoned(uri: String)

}

trait FileWatchingAbilities {
  def register(uri: String): Unit = ???
}

class FileWatcher(sourceUri: String, logProcSupervisor: ActorRef) extends Actor with FileWatchingAbilities {
  register(sourceUri)

  import FileWatcherProtocol._
  import LogProcessingProtocol._

  def receive = {
    case NewFile(file, _) =>
      logProcSupervisor ! LogFile(file)
    case SourceAbandoned(uri) if uri == sourceUri =>
      self ! PoisonPill
  }
}

object FileWatcher {
  def props(source: String, logProcSupervisor: ActorRef) =
    Props(new FileWatcher(source, logProcSupervisor))
}