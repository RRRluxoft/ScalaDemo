package home.workonboarding.orderservice.transport.config

final case class MqConfig(url: String, username: String, password: String)

final case class HttpConfig(host: String, port: Int)

final case class AutoStoreCommunicationServiceConfig(uri: String)

final case class Config(mq: MqConfig, http: HttpConfig, autostore: AutoStoreCommunicationServiceConfig)

object Config {
  import pureconfig.generic.semiauto._
  import pureconfig.ConfigReader

  implicit val configReader: ConfigReader[Config] = {
    import pureconfig.generic.auto._
    deriveReader
  }
}
