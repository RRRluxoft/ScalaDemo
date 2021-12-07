package home.workonboarding.orderservice.transport.autostore

import cats.effect.Sync
import cats.implicits._
import cats.tagless.Derive
import cats.tagless.FunctorK
import io.chrisdavenport.log4cats.Logger
import home.workonboarding.orderservice.transport.config.AutoStoreCommunicationServiceConfig
import io.circe._
import io.circe.syntax._
import sttp.client.NothingT
import sttp.client.Request
import sttp.client.Response
import sttp.client.SttpBackend
import sttp.client.UriContext
import sttp.client.basicRequest
import sttp.model.MediaType.ApplicationJson

trait AutoStoreClient[F[_]] {
  def openPort(portId: PortId): F[Unit]
  def closePort(portId: PortId): F[Unit]
  def openBin(portId: PortId, binLpn: BinLpn): F[Unit]
  def closeBin(portId: PortId, binLpn: BinLpn): F[Unit]
  def updateBinContent(binLpn: BinLpn, empty: Boolean): F[Unit]
}

object AutoStoreClient {
  private val NonEmptyBinContent = BinContent(9) // any non-zero bin content means non-empty

  implicit val functorK: FunctorK[AutoStoreClient] = Derive.functorK

  def apply[F[_]](implicit ev: AutoStoreClient[F]): AutoStoreClient[F] = ev

  def instance[F[_]: Sync: Logger](
    implicit
    sttpBackend: SttpBackend[F, Nothing, NothingT],
    config: AutoStoreCommunicationServiceConfig
  ): AutoStoreClient[F] =
    new AutoStoreClient[F] {
      private val logger = Logger[F]

      override def openPort(portId: PortId): F[Unit] =
        logger.info(s"Opening port $portId") *>
          sendRequest[Unit](basicRequest.post(uri"${config.uri}/v2/ports/${portId.id}/open"), handleOpenPortResponse)

      override def closePort(portId: PortId): F[Unit] =
        logger.info(s"Closing port $portId") *>
          sendRequestWithEmptyResponse(basicRequest.put(uri"${config.uri}/v2/ports/${portId.id}/close"))

      override def openBin(portId: PortId, binLpn: BinLpn): F[Unit] =
        logger.info(s"Opening bin $binLpn on port $portId") *>
          sendRequestWithEmptyResponse(basicRequest.put(uri"${config.uri}/v2/ports/${portId.id}/bins/${binLpn.binId}/open"))

      override def closeBin(portId: PortId, binLpn: BinLpn): F[Unit] =
        logger.info(s"Closing bin $binLpn on port $portId") *>
          sendRequestWithEmptyResponse(
            basicRequest
              .post(uri"${config.uri}/v2/ports/${portId.id}/bins/${binLpn.binId}/close")
              .contentType(ApplicationJson)
              .body(NonEmptyBinContent.asJson.noSpaces)
          )

      override def updateBinContent(binLpn: BinLpn, empty: Boolean): F[Unit] =
        logger.info(s"Updating bin $binLpn. Empty: $empty") *>
          sendRequestWithEmptyResponse(
            basicRequest
              .put(uri"${config.uri}/v2/bins/${binLpn.binId}")
              .contentType(ApplicationJson)
              .body(
                BinContent(if (empty) 0 else 1).asJson.noSpaces
              )
          )

      private def sendRequestWithEmptyResponse(request: Request[Either[String, String], Nothing]): F[Unit] =
        sendRequest(request, toUnitResponse)

      private def sendRequest[T](
        request: Request[Either[String, String], Nothing],
        handleResponse: Response[Either[String, String]] => Either[Exception, T]
      ): F[T] =
        logger.trace(s"Prepared request to be sent to ${request.uri}: $request") *>
          request
            .send
            .map(handleResponse(_).leftWiden[Throwable])
            .rethrow

      private def handleOpenPortResponse(response: Response[Either[String, String]]): Either[Exception, Unit] = {
        def decodeAutoStoreError(errorBody: String): OpenPortResponse =
          parser
            .decode[AutoStoreError](errorBody)
            .left
            .map(_ => OpenPortResponse.DefaultError(new Exception(errorBody)))
            .map(OpenPortResponse.AutoStoreError(_, errorBody))
            .merge

        val decodedResponse: OpenPortResponse = response.body match {
          case Left(err) if response.code.code === AutoStoreServerError.code =>
            decodeAutoStoreError(err)
          case _                                                             =>
            toUnitResponse(response).leftMap(OpenPortResponse.DefaultError).as(OpenPortResponse.Opened).merge
        }

        decodedResponse match {
          case OpenPortResponse.Opened                  => Either.right(())
          case OpenPortResponse.AutoStoreError(_, body) => Left(new Exception(body))
          case OpenPortResponse.DefaultError(error)     => Left(error)
        }
      }

      private val toUnitResponse: Response[Either[String, String]] => Either[Exception, Unit] =
        _.body.leftMap(new Exception(_)).void
    }

  sealed trait OpenPortResponse extends Product with Serializable

  object OpenPortResponse {
    case class AutoStoreError(
      error: home.workonboarding.orderservice.transport.autostore.AutoStoreError,
      rawBody: String
    ) extends OpenPortResponse

    case class DefaultError(error: Exception) extends OpenPortResponse

    case object Opened extends OpenPortResponse
  }
}
