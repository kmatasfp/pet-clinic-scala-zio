package com.example.model

import com.example.config.Configuration.DbConfig
import doobie.implicits._
import doobie.quill.DoobieContext
import doobie.util.transactor.Transactor
import io.getquill._
import io.getquill.idiom.Idiom
import io.getquill.context.Context
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.jdbc.JdbcContextBase
import zio.{ Has, RIO, Task, ULayer, URLayer, ZLayer }
import zio.interop.catz._
import zio.Ref

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
      def findAll: zio.Task[List[(Vet, Option[Specialty])]] = {

        val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)
          with DbQueries[MySQLDialect, SnakeCase]

        import dc._

        dc.run(all).transact(resource.xa)

      }
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

trait DbQueries[I <: Idiom, N <: NamingStrategy] { this: Context[I, N] =>

  private val vets = quote(querySchema[Vet]("vets"))

  private val vetSpecialities = quote(querySchema[VetSpecialty]("vet_specialties"))

  private val specialties = quote(querySchema[Specialty]("specialties"))

  val all = quote {
    for {
      v <- vets
      vs <- vetSpecialities.leftJoin(_.vetId == v.id)
      s <- specialties.leftJoin(s => vs.exists(_.specialtyId == s.id))
    } yield {
      (v, s)
    }
  }
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
