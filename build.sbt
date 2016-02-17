import sbt.Keys._

organization in ThisBuild := "io.github.junheng.akka"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.11.7"

scalacOptions in ThisBuild := Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions", "-encoding", "utf8")

resolvers in ThisBuild += "cloudera" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

resolvers in ThisBuild += "bintray" at "http://dl.bintray.com/lunadancer/akka"

lazy val protocol = project
  .settings(
    name := "protocol",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= library.test,
    libraryDependencies ++= library.json,
    libraryDependencies ++= library.reflection
  )


lazy val service = project
  .dependsOn(protocol)
  .settings(
    name := "service",
    version := "0.1-SNAPSHOT",
    libraryDependencies ++= library.hbase_CDH5,
    libraryDependencies ++= library.reflection
  )
