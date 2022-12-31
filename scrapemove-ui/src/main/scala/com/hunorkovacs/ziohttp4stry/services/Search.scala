package com.hunorkovacs.ziohttp4stry.services

import com.hunorkovacs.ziohttp4stry.models.AppExceptions.SearchException
import com.hunorkovacs.ziohttp4stry.models.PropertyDetails
import com.sksamuel.elastic4s.ElasticDsl.{search, _}
import com.sksamuel.elastic4s.{ElasticClient, ElasticError, ElasticProperties}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.{GeoPoint, SearchResponse}
import com.sksamuel.elastic4s.requests.searches.sort.{ScriptSortType, SortOrder}
import com.sksamuel.elastic4s.zio.instances._
import zio.{Task, ULayer, ZIO, ZLayer}
import cats._
import cats.data._
import cats.syntax.all._
import com.hunorkovacs.ziohttp4stry.config.Settings
import com.sksamuel.elastic4s.circe._
import com.sksamuel.elastic4s.requests.searches.queries.funcscorer.{FunctionScoreQuery, GaussianDecayScore, ScriptScore}

import scala.util.{Failure, Success}

trait SearchService {
  def searchHouses(from: Int): Task[Seq[PropertyDetails]]
}

class SearchServiceLive(settings: Settings) extends SearchService {
  val props  = ElasticProperties(settings.elasticSettings.url)
  val client = ElasticClient(JavaClient(props))

  override def searchHouses(from: Int): Task[Seq[PropertyDetails]] =
    for {
      _      <- zio.Console.printLine("starting search")
      result <- searchInternal(from)
      _      <- zio.Console.printLine("search complete")
    } yield result

  private def searchInternal(from: Int): Task[Seq[PropertyDetails]] =
    ZIO
      .absolve(
        client.execute {
          search(settings.elasticSettings.index)
            .from(from)
            .query(
              FunctionScoreQuery(
                functions = Seq(
                  GaussianDecayScore(
                    field="location",
                    origin="51.5553, -0.0921",
                    scale="2km",
                  ),
                  ScriptScore(
                    """
                      |if (doc['area_sqft'].size() != 0 && doc['price.amount'].size() != 0)
                      | return doc['area_sqft'].value / doc['price.amount'].value;
                      | return 0
                      |""".stripMargin,
                    weight = Some(100)
                  )
                )
              )
          )
        }.map(_.toEither)
          .map(_.left.map(SearchServiceLive.toException))
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

object SearchServiceLive {
  def layer: ZLayer[Settings, Nothing, SearchServiceLive] = ZLayer {
    for {
      settings <- ZIO.service[Settings]
    } yield new SearchServiceLive(settings)
  }

  def toException(err: ElasticError): Exception =
    if (err.failedShards.isEmpty) err.asException
    else new SearchException(err.failedShards.flatMap(_.reason).map(_.asException))
}
