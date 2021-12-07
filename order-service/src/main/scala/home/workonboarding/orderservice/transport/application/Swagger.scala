package home.workonboarding.orderservice.transport.application

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

final class Swagger {

  val routes: Route = {
    val router = new SwaggerRouter(
      version = "v1",
      applicationName = s"${BuildInfo.name}",
      description = s"""version: ${BuildInfo.version}
                       |tags: ${BuildInfo.gitCurrentTags.mkString(", ")}
                       |branch: ${BuildInfo.gitCurrentBranch}
                       |commit: ${BuildInfo.gitHeadCommit.getOrElse("")}
                       |""".stripMargin,
      apiClasses = Set()
    )

    val fileRoute = new SwaggerSite {}.swaggerSiteRoute

    fileRoute ~ router.routes
  }
}
