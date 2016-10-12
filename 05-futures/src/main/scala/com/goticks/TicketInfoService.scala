package com.goticks

import scala.concurrent.Future
import com.github.nscala_time.time.Imports._
import scala.util.control.NonFatal

// what about timeout? or at least termination condition?
// future -> actors scheduling time
trait TicketInfoService extends WebServiceCalls {

  import scala.concurrent.ExecutionContext.Implicits.global

  type Recovery[T] = PartialFunction[Throwable, T]

  // recover with None
  def withNone[T]: Recovery[Option[T]] = {
    case NonFatal(e) => None
  }

  // recover with empty sequence
  def withEmptySeq[T]: Recovery[Seq[T]] = {
    case NonFatal(e) => Seq()
  }

  // recover with the ticketInfo that was built in the previous step
  def withPrevious(previous: TicketInfo): Recovery[TicketInfo] = {
    case NonFatal(e) => previous
  }

  def getTicketInfoTravelAndWeatherFirst(ticketNr: String, location: Location): Future[TicketInfo] = {

    val emptyTicketInfo: TicketInfo = TicketInfo(ticketNr, location)

    // Future (1), get event
    val infoWithEvent_Future: Future[TicketInfo] = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    infoWithEvent_Future.flatMap { infoWithEvent =>

      println(s"[ticketNr: $ticketNr] => Got the event")

      // Note that infoWithWeather and infoWithTravelAdvice are independent, with just their
      // specific parts of TicketInfo filled in

      // Future (2), get weather info
      val infoWithWeather_Future: Future[TicketInfo] = getWeather(infoWithEvent)

      // Future (3), get travel advice
      val infoWithTravelAdvice_Future: Future[TicketInfo] = infoWithEvent.event.map { event =>
        getTravelAdvice(infoWithEvent, event)
      }.getOrElse(infoWithEvent_Future)

      // Future (4), get suggested events
      val suggestedEvents_Future: Future[Seq[Event]] = infoWithEvent.event.map { event =>
        getSuggestions(event, ticketNr)
      }.getOrElse(Future.successful(Seq()))

      // Combined weather and travel into one
      val ticketInfos_Future: Seq[Future[TicketInfo]] = Seq(infoWithTravelAdvice_Future, infoWithWeather_Future)

      // NOTE: The fold is performed on the thread where the last future is completed. Implies that this will not
      //       execute until all of the futures have completed
      val infoWithTravelAndWeather_Future: Future[TicketInfo] = Future.fold(ticketInfos_Future)(infoWithEvent) { (acc, elem) =>
        println(s"[ticketNr: $ticketNr] => !!! Combining travel and weather !!!")
        val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)

        // Copy over values which are present, or persist previous value
        acc.copy(
          travelAdvice = travelAdvice.orElse(acc.travelAdvice),
          weather = weather.orElse(acc.weather)
        )
      }

      // Now add in suggested events
      for (info <- infoWithTravelAndWeather_Future;
           dummy = println(s"[ticketNr: $ticketNr] => !!! Adding suggestions !!!");
           suggestions <- suggestedEvents_Future
      ) yield info.copy(suggestions = suggestions)
    }
  }

  def getTicketInfoEventSuggestionsFirstTake1(ticketNr: String, location: Location): Future[TicketInfo] = {

    val emptyTicketInfo: TicketInfo = TicketInfo(ticketNr, location)

    // Future (1), get event
    val infoWithEvent_Future: Future[TicketInfo] = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    infoWithEvent_Future.flatMap { infoWithEvent =>

      println(s"[ticketNr: $ticketNr] => Got the event")

      // Get suggested events

      // Future (4), get suggested events
      val suggestedEvents_Future: Future[Seq[Event]] = infoWithEvent.event.map { event =>
        getSuggestions(event, ticketNr)
      }.getOrElse(Future.successful(Seq()))

      // And process them straight away
      val infoWithSuggestedEvents_Future = for (
        info <- Future.successful(infoWithEvent);
        suggestions <- suggestedEvents_Future;
        dummy = println(s"[ticketNr: $ticketNr] => !!! Adding suggestions !!!")
      ) yield info.copy(suggestions = suggestions)

      // Future (2), get weather info
      val infoWithWeather_Future: Future[TicketInfo] = getWeather(infoWithEvent)

      // Future (3), get travel advice
      val infoWithTravelAdvice_Future: Future[TicketInfo] = infoWithEvent.event.map { event =>
        getTravelAdvice(infoWithEvent, event)
      }.getOrElse(infoWithEvent_Future)

      // Combined weather and travel into one
      val ticketInfos_Future: Seq[Future[TicketInfo]] = Seq(infoWithTravelAdvice_Future, infoWithWeather_Future)

      // NOTE: The fold is performed on the thread where the last future is completed. Implies that this will not
      //       execute until all of the futures have completed
      infoWithSuggestedEvents_Future.flatMap {
        Future.fold(ticketInfos_Future)(_) { (acc, elem) =>
          println(s"[ticketNr: $ticketNr] => !!! Combining travel and weather !!!")
          val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)

          // Copy over values which are present, or persist previous value
          acc.copy(
            travelAdvice = travelAdvice.orElse(acc.travelAdvice),
            weather = weather.orElse(acc.weather)
          )
        }
      }
    }
  }

  def getTicketInfoEventSuggestionsFirstTake2(ticketNr: String, location: Location): Future[TicketInfo] = {

    val emptyTicketInfo: TicketInfo = TicketInfo(ticketNr, location)

    // Future (1), get event
    val infoWithEvent_Future: Future[TicketInfo] = getEvent(ticketNr, location).recover(withPrevious(emptyTicketInfo))

    infoWithEvent_Future.flatMap { infoWithEvent =>

      println(s"[ticketNr: $ticketNr] => Got the event")

      // Note that infoWithWeather and infoWithTravelAdvice are independent, with just their
      // specific parts of TicketInfo filled in

      println(s"[ticketNr: $ticketNr] => 1")
      // Future (2), get weather info
      val infoWithWeather_Future: Future[TicketInfo] = getWeather(infoWithEvent)

      println(s"[ticketNr: $ticketNr] => 2")
      // Future (3), get travel advice
      val infoWithTravelAdvice_Future: Future[TicketInfo] = infoWithEvent.event.map { event =>
        getTravelAdvice(infoWithEvent, event)
      }.getOrElse(infoWithEvent_Future)

      println(s"[ticketNr: $ticketNr] => 3")
      // Combined weather and travel into one
      val ticketInfos_Future: Seq[Future[TicketInfo]] = Seq(infoWithTravelAdvice_Future, infoWithWeather_Future)

      // Get suggested events, but only after setting up the other futures

      println(s"[ticketNr: $ticketNr] => 4")
      // Future (4), get suggested events
      val suggestedEvents_Future: Future[Seq[Event]] = infoWithEvent.event.map { event =>
        getSuggestions(event, ticketNr)
      }.getOrElse(Future.successful(Seq()))

      println(s"[ticketNr: $ticketNr] => 5")


      // Now add in suggested events
      val infoWithSuggestedEvents_Future = for (
        suggestions <- suggestedEvents_Future;
        dummy = println(s"[ticketNr: $ticketNr] => !!! Adding suggestions !!!")
      ) yield infoWithEvent.copy(suggestions = suggestions)

      println(s"[ticketNr: $ticketNr] => 6");

      // NOTE: The fold is performed on the thread where the last future is completed. Implies that this will not
      //       execute until all of the futures have completed
      val result = infoWithSuggestedEvents_Future.flatMap {
        Future.fold(ticketInfos_Future)(_) { (acc, elem) =>
          println(s"[ticketNr: $ticketNr] => !!! Combining travel and weather !!!")
          val (travelAdvice, weather) = (elem.travelAdvice, elem.weather)

          // Copy over values which are present, or persist previous value
          acc.copy(
            travelAdvice = travelAdvice.orElse(acc.travelAdvice),
            weather = weather.orElse(acc.weather)
          )
        }
      }

      println(s"[ticketNr: $ticketNr] => 7");

      result
    }
  }

  def getWeather(ticketInfo: TicketInfo): Future[TicketInfo] = {

    val futureWeatherX = callWeatherXService(ticketInfo).recover(withNone)

    val futureWeatherY = callWeatherYService(ticketInfo).recover(withNone)

    Future.firstCompletedOf(Seq(futureWeatherX, futureWeatherY)).map { weatherResponse =>
      ticketInfo.copy(weather = weatherResponse)
    }
  }

  def getTravelAdvice(info: TicketInfo, event: Event): Future[TicketInfo] = {

    val futureRoute: Future[Option[RouteByCar]] =
      callTrafficService(info.userLocation, event.location, event.time, info.ticketNr).recover(withNone)

    val futurePublicTransport: Future[Option[PublicTransportAdvice]] =
      callPublicTransportService(info.userLocation, event.location, event.time, info.ticketNr).recover(withNone)

    val futures: Future[(Option[RouteByCar], Option[PublicTransportAdvice])] = futureRoute.zip(futurePublicTransport)

    futures.map { case (routeByCar, publicTransportAdvice) =>
      val travelAdvice = TravelAdvice(routeByCar, publicTransportAdvice)
      info.copy(travelAdvice = Some(travelAdvice))
    }
  }

  def getPlannedEvents(event: Event, artists: Seq[Artist], ticketNr: String): Future[Seq[Event]] = {
    val events: Seq[Future[Event]] = artists.map(artist => callArtistCalendarService(artist, event.location, ticketNr))
    Future.sequence(events)
  }

  def getSuggestions(event: Event, ticketNr: String): Future[Seq[Event]] = {
    val futureArtists: Future[Seq[Artist]] = callSimilarArtistsService(event, ticketNr).recover(withEmptySeq)

    println(s"[ticketNr: $ticketNr] => getSuggestions for each artist");
    for (artists <- futureArtists.recover(withEmptySeq); // How is this recover different to recover on previous line?
         events <- getPlannedEvents(event, artists, ticketNr).recover(withEmptySeq)
    ) yield events
  }

  // Following are not used (i.e. described in book, or tested)

  // Call the traffic service and place result in TicketInfo
  // Assumes that TravelAdvice is empty i.e. we can just overwrite existing value
  def getTraffic(ticketInfo: TicketInfo): Future[TicketInfo] = {
    val theTicketInfo: Option[Future[TicketInfo]] = ticketInfo.event.map { event =>
      val routeByCar: Future[Option[RouteByCar]] =
        callTrafficService(ticketInfo.userLocation, event.location, event.time, ticketInfo.ticketNr)
      routeByCar.map { routeResponse =>
        ticketInfo.copy(travelAdvice = Some(TravelAdvice(routeByCar = routeResponse)))
      }
    }

    // getOrElse handles the case where the ticketInfo.event is None
    theTicketInfo.getOrElse(Future.successful(ticketInfo))
  }

  // Call the traffic service and place result in TicketInfo
  // Assumes that TravelAdvice is not empty i.e. it has to be updated with Option[RouteByCar]
  // returned by traffic service call
  def getCarRoute(ticketInfo: TicketInfo): Future[TicketInfo] = {
    // recover used to return previous ticket if call to service fails
    val theTicketInfo: Option[Future[TicketInfo]] = ticketInfo.event.map { event =>
      callTrafficService(ticketInfo.userLocation, event.location, event.time, ticketInfo.ticketNr).map { routeResponse =>
        val newTravelAdvice = ticketInfo.travelAdvice.map(_.copy(routeByCar = routeResponse))
        ticketInfo.copy(travelAdvice = newTravelAdvice)
      }.recover(withPrevious(ticketInfo))
    }

    // Note: getOrElse call seems to be doubling up on failure, since recover is already used
    //       Perhaps we don't need the recover
    theTicketInfo.getOrElse(Future.successful(ticketInfo))
  }

  // Call the public transport service and place result in TicketInfo
  // Assumes that TravelAdvice is not empty i.e. it has to be updated with Option[PublicTransportAdvice]
  // returned by public transport service call
  def getPublicTransportAdvice(ticketInfo: TicketInfo): Future[TicketInfo] = {
    val theTicketInfo: Option[Future[TicketInfo]] = ticketInfo.event.map { event =>
      val transportAdvice: Future[Option[PublicTransportAdvice]] =
        callPublicTransportService(ticketInfo.userLocation, event.location, event.time, ticketInfo.ticketNr)

      val infoAboutTicket: Future[TicketInfo] = transportAdvice.map { publicTransportResponse =>
        val currentTravelAdvice = ticketInfo.travelAdvice
        val newTravelAdvice = currentTravelAdvice.map(ta => ta.copy(publicTransportAdvice = publicTransportResponse))
        ticketInfo.copy(travelAdvice = newTravelAdvice)
      }

      infoAboutTicket.recover(withPrevious(ticketInfo))
    }

    theTicketInfo.getOrElse(Future.successful(ticketInfo))
  }

  def getSuggestionsWithFlatMapAndMap(event: Event, ticketNr: String): Future[Seq[Event]] = {

    val futureArtists: Future[Seq[Artist]] = callSimilarArtistsService(event, ticketNr).recover(withEmptySeq)

    // This is effectively getPlannedEventsImprovedByUsingTraverse
    futureArtists.flatMap { artists =>
      val futureEvents: Future[Seq[Event]] = Future.traverse(artists)(artist => callArtistCalendarService(artist, event.location, ticketNr))
      futureEvents
    }.recover(withEmptySeq)
  }

  def getSuggestionsWithFlatMapAndMapConcise(event: Event, ticketNr: String): Future[Seq[Event]] = {

    val futureArtists: Future[Seq[Artist]] = callSimilarArtistsService(event, ticketNr).recover(withEmptySeq)

    futureArtists.flatMap {
      getPlannedEventsImprovedByUsingTraverse(event, _, ticketNr)
    }.recover(withEmptySeq)
  }

  // Following are in book, but not tested
  def getFirstValidWeatherForecast(ticketInfo: TicketInfo): Future[TicketInfo] = {

    val futureWeatherX = callWeatherXService(ticketInfo).recover(withNone)

    val futureWeatherY = callWeatherYService(ticketInfo).recover(withNone)

    val futures: Seq[Future[Option[Weather]]] = Seq(futureWeatherX, futureWeatherY)

    val fastestSuccessfulResponse: Future[Option[Weather]] =
      Future.find(futures)(maybeWeather => !maybeWeather.isEmpty).map(_.flatten)

    fastestSuccessfulResponse.map { weatherResponse =>
      ticketInfo.copy(weather = weatherResponse)
    }
  }

  def getTravelAdviceUsingForComprehension(info: TicketInfo, event: Event): Future[TicketInfo] = {

    val futureRoute = callTrafficService(info.userLocation, event.location, event.time, info.ticketNr).recover(withNone)

    val futurePublicTransport =
      callPublicTransportService(info.userLocation, event.location, event.time, info.ticketNr).recover(withNone)

    for ((routeByCar, publicTransportAdvice) <- futureRoute.zip(futurePublicTransport);
         travelAdvice = TravelAdvice(routeByCar, publicTransportAdvice)
    ) yield info.copy(travelAdvice = Some(travelAdvice))
  }

  def getPlannedEventsImprovedByUsingTraverse(event: Event, artists: Seq[Artist], ticketNr: String): Future[Seq[Event]] = {
    Future.traverse(artists) { artist =>
      callArtistCalendarService(artist, event.location, ticketNr)
    }
  }
}

trait WebServiceCalls {
  def getEvent(ticketNr: String, location: Location): Future[TicketInfo]

  def callWeatherXService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callWeatherYService(ticketInfo: TicketInfo): Future[Option[Weather]]

  def callTrafficService(origin: Location, destination: Location, time: DateTime, ticketNr: String): Future[Option[RouteByCar]]

  def callPublicTransportService(origin: Location, destination: Location, time: DateTime, ticketNr: String): Future[Option[PublicTransportAdvice]]

  def callSimilarArtistsService(event: Event, ticketNr: String): Future[Seq[Artist]]

  def callArtistCalendarService(artist: Artist, nearLocation: Location, ticketNr: String): Future[Event]
}