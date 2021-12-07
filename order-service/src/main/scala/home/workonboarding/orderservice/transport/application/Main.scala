package home.workonboarding.orderservice.transport.application

import akka.actor.ActorSystem
import cats.effect._
import com.typesafe.config.ConfigFactory
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import javax.jms.ConnectionFactory
import pureconfig.ConfigSource
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import sttp.client._
import sttp.client.akkahttp.AkkaHttpBackend
import sttp.client.impl.cats.implicits._

object Main extends KamonApp {
  val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  private def configurationF[F[_]: Sync]: F[Config] =
    Sync[F].delay(
      ConfigSource.fromConfig(ConfigFactory.load(getClass.getClassLoader)).loadOrThrow[Config]
    )

  private val databaseConfig = Resource.make {
    IO {
      val dbc = DatabaseConfig.forConfig[JdbcProfile with SequenceSupport]("datasource")
      dbc.db
      dbc
    }
  } { dbc =>
    IO.fromFuture(IO(dbc.db.shutdown))
  }

  def run(args: List[String]): IO[ExitCode] = serverResource.use { _ =>
    IO.never
  }

  def serverResource: Resource[IO, Unit] =
    for {
      _                                                                     <- logger.info(s"Starting ${BuildInfo.name}").liftToResource
      implicit0(configuration: Config)                                      <- configurationF[IO].liftToResource
      implicit0(dbConfig: DatabaseConfig[JdbcProfile with SequenceSupport]) <- databaseConfig
      blocker                                                               <- Blocker[IO].map(b => Blocker.liftExecutionContext(addKamon(b.blockingContext)))
      implicit0(akka: Akka)                                                 <- Akka.make[IO](BuildInfo.name)
      implicit0(backend: SttpBackend[IO, Nothing, NothingT]) = buildSttpBackend[IO]
      implicit0(system: ActorSystem)                         = akka.actorSystem
      implicit0(mq: ConnectionFactory)                  <- buildConnectionFactory[IO](configuration)
      implicit0(producerConfig: ProducerConfig.Ask[IO]) <- ProducerConfig.fromActorSystem[IO](akka.actorSystem).liftToResource
      implicit0(consumerConfig: ConsumerConfig.Ask[IO]) <- ConsumerConfig.fromActorSystem[IO](akka.actorSystem).liftToResource
      implicit0(tapSender: TapSender[IO, TapMessage])   <- TapSender.make[IO]()
      _                                                 <- logger
                                                             .info(
                                                               s"Starting HTTP server on ${configuration.http.host}:${configuration.http.port}"
                                                             )
                                                             .liftToResource
      _                                                 <- new OrderServiceApplication(blocker).run
      _                                                 <- logger.info("Started HTTP server").liftToResource
    } yield ()

  private def buildConnectionFactory[F[_]: Sync: DeferFutureShift: ContextShift: ConcurrentEffect](configuration: Config) =
    ConnectionFactories.pooled(
      configuration.mq.username,
      configuration.mq.password,
      configuration.mq.url
    )

  private def buildSttpBackend[F[_]: Sync: DeferFutureShift](implicit akka: Akka) =
    new DeferringFutureBackend[F, Nothing, NothingT](
      AkkaHttpBackend.usingActorSystem(akka.actorSystem)
    )

}
