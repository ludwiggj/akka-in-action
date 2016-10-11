package aia.faulttolerance.dbstrategy_alt1

package aia.faulttolerance.dbstrategy_alt1

import akka.actor._

import scala.language.postfixOps

object LogProcessingApp extends App {
  val sources = Vector("file:///source1/", "file:///source2/")
  val system = ActorSystem("logprocessing")

  val databaseUrl = "http://mydatabase1"

  system.actorOf(LogProcessingSupervisor.props(sources, databaseUrl), LogProcessingSupervisor.name)
}