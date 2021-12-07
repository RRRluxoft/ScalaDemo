credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

resolvers += "Sonatype Nexus Repository Manager Releases" at "https://my/repository/maven-releases/"
resolvers += "Sonatype Nexus Repository Manager Snapshots" at "https://my/repository/maven-snapshots/"

addSbtPlugin("se.marcuslonnberg"         % "sbt-docker"   % "1.5.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.11")
