package com.example.model

import doobie.implicits._
import doobie.quill.DoobieContext
import io.getquill._
import zio.Task
import zio.URLayer
import zio.ZLayer
import zio.interop.catz._
import zio.macros.accessible

@accessible
object OwnerDao {
  trait Service {
    def findAll: Task[List[(Owner, Option[(Pet, PetType)])]]
    def findById(ownerId: Int): Task[List[(Owner, Option[(Pet, PetType)])]]
    def insert(owner: Owner): Task[Owner]
    def update(owner: Owner): Task[Owner]
  }

  val mySql: URLayer[DbTransactor, OwnerDao] =
    ZLayer.fromService(resource =>
      new OwnerDao.Service {

        private val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)

        import dc._

        private val pets = quote(querySchema[Pet]("pets"))

        private val types = quote(querySchema[PetType]("types"))

        private val owners = quote(querySchema[Owner]("owners"))

        def findAll: Task[List[(Owner, Option[(Pet, PetType)])]] =
          dc.run(quote {
              for {
                o <- owners
                maybePets <- pets.leftJoin(_.ownerId == o.id)
                maybePetTypes <- types.leftJoin(pt => maybePets.exists(_.typeId == pt.id))
              } yield { (o, maybePets.flatMap(p => maybePetTypes.map(pt => (p, pt)))) }
            })
            .transact(resource.xa)

        def findById(ownerId: Int): Task[List[(Owner, Option[(Pet, PetType)])]] =
          dc.run(quote {
              for {
                o <- owners.filter(_.id == lift(ownerId))
                maybePets <- pets.leftJoin(_.ownerId == o.id)
                maybePetTypes <- types.leftJoin(pt => maybePets.exists(_.typeId == pt.id))
              } yield { (o, maybePets.flatMap(p => maybePetTypes.map(pt => (p, pt)))) }
            })
            .transact(resource.xa)

        def insert(owner: Owner): Task[Owner] =
          dc.run(owners.insert(lift(owner)).returningGenerated(_.id))
            .transact(resource.xa)
            .map(id => owner.copy(id = id))

        def update(owner: Owner): zio.Task[Owner] =
          dc.run(
              owners
                .filter(_.id == lift(owner.id))
                .update(
                  _.firstName -> lift(owner.firstName),
                  _.lastName -> lift(owner.lastName),
                  _.address -> lift(owner.address),
                  _.city -> lift(owner.city),
                  _.telephone -> lift(owner.telephone)
                )
            )
            .transact(resource.xa)
            .map(_ => owner)
      }
    )
}
