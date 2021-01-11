package com.example

import zio.Has

package object domain {
  type PetRepository = Has[PetRepository.Service]
}
