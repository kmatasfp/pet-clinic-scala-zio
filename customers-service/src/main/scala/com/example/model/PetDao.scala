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
object PetDao {
  trait Service {
    def findById(petId: Int): Task[List[(Pet, PetType, Owner)]]
    def getPetTypes: Task[List[PetType]]
    def save(pet: Pet): Task[Pet]
  }

  val mySql: URLayer[DbTransactor, PetDao] = ZLayer.fromService(resource =>
    new PetDao.Service {

      private val dc = new DoobieContext.MySQL[SnakeCase](SnakeCase)

      import dc._

      private val pets = quote(querySchema[Pet]("pets"))

      private val types = quote(querySchema[PetType]("types"))

      private val owners = quote(querySchema[Owner]("owners"))

      def findById(petId: Int): Task[List[(Pet, PetType, Owner)]] = 
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

      def getPetTypes: Task[List[PetType]] =
        dc.run(types).transact(resource.xa)

      def save(pet: Pet): Task[Pet] =
        dc.run(pets.insert(lift(pet)).returningGenerated(_.id)).transact(resource.xa)
        .map(id => pet.copy(id = id))    
        
    }
  )
}
