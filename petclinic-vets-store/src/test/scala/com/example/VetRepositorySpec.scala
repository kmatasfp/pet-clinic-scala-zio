package com.example

import zio.test._
import zio.test.DefaultRunnableSpec
import com.example.domain.VetRepository
import com.example.domain.Vet
import com.example.domain.Specialty
import zio.ZLayer
import zio.Ref
import com.example.model.VetDao

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
              model.Vet(1, "James", "Carter") -> None,
              model.Vet(2, "Helen", "Leary") -> Some(model.Specialty(1, "radiology")),
              model.Vet(3, "Linda", "Douglas") -> Some(model.Specialty(2, "surgery")),
              model.Vet(3, "Linda", "Douglas") -> Some(model.Specialty(3, "dentistry")),
              model.Vet(4, "Rafael", "Ortega") -> Some(model.Specialty(2, "surgery")),
              model.Vet(5, "Henry", "Stevens") -> Some(model.Specialty(1, "radiology")),
              model.Vet(6, "Sharon", "Jenkins") -> None
            )
          )
        ) >>> VetDao.inMemory >>> VetRepository.live
      )
    }
  )
}
