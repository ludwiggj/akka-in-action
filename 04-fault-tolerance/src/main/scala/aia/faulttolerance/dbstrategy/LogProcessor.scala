package aia.faulttolerance.dbstrategy

import java.io.File
import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, PoisonPill, Props, Terminated}

trait LogParsing {

  import DbWriter._

  // Parses log files. creates line objects from the lines in the log file.
  // If the file is corrupt a CorruptedFileException is thrown
  def parse(file: File): Vector[Line] = {
    // implement parser here, now just return dummy value
    Vector.empty[Line]
  }
}

class LogProcessor(databaseUrls: Vector[String]) extends Actor with ActorLogging with LogParsing {
  require(databaseUrls.nonEmpty)

  val initialDatabaseUrl = databaseUrls.head
  var alternateDatabases = databaseUrls.tail

  override def supervisorStrategy = OneForOneStrategy() {
    case _: DbBrokenConnectionException => Restart // Retrying a connection might work
    case _: DbNodeDownException => Stop
  }

  var dbWriter = context.actorOf(DbWriter.props(initialDatabaseUrl), DbWriter.name(initialDatabaseUrl))

  // Watch the dbWriter; see below for why...
  context.watch(dbWriter)

  import LogProcessor._

  def receive = {
    case LogFile(file) =>
      val lines: Vector[DbWriter.Line] = parse(file)
      lines.foreach(dbWriter ! _)
    case Terminated(_) =>

      // If dbWriter is terminated, create a new dbWriter from the next alternative URL and watch it
      if (alternateDatabases.nonEmpty) {
        val newDatabaseUrl = alternateDatabases.head
        alternateDatabases = alternateDatabases.tail

        // create new dbWriter and watch it
        dbWriter = context.actorOf(DbWriter.props(newDatabaseUrl), DbWriter.name(newDatabaseUrl))
        context.watch(dbWriter)
      } else {
        log.error("All Db nodes broken, stopping.")
        self ! PoisonPill
      }
  }
}

object LogProcessor {
  def props(databaseUrls: Vector[String]) = Props(new LogProcessor(databaseUrls))

  // Every LogProcessor gets a unique name
  def name = s"log_processor_${UUID.randomUUID.toString}"

  // represents a new log file
  case class LogFile(file: File)

}