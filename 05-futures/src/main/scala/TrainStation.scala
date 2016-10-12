import org.joda.time.{DateTime, Duration}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TrafficServiceException(msg: String) extends Exception(msg)

object TrainStation extends App {

  case class EventRequest(ticketNr: Int)

  case class EventResponse(event: Event)

  case class Event(name: String, location: Location, time: DateTime)

  case class Location(lat: Double, lon: Double)

  case class TrafficRequest(destination: Location, arrivalTime: DateTime)

  case class TrafficResponse(route: Route)

  case class Route(route: String, timeToLeave: DateTime, origin: Location, destination: Location,
                   estimatedDuration: Duration, trafficJamTime: Duration)

  // All extra information about the ticketNr is optional and empty by default
  case class TicketInfo(ticketNr: String, event: Option[Event] = None, route: Option[Route] = None)

  // A blocking call
  def callEventService(request: EventRequest): EventResponse = ???

  // A blocking call
  def callTrafficService(request: TrafficRequest): TrafficResponse = ???

  def getEvent(ticketNr: Int): Future[TicketInfo] = {
    val request = EventRequest(ticketNr)

    Future {
      TicketInfo(ticketNr.toString, Some(callEventService(request).event))
    }
  }

  // Get traffic, but fail if there's a TrafficServiceException
  def getTraffic(ticketInfo: TicketInfo): Future[TicketInfo] = {
    if (ticketInfo.event.isEmpty) {
      Future.failed(new TrafficServiceException("No event!"))
    } else {
      val event = ticketInfo.event.get
      val trafficRequest = TrafficRequest(event.location, event.time)
      Future {
        ticketInfo.copy(route = Some(callTrafficService(trafficRequest).route))
      }
    }
  }

  // Idiomatic way of achieving the same thing
  def getTrafficIdiomatic(ticketInfo: TicketInfo): Future[TicketInfo] = {
    val theTicketInfo: Option[Future[TicketInfo]] = ticketInfo.event.map { event =>
      val trafficRequest = TrafficRequest(event.location, event.time)
      Future {
        ticketInfo.copy(route = Some(callTrafficService(trafficRequest).route))
      }
    }

    theTicketInfo.getOrElse(Future.failed(new TrafficServiceException("No event!")))
  }

  // Note, if getEvent fails, this will percolate through to result
  def getTicketInfo1(ticketNr: Int): Future[TicketInfo] = {
    val futureStep1: Future[TicketInfo] = getEvent(ticketNr)

    val futureStep2: Future[TicketInfo] = futureStep1.flatMap { ticketInfo =>
      getTraffic(ticketInfo).recover {
        // Recover with a future containing the initial TicketInfo value
        case _: TrafficServiceException => ticketInfo
      }
    }

    futureStep2
  }

  // Improved version, if getEvent fails will return empty TicketInfo
  def getTicketInfo2(ticketNr: Int): Future[TicketInfo] = {
    val futureStep1: Future[TicketInfo] = getEvent(ticketNr)

    val futureStep2: Future[TicketInfo] = futureStep1.flatMap { ticketInfo =>
      getTrafficIdiomatic(ticketInfo).recover {
        // Recover with a future containing the initial TicketInfo value
        case _: TrafficServiceException => ticketInfo
      }
    }.recover {
      // Recover from failure of step 1 with a future containing an empty TicketInfo
      case e => TicketInfo(ticketNr.toString)
    }

    futureStep2
  }

  // Main

  val ticketNr = 1

  getTicketInfo1(ticketNr)
  getTicketInfo2(ticketNr)
}