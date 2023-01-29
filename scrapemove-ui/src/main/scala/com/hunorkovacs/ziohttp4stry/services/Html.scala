package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.{ FilterMethod, PropertyDetails, QueryParamUpdater, SearchParams, UserId }
import scalatags.Text.TypedTag
import scalatags.Text.tags2.{ main, nav }
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
      headElement,
      body(
        `class` := "dark:bg-slate-900",
        div(
          header(`class` := "sticky top-0 z-50", navBar(searchParams)),
          main(`class` := "relative flex flex-col gap-4 items-center", items)
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

  private def navBar(searchParams: SearchParams): TypedTag[String] =
    nav(
      `class` :=
        """
          |w-full py-2
          |border-x border-b rounded-b-md
          |bg-gray-100 border-gray-200
          |dark:bg-gray-900 dark:border-gray-700
          |flex flex-row justify-center gap-2
          |""".stripMargin,
      filterForm(searchParams)
    )

  private def filterForm(searchParams: SearchParams): Seq[TypedTag[String]] =
    searchParams.user.foldLeft(
      Seq(
        QueryParamUpdater(
          "user",
          Map(
            UserId(1).id -> "Sam",
            UserId(2).id -> "Pippy"
          ),
          "None",
          searchParams.user.map(_.id)
        )
      )
    ) {
      case (form, _) =>
        form :+ QueryParamUpdater(
          "filter",
          Map(
            FilterMethod.Viewed.entryName    -> "viewed",
            FilterMethod.NotViewed.entryName -> "not viewed"
          ),
          "all",
          searchParams.filter.map(_.entryName)
        )
    }

  private val headElement: TypedTag[String] = head(
    meta(name := "viewport", content := "width=device-width, initial-scale=1"),
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
        |  if (value === 'null') {
        |     u.searchParams.delete(parameter);
        |  }
        |  else {
        |     u.searchParams.set(parameter, value);
        |  }
        |  return u.toString();
        |}
        |""".stripMargin
    )
  )
}

object HtmlServiceLive {
  val layer: ZLayer[SearchService, Nothing, HtmlService] = ZLayer {
    for {
      search <- ZIO.service[SearchService]
    } yield new HtmlServiceLive(search)
  }
}
