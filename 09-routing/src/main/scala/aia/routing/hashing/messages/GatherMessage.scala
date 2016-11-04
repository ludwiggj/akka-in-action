package aia.routing.hashing.messages

trait GatherMessage {
  val id: String
  val values: Seq[String]
}
