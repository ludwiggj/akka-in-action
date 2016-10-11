package aia.faulttolerance.dbstrategy_alt2

import java.io.File

object LogProcessingProtocol {

  // represents a new log file
  case class LogFile(file: File)

  // A line in the log file parsed by the LogProcessor Actor
  case class Line(time: Long, message: String, messageType: String)

}