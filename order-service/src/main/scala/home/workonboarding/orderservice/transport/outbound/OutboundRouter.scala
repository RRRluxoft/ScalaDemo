package home.workonboarding.orderservice.transport.outbound

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.Functor
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._

final class OutboundRouter[F[_]: OutboundService: ToFuture: Functor](val apiPrefix: String) {
  val pathPrefix = "outbound"
  private val defaultAutoStorePort = PortId(2)

  val routes: Route =
    path(apiPrefix / pathPrefix) {
      post {
        entity(as[OutboundSku]) { request =>
          complete {
            OutboundService[F].outboundSkuEach(request.sku, defaultAutoStorePort)
              .map(_ => "Success")
              .as(StatusCodes.OK)
              .unsafeToFuture()
          }
        }
      }
    }

}
