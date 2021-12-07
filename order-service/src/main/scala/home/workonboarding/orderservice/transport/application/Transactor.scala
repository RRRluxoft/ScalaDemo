package home.workonboarding.orderservice.transport.application

trait Transactor[DB[_], F[_]] {
  def transact[A](fa: DB[A]): F[(List[TapMessage], A)]
}

object Transactor {
  def apply[F[_], G[_]](implicit ev: Transactor[F, G]): Transactor[F, G] = ev
}
