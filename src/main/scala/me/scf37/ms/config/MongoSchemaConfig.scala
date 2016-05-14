package me.scf37.ms.config

import me.scf37.config2.Config
import me.scf37.config2.Flags

class MongoSchemaConfig(args: Map[String, String]) {
  val source = Config.from(System.getenv())
    .overrideWith(args)

  val flags = new Flags(source.properties())

  val host: String = flags("host", "MongoDB hostname", Some("localhost"))(identity)
  val port: Int = flags("port", "MongoDB port", Some("27017"))(_.toInt)
  val db: String = flags("db", "Database to analyze", Some(""))(identity)
  val coll: String = flags("coll", "Collection to analyze", Some(""))(identity)
  val authDb: String = flags("authDb", "MongoDB database to use for authentication", Some("test"))(identity)
  val login: String = flags("login", "login name to use to connect to the database", Some(""))(identity)
  val password: String = flags("password", "password to use to connect to the database", Some(""))(identity)
  val enumMaxCount: Int = flags("enumMaxCount", "Enables string enum analysis. Value is maximum count of values for field to be recognized as enum", Some("4"))(_.toInt)
  val enumMaxLength: Int = flags("enumMaxLength", "Maximum length of string to be recognized as enum value", Some("32"))(_.toInt)

}
