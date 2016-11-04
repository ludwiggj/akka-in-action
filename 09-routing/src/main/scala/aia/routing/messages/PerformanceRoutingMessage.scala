package aia.routing.messages

case class PerformanceRoutingMessage(photo: String,
                                     license: Option[String],
                                     processedBy: Option[String],
                                     id: String = "")
