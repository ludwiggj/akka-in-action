package aia.faulttolerance.dbstrategy_alt1

import java.io.File
import java.util.UUID

import akka.actor.{Actor, ActorRef, PoisonPill, Props}


trait FileWatchingAbilities {
  def register(uri: String): Unit = ???
}

class FileWatcher(source: String, logProcessor: ActorRef) extends Actor with FileWatchingAbilities {
  register(source)

  import FileWatcher._

  def receive = {
    case NewFile(file, _) =>
      logProcessor ! LogProcessor.LogFile(file)

    case SourceAbandoned(uri) if uri == source =>
      self ! PoisonPill
  }
}

object FileWatcher {
  def props(source: String, logProcessor: ActorRef) =
    Props(new FileWatcher(source, logProcessor))

  def name = s"file-watcher-${UUID.randomUUID.toString}"

  case class NewFile(file: File, timeAdded: Long)

  case class SourceAbandoned(uri: String)
}