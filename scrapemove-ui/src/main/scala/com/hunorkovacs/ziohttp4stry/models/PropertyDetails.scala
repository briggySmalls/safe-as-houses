package com.hunorkovacs.ziohttp4stry.models

import cats.instances.float
import com.hunorkovacs.ziohttp4stry.models.PropertyDetails.{ ListingUpdate, Price, Station }
import org.http4s.blaze.http.Url
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.auto._
import com.hunorkovacs.ziohttp4stry.utils.Configs.snakeCaseConfig
import PropertyDetails._

import java.time.Instant

@ConfiguredJsonCodec
case class PropertyDetails(
  bedrooms: Int,
  bathrooms: Option[Int],
  number_of_images: Int,
  number_of_floorplans: Int,
  summary: Option[String],
  displayAddress: Option[String],
  countryCode: Option[String],
  location: Location,
  propertySubType: Option[String],
  listingUpdate: ListingUpdate,
  price: Price,
  transactionType: String,
  productLabel: String,
  commercial: Boolean,
  development: Boolean,
  residential: Boolean,
  students: Boolean,
  auction: Boolean,
  feesApply: Boolean,
  displaySize: String,
  propertyUrl: String,
  contactUrl: String,
  firstVisibleDate: Instant,
  title: String,
  description: String,
  shareDescription: String,
  propertyPhrase: String,
  keyFeatures: List[String],
  postcode: String,
  images: List[Url],
  floorplans: List[Url],
  nearestStations: List[Station],
  brochures: List[String]
)

object PropertyDetails {
  case class Station(
    name: String,
    types: List[String],
    distance: Double,
    unit: String
  )

  case class ListingUpdate(
    reason: String,
    date: Instant
  )

  case class Location(
    lat: Option[Double],
    lon: Double
  )

  @ConfiguredJsonCodec
  case class Price(
    amount: Option[Int],
    currencyCode: Option[String],
    frequency: Option[String],
    qualifier: Option[String]
  )
}
