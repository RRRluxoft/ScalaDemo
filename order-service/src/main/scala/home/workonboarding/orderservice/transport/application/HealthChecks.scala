package home.workonboarding.orderservice.transport.application

import cats.effect.Blocker

import scala.concurrent.ExecutionContext

final class HealthChecks(blocker: Blocker, conf: Config)(
  implicit ec: ExecutionContext
) {

  import akka.http.scaladsl.server.Route

  private val jmx: JMXCustomHealthIndicator = {
    val indicator = new JMXCustomHealthIndicator
    JMXCustomHealthIndicator.register(indicator)
    indicator
  }

  private def jms(conf: MqConfig) = {
    import org.apache.activemq.ActiveMQConnectionFactory

    new JmsHealthIndicator(
      s"JMS-${conf.url}",
      new ActiveMQConnectionFactory(conf.username, conf.password, conf.url),
      blocker.blockingContext
    )
  }

  val routes: Route = {
    val checks = List(jms(conf.mq), jmx)

    HealthCheckRouting.router(checks, BuildInfo).routes
  }
}
