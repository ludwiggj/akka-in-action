package aia.faulttolerance.dbstrategy_alt1

import java.io.File
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

trait LogParsing {

  import DbWriter._

  // Parses log files. creates line objects from the lines in the log file.
  // If the file is corrupt a CorruptedFileException is thrown
  def parse(file: File): Vector[Line] = {
    // implement parser here, now just return dummy value
    Vector.empty[Line]
  }
}

class LogProcessor(dbWriter: ActorRef) extends Actor with ActorLogging with LogParsing {

  import LogProcessor._

  def receive = {
    case LogFile(file) =>
      val lines: Vector[DbWriter.Line] = parse(file)
      lines.foreach(dbWriter ! _)
  }
}

object LogProcessor {
  def props(dbWriter: ActorRef) = Props(new LogProcessor(dbWriter))

  def name = s"log_processor_${UUID.randomUUID.toString}"

  // represents a new log file
  case class LogFile(file: File)

}