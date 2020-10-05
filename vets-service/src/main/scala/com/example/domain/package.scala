package com.example

import zio.Has

package object domain {
  type VetRepository = Has[VetRepository.Service]
}
