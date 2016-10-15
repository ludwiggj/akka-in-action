package com.goticks

import akka.actor.{ActorIdentity, Identify, _}

import scala.concurrent.duration._

class RemoteLookupProxy(path: String) extends Actor with ActorLogging {

  context.setReceiveTimeout(3 seconds)
  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    log.info("identify: sendIdentifyRequest")
    val selection: ActorSelection = context.actorSelection(path)
    selection ! Identify(path)
  }

  def receive = identify

  def identify: Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      context.setReceiveTimeout(Duration.Undefined)
      log.info("identify: switching to active state")
      context.become(active(actor))
      context.watch(actor)

    case ActorIdentity(`path`, None) =>
      log.error(s"identify: remote actor with path $path is not available.")

    case ReceiveTimeout =>
      sendIdentifyRequest()

    case msg: Any =>
      log.error(s"identify: ignoring message $msg, remote actor is not ready yet.")
      // Try to send message again
      Thread.sleep((1 seconds).toMillis)
      log.info(s"identify: try again with message $msg")
      self forward msg

  }

  def active(actor: ActorRef): Receive = {
    case Terminated(actorRef) =>
      log.info("active: actor $actorRef terminated.")
      log.info("active: switching to identify state")
      context.become(identify)
      context.setReceiveTimeout(3 seconds)
      sendIdentifyRequest()

    case msg: Any =>
      log.info(s"active: got message $msg")
      actor forward msg
      log.info(s"active: forwarded message $msg")
  }
}
