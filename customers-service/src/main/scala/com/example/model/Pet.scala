package com.example.model

import java.time.LocalDate

final case class Pet(
    id: Int = 0,
    name: String,
    birthDate: LocalDate,
    typeId: Int,
    ownerId: Int
  )
