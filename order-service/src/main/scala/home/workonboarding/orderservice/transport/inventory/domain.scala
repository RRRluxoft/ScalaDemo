package home.workonboarding.orderservice.transport.inventory

import cats.kernel.Eq
import home.workonboarding.orderservice.transport.application.CirceConfig
import io.circe.Codec
import io.circe.generic.extras.semiauto._

object domain extends CirceConfig {
  final case class Bin(lpn: String) extends AnyVal

  object Bin {
    implicit val eq: Eq[Bin] = Eq.fromUniversalEquals
    implicit val codec: Codec[Bin] = deriveConfiguredCodec
  }

  final case class Sku(id: Sku.Id, name: String)

  object Sku {
    final case class Id(value: String) extends AnyVal

    object Id {
      implicit val eq: Eq[Sku.Id] = Eq.fromUniversalEquals
      implicit val codec: Codec[Sku.Id] = deriveUnwrappedCodec
    }
    implicit val codec: Codec[Sku] = deriveConfiguredCodec
  }

  final case class Each(skuId: Sku.Id, bin: Bin)

  object Each {
    implicit val codec: Codec[Each] = deriveConfiguredCodec
  }

}
