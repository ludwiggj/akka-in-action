package com.goticks

import com.github.nscala_time.time.DurationBuilder
import com.goticks.FutureCallType._
import org.scalatest.MustMatchers
import org.scalatest.WordSpec

import scala.concurrent.{Await, Future}

class GetTicketInfoSpec extends WordSpec with MustMatchers {

  object TicketInfoService extends TicketInfoService with MockWebServiceCalls {
  }

  object TicketInfoServiceWithDelays extends TicketInfoService with MockWebServiceCalls {

    import com.github.nscala_time.time.Imports._

    override val delays: Map[FutureCallType, DurationBuilder] =
      Map(
        EVENT -> 0.seconds,
        WEATHER_X -> 3.seconds,
        WEATHER_Y -> 14.seconds,
        TRAFFIC_SERVICE -> 3.seconds,
        PUBLIC_TRANSPORT_SERVICE -> 8.seconds,
        SIMILAR_ARTISTS_SERVICE -> 1.seconds,
        ARTIST_CALENDAR_SERVICE -> 20.millis
      )
  }

  import scala.concurrent.duration._

  "getTicketInfo" must {
    "return a complete ticket info when all futures are successful" in {
      import TicketInfoServiceWithDelays._
      val ticketInfo = Await.result(getTicketInfoTravelAndWeatherFirst("1234", Location(1d, 2d)), 10.seconds)
//      val ticketInfo = Await.result(getTicketInfoEventSuggestionsFirstTake1("1234", Location(1d, 2d)), 10.seconds)
//      val ticketInfo = Await.result(getTicketInfoEventSuggestionsFirstTake2("1234", Location(1d, 2d)), 10.seconds)

      ticketInfo.event.isEmpty must be(false)
      ticketInfo.event.foreach(event => event.name must be("Quasimoto"))
      ticketInfo.travelAdvice.isEmpty must be(false)
      ticketInfo.weather.isEmpty must be(false)
      ticketInfo.suggestions.map(_.name) must be(Seq("Madlib", "OhNo", "Flying Lotus"))
    }

    "return an incomplete ticket info when getEvent fails" in {
      import TicketInfoService._

      val ticketInfo = Await.result(getTicketInfoTravelAndWeatherFirst("4321", Location(1d, 2d)), 10.seconds)

      ticketInfo.event.isEmpty must be(true)
      ticketInfo.travelAdvice.isEmpty must be(true)
      ticketInfo.weather.isEmpty must be(false)
      ticketInfo.suggestions.isEmpty must be(true)
    }
  }
}

trait FutureDelays {

  import com.github.nscala_time.time.Imports._
  import org.joda.time.Duration

  val delays: Map[FutureCallType, DurationBuilder] = Map()

  def getDelay(futureCallType: FutureCallType): Duration = {
    delays.getOrElse(futureCallType, 0.millis).toDuration
  }
}

trait MockWebServiceCalls extends WebServiceCalls with FutureDelays {

  import com.github.nscala_time.time.Imports._
  import scala.concurrent.ExecutionContext.Implicits.global

  def sleep(futureCall: FutureCallType, ticketNr: String) = {
    val millis = getDelay(futureCall).getMillis
    println(s"[ticketNr: $ticketNr] => $futureCall SLEEPING for $millis")
    Thread.sleep(millis)
    println(s"[ticketNr: $ticketNr] => $futureCall WAKING")
  }

  def getEvent(ticketNr: String, location: Location): Future[TicketInfo] = {
    println(s"[ticketNr: $ticketNr] => getEvent")
    Future {
      sleep(EVENT, ticketNr)
      if (ticketNr == "1234") {
        TicketInfo(ticketNr, location, event =
          Some(Event("Quasimoto", Location(4.324218908d, 53.12311144d), new DateTime(2013, 10, 1, 22, 30))))
      } else throw new Exception("crap")
    }
  }

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]] = {
    val ticketNr = ticketInfo.ticketNr
    println(s"[ticketNr: ${ticketNr}] => callWeatherXService")
    Future {
      sleep(WEATHER_X, ticketNr)
      Some(Weather(30, false))
    }
  }

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]] = {
    val ticketNr = ticketInfo.ticketNr
    println(s"[ticketNr: ${ticketNr}] => callWeatherYService")
    Future {
      sleep(WEATHER_Y, ticketNr)
      Some(Weather(30, false))
    }
  }

  def callTrafficService(origin: Location, destination: Location, time: DateTime, ticketNr: String): Future[Option[RouteByCar]] = {
    println(s"[ticketNr: $ticketNr] => callTrafficService")
    Future {
      sleep(TRAFFIC_SERVICE, ticketNr)
      Some(RouteByCar("route1", time - (35.minutes), origin, destination, 30.minutes, 5.minutes))
    }
  }

  def callPublicTransportService(origin: Location, destination: Location, time: DateTime, ticketNr: String): Future[Option[PublicTransportAdvice]] = {
    println(s"[ticketNr: $ticketNr] => callPublicTransportService")
    Future {
      sleep(PUBLIC_TRANSPORT_SERVICE, ticketNr)
      Some(PublicTransportAdvice("public transport route 1", time - (20.minutes), origin, destination, 20.minutes))
    }
  }

  def callSimilarArtistsService(event: Event, ticketNr: String): Future[Seq[Artist]] = {
    println(s"[ticketNr: $ticketNr] => callSimilarArtistsService")
    Future {
      sleep(SIMILAR_ARTISTS_SERVICE, ticketNr)
      Seq(Artist("Madlib", "madlib.com/calendar"),
        Artist("OhNo", "ohno.com/calendar"),
        Artist("Flying Lotus", "fly.lo/calendar")
      )
    }
  }

  def callArtistCalendarService(artist: Artist, nearLocation: Location, ticketNr: String): Future[Event] = {
    println(s"[ticketNr: $ticketNr] => callArtistCalendarService")
    Future {
      sleep(ARTIST_CALENDAR_SERVICE, ticketNr)
      Event(artist.name, Location(1d, 1d), DateTime.now)
    }
  }
}