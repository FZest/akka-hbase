import CommonDependency.dependencies

organization in ThisBuild := "io.github.junheng.akka"

scalaVersion in ThisBuild := "2.11.7"

lazy val protocol = project
  .settings(
    name := "akka-hbase-protocol",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= dependencies.reflection,
    libraryDependencies ++= dependencies.hbase_CDH5,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-pickling" % "0.10.1" withSources()
    )
  )

lazy val proxy = project
  .dependsOn(service, protocol)
  .settings(
    name := "akka-hbase-proxy",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= dependencies.logs,
    libraryDependencies ++= Seq(
      "io.github.junheng.akka" %% "akka-accessor" % "0.1-SNAPSHOT" withSources(),
      "io.github.junheng.akka" %% "akka-locator" % "0.1-SNAPSHOT" withSources(),
      "io.github.junheng.akka" %% "akka-monitor" % "0.1-SNAPSHOT" withSources()
    )
  )

lazy val service = project
  .dependsOn(protocol)
  .settings(
    name := "akka-hbase-service",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= dependencies.scala,
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= Seq(
      "io.github.junheng.akka" %% "akka-utils" % "0.1-SNAPSHOT" withSources()
    )
  )
