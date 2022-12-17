package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.PropertyDetails
import scalatags.Text.TypedTag
import scalatags.Text.all.{ input, _ }
import zio.{ RIO, Task, UIO, ULayer, URIO, ZIO, ZLayer }

trait HtmlService {
  def renderPage(): Task[TypedTag[String]]

  def renderItems(from: Int): Task[Seq[TypedTag[String]]]
}

object HtmlService {
  def getRenderPage(): RIO[HtmlService, TypedTag[String]] =
    ZIO.serviceWithZIO[HtmlService](_.renderPage())

  def getRenderItems(from: Int = 0): RIO[HtmlService, Seq[TypedTag[String]]] =
    ZIO.serviceWithZIO[HtmlService](_.renderItems(from))
}

class HtmlServiceLive(searchService: SearchService) extends HtmlService {
  override def renderPage(): Task[TypedTag[String]] =
    for {
      items <- renderItems(0)
    } yield html(
      head(
        script(
          src := "https://cdn.tailwindcss.com"
        ),
        script(
          src := "https://unpkg.com/htmx.org@1.8.4",
          integrity := "sha384-wg5Y/JwF7VxGk4zLsJEcAojRtlVp1FKKdGy1qN+OMtdq72WRvX/EdRdqg/LOhYeV",
          crossorigin := "anonymous"
        )
      ),
      body(
        `class` := "bg-slate-900",
        div(
          `class` := "flex flex-col gap-4 items-center",
          items
        )
      )
    )

  override def renderItems(from: Int): Task[Seq[TypedTag[String]]] =
    for {
      _      <- zio.Console.printLine("Starting render!")
      result <- searchService.searchHouses(from)
      newFrom = from + result.size
      components = result.map(_.present) match {
        case Nil => Nil
        case init :+ last =>
          init :+ last(
            attr("hx-get") := s"/api/v1/properties?from=$newFrom",
            attr("hx-trigger") := "revealed",
            attr("hx-swap") := "afterend"
          )
      }
    } yield components
}

object HtmlServiceLive {
  val layer: ZLayer[SearchService, Nothing, HtmlService] = ZLayer {
    for {
      search <- ZIO.service[SearchService]
    } yield new HtmlServiceLive(search)
  }
}
