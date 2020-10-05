package com.example.domain

import com.example.model.{ VetDao, Vet => MVet, Specialty => MSpecialty }
import com.example.domain.{ Specialty, Vet, VetRepository }
import zio.{ Ref, ZLayer }
import zio.test._
import zio.test.DefaultRunnableSpec

object VetRepositorySpec extends DefaultRunnableSpec {
  def spec = suite("VetRepository")(
    testM("should return list of Vets") {

      assertM(VetRepository.all)(
        Assertion.hasSameElements(
          List(
            Vet(1, "James", "Carter", List.empty),
            Vet(2, "Helen", "Leary", List(Specialty("radiology"))),
            Vet(3, "Linda", "Douglas", List(Specialty("surgery"), Specialty("dentistry"))),
            Vet(4, "Rafael", "Ortega", List(Specialty("surgery"))),
            Vet(5, "Henry", "Stevens", List(Specialty("radiology"))),
            Vet(6, "Sharon", "Jenkins", List.empty)
          )
        )
      ).provideLayer(
        ZLayer.fromEffect(
          Ref.make(
            List(
              MVet(1, "James", "Carter") -> None,
              MVet(2, "Helen", "Leary") -> Some(MSpecialty(1, "radiology")),
              MVet(3, "Linda", "Douglas") -> Some(MSpecialty(2, "surgery")),
              MVet(3, "Linda", "Douglas") -> Some(MSpecialty(3, "dentistry")),
              MVet(4, "Rafael", "Ortega") -> Some(MSpecialty(2, "surgery")),
              MVet(5, "Henry", "Stevens") -> Some(MSpecialty(1, "radiology")),
              MVet(6, "Sharon", "Jenkins") -> None
            )
          )
        ) >>> VetDao.inMemory >>> VetRepository.live
      )
    }
  )
}
