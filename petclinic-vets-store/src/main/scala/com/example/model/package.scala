package com.example

import io.getquill.Embedded

package object model {

  case class Specialty(id: Int, first: String) extends Embedded

  case class Vet(
      id: Int,
      firstName: String,
      lastName: String
    )

  case class VetSpecialty(vetId: Int, specaltyId: Int)
}
