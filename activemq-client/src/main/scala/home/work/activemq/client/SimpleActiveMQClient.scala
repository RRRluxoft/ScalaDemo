package home.work.activemq.client

import io.circe.Decoder
import scala.concurrent.Future
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import io.circe.Encoder
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._
import java.util.concurrent.Executors
import cats.effect.IO
import cats.effect.ContextShift
import cats.effect.Resource
import cats.effect.Timer
import cats.implicits._

trait SimpleResource[A] {
  def use[B](f: A => Future[B]): Future[B]
}

object SimpleResource {
  import cats.effect.Resource
  import cats.effect.IO
  import cats.effect.ContextShift

  def fromResource[A](resource: Resource[IO, A])(implicit ec: ExecutionContext): SimpleResource[A] = new SimpleResource[A] {
    private implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

    def use[B](f: A => Future[B]): Future[B] = resource.use(r => IO.fromFuture(IO(f(r)))).unsafeToFuture()
  }
}

trait SimpleProducer[-A] {
  def send(msg: A): Future[Unit]
}

trait SimpleActiveMQClient {
  def consumer[A: Decoder](destination: TapDestination)(handle: A => Future[Unit]): SimpleResource[Unit]
  def producer[A: Encoder](destination: TapDestination): SimpleResource[SimpleProducer[A]]
}

object SimpleActiveMQClient {

  def create(
    brokerUrl: String,
    username: String,
    password: String
  )(
    implicit
    system: ActorSystem,
    executionContext: ExecutionContext
  ): SimpleResource[SimpleActiveMQClient] = {

    implicit val contextShift: ContextShift[IO] = IO.contextShift(executionContext)
    implicit val timer: Timer[IO] = IO.timer(executionContext)
    implicit val utf: ToFuture[IO] = ToFuture.effectInstance

    val configs =
      ConnectionFactories.pooled[IO](username, password, brokerUrl).evalMap { implicit cf =>
        (ConsumerConfig.fromActorSystem[IO](system), ProducerConfig.fromActorSystem[IO](system)).tupled
      }

    SimpleResource.fromResource(configs.map {
      case (implicit0(cc: ConsumerConfig.Ask[IO]), implicit0(pc: ProducerConfig.Ask[IO])) =>
        def simpleConsumer[A: Decoder](destination: TapDestination, handle: A => IO[Unit]): Resource[IO, Unit] = {
          implicit val sourceConfig: SourceConfig[A] = SourceConfig.singleSession(destination)

          Tap.stream[IO, A, Unit](_.evalMap(handle)).compile.drain.background.void
        }

        def simpleProducer[A: Encoder](destination: TapDestination): Resource[IO, A => IO[Unit]] =
          TapSender.make[IO]().map(_.asJsonSender[A](destination).sendOne)

        new SimpleActiveMQClient {
          def consumer[A: Decoder](destination: TapDestination)(handle: A => Future[Unit]): SimpleResource[Unit] =
            SimpleResource.fromResource(simpleConsumer[A](destination, a => IO.fromFuture(IO(handle(a)))))

          def producer[A: Encoder](destination: TapDestination): SimpleResource[SimpleProducer[A]] =
            SimpleResource.fromResource {
              simpleProducer(destination).map(produce => msg => utf.unsafeToFuture(produce(msg)))
            }
        }
    })
  }
}

object demo {
  import ExecutionContext.Implicits.global

  def run(): Future[Unit] = {
    val blockingEc = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

    implicit val as: ActorSystem = ActorSystem()

    SimpleActiveMQClient
      .create("failover:tcp://localhost:61616", "admin", "admin")
      .use { client =>
        client.producer[String](TapDestination.queue("Onboarding.Picking.Events")).use { producer =>
          client
            .consumer[String](TapDestination.queue("Onboarding.Picking.Events")) { e =>
              println(s"Received: $e")
              Future.successful(())
            }
            .use { _ =>
              producer.send("hello")
              Future(StdIn.readLine("Press ENTER to exit..."))(blockingEc)
            }
        }
      }
      .flatMap(_ => as.terminate())
      .map(_ => ())
  }

  def main(args: Array[String]): Unit = {
    val _ = Await.result(run(), Duration.Inf)
  }
}
