package home.workonboarding.orderservice.transport.application

import io.circe.generic.extras.Configuration

trait CirceConfig {
  implicit val circeConfiguration: Configuration =
    Configuration.default.withDiscriminator("@type")
}
