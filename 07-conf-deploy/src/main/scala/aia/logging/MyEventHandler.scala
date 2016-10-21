package aia.logging

import akka.actor.Actor
import akka.event.Logging.InitializeLogger
import akka.event.Logging.LoggerInitialized
import akka.event.Logging.Error
import akka.event.Logging.Warning
import akka.event.Logging.Info
import akka.event.Logging.Debug

class MyEventListener extends Actor {

  def receive = {
    case InitializeLogger(_) =>
      sender ! LoggerInitialized
    case Error(cause, logSource, logClass, message) =>
      println("ERROR > " + message)
    case Warning(logSource, logClass, message) =>
      println("WARN >> " + message)
    case Info(logSource, logClass, message) =>
      println("INFO >> " + message)
    case Debug(logSource, logClass, message) =>
      println("DEBUG > " + message)
  }
}