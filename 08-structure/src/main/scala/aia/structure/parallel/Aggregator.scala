package aia.structure.parallel

import akka.actor.{Actor, ActorRef}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.FiniteDuration

class Aggregator(timeout: FiniteDuration, pipe: ActorRef) extends Actor {

  val messages = new ListBuffer[PhotoMessage]
  implicit val ec = context.system.dispatcher

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // Sends all received messages to our own mailbox
    super.preRestart(reason, message)
    messages.foreach(self ! _)
    messages.clear()
  }

  def receive = {
    case rcvMsg: PhotoMessage => {
      messages.find(_.id == rcvMsg.id) match {
        case Some(alreadyRcvMsg) => {
          val newCombinedMsg = new PhotoMessage(rcvMsg.id, rcvMsg.photo,
            rcvMsg.creationTime.orElse(alreadyRcvMsg.creationTime),
            rcvMsg.speed.orElse(alreadyRcvMsg.speed))
          pipe ! newCombinedMsg

          // cleanup message
          messages -= alreadyRcvMsg
        }

        case None => {
          messages += rcvMsg

          // Send a timeout message to handle case where second message isn't received
          context.system.scheduler.scheduleOnce(timeout, self, new TimeoutMessage(rcvMsg))
        }
      }
    }

    case TimeoutMessage(rcvMsg) => {
      println(s"Received TimeoutMessage: $rcvMsg")
      messages.find(_.id == rcvMsg.id) match {
        case Some(alreadyRcvMsg) => {
          println(s"TimeoutMessage LOCATED: $rcvMsg")
          pipe ! alreadyRcvMsg
          messages -= alreadyRcvMsg
        }
        case None =>
          // message is already processed
          println(s"TimeoutMessage NOT LOCATED: $rcvMsg")
      }
    }

    // Send this Actor an exception to cause it to restart
    case ex: Exception =>
      throw ex
  }
}