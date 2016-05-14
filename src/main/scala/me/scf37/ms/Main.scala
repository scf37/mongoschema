package me.scf37.ms

import java.util.Collections

import com.mongodb.MongoClient
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import com.mongodb.client.MongoCollection
import me.scf37.ms.config.MongoSchemaConfig
import me.scf37.ms.schema.Type
import org.bson.BsonDocument
import org.bson.Document

import scala.collection.JavaConversions._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
  * Created by asm on 14.05.16.
  */
object Main {

  def main(argv: Array[String]): Unit = {
    val conf = parseArgv(argv) match {
      case Success(source) => new MongoSchemaConfig(source)
      case Failure(e) =>
        println(e.getMessage)
        printUsage(new MongoSchemaConfig(Map.empty))
        System.exit(1)
        ???
    }

    if (conf.flags.errors.nonEmpty) {
      conf.flags.errors.foreach(println)
      println()
      printUsage(conf)
      System.exit(1)
    }

    try {
      doMain(conf)
    } catch {
      case e: Exception =>
        println(e.getMessage)
    }

  }

  def doMain(conf: MongoSchemaConfig): Unit = {
    val client = if (conf.login.isEmpty) {
      new MongoClient(new ServerAddress(conf.host, conf.port))
    } else {
      new MongoClient(new ServerAddress(conf.host, conf.port),
        Collections.singletonList(MongoCredential.createScramSha1Credential(conf.login, conf.authDb, conf.password.toCharArray)))
    }

    client.listDatabaseNames().foreach { db =>
      if (conf.db.isEmpty || conf.db == db) {
        client.getDatabase(db).listCollectionNames().foreach { coll =>
          if (conf.coll.isEmpty || conf.coll == coll) {
            analyze(client.getDatabase(db).getCollection(coll))
          }
        }
      }
    }
  }

  def analyze(coll: MongoCollection[Document]): Unit = {
    var t: Option[Type] = None

    coll.find(new Document(), classOf[BsonDocument]).foreach { doc =>
      t = t match {
        case None => Some(Type.parse(doc))
        case Some(t) => Some(Type.merge(t, Type.parse(doc)))
      }
    }
    println(coll.getNamespace)
    println(t.map(Type.print(_, 8)).getOrElse("Empty collection"))
    println()
  }

  private[this] def printUsage(conf: MongoSchemaConfig) = {
    println("MongoDB schema analyzer utility")
    println("ms [-parameterName parameterValue]")
    print(conf.flags.usageString)
    println("Parameters are accepted via command line or env variables")
  }

  private[this] def parseArgv(argv: Array[String]): Try[Map[String, String]] = Try {
    val conf = new MongoSchemaConfig(Map.empty)
    val flagNames = conf.flags.flags.map(_.name).toSet

    if (argv.length % 2 != 0) {
      throw new RuntimeException("Last parameter is missing a value")
    }

    argv.grouped(2).map(_ match {
      case Array(key, value) =>
        if (!key.startsWith("-")) {
          throw new RuntimeException(s"Parameter name '$key' must start with '-'")
        }

        if (!flagNames.contains(key.drop(1))) {
          throw new RuntimeException(s"Unknown parameter name: '$key'")
        }

        key.drop(1) -> value
    }).toMap

  }

}
