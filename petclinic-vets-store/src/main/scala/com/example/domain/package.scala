package com.example

package object domain {

  case class Specialty(name: String)

  case class Vet(
      fistName: String,
      lastName: String,
      specalities: List[Specialty]
    )

}
