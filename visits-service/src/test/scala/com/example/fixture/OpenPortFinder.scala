package com.example.fixture

import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger

import zio.Task
import zio.UIO
import zio.ZLayer
import zio.macros.accessible

@accessible
object OpenPortFinder {

  val portRef = new AtomicInteger(49352)

  trait Service {

    def find: UIO[Int]
  }

  private def findOpenPort =
    (for {
      portToTry <- UIO.effectTotal(portRef.getAndIncrement())
      _ <- Task(new ServerSocket(portToTry)).bracket(s => Task(s.close()).orDie)(_ =>
        UIO.effectTotal(true)
      )
    } yield {
      portToTry
    }).retryN(100).orDie

  val live: ZLayer[Any, Nothing, OpenPortFinder] = {

    ZLayer.fromEffect(UIO.effectTotal(new OpenPortFinder.Service {
      def find: UIO[Int] =
        findOpenPort
    }))

  }
}
