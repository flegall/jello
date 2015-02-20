package com.xebialabs

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

package object jello {

  implicit val timeout: Duration = 10 seconds

  implicit class PrintableFuture[T](f: Future[T]) {

    def thenPrint(): Future[T] = f.andThen {
        case Success(t) => println(t)
        case Failure(e) => e.printStackTrace()
      }
  }
}
