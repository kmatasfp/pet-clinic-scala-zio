package com.example.domain

import scala.util.Random

import com.example.domain.Vet
import com.example.domain.VetRepository
import com.example.model.VetDao
import com.example.model.{ Specialty => MSpecialty }
import com.example.model.{ Vet => MVet }
import zio.Ref
import zio.ZLayer
import zio.test.Assertion._
import zio.test.DefaultRunnableSpec
import zio.test._
import zio.test.magnolia._

object VetRepositorySpec extends DefaultRunnableSpec {
  def spec = suite("VetRepository")(
    testM("should return list of Vets") {

      checkM(Gen.mapOf(Gen.anyInt, DeriveGen[Vet])) {
        someGenVets =>
          val vets = someGenVets.map { case (k, v) => k -> v.copy(id = k) }.values.toList

          assertM(VetRepository.all)(
            hasSameElements(vets)
          ).provideLayer(
            ZLayer.fromEffect(
              Ref.make {
                vets.flatMap { vet =>
                  if (vet.specalities.isEmpty)
                    List(MVet(vet.id, vet.fistName, vet.lastName) -> None)
                  else
                    vet.specalities.foldRight(List.empty[(MVet, Option[MSpecialty])]) {
                      case (s, acc) =>
                        (MVet(vet.id, vet.fistName, vet.lastName) -> Some(
                          MSpecialty(Random.nextInt(), s.name)
                        )) +: acc
                    }

                }
              }
            ) >>> VetDao.inMemory >>> VetRepository.live
          )
      }

    }
  )
}
