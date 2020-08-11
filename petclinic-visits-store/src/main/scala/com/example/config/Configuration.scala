package com.example.config

object Configuration {
  final case class DbConfig(
      driver: String,
      url: String,
      user: String,
      password: String
    )
}
