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
          healthcheck :+: helloWorld :+: hello :+: tokens :+: users :+: unprotected :+: new Endpoints(
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
