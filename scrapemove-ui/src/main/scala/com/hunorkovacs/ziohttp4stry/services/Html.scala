package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.PropertyDetails
import scalatags.Text.TypedTag
import scalatags.Text.all.{ input, _ }
import zio.{ RIO, Task, UIO, ULayer, URIO, ZIO, ZLayer }

trait HtmlService {
  def render(input: String): Task[TypedTag[String]]
}

object HtmlService {
  def getRender(input: String): RIO[HtmlService, TypedTag[String]] =
    ZIO.serviceWithZIO[HtmlService](_.render(input))
}

class HtmlServiceLive(searchService: SearchService) extends HtmlService {
  override def render(input: String): Task[TypedTag[String]] =
    for {
      _      <- zio.Console.printLine("Starting render!")
      result <- searchService.searchHouses()
    } yield {
      html(
        head(
          script(
            src := "https://cdn.tailwindcss.com"
          )
        ),
        body(
          `class` := "bg-slate-900",
          div(
            `class` := "flex flex-col gap-4 items-center",
            result.map(_.present)
          )
        )
      )
    }
}

object HtmlServiceLive {
  val layer: ZLayer[SearchService, Nothing, HtmlService] = ZLayer {
    for {
      search <- ZIO.service[SearchService]
    } yield new HtmlServiceLive(search)
  }
}
