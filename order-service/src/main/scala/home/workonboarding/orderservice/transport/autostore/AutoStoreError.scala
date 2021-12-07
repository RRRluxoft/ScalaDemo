package home.workonboarding.orderservice.transport.autostore

import io.circe.Encoder
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
final case class AutoStoreError(value: Int, message: String)
object AutoStoreError {
  implicit val encoder: Encoder[AutoStoreError] = deriveEncoder
  implicit val decoder: Decoder[AutoStoreError] = deriveDecoder
}
