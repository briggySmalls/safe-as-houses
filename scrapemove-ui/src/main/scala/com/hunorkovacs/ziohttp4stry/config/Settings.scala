package com.hunorkovacs.ziohttp4stry.config

import zio.config.ReadError
import zio.{ config, ZLayer }
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource

import java.net.URL

case class Settings(
  elasticSettings: ElasticSettings,
  serverSettings: ServerSettings
)

case class ElasticSettings(
  url: String,
  index: String
)

case class ServerSettings(
  host: String,
  port: Int
)

object Settings {
  def layer: ZLayer[Any, ReadError[String], Settings] =
    ZLayer {
      config.read {
        descriptor[Settings].from(
          TypesafeConfigSource.fromResourcePath
        )
      }
    }
}
