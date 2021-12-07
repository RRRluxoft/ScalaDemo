package home.workonboarding.orderservice.transport.inventory

import cats.tagless.Derive
import cats.tagless.FunctorK
import domain.Sku.Id
import home.workonboarding.orderservice.transport.inventory.domain.{Bin, Sku}
import slick.dbio.DBIO
import slick.jdbc.H2Profile.BaseColumnType
import slick.jdbc.JdbcProfile
import slick.lifted.{ProvenShape, TableQuery}
import slick.model.Table

import scala.concurrent.ExecutionContext

trait InventoryRepository[F[_]] {
  def findBinsWithSku(skuId: Sku.Id): F[List[Bin]]
  def findBinInventory(bin: Bin): F[List[Sku.Id]]
  def insertSkuIntoBin(skuId: Sku.Id, bin: Bin): F[Long]
  def removeSkuFromBin(skuId: Sku.Id, bin: Bin): F[Unit]
}

object InventoryRepository {
  def apply[F[_]](implicit ev: InventoryRepository[F]): InventoryRepository[F] = ev
  implicit val functorK: FunctorK[InventoryRepository] = Derive.functorK

  def dbIoInstance(jdbcProfile: JdbcProfile with SequenceSupport)(implicit ec: ExecutionContext): InventoryRepository[DBIO] =
    new InventoryRepository[DBIO] with InventoryTableDefinition {

      override lazy val profile = jdbcProfile
      import profile.api.{DBIO => _, _}

      override def findBinsWithSku(skuId: Id): DBIO[List[Bin]] =
        inventory.filter(_.skuId === skuId).result.map(s => s.map(_.bin).toList)

      override def findBinInventory(bin: Bin): DBIO[List[Sku.Id]] =
        inventory.filter(_.binId === bin).result.map(s => s.map(_.skuId).toList)

      override def insertSkuIntoBin(skuId: Sku.Id, bin: Bin): DBIO[Long] =
        (inventory returning inventory.map(_.id)) += InventoryEntity(0, skuId, bin)

      override def removeSkuFromBin(skuId: Sku.Id, bin: Bin): DBIO[Unit] =
        inventory.filter(r => r.skuId === skuId && r.binId === bin).take(1).delete.map(_ => ())
    }
}

case class InventoryEntity(id: Long, skuId: Sku.Id, bin: Bin)

trait InventoryTableDefinition extends TableDefinition {
  import profile.api._

  implicit val skuIdMapping: BaseColumnType[Sku.Id] =
    deriveBaseColumnType

  implicit val binMapping: BaseColumnType[Bin] =
    deriveBaseColumnType

  val inventory = TableQuery[InventoryTable]

  class InventoryTable(tag: Tag) extends Table[InventoryEntity](tag, Some(TableDefinition.Schema), "inventory") {
    val id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    val skuId = column[Sku.Id]("sku_id")
    val binId = column[Bin]("bin_id")

    def * : ProvenShape[InventoryEntity] =
      (id, skuId, binId) <> ((InventoryEntity.apply _).tupled, InventoryEntity.unapply)
  }
}
