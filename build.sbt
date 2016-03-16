import CommonDependency.dependencies

organization in ThisBuild := "io.github.junheng.akka"

scalaVersion in ThisBuild := "2.11.7"

version in ThisBuild := "0.12-SNAPSHOT"

lazy val protocol = project
  .settings(
    name := "akka-hbase-protocol",
    libraryDependencies ++= Seq(
      "io.github.junheng.akka" %% "akka-utils" % "0.1-SNAPSHOT" withSources()
    ),
    libraryDependencies ++= dependencies.scala,
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= dependencies.reflection,
    libraryDependencies ++= dependencies.curator,
    libraryDependencies ++= dependencies.hbase_CDH5,
    libraryDependencies ++= dependencies.test
  )

lazy val proxy = project
  .enablePlugins(JavaServerAppPackaging)
  .dependsOn(service, protocol)
  .settings(
    name := "akka-hbase-proxy",
    libraryDependencies ++= dependencies.logs,
    libraryDependencies ++= Seq(
      "io.github.junheng.akka" %% "akka-accessor" % "0.1-SNAPSHOT" withSources(),
      "io.github.junheng.akka" %% "akka-locator" % "0.13-SNAPSHOT" withSources(),
      "io.github.junheng.akka" %% "akka-monitor" % "0.1-SNAPSHOT" withSources()
    )
  )

lazy val service = project
  .dependsOn(protocol)
  .settings(
    name := "akka-hbase-service",
    libraryDependencies ++= dependencies.scala,
    libraryDependencies ++= dependencies.akka
  )

