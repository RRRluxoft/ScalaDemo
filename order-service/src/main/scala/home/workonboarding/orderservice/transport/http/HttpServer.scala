package home.workonboarding.orderservice.transport.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import cats.Functor
import cats.effect.Resource
import cats.syntax.functor._
import home.workonboarding.orderservice.transport.application.Akka
import home.workonboarding.orderservice.transport.config.Config

final class HttpServer[F[_]: Functor: DeferFutureShift](routes: Route, config: Config)(implicit akka: Akka) {

  val httpServer: Resource[F, ServerBinding] = {
    implicit val system: ActorSystem = akka.actorSystem

    val serverF = DeferFutureShift[F].deferShift {
      Http().bindAndHandle(
        routes,
        config.http.host,
        config.http.port
      )
    }

    Resource.make(serverF)(server => DeferFutureShift[F].deferShift(server.unbind()).void)
  }
}
