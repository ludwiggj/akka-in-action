package aia.faulttolerance.dbstrategy

import java.io.File

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, PoisonPill, Props, Terminated}

// A file watching API (it's magic!)
trait FileWatchingAbilities {
  def register(uri: String): Unit = ???
}

class FileWatcher(source: String, databaseUrls: Vector[String]) extends Actor with ActorLogging with FileWatchingAbilities {
  register(source)

  override def supervisorStrategy = OneForOneStrategy() {
    case _: CorruptedFileException => Resume // carry on if corrupt file detected
  }

  // create and watch LogProcessor
  val logProcessor = context.actorOf(LogProcessor.props(databaseUrls), LogProcessor.name)
  context.watch(logProcessor)

  import FileWatcher._

  def receive = {
    case NewFile(file, _) =>
      // Sent by file-watching API when a new file is encountered
      logProcessor ! LogProcessor.LogFile(file)
    case SourceAbandoned(uri) if uri == source =>
      // FileWatcher kills itself when source has been abandoned, indicating to file-watching API
      // not to expect any more files from the source

      // NOTE: Code doesn't show where this message is sent from...
      log.info(s"$uri abandoned, stopping file watcher.")
      self ! PoisonPill
    case Terminated(`logProcessor`) =>
      // FileWatcher stops when LogProcessor has stopped, because DbWriter has exhausted the database alternatives
      log.info(s"Log processor terminated, stopping file watcher.")
      self ! PoisonPill
  }
}

object FileWatcher {

  case class NewFile(file: File, timeAdded: Long)

  case class SourceAbandoned(uri: String)

  def props(source: String, databaseUrls: Vector[String]) = Props(new FileWatcher(source, databaseUrls))
}