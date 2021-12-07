package home.workonboarding.orderservice.transport.order

import cats.Apply
import cats.data.NonEmptyList
import cats.implicits._

trait OrderService[F[_]] {
  def save(order: Order, lines: NonEmptyList[OrderLine]): F[Unit]
  def setOrderStatus(id: Order.Id, newStatus: Order.Status): F[Unit]
  def allOrders(): F[List[(Order, NonEmptyList[OrderLine])]]
  def findOrderWithLines(id: Order.Id): F[Option[(Order, NonEmptyList[OrderLine])]]
}

object OrderService {

  /* todo: implement all methods using the repository */
  /*
    NOTE: there are 3 types bound to F already:
      - OrderRepository - for operating on repository in effect F
      - Functor - to guarantee `map` over F
      - Semigroupal - to guarantee `tupled` and `mapN` over F
      - Apply == Functor + Semigroupal
   */
  def instance[F[_]: OrderRepository: Apply]: OrderService[F] = new OrderService[F] {
    private val repository = implicitly[OrderRepository[F]]

    override def save(order: Order, lines: NonEmptyList[OrderLine]): F[Unit] = repository.saveOrder(order, lines)

    override def setOrderStatus(id: Order.Id, newStatus: Order.Status): F[Unit] = repository.setStatus(id, newStatus)

    override def allOrders(): F[List[(Order, NonEmptyList[OrderLine])]] =
      (
        repository.allOrders(),
        repository.allLines().map(_.groupByNel(_.orderId).lift)
      ).mapN((ords, lines) => ords.mapFilter(ord => lines(ord.id).map((ord, _))))

    override def findOrderWithLines(id: domain.Order.Id): F[Option[(Order, NonEmptyList[OrderLine])]] =
      (
        repository.orderById(id),
        repository.linesByOrder(id).map(_.toNel)
      ).tupled.map(_.tupled)
  }
}
