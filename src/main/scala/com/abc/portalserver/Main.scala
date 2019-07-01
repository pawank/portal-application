package com.abc.portalserver

//More at https://typelevel.org/cats-effect/datatypes/io.html
import cats.effect.{ExitCode, IO, IOApp}
//Ref detail at https://typelevel.org/cats-effect/concurrency/ref.html
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http.filter.Cors
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Await
import io.finch._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import io.finch.catsEffect._
import io.finch.circe._
import io.circe.generic.auto._

import com.twitter.finagle.oauth2.{AuthInfo, GrantResult}
import io.finch.oauth2._
import repository._

import com.outr.arango.{
  ArangoDB,
  Credentials,
  ArangoException,
  DatabaseState,
  Document,
  DocumentModel,
  Id,
  Index,
  IndexType,
  Serialization
}

object Main extends IOApp with Config with Filters {

  case class Message(hello: String)

  case class UnprotectedUser(name: String)

  def authInfo: Endpoint[IO, AuthInfo[OAuthUser]] =
    authorize[IO, OAuthUser](InMemoryDataHandler)
  def accessToken: Endpoint[IO, GrantResult] =
    issueAccessToken[IO, OAuthUser](InMemoryDataHandler)

  def users: Endpoint[IO, OAuthUser] = get("users" :: "current" :: authInfo) {
    ai: AuthInfo[OAuthUser] =>
      Ok(ai.user)
  }

  def tokens: Endpoint[IO, GrantResult] = post("users" :: "auth" :: accessToken)

  def unprotected: Endpoint[IO, UnprotectedUser] =
    get("users" :: "unprotected") {
      Ok(UnprotectedUser("unprotected"))
    }

  def healthcheck: Endpoint[IO, String] = get(pathEmpty) {
    Ok("OK")
  }

  def helloWorld: Endpoint[IO, Message] = get("hello") {
    Future.successful(Ok(Message("World")))
  }

  def hello: Endpoint[IO, Message] = get("hello" :: path[String]) { s: String =>
    Ok(Message(s))
  }
  def orientdb: Endpoint[IO, String] = get("orientdb" :: path[String]) {
    s: String =>
      import com.orientechnologies.orient.core.db.ODatabaseSession
      import com.orientechnologies.orient.core.db.OrientDB
      import com.orientechnologies.orient.core.db.OrientDBConfig
      import com.orientechnologies.orient.core.metadata.schema.OClass
      import com.orientechnologies.orient.core.metadata.schema.OType
      import com.orientechnologies.orient.core.record.OEdge
      import com.orientechnologies.orient.core.record.OVertex
      import com.orientechnologies.orient.core.sql.executor.OResult
      import com.orientechnologies.orient.core.sql.executor.OResultSet
      val client: OrientDB =
        new OrientDB("remote:localhost", OrientDBConfig.defaultConfig())
      val db: ODatabaseSession =
        client.open("portalapp", "root", "[guessme321]")

      def createPerson(
          db: ODatabaseSession,
          name: String,
          email: String,
          mobile: String
      ): OVertex = {
        val result: OVertex = db.newVertex("Candidate")
        result.setProperty("name", name)
        result.setProperty("email", email)
        result.setProperty("mobile", mobile)
        result.save()
        result
      }

      if (db.getClass("Candidate") == null) {
        db.createVertexClass("Candidate")
      }
      var person: OClass = db.getClass("Candidate");

      if (person == null) {
        person = db.createVertexClass("Candidate");
      }

      if (person.getProperty("mobile") == null) {
        person.createProperty("mobile", OType.STRING);

        person.createIndex(
          "Person_mobile_index",
          OClass.INDEX_TYPE.UNIQUE,
          "mobile"
        );
      }
      if (person.getProperty("email") == null) {
        person.createProperty("email", OType.STRING);

        person.createIndex(
          "Person_email_index",
          OClass.INDEX_TYPE.UNIQUE,
          "email"
        );
      }
      if (person.getProperty("name") == null) {
        person.createProperty("name", OType.STRING);

        person.createIndex(
          "Person_name_index",
          OClass.INDEX_TYPE.NOTUNIQUE,
          "name"
        );
      }
      if (db.getClass("AppliedFor") == null) {
        db.createEdgeClass("AppliedFor")
      }
      createPerson(db, "Pawan Kumar", "sample1@gmail.com", "879213880890")
      db.close()
      client.close()
      Ok("DONE")
  }

