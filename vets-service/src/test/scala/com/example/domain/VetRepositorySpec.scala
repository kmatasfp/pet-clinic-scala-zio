package com.example.domain

import com.example.domain.Specialty
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

      val vetsWithSpecialtiesGen =
        Gen
          .listOf((DeriveGen[MVet] <&> Gen.listOf(DeriveGen[MSpecialty])))
          .map(_.distinctBy(_._1.id))

      checkM(vetsWithSpecialtiesGen) {
        vetsWithSpecialties =>
          assertM(VetRepository.all)(
            hasSameElements(vetsWithSpecialties.map {
              case (mVet, mSpecialties) =>
                Vet(
                  mVet.id,
                  mVet.firstName,
                  mVet.lastName,
                  mSpecialties.map(ms => Specialty(ms.name))
                )
            })
          ).provideLayer(
            ZLayer.fromEffect(
              Ref.make {
                vetsWithSpecialties.flatMap {
                  case (mvet, Nil) =>
                    List((mvet -> None))
                  case (mvet, mSpecialties) =>
                    mSpecialties.map(s => mvet -> Some(s))
                }
              }
            ) >>> VetDao.inMemory >>> VetRepository.live
          )
      }

    }
  )
}
