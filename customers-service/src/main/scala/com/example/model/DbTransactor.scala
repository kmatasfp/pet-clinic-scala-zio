package com.example.model

import com.example.config.Configuration.DbConfig
import doobie.util.transactor.Transactor
import zio.Has
import zio.Task
import zio.URLayer
import zio.ZLayer
import zio.interop.catz._

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
