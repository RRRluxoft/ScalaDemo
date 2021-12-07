package home.workonboarding.orderservice.transport.order

import cats.Show
import cats.kernel.Eq
import cats.instances.all._
import io.circe.Codec
import io.circe.generic.extras.semiauto._

object domain {
  final case class Order(id: Order.Id, status: Order.Status)

  object Order extends CirceConfig {
    final case class Id(value: String) extends AnyVal

    object Id {
      implicit val show: Show[Id] = Show.show(id => s"Order.Id(${id.value})")
      implicit val eq: Eq[Id] = Eq.fromUniversalEquals
      implicit val order: cats.Order[Id] = cats.Order.by(_.value)
      implicit val codec: Codec[Id] = deriveUnwrappedCodec
    }

    sealed trait Status extends Product with Serializable

    object Status {
      case object Waiting extends Status
      case object Picking extends Status
      case object Ready   extends Status

      val stringify: Order.Status => String = {
        case Order.Status.Waiting => "Waiting"
        case Order.Status.Picking => "Picking"
        case Order.Status.Ready   => "Ready"
      }
      implicit val codec: Codec[Status] = deriveEnumerationCodec
      implicit val eq: Eq[Status] = Eq.fromUniversalEquals
    }
    implicit val codec: Codec[Order] = deriveConfiguredCodec
  }

  case class OrderLine(
    lineId: LineId,
    orderId: Order.Id,
    picked: Boolean,
    skuId: Sku.Id
  )

  object OrderLine extends CirceConfig {
    implicit val codec: Codec[OrderLine] = deriveConfiguredCodec
  }

  final case class LineId(value: String) extends AnyVal

  object LineId {
    implicit val codec: Codec[LineId] = deriveUnwrappedCodec
  }

  object errors {
    case class OrderAlreadyExists(id: Order.Id)       extends Exception(s"Order $id already exists")
    case class OrderLinesDontMatchOrder(id: Order.Id) extends Exception(s"Not all lines match order $id")
    case class OrderDoesNotExist(id: Order.Id)        extends Exception(s"Order $id doesn't exists")
  }
}
