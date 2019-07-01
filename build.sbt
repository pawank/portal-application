val finchVersion = "0.29.0"
val circeVersion = "0.11.1"
val scalatestVersion = "3.0.5"

lazy val root = (project in file("."))
  .settings(
    organization := "com.abc",
    name := "portal-server",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    mainClass in assembly := Some("com.abc.portalserver.Main"),
    assemblyMergeStrategy in assembly := {
      case PathList("io", "netty", xs @ _*)             => MergeStrategy.last
      case PathList("org", "aopalliance", xs @ _*)      => MergeStrategy.last
      case PathList("javax", "inject", xs @ _*)         => MergeStrategy.last
      case PathList("javax", "servlet", xs @ _*)        => MergeStrategy.last
      case PathList("javax", "activation", xs @ _*)     => MergeStrategy.last
      case PathList("org", "apache", xs @ _*)           => MergeStrategy.last
      case PathList("com", "google", xs @ _*)           => MergeStrategy.last
      case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
      case PathList("com", "codahale", xs @ _*)         => MergeStrategy.last
      case PathList("com", "yammer", xs @ _*)           => MergeStrategy.last
      case "about.html"                                 => MergeStrategy.rename
      case "META-INF/ECLIPSEF.RSA"                      => MergeStrategy.last
      case "META-INF/mailcap"                           => MergeStrategy.last
      case "META-INF/mimetypes.default"                 => MergeStrategy.last
      case "plugin.properties"                          => MergeStrategy.last
      case "log4j.properties"                           => MergeStrategy.last
      case x if x.contains("io.netty.versions.properties") =>
        MergeStrategy.discard
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core" % finchVersion,
      "com.github.finagle" %% "finchx-circe" % finchVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "com.github.finagle" %% "finchx-oauth2" % "0.29.0-SNAPSHOT",
      "com.outr" %% "scarango-driver" % "2.0.0",
      "com.orientechnologies" % "orientdb-client" % "3.1.0-M2",
      "com.michaelpollmeier" %% "gremlin-scala" % "3.4.1.6",
      "com.orientechnologies" % "orientdb-gremlin" % "3.1.0-M2",
      "io.scalaland" %% "chimney" % "0.3.2",
      //"com.github.finagle" %% "finagle-oauth2" % "19.4.0",
      "org.scalatest" %% "scalatest" % scalatestVersion % "test"
    )
  )
