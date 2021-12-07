package home.workonboarding.orderservice.transport.order

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.Functor
import cats.syntax.functor._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

final class OrderRouter[F[_]: OrderService: ToFuture: Functor](val apiPrefix: String) {
  val pathPrefix = "orders"
  val orderService: OrderService[F] = implicitly[OrderService[F]]

  val routes: Route =
    path(apiPrefix / pathPrefix) {
      get {
        parameter('orderId) { orderId =>
          complete {
            orderService.findOrderWithLines(Order.Id(orderId)).map(_.map((OrderWithLines.apply _).tupled)).unsafeToFuture()
          }
        } /* TODO: add endpoint for getting all orders with lines */
        complete {
          (for {
            orders <- orderService.allOrders()
            lines = orders.map((OrderWithLines.apply _).tupled)
          } yield lines).unsafeToFuture
        }
      } ~ post {
        entity(as[CreateOrder]) { request =>
          complete {
            /* TODO: save incoming order */
            (orderService.save _).tupled(request.toDomainOrderWithLines)
            println(request.toDomainOrderWithLines)
            StatusCodes.OK
          }
        }
      }
    }

}
