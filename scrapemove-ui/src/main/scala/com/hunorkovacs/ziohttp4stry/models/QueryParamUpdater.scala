package com.hunorkovacs.ziohttp4stry.models

import scalatags.Text.TypedTag
import scalatags.Text.all._

object QueryParamUpdater {
  def apply[T](
    param: String,
    options: Map[T, String],
    emptyText: String,
    current: Option[T]
  ): TypedTag[String] =
    select(
      name := "user",
      onchange := s"location = updateUrl(document.URL, '$param', this.options[this.selectedIndex].value)",
      option(
        value := "null",
        emptyText
      ) +: options.toSeq.map {
        case (obj, name) =>
          val opt = option(
            value := obj.toString,
            name
          )
          if (current.contains(obj))
            opt(selected)
          else
            opt
      }
    )
}
