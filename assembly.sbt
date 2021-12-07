import sbtassembly.MergeStrategy

test in assembly := {}

assemblyJarName in assembly := name.value + ".jar"

assemblyOutputPath in assembly := file(
  "target/out/" + (assemblyJarName in assembly).value
)

publishArtifact in (Compile, packageDoc) := false

assemblyMergeStrategy in assembly := SbtAssemblyAopPlugin
  .defaultStrategyWithAop {
    case PathList("javax", "jms", xs @ _*)                   => MergeStrategy.first
    case PathList(ps @ _*) if ps.last == "overview.html"     => MergeStrategy.first
    case PathList(ps @ _*) if ps.last endsWith ".properties" =>
      MergeStrategy.concat
    case x                                                   =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
