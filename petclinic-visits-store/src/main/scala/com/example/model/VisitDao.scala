package com.example.model

import java.time.LocalDate

import com.example.config.Configuration.DbConfig
import doobie.implicits._
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill._
import io.getquill.idiom.Idiom
import io.getquill.context.Context
import zio.{ Has, RIO, Ref, Task, URLayer, ZLayer }
import zio.interop.catz._

case class Visit(
    id: Option[Int],
    petId: Int,
    date: LocalDate,
    descripion: String
  )

object VisitDao {

  trait Service {
    def save(v: Visit): Task[Visit]

    def findByPetId(petid: Int): Task[List[Visit]]
  }

  val mySql: URLayer[DbTransactor, VisitDao] = ZLayer.fromService(resource =>
    new VisitDao.Service {
      def save(v: Visit): Task[Visit] = ???

      def findByPetId(petId: Int): Task[List[Visit]] = ???
    }
  )

  def save(v: Visit): RIO[VisitDao, Visit] =
    RIO.accessM(_.get.save(v))

  def findByPetId(petId: Int): RIO[VisitDao, List[Visit]] =
    RIO.accessM(_.get.findByPetId(petId))

}

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
