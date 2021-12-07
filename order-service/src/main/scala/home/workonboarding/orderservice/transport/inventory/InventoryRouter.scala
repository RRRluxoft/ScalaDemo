package home.workonboarding.orderservice.transport.inventory

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import cats.Functor
import cats.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import home.workonboarding.orderservice.transport.inventory.domain.{Bin, Sku}
import io.circe.generic.auto._

class InventoryRouter[F[_]: InventoryService: SkuService: ToFuture: Functor](val apiPrefix: String) {
  val inventoryPath = "inventory"

  val routes =
    pathPrefix(apiPrefix / inventoryPath) {
      path("bin") {
        get {
          parameter('sku) { skuId =>
            complete(
              InventoryService[F].findBinsWithSku(Sku.Id(skuId)).unsafeToFuture()
            )
          }
        } ~ path("removeEach") {
          post {
            entity(as[Bin]) { bin =>
              complete(
                InventoryService[F]
                  .removeEachFromBin(bin)
                  .as(StatusCodes.OK)
                  .unsafeToFuture()
              )
            }
          }
        }
      } ~ path("sku") {
        get {
          complete(
            SkuService[F].all.unsafeToFuture()
          )
        }
      }
    }
}
