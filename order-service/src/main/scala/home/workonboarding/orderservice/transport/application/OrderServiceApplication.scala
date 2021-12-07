package home.workonboarding.orderservice.transport.application

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import cats.data.Chain
import cats.data.WriterT
import cats.effect.IO
import cats.effect._
import cats.instances.all._
import cats.syntax.all._
import cats.tagless.implicits._
import cats.~>
import com.typesafe.config.ConfigFactory
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource
import slick.basic.DatabaseConfig
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcProfile
import slickeffect.implicits._
import sttp.client.NothingT
import sttp.client.SttpBackend

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object OrderServiceApplication {
  sealed trait AppError extends Throwable
}

class OrderServiceApplication(
  blocker: Blocker
)(
  implicit
  akka: Akka,
  actorSystem: ActorSystem,
  config: Config,
  materializer: Materializer,
  cpuEC: ExecutionContext,
  contextShift: ContextShift[IO],
//   internalBroker: InternalBrokerConfig.Ask[IO],
//   externalBroker: ExternalBrokerConfig.Ask[IO],
  tapSender: TapSender[IO, TapMessage],
  sttpBackend: SttpBackend[IO, Nothing, NothingT],
  dbConfig: DatabaseConfig[JdbcProfile with SequenceSupport]
) extends SwaggerSite
  with Logging
  with TerminatingExceptionHandler {

  type AppEffect[A] = WriterT[DBIO, Chain[TapMessage], A]

  def configurationF[F[_]: Sync]: F[Config] =
    Sync[F].delay(
      ConfigSource.fromConfig(ConfigFactory.load(getClass.getClassLoader)).loadOrThrow[Config]
    )

  def DBIOToF[F[_]: DeferFutureShift]: DBIO ~> F = λ[DBIO ~> F](x => DeferFutureShift[F].deferShift(dbConfig.db.run(x)))

  implicit val db: JdbcBackend#Database = dbConfig.db

  implicit val clock: Clock[AppEffect] = Clock.create[AppEffect]

  implicit val DBIOToAppEffect: DBIO ~> AppEffect = WriterT.liftK

  implicit val IOToAppEffect: IO ~> AppEffect = LiftIO.liftK[DBIO].andThen(DBIOToAppEffect)

  implicit val deferFuture: DeferFutureShift[AppEffect] = DeferFutureShift[IO].mapK(IOToAppEffect)

  implicit val appEffectTransactor: Transactor[AppEffect, IO] = new Transactor[AppEffect, IO] {
    private val dbioToIO: DBIO ~> IO = DBIOToF[IO]

    override def transact[A](fa: AppEffect[A]): IO[(List[TapMessage], A)] = dbioToIO.apply(fa.run).map {
      case (messages, result) => (messages.toList, result)
    }
  }

  implicit val appEffectToIO: AppEffect ~> IO = λ[AppEffect ~> IO] {
    appEffectTransactor.transact(_).flatMap {
      case (messages, result) =>
        messages.traverse_(tapSender.sendOne).as(result)
    }
  }

  implicit val appEffectToFuture: AppEffect ~> Future = appEffectToIO.andThen(λ[IO ~> Future](_.unsafeToFuture()))

  implicit val toFuture: ToFuture[AppEffect] = ToFuture.fromFunctionK(appEffectToFuture)

  implicit val profile: JdbcProfile with SequenceSupport = dbConfig.profile
  val apiPrefix = "api"
  implicit val appEffectLogger: Logger[AppEffect] = Slf4jLogger.getLogger[AppEffect]
  implicit val ioLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val run: Resource[IO, Http.ServerBinding] = for {
    _                                                      <- IO.unit.liftToResource
    // NOTE: Now when `refInstance` returns F[OrderRepository[F]] flat map is required instead of assignment
    implicit0(orderRepository: OrderRepository[AppEffect]) <- OrderRepository.refInstance[IO].map(_.mapK(IOToAppEffect)).liftToResource
    implicit0(inventoryRepository: InventoryRepository[AppEffect]) = InventoryRepository.dbIoInstance(profile).mapK(DBIOToAppEffect)
    implicit0(orderService: OrderService[AppEffect])               = OrderService.instance[AppEffect]
    implicit0(skuService: SkuService[AppEffect])                   = SkuService.dummyInstance[AppEffect]
    implicit0(inventoryService: InventoryService[AppEffect])       = InventoryService.instance[AppEffect]
    implicit0(asConfig: AutoStoreCommunicationServiceConfig)       = config.autostore
    implicit0(autoStoreClient: AutoStoreClient[AppEffect])         = AutoStoreClient.instance[IO].mapK(IOToAppEffect)
    implicit0(outboundService: OutboundService[AppEffect])         = OutboundService.instance[AppEffect]

    orderRouter          = new OrderRouter[AppEffect](apiPrefix)
    outboundRouter       = new OutboundRouter[AppEffect](apiPrefix)
    inventoryRouter      = new InventoryRouter[AppEffect](apiPrefix)
    healthChecksInstance = new HealthChecks(blocker, config)
    swaggerInstance      = new Swagger
    routes               = healthChecksInstance.routes ~ swaggerInstance.routes ~ orderRouter.routes ~ inventoryRouter.routes ~ outboundRouter.routes
    server <- new HttpServer[IO](
                routes,
                config
              ).httpServer
  } yield server

}
