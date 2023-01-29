package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.AppExceptions.SearchException
import com.hunorkovacs.ziohttp4stry.models.{DocumentId, FilterMethod, PropertyDetails, SearchParams, UserId}
import com.sksamuel.elastic4s.ElasticDsl.{search, _}
import com.sksamuel.elastic4s.{ElasticClient, ElasticError, ElasticProperties}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.{GeoPoint, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.sort.{ScriptSortType, SortOrder}
import com.sksamuel.elastic4s.zio.instances._
import zio.{RIO, Task, ULayer, ZIO, ZLayer}
import cats._
import cats.data._
import cats.syntax.all._
import com.hunorkovacs.ziohttp4stry.config.Settings
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.script.Script
import com.sksamuel.elastic4s.requests.searches.queries.{RankFeatureQuery, ScriptScoreQuery}
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.{FunctionScoreQuery, GaussianDecayScore, ScriptScore}
import com.sksamuel.elastic4s.requests.searches.queries.geo.GeoDistanceQuery
import com.hunorkovacs.ziohttp4stry.utils.Extensions.ResponseExtensions
import com.sksamuel.elastic4s.requests.searches.term.{TermQuery, TermsQuery}
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import scalatags.Text.TypedTag

import scala.util.{Failure, Success}

trait SearchService {
  def searchHouses(searchParams: SearchParams): Task[Seq[PropertyDetails]]

  def updateViewer(document: DocumentId, user: UserId): Task[Unit]
}

object SearchService {
  def getSearchHouses(searchParams: SearchParams): RIO[SearchService, Seq[PropertyDetails]] =
    ZIO.serviceWithZIO[SearchService](_.searchHouses(searchParams: SearchParams))

  def getUpdateViewer(document: DocumentId, user: UserId): RIO[SearchService, Unit] =
    ZIO.serviceWithZIO[SearchService](_.updateViewer(document, user))
}

class SearchServiceLive(settings: Settings) extends SearchService {
  val props  = ElasticProperties(settings.elasticSettings.url)
  val client = ElasticClient(JavaClient(props))

  override def searchHouses(searchParams: SearchParams): Task[Seq[PropertyDetails]] =
    for {
      _      <- zio.Console.printLine("starting search")
      result <- searchInternal(searchParams)
      _      <- zio.Console.printLine("search complete")
    } yield result

  private def searchInternal(searchParams: SearchParams): Task[Seq[PropertyDetails]] = {
    val query = should(
      FunctionScoreQuery(
        functions = Seq(
          GaussianDecayScore(
            field = "location",
            origin = "51.5553, -0.0921",
            scale = "2km"
          )
        )
      ),
      RankFeatureQuery(
        "price_per_sqft"
      )
    )

    val filteredQuery = (searchParams.user, searchParams.filter) match {
      case (Some(u), Some(FilterMethod.Viewed)) =>
        query.filter(TermQuery(field="viewedBy", value=u.id))
      case (Some(u), Some(FilterMethod.NotViewed)) =>
        query.filter(should(
          not(existsQuery("viewedBy")),
          not(TermQuery(field="viewedBy", value=u.id))
        ))
      case (_, _) => query
    }

    ZIO
      .absolve(
        client.execute {
          search(settings.elasticSettings.index)
            .from(searchParams.from)
            .query(filteredQuery)
        }.map(_.toSubmergableError)
      )
      .map(_.safeTo[PropertyDetails])
      .foldZIO(
        f => ZIO.fail(f),
        s => s.toList.sequence match {
          case Success(hits) => ZIO.succeed(hits)
          case Failure(err) => ZIO.fail(err)
        }
      )
  }

  override def updateViewer(document: DocumentId, user: UserId): Task[Unit] =
    ZIO.absolve(
      client.execute {
        updateById(settings.elasticSettings.index, document.id.toString).script(
          Script(
          """
            |List viewedBy=ctx._source.viewedBy;
            |if (viewedBy == null) {
            |  ctx._source.viewedBy = [params.user]
            |} else if (!viewedBy.stream().anyMatch(id-> id.equals(params.user))) {
            |   ctx._source.viewedBy.add(params.user)
            |}
            |""".stripMargin,
          params=Map(
            "user" -> user.id
          )
        )
        )
      }.map(_.toSubmergableError)
    ).map(r => ())
}

object SearchServiceLive {
  def layer: ZLayer[Settings, Nothing, SearchService] = ZLayer {
    for {
      settings <- ZIO.service[Settings]
    } yield new SearchServiceLive(settings)
  }
}
