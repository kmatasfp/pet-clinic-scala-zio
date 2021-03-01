package com.example.fixture

import java.net.ServerSocket

import zio.Ref
import zio.Task
import zio.UIO
import zio.ZLayer
import zio.macros.accessible

@accessible
object OpenPortFinder {

  trait Service {

    def find: UIO[Int]
  }

  private def findOpenPort(portToTry: Ref[Int]) =
    (for {
      port <- portToTry.getAndUpdate(_ + 1)
      _ <- Task(new ServerSocket(port)).bracket(s => Task(s.close()).orDie)(_ =>
        UIO.effectTotal(true)
      )
    } yield {
      port
    }).retryN(100).orDie

  def live(initialPortToTry: Ref[Int]): ZLayer[Any, Nothing, OpenPortFinder] =
    ZLayer.fromEffect(UIO.effectTotal(new OpenPortFinder.Service {
      def find: UIO[Int] =
        findOpenPort(initialPortToTry)
    }))

}
