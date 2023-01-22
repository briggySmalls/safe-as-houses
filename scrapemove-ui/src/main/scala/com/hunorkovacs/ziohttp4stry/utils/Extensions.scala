package com.hunorkovacs.ziohttp4stry.utils

import zio.{ Console, RIO, Task }

object Extensions {
  implicit class TaskExtensions[R, A](val underlying: RIO[R, A]) {
    def logIssues() =
      underlying.absorb.tapErrorTrace {
        case (t, st) => Console.printLineError(st)
      }
  }
}
