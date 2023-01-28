package com.hunorkovacs.ziohttp4stry.models

import com.hunorkovacs.ziohttp4stry.models.PropertyDetails.{ ListingUpdate, Price, Station }
import org.http4s.blaze.http.Url
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.auto._
import com.hunorkovacs.ziohttp4stry.utils.Configs.snakeCaseConfig
import PropertyDetails._
import cats.data.Chain.Wrap
import scalatags.Text.TypedTag
import scalatags.Text.all._
import cats.implicits._

import java.time.{ Duration, Instant }
import java.util.Currency

@ConfiguredJsonCodec
case class PropertyDetails(
  id: Int,
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
  areaSqft: Option[Double],
  pricePerSqft: Option[Double]
) {
  def present(searchParams: SearchParams): TypedTag[String] = {
    val components = Seq(
      images.headOption.map(url =>
        img(
          `class` :=
            """
              |object-cover w-full rounded-t-lg
              |md:w-1/5 md:h-full md:rounded-none md:rounded-l-lg
              |""".stripMargin,
          src := url
        )
      ),
      Some(
        div(
          `class` :=
            """
              |flex flex-col justify-between p-4 leading-normal
              |w-full
              |text-gray-700 dark:text-gray-400
              |""".stripMargin,
          h5(
            `class` :=
              """
                |mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white
                |""".stripMargin,
            titleMarkup
          ),
          p(
            `class` := "mb-3 font-normal",
            shareDescription
          ),
          div(
            `class` := "flex flex-col md:flex-row justify-between",
            attributes.toSeq
          )
        )
      )
    ).flatten

    // Wrap the components in an anchor that links to rightmove
    val item = a(
      href := s"https://www.rightmove.co.uk$propertyUrl",
      target := "_blank",
      `class` :=
        """
          |flex flex-col items-center
          |border rounded-lg shadow
          |md:flex-row md:max-w-6xl
          |bg-white border-gray-200 hover:bg-gray-100 visited:bg-gray-50
          |dark:border-gray-700 dark:bg-gray-800 dark:hover:bg-gray-700 dark:visited:bg-gray-600
          |""".stripMargin,
      components
    )

    // Wrap the anchor in an element with a user viewed click action (if user supplied)
    searchParams.user.foldLeft(
      div(`class` := "viewed-wrapper my-2", item)
    ) {
      case (el, u) =>
        el(
          attr("hx-trigger") := "click",
          attr("hx-put") := s"/api/v1/views?user=${u.id}&document=${id}",
          attr("hx-swap") := "none"
        )
    }
  }

  def titleMarkup: Seq[TypedTag[String]] =
    Seq(
      displayAddress.map(span(_)),
      if (isListedInPast(Duration.ofDays(3)))
        Some(
          span(
            `class` := "bg-red-100 text-red-800 text-xs font-medium mr-2 px-2.5 py-0.5 rounded-full dark:bg-red-900 dark:text-red-300",
            "Past 3 days!"
          )
        )
      else
        None
    ).flatten

  def isListedInPast(duration: Duration): Boolean = firstVisibleDate.isAfter(Instant.now().minus(duration))

  private def attributes = {

    val attributes: Seq[(String, Option[Any])] = Seq(
      "Price"     -> price.displayPrice,
      "Size"      -> areaSqft.map(Math.round),
      "£/sqft"    -> pricePerSqft.map(Math.round),
      "Bathrooms" -> bathrooms,
      "Bedrooms"  -> Some(bedrooms)
    )
    attributes.map {
      case (key, value) =>
        val v = value.map(_.toString).getOrElse("?")
        div(
          span(`class` := "mr-1", s"$key:"),
          span(v)
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
