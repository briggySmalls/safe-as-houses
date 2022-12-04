package com.hunorkovacs.ziohttp4stry.services

import scalatags.Text.TypedTag
import scalatags.Text.all._
import zio.{ UIO, ULayer, URIO, ZIO, ZLayer }

trait HtmlService {
  def render(input: String): UIO[TypedTag[String]]
}

object HtmlService {
  def getRender(input: String): URIO[HtmlService, TypedTag[String]] = ZIO.serviceWithZIO[HtmlService](_.render(input))
}

class HtmlServiceLive extends HtmlService {
  override def render(input: String): UIO[TypedTag[String]] =
    ZIO.succeed(
      html(
        body(
          p(s"Hello: $input")
        )
      )
    )
}

object HtmlServiceLive {
  val layer: ULayer[HtmlService] = ZLayer.succeed(new HtmlServiceLive)
}
