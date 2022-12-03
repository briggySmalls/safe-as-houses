package com.hunorkovacs.ziohttp4stry.services

import zio.{UIO, ULayer, URIO, ZIO, ZLayer}

trait HtmlService {
  def render(input: String): UIO[String]
}

object HtmlService {
  def getRender(input: String): URIO[HtmlService, String] = ZIO.serviceWithZIO[HtmlService](_.render(input))
}

class HtmlServiceLive extends HtmlService {
  override def render(input: String): UIO[String] =
    ZIO.succeed(s"<html><body><p>${input}</p></body></html>")
}

object HtmlServiceLive {
  val layer: ULayer[HtmlService] = ZLayer.succeed(new HtmlServiceLive)
}
