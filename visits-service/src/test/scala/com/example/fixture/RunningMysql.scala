package com.example.fixture

import com.dimafeng.testcontainers.MySQLContainer
import zio.macros.accessible
import zio.{Task, UIO, ZLayer, ZManaged}

import scala.jdk.CollectionConverters._

@accessible
object RunningMysql {
  trait Service {
    def username: UIO[String]
    def password: UIO[String]
    def driverClassName: UIO[String]
    def jdbcUrl: Task[String]
  }

  val live: ZLayer[Any, Throwable, RunningMysql] = {
    val acquire = Task {

      val mysql = MySQLContainer().configure { c =>
        c.withUsername("root")
        c.withPassword("")
        c.withInitScript("db/mysql/init.sql")
        c.withTmpFs(Map("/testtmpfs" -> "rw").asJava)
        c.withDatabaseName("petclinic")
      }

      mysql.start()

      mysql
    }

    val release = (m: MySQLContainer) => Task(m.close()).orDie

    ZManaged
      .make(acquire)(release)
      .map(mc =>
        new RunningMysql.Service {
          def username: UIO[String] = UIO.effectTotal(mc.username)
          def password: UIO[String] = UIO.effectTotal(mc.password)
          def driverClassName: UIO[String] = UIO.effectTotal(mc.driverClassName)
          def jdbcUrl: Task[String] = Task(mc.jdbcUrl)

        }
      )
      .toLayer
  }

}
