package aia.routing.hashing.messages

import akka.routing.ConsistentHashingRouter.ConsistentHashable

case class GatherMessageWithHash(id: String, values: Seq[String]) extends GatherMessage with ConsistentHashable {
  override def consistentHashKey: Any = id
}
