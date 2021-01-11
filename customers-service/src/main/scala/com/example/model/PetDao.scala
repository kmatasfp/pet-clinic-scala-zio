package com.example.model

import java.time.LocalDate

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

case class PetOwner(
    id: Int = 0,
    firstName: String,
    lastName: String,
    address: String,
    city: String,
    telephone: String
  )

case class PetType(id: Int = 0, name: String)

case class Pet(
    id: Int = 0,
    name: String,
    birthDate: LocalDate,
    typeId: Int,
    ownerId: Int
  )

object PetDao {
  trait Service {
    def findById(petId: Int): Task[List[(Pet, PetType, PetOwner)]]
  }

  val mySql: URLayer[DbTransactor, PetDao] = ZLayer.fromService(resource =>
    new PetDao.Service {

      private val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)

      import dc._

      private val pets = quote(querySchema[Pet]("pets"))

      private val types = quote(querySchema[PetType]("types"))

      private val owners = quote(querySchema[PetOwner]("owners"))

      def findById(petId: Int): zio.Task[List[(Pet, PetType, PetOwner)]] = 
        dc.run(
            quote {
                for {
                    p <- pets if(p.id == lift(petId))
                    t <- types if(t.id == p.typeId)
                    o <- owners if(o.id == p.ownerId) 
                } yield {
                    (p, t, o)
                }
            }
        ).transact(resource.xa)
        
    }
  )

  val inMemory: URLayer[Has[Ref[List[(Pet, PetType, PetOwner)]]], PetDao] =
    ZLayer.fromService(ref =>
      new PetDao.Service {
        def findById(petID: Int): zio.Task[List[(Pet, PetType, PetOwner)]] = ref.get
      }
    )

  def findById(petId: Int): RIO[PetDao, List[(Pet, PetType, PetOwner)]] =
    RIO.accessM(_.get.findById(petId))
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
