package com.example

import zio.Has

package object fixture {
  type RunningMysql = Has[RunningMysql.Service]
}
