package com.example.model

import com.example.config.Configuration.DbConfig
import doobie.implicits._
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill._
import zio.Has
import zio.RIO
import zio.Ref
import zio.Task
import zio.URLayer
import zio.ZLayer
import zio.interop.catz._

final case class Specialty(id: Int, name: String)

final case class Vet(
    id: Int,
    firstName: String,
    lastName: String
  )

final case class VetSpecialty(vetId: Int, specialtyId: Int)

object VetDao {

  trait Service {
    def findAll: Task[List[(Vet, Option[Specialty])]]
  }

  val mySql: URLayer[DbTransactor, VetDao] = ZLayer.fromService(resource =>
    new VetDao.Service {

      private val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)

      import dc._

      private val vets = quote(querySchema[Vet]("vets"))

      private val vetSpecialities = quote(querySchema[VetSpecialty]("vet_specialties"))

      private val specialties = quote(querySchema[Specialty]("specialties"))

      def findAll: zio.Task[List[(Vet, Option[Specialty])]] =
        dc.run(
            quote {
              for {
                v <- vets
                vs <- vetSpecialities.leftJoin(_.vetId == v.id)
                s <- specialties.leftJoin(s => vs.exists(_.specialtyId == s.id))
              } yield {
                (v, s)
              }
            }
          )
          .transact(resource.xa)

    }
  )

  val inMemory: URLayer[Has[Ref[List[(Vet, Option[Specialty])]]], VetDao] =
    ZLayer.fromService(ref =>
      new VetDao.Service {
        def findAll: zio.Task[List[(Vet, Option[Specialty])]] = ref.get
      }
    )

  def findAll: RIO[VetDao, List[(Vet, Option[Specialty])]] =
    RIO.accessM(_.get.findAll)
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
