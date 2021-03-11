package com.example.domain

import com.example.domain.{Specialty, Vet, VetRepository}
import com.example.model.{Specialty => MSpecialty, Vet => MVet, VetDao}
import zio.random.Random
import zio.test.Assertion._
import zio.test.magnolia._
import zio.test.{DefaultRunnableSpec, _}
import zio.{Ref, ZLayer}

object VetRepositorySpec extends DefaultRunnableSpec {
  def spec: Spec[TestConfig with Random with Sized, TestFailure[Throwable], TestSuccess] = suite("VetRepository")(
    testM("should return list of Vets") {

      val vetsWithSpecialtiesGen =
        Gen
          .listOf((DeriveGen[MVet] <&> Gen.listOf(DeriveGen[MSpecialty])))
          .map(_.distinctBy(_._1.id))

      checkM(vetsWithSpecialtiesGen) { vetsWithSpecialties =>
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
