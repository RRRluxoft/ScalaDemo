package home.workonboarding.orderservice.transport.order

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.tagless.Derive
import cats.tagless.FunctorK

import scala.collection.concurrent.TrieMap

trait OrderRepository[F[_]] {
  def saveOrder(order: Order, lines: NonEmptyList[OrderLine]): F[Unit]
  def setStatus(id: Order.Id, status: Order.Status): F[Unit]
  def allOrders(): F[List[Order]]
  def allLines(): F[List[OrderLine]]
  def linesByOrder(id: Order.Id): F[List[OrderLine]]
  def orderById(id: Order.Id): F[Option[Order]]
}

object OrderRepository {

  implicit val functorK: FunctorK[OrderRepository] = Derive.functorK

  private trait Helpers {
    protected def matchingOrder(id: Order.Id)(p: (Order, Any)): Boolean = p._1.id === id
  }

  /* todo: implement all methods using the map with the state */
  def mapInstance[F[_]: Sync](): OrderRepository[F] =
    new OrderRepository[F] with Helpers {
      private val state = TrieMap.empty[Order, List[OrderLine]]

      private def exists(orderId: Order.Id): F[Boolean] = Sync[F].delay(state.exists(matchingOrder(orderId)))

      override def saveOrder(order: Order, lines: NonEmptyList[OrderLine]): F[Unit] =
        Sync[F].ifM(exists(order.id))(
          Sync[F].raiseError(OrderAlreadyExists(order.id)),
          Sync[F].delay(state put (order, lines.toList)).void
        )

      override def setStatus(id: Order.Id, status: Order.Status): F[Unit] =
        Sync[F].ifM(exists(id))(
          Sync[F].delay {
            state.find(matchingOrder(id)).foreach {
              case (ord, lines) =>
                state - ord
                state + ((ord.copy(status = status), lines))
            }
          }.void,
          Sync[F].pure(()))

      override def allOrders(): F[List[Order]] = Sync[F].delay(state.keys.toList)

      override def allLines(): F[List[OrderLine]] = Sync[F].delay(state.values.flatten.toList)

      override def linesByOrder(id: Order.Id): F[List[OrderLine]] = allLines.map(_.filter(_.orderId === id))

      override def orderById(id: Order.Id): F[Option[Order]] = allOrders.map(_.find(_.id === id))
    }

  def apply[F[_]](implicit repo: OrderRepository[F]): OrderRepository[F] = repo

  /* todo: implement all methods using the Ref on map */
  def refInstance[F[_]: Sync]: F[OrderRepository[F]] = Ref[F].of(Map.empty[Order.Id, (Order, List[OrderLine])]).map { ref =>
    new OrderRepository[F] with Helpers {
      override def saveOrder(order: Order, lines: NonEmptyList[OrderLine]): F[Unit] =
        ref.modify { oldState =>
          oldState.contains(order.id) match {
            case true  => (oldState, Left(OrderAlreadyExists(order.id)))
            case false =>
              val updated = oldState + ((order.id, (order, lines.toList)))
              (updated, Right(()))
          }
        }.rethrow

      override def setStatus(id: Order.Id, status: Order.Status): F[Unit] = ref.update { oldState =>
        // TODO: implement the rest of method body
        oldState.get(id).map {
          case (ord, lines) =>
            val newOrd = ord.copy(status = status)
            oldState + ((id, (newOrd, lines)))
        } match {
          case Some(updatedMap) => updatedMap
          case None             => oldState
        }
      }

      override def allOrders(): F[List[Order]] = ref.get.map(_.values.map(_._1).toList)

      override def allLines(): F[List[OrderLine]] = ref.get.map(_.values.flatMap(_._2).toList)

      override def linesByOrder(id: Order.Id): F[List[OrderLine]] = allLines().map(_.filter(_.orderId === id))

      override def orderById(id: Order.Id): F[Option[Order]] = allOrders().map(_.find(_.id === id))
    }
  }
}
