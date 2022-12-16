package com.hunorkovacs.ziohttp4stry.utils

import io.circe.generic.extras.Configuration

object Configs {
  implicit val snakeCaseConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