  def gremlinApi: Endpoint[IO, String] = get("gremlin" :: path[String]) {
    s: String =>
      import com.orientechnologies.orient.core.metadata.schema.OType
      import com.orientechnologies.orient.core.sql.executor.OResult
      import com.orientechnologies.orient.core.sql.executor.OResultSet
      import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
      import gremlin.scala._
      import java.util.{ArrayList => JArrayList}
      import org.apache.commons.configuration.BaseConfiguration
      import org.apache.tinkerpop.gremlin.orientdb._
      import scala.collection.JavaConversions._

      val graph =
        new OrientGraphFactory(
          "remote:localhost/portalapp",
          "root",
          "[guessme321]"
        ).getNoTx()
      val Candidate = "Candidate"
      val Name = Key[String]("name")
      val Email = Key[String]("email")
      val Mobile = Key[String]("mobile")
      //val pawan = graph + (Candidate, Name -> "PK", Email -> "pawan@gmail.com", Mobile -> "98989080")
      com.abc.portalserver.dsl.DSL.testing
      Ok("DONE")
  }

  def dbChecks: Endpoint[IO, String] = get("db" :: path[String]) { s: String =>
    val dbConfig = com.outr.arango.Config(
      "portalapp",
      io.youi.net.URL("http://localhost:8529"),
      true,
      credentials = Credentials("root", "[guessme321]")
    )
    //profig.Profig.loadDefaults()
    val db = new ArangoDB(
      database = dbConfig.db,
      baseURL = dbConfig.url,
      credentials = Some(dbConfig.credentials)
    )
    //val db = new ArangoDB()
    println(s"Arangodb config: $dbConfig")
    val dbConnectResponse =
      scala.concurrent.Await
        .result(db.init(), scala.concurrent.duration.Duration.Inf)
    println(dbConnectResponse)
    db.api.db.list().map { response =>
      println(response.value)
    }
    /*
    val dbExample = db.api.db("portalapp")
    val collection = dbExample.collection("sample")
    db.api.db.validate("FOR user IN sample RETURN user").map { parseResult =>
      println(parseResult)
    }*/
    Ok("OK")
  }

  //Auth examples from https://github.com/finch/finch-oauth2/blob/master/examples/src/main/scala/io/finch/oauth2/Main.scala
  /*
  def authService: Service[Request, Response] =
    Bootstrap
      .serve[Text.Plain](healthcheck)
      .serve[Application.Json](
        helloWorld :+: hello :+: tokens :+: users :+: unprotected
      )
      .toService
  val corsService: Service[Request, Response] = new Cors.HttpFilter(policy).andThen(service)
   */

  //Await.ready(Http.server.serve(":8081", corsService))
  override def run(args: List[String]): IO[ExitCode] =
    for {
      _ <- IO.delay(
        println(
          s"Setting up API server with host=$host, port=$port, externalUrl=$externalUrl..."
        )
      )
      repo <- emptyState.map(new Repo(_))
      api = {
        val apiPaths: Service[Request, Response] = {
          healthcheck :+: helloWorld :+: hello :+: tokens :+: users :+: unprotected :+: orientdb :+: gremlinApi :+: dbChecks :+: new Endpoints(
            externalUrl,
            repo
          ).apiEndpoint
        }.toService
        corsFilter
          .andThen(
            apiPaths
          )
      }
      server <- IO.delay(
        Http.server.serve(
          internalUrl,
          api
        )
      )
      _ <- IO.delay(Await.ready(server))
    } yield ExitCode.Success
}
