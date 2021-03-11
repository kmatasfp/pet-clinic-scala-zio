package com.example

import com.example.config.Configuration
import zio.{Has, ZLayer}

package object fixture {
  type RunningMysql = Has[RunningMysql.Service]
  type OpenPortFinder = Has[OpenPortFinder.Service]

  val mysqlDbConf: ZLayer[Has[RunningMysql.Service] with Any, Throwable, Has[Configuration.DbConfig]] =
    ZLayer.fromEffect(for {
      dbUser <- RunningMysql.username
      dbPassword <- RunningMysql.password
      dbUrl <- RunningMysql.jdbcUrl
      jdbcClassName <- RunningMysql.driverClassName
    } yield Configuration.DbConfig(jdbcClassName, dbUrl, dbUser, dbPassword))
}
