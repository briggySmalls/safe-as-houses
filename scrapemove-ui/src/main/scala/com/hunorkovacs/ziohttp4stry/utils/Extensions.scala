package com.hunorkovacs.ziohttp4stry.utils

import com.hunorkovacs.ziohttp4stry.models.AppExceptions.SearchException
import com.sksamuel.elastic4s.{ ElasticError, Response }
import zio.{ Console, IO, RIO, Task }

import java.io.{ IOException, PrintWriter, StringWriter }

object Extensions {
  implicit class TaskExtensions[R, A](val underlying: RIO[R, A]) {
    def logIssues() =
      underlying.absorb.tapError { err =>
        Console.printLineError(printError(err))
      }

    private def printError(err: Throwable): String = {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)
      err.printStackTrace(pw)
      sw.toString
    }
  }

  implicit class ElasticErrorExtensions(val underlying: ElasticError) {
    def toDomainException: Exception =
      if (underlying.failedShards.isEmpty) underlying.asException
      else new SearchException(underlying.failedShards.flatMap(_.reason).map(_.asException))
  }

  implicit class ResponseExtensions[U](val underlying: Response[U]) {
    def toSubmergableError: Either[Exception, U] =
      underlying.toEither.left.map(_.toDomainException)
  }
}
