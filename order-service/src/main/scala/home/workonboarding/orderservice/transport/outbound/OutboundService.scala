package home.workonboarding.orderservice.transport.outbound

import cats.Monad
import cats.implicits._

trait OutboundService[F[_]] {
  def outboundSkuEach(sku: Sku.Id, portId: PortId): F[Unit]
}

object OutboundService {
  def apply[F[_]](implicit ev: OutboundService[F]): OutboundService[F] = ev

  def instance[F[_]: InventoryRepository: AutoStoreClient: Monad]: OutboundService[F] =
    (skuId: Sku.Id, portId: PortId) =>
      {
        for {
          bins <- InventoryRepository[F].findBinsWithSku(skuId).liftToOptionT
          bin  <- bins.headOption.toOptionT[F]
          binLpn = BinLpn(bin.lpn)
          _ <- AutoStoreClient[F].openPort(portId).liftToOptionT
          _ <- AutoStoreClient[F].openBin(portId, binLpn).liftToOptionT
          _ <- AutoStoreClient[F].updateBinContent(binLpn, empty = false).liftToOptionT
          _ <- AutoStoreClient[F].closeBin(portId, binLpn).liftToOptionT
          _ <- AutoStoreClient[F].closePort(portId).liftToOptionT
        } yield ()
      }.value.void
}
