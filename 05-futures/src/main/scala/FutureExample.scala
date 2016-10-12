import FutureExample._
import org.joda.time.{DateTime, Duration}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object FutureExample {

  case class EventRequest(ticketNr: Int)

  case class EventResponse(event: Event)

  case class Event(name: String, location: Location, time: DateTime)

  case class Location(lat: Double, lon: Double)

  case class TrafficRequest(destination: Location, arrivalTime: DateTime)

  case class TrafficResponse(route: Route)

  case class Route(route: String, timeToLeave: DateTime, origin: Location, destination: Location,
                   estimatedDuration: Duration, trafficJamTime: Duration)

}

class FutureExample {

  // A blocking call
  def callEventService(request: EventRequest): EventResponse = ???

  // A blocking call
  def callTrafficService(request: TrafficRequest): TrafficResponse = ???

  def getEventSynchronous(ticketNr: Int): Event = {
    val request = EventRequest(ticketNr)

    val response = callEventService(request)

    response.event
  }

  def getEventAsynchronous(ticketNr: Int): Future[Event] = {
    val request = EventRequest(ticketNr)

    Future {
      val response = callEventService(request)

      response.event
    }
  }

  def getAndDisplayRoute(ticketNr: Int): Unit = {
    val futureEvent = getEventAsynchronous(ticketNr)

    futureEvent.foreach { event =>
      // Asynchronously processes the event result when it becomes available
      val trafficRequest = TrafficRequest(event.location, event.time)

      // Call the traffic service synchronously with a request based on the event,
      val trafficResponse = callTrafficService(trafficRequest)

      println(trafficResponse.route)
    }
  }

  def getRouteTake1(ticketNr: Int): Future[Route] = {
    val futureEvent = getEventAsynchronous(ticketNr)

    val futureRoute: Future[Route] = futureEvent.map { event =>
      val trafficRequest = TrafficRequest(event.location, event.time)

      // Still calling the callTrafficService synchronously
      val trafficResponse = callTrafficService(trafficRequest)

      trafficResponse.route
    }

    futureRoute
  }

  // Exactly the same code as getRouteTake1, just run in together
  def getRouteChained(ticketNr: Int): Future[Route] = {

    val request = EventRequest(ticketNr)

    val futureRoute: Future[Route] = Future {
      callEventService(request).event
    }.map { event =>
      val trafficRequest = TrafficRequest(event.location, event.time)

      callTrafficService(trafficRequest).route
    }

    futureRoute
  }

  // Now refactor into getEvent: Future[Event]
  def getEvent(ticketNr: Int): Future[Event] = {
    val request = EventRequest(ticketNr)

    Future {
      callEventService(request).event
    }
  }

  // and getRoute: Future[Route]
  // This now takes an event, and is now also an asynchronous call
  def getRoute(event: Event): Future[Route] = {
    val trafficRequest = TrafficRequest(event.location, event.time)

    Future {
      callTrafficService(trafficRequest).route
    }
  }

  // Now chain the calls together
  val ticketNr = 1

  val futureFutureRoute: Future[Future[Route]] = getEvent(ticketNr).map { event =>
    getRoute(event)
  }

  val futureRoute: Future[Route] = getEvent(ticketNr).flatMap { event =>
    getRoute(event)
  }
}