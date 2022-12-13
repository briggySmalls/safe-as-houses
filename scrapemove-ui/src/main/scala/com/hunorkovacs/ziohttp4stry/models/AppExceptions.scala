package com.hunorkovacs.ziohttp4stry.models

import cats.data.NonEmptySeq

object AppExceptions {
  class SearchException(causes: Seq[Exception]) extends Exception {
    override def getMessage: String = causes match {
      case head :: Nil => head.getMessage
      case ls          => s"Multi-cause exception: ${ls.map(_.getMessage)}"
    }

    override def getCause: Throwable = createCause(causes)

    private def createCause(causes: Seq[Exception]): Throwable = causes match {
      case head :: Nil  => head
      case head :: tail => head.initCause(createCause(tail))
    }
  }
}
