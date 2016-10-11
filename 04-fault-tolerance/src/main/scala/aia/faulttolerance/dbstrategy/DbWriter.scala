package aia.faulttolerance.dbstrategy

import akka.actor.{Actor, Props}

class DbWriter(databaseUrl: String) extends Actor {
  val connection = new DbCon(databaseUrl)

  import DbWriter._

  def receive = {
    case Line(time, message, messageType) =>
      connection.write(Map('time -> time, 'message -> message, 'messageType -> messageType))
  }

  // close connection if actor crashes or stops
  override def postStop(): Unit = {
    connection.close()
  }
}

object DbWriter {
  def props(databaseUrl: String) = Props(new DbWriter(databaseUrl))

  // human-readable name
  def name(databaseUrl: String) = s"""db-writer-${databaseUrl.split("/").last}"""

  // A line in the log file parsed by the LogProcessor Actor, to be written to the database
  case class Line(time: Long, message: String, messageType: String)

}

class DbCon(url: String) {
  /**
    * Writes a map to a database.
    *
    * @param map the map to write to the database.
    * @throws DbBrokenConnectionException when the connection is broken. It might be back later
    * @throws DbNodeDownException         when the database Node has been removed from the database cluster.
    *                                     It will never work again.
    */
  def write(map: Map[Symbol, Any]): Unit = ???

  def close(): Unit = ???
}