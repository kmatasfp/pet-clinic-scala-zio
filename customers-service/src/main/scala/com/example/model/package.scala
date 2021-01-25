package com.example

import zio.Has

package object model {

  type PetDao = Has[PetDao.Service]
  type OwnerDao = Has[OwnerDao.Service]
  type DbTransactor = Has[DbTransactor.Resource]

}
