package aia.routing.hashing.gather_via_traits

import aia.routing.hashing.messages.GatherMessage

trait KeyedOnMessageIdValue {
  def messageKey(msg: GatherMessage): String = {
    msg.id
  }
}