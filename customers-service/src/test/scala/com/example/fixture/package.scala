package com.example

import zio.Has
import zio.ZLayer
import com.example.config.Configuration

package object fixture {
  type RunningMysql = Has[RunningMysql.Service]
  type OpenPortFinder = Has[OpenPortFinder.Service]

  val mysqlDbConf = ZLayer.fromEffect(for {
    dbUser <- RunningMysql.username
    dbPassword <- RunningMysql.password
    dbUrl <- RunningMysql.jdbcUrl
    jdbcClassName <- RunningMysql.driverClassName
  } yield {
    Configuration.DbConfig(jdbcClassName, dbUrl, dbUser, dbPassword)
  })
}
