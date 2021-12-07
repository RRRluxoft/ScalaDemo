package home.workonboarding.orderservice.transport.application

import akka.actor.ActorSystem
import cats.effect.Resource
import cats.effect.Sync
import cats.syntax.functor._

trait Akka {
  def actorSystem: ActorSystem
}

object Akka {

  def make[F[_]: Sync: DeferFutureShift](actorSystemName: String): Resource[F, Akka] =
    Resource
      .make(
        Sync[F].delay(
          ActorSystem(
            actorSystemName,
            classLoader = Some(getClass.getClassLoader)
          )
        )
      )(as => DeferFutureShift[F].deferShift(as.terminate()).void)
      .map { implicit system =>
        new Akka {
          override val actorSystem: ActorSystem = system
        }
      }
}
