package aia.routing.hashing.gather_via_traits

import aia.routing.hashing.messages.GatherMessage

trait KeyedOnMessageIdLength {
  def messageKey(msg: GatherMessage): String = {
    msg.id.length.toString
  }
}