package home.workonboarding.orderservice.transport.inventory

import cats.Applicative
import cats.implicits._

trait SkuService[F[_]] {
  def findSkuById(id: Sku.Id): F[Sku]
  def all: F[List[Sku]]
}

object SkuService {
  def apply[F[_]](implicit ev: SkuService[F]): SkuService[F] = ev

  def dummyInstance[F[_]: Applicative]: SkuService[F] = new SkuService[F] {
    private val products = List("apple", "banana", "juice", "bread", "cheese", "corona beer")
    private val adjectives = List("tasty", "expensive", "bestselling", "fresh")
    private val descriptions = (adjectives, products).mapN(_ + " " + _)

    private def pickDescription(id: Sku.Id): String =
      if (id.value.forall(_.isDigit))
        descriptions(descriptions.length % id.value.toInt)
      else
        descriptions(descriptions.length % id.value.length)

    override def findSkuById(id: Sku.Id): F[Sku] =
      Applicative[F].pure(Sku(id, pickDescription(id)))

    override val all: F[List[Sku]] = Applicative[F].pure {
      descriptions.zipWithIndex.map {
        case (description, id) => Sku(Sku.Id(id.toString), description)
      }
    }
  }
}
