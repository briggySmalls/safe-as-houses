package com.hunorkovacs.ziohttp4stry.models

import cats.instances.float
import com.hunorkovacs.ziohttp4stry.models.PropertyDetails.{ ListingUpdate, Price, Station }
import org.http4s.blaze.http.Url
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.auto._
import com.hunorkovacs.ziohttp4stry.utils.Configs.snakeCaseConfig
import PropertyDetails._
import com.sun.tools.javac.code.TypeTag
import scalatags.Text.TypedTag
import scalatags.Text.all.{ input, _ }
import cats.implicits._

import java.time.Instant
import java.util.{ Currency, Locale }

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
  brochures: List[String],
  areaSqft: Option[Double]
) {
  def present: TypedTag[String] = {
    val components = Seq(
      images.headOption.map(url =>
        img(
          `class` := "object-cover w-full rounded-t-lg h-96 md:h-auto md:w-48 md:rounded-none md:rounded-l-lg",
          src := url
        )
      ),
      Some(
        div(
          `class` := "flex flex-col justify-between p-4 leading-normal text-gray-700 dark:text-gray-400",
          h5(
            `class` := "mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white",
            displayAddress
          ),
          p(
            `class` := "mb-3 font-normal",
            shareDescription
          ),
          div(
            `class` := "flex justify-between",
            attributes.toSeq
          )
        )
      )
    ).flatten

    a(
      href := s"https://www.rightmove.co.uk$propertyUrl",
      target := "_blank",
      `class` :=
        """
          |flex flex-col items-center
          |border rounded-lg shadow-md
          |md:flex-row md:max-w-6xl
          |bg-white hover:bg-gray-100 visited:bg-gray-50
          |dark:border-gray-700 dark:bg-gray-800 dark:hover:bg-gray-700 dark:visited:bg-gray-600
          |""".stripMargin,
      components
    )
  }

  private def attributes = {

    val attributes: Seq[(String, Option[Any])] = Seq(
      "Price"     -> price.displayPrice,
      "Size"      -> areaSqft,
      "£/sqft"    -> (areaSqft, price.amount).tupled.map { case (area, price) => Math.round(price / area) },
      "Bathrooms" -> bathrooms,
      "Bedrooms"  -> Some(bedrooms)
    )
    attributes.collect { case (key, Some(value)) => key -> value }.map {
      case (key, value) =>
        div(
          span(`class` := "mr-1", s"$key:"),
          span(value.toString)
        )
    }
  }
}

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
  ) {
    def displayPrice: Option[String] =
      (currencyCode, amount).tupled.map {
        case (someCurrencyCode, somePrice) =>
          val currency  = Currency.getInstance(someCurrencyCode)
          val formatter = java.text.NumberFormat.getCurrencyInstance
          formatter.setCurrency(currency)
          formatter.format(somePrice)
      }
  }
}
