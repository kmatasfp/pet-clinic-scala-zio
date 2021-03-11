package com.example.model

import com.example.config.Configuration.DbConfig
import doobie.util.transactor.Transactor
import zio.interop.catz._
import zio.{Has, Task, URLayer, ZLayer}

object DbTransactor {
  trait Resource {
    val xa: Transactor[Task]
  }

  val live: URLayer[Has[DbConfig], DbTransactor] = ZLayer.fromService { db =>
    new Resource {
      val xa: Transactor[Task] =
        Transactor.fromDriverManager(db.driver, db.url, db.user, db.password)
    }
  }
}
