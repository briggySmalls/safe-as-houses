package com.hunorkovacs.ziohttp4stry

import cats.effect.{ ExitCode => CatsExitCode }
import com.hunorkovacs.ziohttp4stry.config.Settings
import com.hunorkovacs.ziohttp4stry.models.{ DocumentId, FilterMethod, SearchParams, UserId }
import com.hunorkovacs.ziohttp4stry.services.{ HtmlService, HtmlServiceLive, SearchService, SearchServiceLive }
import com.hunorkovacs.ziohttp4stry.utils.Extensions._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.scalatags._
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.concurrent.ExecutionContext

object Main extends ZIOAppDefault {

  implicit val userQueryParamDecoder     = QueryParamDecoder[Int].map(UserId)
  implicit val documentQueryParamDecoder = QueryParamDecoder[Int].map(DocumentId)
  implicit val filterQueryParamDecoder   = QueryParamDecoder[String].map(FilterMethod.withName)
  object FromQueryParamMatcher           extends QueryParamDecoderMatcher[Int]("from")
  object OptionalUserQueryParamMatcher   extends OptionalQueryParamDecoderMatcher[UserId]("user")
  object UserQueryParamMatcher           extends QueryParamDecoderMatcher[UserId]("user")
  object DocumentQueryParamMatcher       extends QueryParamDecoderMatcher[DocumentId]("document")
  object OptionalFilterQueryParamMatcher extends OptionalQueryParamDecoderMatcher[FilterMethod]("filter")

  type AppEnvironment = SearchService with HtmlService with Settings
  type AppTask[A]     = RIO[AppEnvironment, A]

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private val appEnvironment = SearchServiceLive.layer ++ HtmlServiceLive.layer ++ Settings.layer

  private val helloWorldService = HttpRoutes
    .of[AppTask] {
      case GET -> Root :? OptionalUserQueryParamMatcher(user) :? OptionalFilterQueryParamMatcher(filter) =>
        Ok(
          HtmlService
            .getRenderPage(SearchParams(from = 0, user = user, filter = filter))
            .logIssues()
        )
      case GET -> Root / "api" / "v1" / "properties" :? OptionalUserQueryParamMatcher(user) :? FromQueryParamMatcher(
            from
          ) :? OptionalFilterQueryParamMatcher(filter) =>
        Ok(
          HtmlService
            .getRenderItems(SearchParams(from, user, filter))
            .map(_.map(_.render).mkString(""))
            .logIssues()
        )
      case PUT -> Root / "api" / "v1" / "views" :? UserQueryParamMatcher(user) :? DocumentQueryParamMatcher(
            document
          ) =>
        Ok(
          SearchService
            .getUpdateViewer(document, user)
            .logIssues()
        )
    }
    .orNotFound

  def server =
    for {
      settings <- ZIO.service[Settings]
      server <- ZIO.runtime[AppEnvironment].flatMap { implicit runtime =>
        BlazeServerBuilder[AppTask](ExecutionContext.global)
          .bindHttp(settings.serverSettings.port, settings.serverSettings.host)
          .withHttpApp(helloWorldService)
          .serve
          .compile[AppTask, AppTask, CatsExitCode]
          .drain
          .exitCode
      }
    } yield server

  override def run = server.provide(
    HtmlServiceLive.layer,
    SearchServiceLive.layer,
    Settings.layer
  )
}
