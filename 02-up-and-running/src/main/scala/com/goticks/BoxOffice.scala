package com.goticks

import akka.actor._
import akka.util.Timeout

import scala.concurrent.Future

object BoxOffice {
  def props(implicit timeout: Timeout) = Props(new BoxOffice)
  def name = "boxOffice"

  case class CreateEvent(name: String, tickets: Int)
  case class GetEvent(name: String)
  case object GetEvents
  case class GetTickets(event: String, tickets: Int)
  case class CancelEvent(name: String)

  case class Event(name: String, tickets: Int)
  case class Events(events: Vector[Event])

  sealed trait EventResponse
  case class EventCreated(event: Event) extends EventResponse
  case object EventExists extends EventResponse
}

class BoxOffice(implicit timeout: Timeout) extends Actor {
  import BoxOffice._
  import context._

  def createTicketSeller(name: String) =
    context.actorOf(TicketSeller.props(name), name)

  def receive: PartialFunction[Any, Unit] = {
    case CreateEvent(name, tickets) =>
      def create() = {
        val ticketSeller = createTicketSeller(name)
        val newTickets = (1 to tickets).map { ticketId =>
          TicketSeller.Ticket(ticketId)
        }.toVector
        ticketSeller ! TicketSeller.Add(newTickets)
        sender() ! EventCreated(Event(name, tickets))
      }
      context.child(name).fold(create())(_ => sender() ! EventExists)

    case GetTickets(event, tickets) =>
      def notFound() = sender() ! TicketSeller.Tickets(event)

      def buy(child: ActorRef) =
        child.forward(TicketSeller.Buy(tickets))

      context.child(event).fold(notFound())(buy)

    case GetEvent(event) =>
      def notFound() = sender() ! None
      def getEvent(child: ActorRef) = child forward TicketSeller.GetEvent
      context.child(event).fold(notFound())(getEvent)

    case GetEvents =>
      import akka.pattern.{ask, pipe}

      def getEvents: Iterable[Future[Option[Event]]] = context.children.map { child =>
        val fa: Future[Any] = self.ask(GetEvent(child.path.name))
        val foe: Future[Option[Event]] = fa.mapTo[Option[Event]]
        foe
      }

      def convertToEvents(f: Future[Iterable[Option[Event]]]): Future[Events] = {
        val fies: Future[Iterable[Event]] = f.map(_.flatten)
        fies.map(itEvents => Events(itEvents.toVector))
      }

      // Start here
      val ifoe: Iterable[Future[Option[Event]]] = getEvents
      val fioe: Future[Iterable[Option[Event]]] = Future.sequence(ifoe)
      val futureEvents: Future[Events] = convertToEvents(fioe)

      pipe(futureEvents) to sender()

    case CancelEvent(event) =>
      def notFound() = sender() ! None
      def cancelEvent(child: ActorRef) = child forward TicketSeller.Cancel
      context.child(event).fold(notFound())(cancelEvent)
  }
}

