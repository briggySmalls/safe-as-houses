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
import com.hunorkovacs.ziohttp4stry.utils.Extensions._
import cats._
import cats.data._
import cats.syntax.all._
import com.sksamuel.elastic4s.circe._

import scala.util.{Failure, Success}

trait SearchService {
  def searchHouses(): Task[Seq[PropertyDetails]]
}

class SearchServiceLive extends SearchService {
  val props  = ElasticProperties("http://localhost:9200")
  val client = ElasticClient(JavaClient(props))

  override def searchHouses(): Task[Seq[PropertyDetails]] =
    for {
      _      <- zio.Console.printLine("starting search")
      result <- searchInternal()
      _      <- zio.Console.printLine("search complete")
    } yield result

  private def searchInternal(): Task[Seq[PropertyDetails]] =
    ZIO
      .absolve(
        client.execute {
          search("house-index-4").sortBy(
            geoSort("location") points List(new GeoPoint(51.5553, -0.0921)) order SortOrder.DESC,
            scriptSort(
              "if (doc['area_sqft'].size() != 0 && doc['price.amount'].size() != 0) return doc['price.amount'].value / doc['area_sqft'].value; return 0"
            ) typed ScriptSortType.Number order SortOrder.ASC,
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
  def layer: ULayer[SearchServiceLive] = ZLayer.succeed {
    val s = new SearchServiceLive
    Console.println("Search service constructed!")
    s
  }

  def toException(err: ElasticError): Exception =
    if (err.failedShards.isEmpty) err.asException
    else new SearchException(err.failedShards.flatMap(_.reason).map(_.asException))
}
