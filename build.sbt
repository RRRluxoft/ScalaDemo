inThisBuild(mimaPreviousArtifacts := Set())

val compilerOptions = Seq(
  scalacOptions ++= Seq(
    "-P:clippy:colors=true"
  ),
  scalacOptions -= "-Xfatal-warnings"
)

val Versions = new {
  val circe = "0.13.0"
  // versioned independently!
  val circeGenericExtras = "0.13.0"
  val alpakkaTap = "10.0.0"
  val cats = "2.1.1"
  val catsEffect = "2.1.2"
  val sttp = "2.0.6"
  val catsCommons = "7.1.0"
  val meowMtl = "0.4.0"
}

def crossPlugin(x: sbt.librarymanagement.ModuleID) =
  compilerPlugin(x cross CrossVersion.full)

val compilerPlugins = Seq(
  compilerPlugin(
    "com.softwaremill.clippy"   %% "plugin"             % "0.6.1" classifier "bundle"
  ),
  compilerPlugin("io.tryp"       % "splain"             % "0.5.1" cross CrossVersion.patch),
  crossPlugin("org.typelevel"    % "kind-projector"     % "0.11.0"),
  crossPlugin("com.github.cb372" % "scala-typed-holes"  % "0.1.2"),
  compilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1")
)

val dependencies = {

  val cats = Seq(
    "org.typelevel" %% "cats-core"        % Versions.cats,
    "org.typelevel" %% "cats-effect"      % Versions.catsEffect,
    "home.work.wms" %% "cats-commons"     % Versions.catsCommons,
    "com.olegpy"    %% "meow-mtl-core"    % Versions.meowMtl,
    "com.olegpy"    %% "meow-mtl-effects" % Versions.meowMtl
  )

  val circe = Seq(
    "io.circe" %% "circe-core"           % Versions.circe,
    "io.circe" %% "circe-parser"         % Versions.circe,
    "io.circe" %% "circe-literal"        % Versions.circe,
    "io.circe" %% "circe-generic-extras" % Versions.circeGenericExtras
  )

  val config = Seq(
    "com.github.pureconfig" %% "pureconfig"          % "0.12.1",
    "com.damnhandy"          % "handy-uri-templates" % "2.1.6",
    "eu.timepit"            %% "refined-pureconfig"  % "0.9.10"
  )

  val logging = Seq(
    "ch.qos.logback"       % "logback-classic"          % "1.2.3",
    "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
    "io.chrisdavenport"   %% "log4cats-slf4j"           % "1.0.1",
    //this handles the condition in logback.xml
    "org.codehaus.janino"  % "janino"                   % "3.1.2"
  )

  val healthCheck = Seq(
    "home.work.commons" %% "health-check-akka-http",
    "home.work.commons" %% "health-check-akka-http-circe",
    "home.work.commons" %% "health-check-jmx",
    "home.work.commons" %% "health-check-jms",
    "home.work.commons" %% "health-check-cache",
    "home.work.commons" %% "health-check-slick"
  ).map(_ % "17.0.0")

  val akkaHttp = Seq(
    "com.typesafe.akka" %% "akka-http"            % "10.1.11",
    "de.heikoseeberger" %% "akka-http-circe"      % "1.31.0",
    "io.swagger"         % "swagger-annotations"  % "1.6.1",
    "home.work.commons" %% "swagger-akka-http"    % "4.0.0",
    "home.work.commons" %% "swagger-akka-http-ui" % "4.0.0",
    "home.work.commons" %% "swagger-ui-site"      % "4.0.0"
  )

  val kamon = Seq(
    "home.work.wms" %% "kamon-core"        % "8.0.0",
    "home.work.wms" %% "kamon-cats-effect" % "8.0.0",
    "io.kamon"      %% "kamon-akka-http"   % "2.0.3",
    "io.kamon"      %% "kamon-zipkin"      % "2.0.2"
  )

  val sttp = Seq(
    "home.work.wms"                %% "sttp-extensions"   % "8.0.0",
    "com.softwaremill.sttp.client" %% "core"              % Versions.sttp,
    "com.softwaremill.sttp.client" %% "circe"             % Versions.sttp,
    "com.softwaremill.sttp.client" %% "cats"              % Versions.sttp,
    "com.softwaremill.sttp.client" %% "akka-http-backend" % Versions.sttp
  )

  val slick = Seq(
    "com.typesafe.slick" %% "slick-hikaricp"           % "3.3.2",
    "org.postgresql"      % "postgresql"               % "42.2.12",
    "com.kubukoz"        %% "slick-effect"             % "0.3.0-M3",
    "home.work.commons"  %% "slick-extensions-core"    % "9.0.0",
    "home.work.commons"  %% "slick-extensions-testkit" % "9.0.0" % Test
  )

  val jms = Seq(
    "home.work.wms" %% "alpakka-tap"          % Versions.alpakkaTap,
    "home.work.wms" %% "alpakka-tap-activemq" % Versions.alpakkaTap
  )

  val autostore = Seq(
    "home.work" %% "autostorecommunicationservice-transport" % "4.0.0"
  )

  Seq(
    libraryDependencies ++=
      cats ++ circe ++ config ++ logging ++ healthCheck ++ akkaHttp ++ kamon ++ sttp ++ slick ++ jms ++ autostore
  )
}

val activemqClientTestSettings = {
  val dependencies = {
    libraryDependencies ++= Seq(
      "org.scalactic" %% "scalactic" % "3.1.0",
      "org.scalatest" %% "scalatest" % "3.1.0"
    ).map(_ % Test)
  }

  Seq(
    logBuffered in Test := false,
    dependencies
  )
}

val orderServiceTestSettings = {
  val dependencies = {
    libraryDependencies ++= Seq(
      "org.scalatest"          %% "scalatest"       % "3.1.0",
      "com.ironcorelabs"       %% "cats-scalatest"  % "3.0.5",
      "com.softwaremill.diffx" %% "diffx-scalatest" % "0.3.17"
    ).map(_ % "test,it")
  }

  Seq(
    logBuffered in Test := false,
    dependencies
  )
}

val commonSettings = Seq(
  organization := "home.work.wms",
  scalaVersion := "2.12.11",
  libraryDependencies ++= compilerPlugins
) ++ compilerOptions

val activemqClient = project
  .in(file("activemq-client"))
  .settings(
    name := "activemq-simple-client",
    commonSettings,
    dependencies,
    activemqClientTestSettings
  )

val orderService = project
  .in(file("order-service"))
  .enablePlugins(AppInfoPlugin, EnvReaderPlugin)
  .dependsOn(activemqClient)
  .settings(
    name := "onboarding-order-service",
    commonSettings,
    dependencies,
    orderServiceTestSettings,
    noPublishPlease,
    parallelExecution in IntegrationTest := false
  )
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)

val root = (project in file(".")).aggregate(orderService)
