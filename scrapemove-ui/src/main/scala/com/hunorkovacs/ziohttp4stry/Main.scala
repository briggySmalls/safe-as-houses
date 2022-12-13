package com.hunorkovacs.ziohttp4stry

import cats.effect.{ ExitCode => CatsExitCode }
import com.hunorkovacs.ziohttp4stry.services.{ HtmlService, HtmlServiceLive, SearchService, SearchServiceLive }
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.scalatags._
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.concurrent.ExecutionContext

object Main extends ZIOAppDefault {

  type AppEnvironment = SearchService with HtmlService
  type AppTask[A]     = RIO[AppEnvironment, A]

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private val appEnvironment = SearchServiceLive.layer ++ HtmlServiceLive.layer

  private val helloWorldService = HttpRoutes
    .of[AppTask] {
      case GET -> Root / "hello" =>
        Ok(
          HtmlService
            .getRender("hello")
            .tapError(err => Console.printError(err))
        )
    }
    .orNotFound

  override def run =
    ZIO
      .runtime[AppEnvironment]
      .provide(
        HtmlServiceLive.layer,
        SearchServiceLive.layer
      )
      .flatMap { implicit runtime =>
        BlazeServerBuilder[AppTask](ExecutionContext.global)
          .bindHttp(8080, "localhost")
          .withHttpApp(helloWorldService)
          .serve
          .compile[AppTask, AppTask, CatsExitCode]
          .drain
          .provide(
            HtmlServiceLive.layer,
            SearchServiceLive.layer
          )
          .exitCode
      }
}
