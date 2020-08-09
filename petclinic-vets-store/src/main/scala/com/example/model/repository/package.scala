package com.example.model

import com.example.config.Configuration.DbConfig
import doobie.util.transactor.Transactor
import zio.{ Has, Task, URLayer, ZLayer }
import zio.interop.catz._

package object repository {

  type DbTransactor = Has[DbTransactor.Resource]
  type VetsRepository = Has[VetsRepository.Service]

  object DbTransactor {
    trait Resource {
      val xa: Transactor[Task]
    }

    val h2: URLayer[Has[DbConfig], DbTransactor] = ZLayer.fromService { db =>
      new Resource {
        val xa: Transactor[Task] =
          Transactor.fromDriverManager(db.driver, db.url, db.user, db.password)
      }
    }
  }

}
