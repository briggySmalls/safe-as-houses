package com.hunorkovacs.ziohttp4stry.utils

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
}
