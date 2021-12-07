package home.workonboarding.orderservice.transport.persistence

import slick.jdbc.JdbcProfile

trait TableDefinition {
  val profile: JdbcProfile
}

object TableDefinition {
  val Schema = "onboarding"
}
