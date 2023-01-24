package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.{ FilterMethod, PropertyDetails, QueryParamUpdater, SearchParams, UserId }
import scalatags.Text.TypedTag
import scalatags.Text.all.{ input, _ }
import shapeless.PolyDefns.->
import zio.{ RIO, Task, UIO, ULayer, URIO, ZIO, ZLayer }

trait HtmlService {
  def renderPage(searchParams: SearchParams): Task[TypedTag[String]]

  def renderItems(searchParams: SearchParams): Task[Seq[TypedTag[String]]]
}

object HtmlService {
  def getRenderPage(searchParams: SearchParams): RIO[HtmlService, TypedTag[String]] =
    ZIO.serviceWithZIO[HtmlService](_.renderPage(searchParams))

  def getRenderItems(
    searchParams: SearchParams
  ): RIO[HtmlService, Seq[TypedTag[String]]] =
    ZIO.serviceWithZIO[HtmlService](_.renderItems(searchParams))
}

class HtmlServiceLive(searchService: SearchService) extends HtmlService {
  override def renderPage(searchParams: SearchParams): Task[TypedTag[String]] =
    for {
      items <- renderItems(searchParams)
    } yield html(
      meta(name := "viewport", content := "width=device-width, initial-scale=1"),
      head(
        script(
          src := "https://cdn.tailwindcss.com"
        ),
        script(
          src := "https://unpkg.com/htmx.org@1.8.4",
          integrity := "sha384-wg5Y/JwF7VxGk4zLsJEcAojRtlVp1FKKdGy1qN+OMtdq72WRvX/EdRdqg/LOhYeV",
          crossorigin := "anonymous"
        ),
        script(
          """
            |function updateUrl(url, parameter, value) {
            |  let u = new URL(url);
            |  u.searchParams.set(parameter, value);
            |  return u.toString();
            |}
            |""".stripMargin
        )
      ),
      body(
        `class` := "bg-slate-900",
        div(
          `class` := "flex flex-col gap-4 items-center",
          div(
            QueryParamUpdater(
              "user",
              Map(
                UserId(1).id -> "Sam",
                UserId(2).id -> "Pippy"
              ),
              searchParams.user.map(_.id)
            ),
            QueryParamUpdater(
              "filter",
              Map(
                FilterMethod.Viewed.entryName    -> "viewed",
                FilterMethod.NotViewed.entryName -> "not viewed"
              ),
              searchParams.filter.map(_.entryName)
            )
          ),
          div(items)
        )
      )
    )

  override def renderItems(
    searchParams: SearchParams
  ): Task[Seq[TypedTag[String]]] =
    for {
      _       <- zio.Console.printLine("Starting render!")
      results <- searchService.searchHouses(searchParams)
      newParams  = searchParams.incrementFrom(results.size)
      components = results.map(_.present(newParams))
      // Wrap the components with a div with an infinite scroll action
      wrappedComponents = components.map(c => div(`class` := "scroll-wrapper", c)) match {
        case Nil => Nil
        case init :+ last =>
          init :+ last(
            attr("hx-get") := newParams.buildUrl("/api/v1/properties"),
            attr("hx-trigger") := "revealed",
            attr("hx-swap") := "afterend"
          )
      }
    } yield wrappedComponents
}

object HtmlServiceLive {
  val layer: ZLayer[SearchService, Nothing, HtmlService] = ZLayer {
    for {
      search <- ZIO.service[SearchService]
    } yield new HtmlServiceLive(search)
  }
}
