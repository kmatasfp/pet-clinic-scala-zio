package com.example

import com.example.config.Configuration._
import zio.Has

package object config {
  type Configuration = Has[DbConfig]
}
