package com.hunorkovacs.ziohttp4stry.models

import java.net.URI

case class SearchParams(from: Int = 0, user: Option[UserId] = None, filter: Option[FilterMethod] = None) {
  def buildUrl(url: String): String = {
    val queryParamStrings = params.map { case (key, value) => s"$key=$value" }
    s"$url?${queryParamStrings.mkString("&")}"
  }

  def incrementFrom(inc: Int) = this.copy(from = from + inc)

  private def params: Map[String, String] =
    Map(
      "user"   -> user.map(_.id.toString),
      "filter" -> filter.map(_.entryName),
      "from"   -> Some(from.toString)
    ).collect {
      case (key, Some(value)) => key -> value
    }
}
