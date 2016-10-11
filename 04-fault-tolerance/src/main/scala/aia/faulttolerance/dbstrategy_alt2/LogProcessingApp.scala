package aia.faulttolerance.dbstrategy_alt2

import akka.actor._

import scala.language.postfixOps

object LogProcessingApp extends App {
  val sources = Vector("file:///source1/", "file:///source2/")
  val databaseUrl = "http://mydatabase"

  val system = ActorSystem("logprocessing")

  // create the props and dependencies

  val writerProps = DbWriter.props(databaseUrl)

  val dbSuperProps = DbSupervisor.props(writerProps)

  val logProcSuperProps = LogProcSupervisor.props(dbSuperProps)

  val topLevelProps = FileWatcherSupervisor.props(sources, logProcSuperProps)

  system.actorOf(topLevelProps)
}