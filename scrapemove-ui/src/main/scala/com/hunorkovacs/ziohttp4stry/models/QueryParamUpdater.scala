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
      `class` :=
        """
          |bg-gray-50 border border-gray-300 text-gray-900 rounded-lg
          |focus:ring-blue-500 focus:border-blue-500
          |dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white
          |dark:focus:ring-blue-500 dark:focus:border-blue-500
          |""".stripMargin,
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
