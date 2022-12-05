package com.hunorkovacs.ziohttp4stry.services

import com.sksamuel.elastic4s.ElasticDsl.{ search, _ }
import com.sksamuel.elastic4s.{ ElasticClient, ElasticProperties }
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.requests.searches.{ GeoPoint, SearchResponse }
import com.sksamuel.elastic4s.requests.searches.sort.{ ScriptSortType, SortOrder }
import com.sksamuel.elastic4s.zio.instances._
import zio.{ Task, ULayer, ZIO, ZLayer }
import com.hunorkovacs.ziohttp4stry.utils.Extensions._

trait SearchService {
  def searchHouses(): Task[String]
}

class SearchServiceLive extends SearchService {
  val props  = ElasticProperties("http://localhost:9200")
  val client = ElasticClient(JavaClient(props))

  override def searchHouses(): Task[String] =
    for {
      _      <- zio.Console.printLine("starting search")
      result <- searchInternal()
      _      <- zio.Console.printLine("search complete")
    } yield result

  private def searchInternal(): ZIO[Any, Throwable, String] =
    ZIO
      .absolve(client.execute {
        search("house-index-3").sortBy(
          geoSort("location") points List(new GeoPoint(51.5553, -0.0921)) order SortOrder.DESC,
          scriptSort("doc['price.amount'].value / doc['area_sqft'].value") typed ScriptSortType.Number order SortOrder.ASC
        )
      }.map(_.toEither.left.map(_.asException)))
      .map(_.toString)
}

object SearchServiceLive {
  def layer: ULayer[SearchServiceLive] = ZLayer.succeed {
    val s = new SearchServiceLive
    Console.println("Search service constructed!")
    s
  }
}
