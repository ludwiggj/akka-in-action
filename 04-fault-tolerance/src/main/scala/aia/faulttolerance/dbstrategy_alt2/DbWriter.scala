package aia.faulttolerance.dbstrategy_alt2

import aia.faulttolerance.dbstrategy.DbNodeDownException
import akka.actor.{Actor, Props}

class DbWriter(databaseUrl: String) extends Actor {
  val connection = new DbCon(databaseUrl)

  import LogProcessingProtocol._

  def receive = {
    case Line(time, message, messageType) =>
      connection.write(Map('time -> time, 'message -> message,'messageType -> messageType))
  }
}

object DbWriter {
  def props(databaseUrl: String) = Props(new DbWriter(databaseUrl))
}

class DbCon(url: String) {
  /**
    * Writes a map to a database.
    *
    * @param map the map to write to the database.
    * @throws DbBrokenConnectionException when the connection is broken. It might be back later
    * @throws DbNodeDownException         when the database Node has been removed from the database cluster. It will never work again.
    */
  def write(map: Map[Symbol, Any]): Unit = ???

  def close(): Unit = ???
}