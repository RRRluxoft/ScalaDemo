enablePlugins(DockerPlugin)

buildOptions in docker := BuildOptions(
  pullBaseImage = BuildOptions.Pull.Always
)

imageNames in docker := {
  val dockerRegistryUrl =
    Option(System.getProperty("dockerRegistryUrl")).getOrElse("")
  val ecrRegistryUrl =
    Option(System.getProperty("ecrRegistryUrl")).getOrElse("")
  val dockerImageTag =
    Option(System.getProperty("dockerImageTag")).getOrElse(version.value)
  val dockerImageGroup =
    Option(System.getProperty("dockerImageGroup")).getOrElse("")

  Seq(dockerRegistryUrl, ecrRegistryUrl).map(url =>
    ImageName(
      repository = s"$url$dockerImageGroup${name.value.toLowerCase}",
      tag = Some(dockerImageTag)
    ))
}

def commandToDownloadArtifact(artifactPath: String) = {
  (sys.env.get("NEXUS_USERNAME"), sys.env.get("NEXUS_PASSWORD")) match {
    case (Some(user), Some(password)) =>
      List(
        "wget",
        s"--user=$user",
        s"--password=$password",
        s"""https://nexus..tech.lastmile.com/repository/maven-central/$artifactPath"""
      )
    case _                            => List("wget", s"""http://repo1.maven.org/maven2/$artifactPath""")
  }
}

dockerfile in docker := {
  val targetDir = "/"
  val appJar = assembly.value
  val appJarName = (assemblyJarName in assembly).value
  val aspectjweaverVersion = "1.8.13"

  val logback = (resourceDirectory in Compile).value / "logback.xml"

  new Dockerfile {
    from(
      "internal.docker.rrr.tech.lastmile.com/newrelic-base:5.7.0-jdk8-1.7"
    )
    user("0")
    run(
      commandToDownloadArtifact(
        s"org/aspectj/aspectjweaver/$aspectjweaverVersion/aspectjweaver-$aspectjweaverVersion.jar"
      ): _*
    )
    run("chmod", "755", s"aspectjweaver-$aspectjweaverVersion.jar")
    user("1000")
    add(appJar, targetDir)
    add(logback, targetDir)
    env(
      "JAVA_ADDITIONAL_OPTIONS",
      s"-javaagent:/aspectjweaver-$aspectjweaverVersion.jar"
    )
    env("APP_JAR_PATH", appJarName)
  }
}
