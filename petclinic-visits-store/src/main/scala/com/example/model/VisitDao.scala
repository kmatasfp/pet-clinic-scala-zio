package com.example.model

import java.time.LocalDate

import com.example.config.Configuration.DbConfig
import doobie.implicits._
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill._
import zio.{ Has, RIO, Task, URLayer, ZLayer }
import zio.interop.catz._

case class Visit(
    id: Int = 0,
    petId: Int,
    visitDate: LocalDate,
    description: String
  )

object VisitDao {

  trait Service {
    def save(v: Visit): Task[Visit]

    def findByPetId(petid: Int): Task[List[Visit]]

    def findByPetIdIn(petIds: List[Int]): Task[List[Visit]]
  }

  val mySql: URLayer[DbTransactor, VisitDao] = ZLayer.fromService(resource =>
    new VisitDao.Service {

      private val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)

      import dc._

      private val visits = quote(querySchema[Visit]("visits"))

      def save(v: Visit): Task[Visit] =
        dc.run(quote(visits.insert(lift(v)).returningGenerated(_.id)))
          .transact(resource.xa)
          .map(id => v.copy(id = id))

      def findByPetId(petId: Int): Task[List[Visit]] =
        dc.run(visits.filter(v => v.petId == lift(petId))).transact(resource.xa)

      def findByPetIdIn(petIds: List[Int]): zio.Task[List[Visit]] =
        dc.run(visits.filter(v => liftQuery(petIds).contains(v.petId))).transact(resource.xa)

    }
  )

  def save(v: Visit): RIO[VisitDao, Visit] =
    RIO.accessM(_.get.save(v))

  def findByPetId(petId: Int): RIO[VisitDao, List[Visit]] =
    RIO.accessM(_.get.findByPetId(petId))

  def findByPetIdIn(petIds: List[Int]): RIO[VisitDao, List[Visit]] =
    RIO.accessM(_.get.findByPetIdIn(petIds))

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
