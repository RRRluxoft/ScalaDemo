package home.workonboarding.orderservice.transport.inventory

import cats.Monad
import cats.implicits._

trait InventoryService[F[_]] {
  def findBinsWithSku(skuId: Sku.Id): F[List[Bin]]
  def findBinInventory(bin: Bin): F[List[Sku]]
  def insertSkuIntoBin(skuId: Sku.Id, bin: Bin): F[Unit]
  def removeEachFromBin(bin: Bin): F[Unit] // NOTE: Assuming single no mixed SKUs in bin
}

object InventoryService {
  def apply[F[_]](implicit ev: InventoryService[F]): InventoryService[F] = ev

  def instance[F[_]: InventoryRepository: SkuService: Monad]: InventoryService[F] = new InventoryService[F] {

    override def findBinsWithSku(skuId: Sku.Id): F[List[Bin]] =
      InventoryRepository[F].findBinsWithSku(skuId)

    override def findBinInventory(bin: Bin): F[List[Sku]] =
      for {
        ids <- InventoryRepository[F].findBinInventory(bin)
        sku <- ids.traverse(SkuService[F].findSkuById)
      } yield sku

    override def insertSkuIntoBin(skuId: Sku.Id, bin: Bin): F[Unit] =
      InventoryRepository[F].insertSkuIntoBin(skuId, bin).void

    // NOTE: To simplify the domain let's assume that bin contains infinite amount of sku
    override def removeEachFromBin(bin: Bin): F[Unit] =
      Monad[F].unit
  }
}
