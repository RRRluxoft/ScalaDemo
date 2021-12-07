package home.workonboarding.orderservice.transport.autostore

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder

case class BinContent(content: Int) extends AnyVal

object BinContent {
  implicit val encoder: Encoder[BinContent] = deriveEncoder
  implicit val decoder: Decoder[BinContent] = deriveDecoder
}
