import CommonDependency.dependencies

organization in ThisBuild := "io.github.junheng.akka"

scalaVersion in ThisBuild := "2.11.7"

lazy val protocol = project
  .settings(
    name := "akka-hbase-protocol",
    version := "0.1-SNAPSHOT"
  )

lazy val proxy = project
  .dependsOn(service)
  .settings(
    name := "akka-hbase-proxy",
    version := "0.1-SNAPSHOT"
  )

lazy val service = project
  .dependsOn(protocol)
  .settings(
    name := "akka-hbase-service",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= dependencies.scala,
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= dependencies.reflection,
    libraryDependencies ++= Seq(
      "io.github.junheng.akka" %% "akka-util" % "0.1-SNAPSHOT" withSources(),
      "io.github.junheng.akka" %% "akka-locator" % "0.1-SNAPSHOT" withSources()
    )
  )
