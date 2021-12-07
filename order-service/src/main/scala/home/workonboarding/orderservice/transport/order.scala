package home.workonboarding.orderservice.transport

import cats.data.NonEmptyList
import io.circe.Codec
import io.circe.generic.extras.semiauto._

object order {

  final case class CreateOrder(id: Order.Id, lines: NonEmptyList[Sku.Id]) {
    val toDomainOrderWithLines: (Order, NonEmptyList[OrderLine]) = (
      Order(id, Order.Status.Waiting),
      lines.zipWithIndex.map {
        case (skuId, lineId) =>
          OrderLine(LineId(lineId.toString), id, picked = false, skuId)
      }
    )
  }

  object CreateOrder extends CirceConfig {
    implicit val codec: Codec[CreateOrder] = deriveConfiguredCodec
  }

  final case class OrderWithLines(order: Order, lines: NonEmptyList[OrderLine])
  object OrderWithLines extends CirceConfig {
    implicit val codec: Codec[OrderWithLines] = deriveConfiguredCodec
  }

  final case class OutboundSku(sku: Sku.Id)
  object OutboundSku extends CirceConfig {
    implicit val codec: Codec[OutboundSku] = deriveConfiguredCodec
  }

}
