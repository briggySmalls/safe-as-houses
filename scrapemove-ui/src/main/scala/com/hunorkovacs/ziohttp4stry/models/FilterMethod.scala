package com.hunorkovacs.ziohttp4stry.models

import enumeratum.{ Enum, EnumEntry }

sealed trait FilterMethod extends EnumEntry

object FilterMethod extends Enum[FilterMethod] {
  val values = findValues

  case object All       extends FilterMethod
  case object Viewed    extends FilterMethod
  case object NotViewed extends FilterMethod
}
