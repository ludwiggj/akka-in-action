package aia.faulttolerance.dbstrategy_alt2

import java.io.File

import akka.actor.{Actor, ActorRef, Props}

trait LogParsing {

  import LogProcessingProtocol._

  // Parses log files. creates line objects from the lines in the log file.
  // If the file is corrupt a CorruptedFileException is thrown
  def parse(file: File): Vector[Line] = {
    // implement parser here, now just return dummy value
    Vector.empty[Line]
  }
}

class LogProcessor(dbSupervisor: ActorRef) extends Actor with LogParsing {

  import LogProcessingProtocol._

  def receive = {
    case LogFile(file) =>
      val lines = parse(file)
      lines.foreach(dbSupervisor ! _)
  }
}

object LogProcessor {
  def props(dbSupervisor: ActorRef) = Props(new LogProcessor(dbSupervisor))
}