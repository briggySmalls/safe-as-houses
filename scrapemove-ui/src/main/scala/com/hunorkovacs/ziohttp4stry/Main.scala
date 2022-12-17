package com.hunorkovacs.ziohttp4stry

import cats.effect.{ ExitCode => CatsExitCode }
import com.hunorkovacs.ziohttp4stry.services.{ HtmlService, HtmlServiceLive, SearchService, SearchServiceLive }
import org.apache.http.util.ExceptionUtils
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.scalatags._

import java.io.{ IOException, PrintWriter, StringWriter }
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import java.time.Year
import scala.concurrent.ExecutionContext

object FromQueryParamMatcher extends QueryParamDecoderMatcher[Int]("from")

object Main extends ZIOAppDefault {

  type AppEnvironment = SearchService with HtmlService
  type AppTask[A]     = RIO[AppEnvironment, A]

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private val appEnvironment = SearchServiceLive.layer ++ HtmlServiceLive.layer

  private val helloWorldService = HttpRoutes
    .of[AppTask] {
      case GET -> Root =>
        Ok(
          HtmlService
            .getRenderPage()
            .tapError(err => printError(err))
        )
      case GET -> Root / "api" / "v1" / "properties" :? FromQueryParamMatcher(from) =>
        Ok(
          HtmlService
            .getRenderItems(from)
            .tapError(err => printError(err))
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

  private def printError(err: Throwable): IO[IOException, Unit] = {
    val sw = new StringWriter()
    val pw = new PrintWriter(sw)
    err.printStackTrace(pw)
    val sStackTrace = sw.toString
    Console.printLineError(sStackTrace)
  }
}
