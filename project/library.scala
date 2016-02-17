import sbt._

object library {

  val versionOfScala = "2.11.7"

  val versionOfJson4s = "3.2.11"

  val versionOfAkka = "2.4.1"

  val versionOfSpray = "1.3.3"

  val versionOfKafka = "0.8.2.0"

  val scala = Seq(
    "org.scala-lang" % "scala-reflect" % versionOfScala,
    "org.scala-lang" % "scala-compiler" % versionOfScala
  )

  val mongodb = Seq(
    "com.novus" %% "salat" % "1.9.9"
  )

  val reflection = Seq(
    "org.reflections" % "reflections" % "0.9.10"
  ) map (_ exclude ("com.google.guava","guava")  withSources())

  val logs = Seq(
    "org.slf4j" % "jul-to-slf4j" % "1.7.7",
    "org.slf4j" % "log4j-over-slf4j" % "1.7.7",
    "ch.qos.logback" % "logback-classic" % "1.1.3"
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.5" % "test",
    "org.specs2" %% "specs2" % "2.3.13" % "test",
    "org.apache.curator" % "curator-test" % "2.9.0" % "test"
  )

  val redis = Seq(
    "net.debasishg" %% "redisclient" % "3.0",
    "com.wandoulabs.jodis" % "jodis" % "0.2.2"
  ) map (_ withSources())

  val json = Seq(
    "org.json4s" %% "json4s-jackson" % versionOfJson4s,
    "org.json4s" %% "json4s-ext" % versionOfJson4s,
    "org.json4s" % "json4s-native_2.11" % versionOfJson4s
  ) map (_ withSources())

  val spary = Seq(
    "io.spray" %% "spray-can" % versionOfSpray,
    "io.spray" %% "spray-httpx" % versionOfSpray,
    "io.spray" %% "spray-http" % versionOfSpray
  ) map (_ withSources())

  val curator = Seq(
    "org.apache.curator" % "curator-recipes" % "2.9.0",
    "org.apache.curator" % "curator-x-discovery" % "2.9.0"
  ) map (_ withSources())

  val akka = Seq(
    "com.typesafe.akka" %% "akka-actor" % versionOfAkka,
    "com.typesafe.akka" %% "akka-cluster" % versionOfAkka,
    "com.typesafe.akka" %% "akka-kernel" % versionOfAkka,
    "com.typesafe.akka" %% "akka-slf4j" % versionOfAkka,
    "com.typesafe.akka" %% "akka-contrib" % versionOfAkka,
    "com.typesafe.akka" %% "akka-testkit" % versionOfAkka
  )

  val kafka = Seq(
    "org.apache.kafka" % "kafka-clients" % versionOfKafka,
    "org.apache.kafka" %% "kafka" % versionOfKafka
  ) map (_ exclude("org.slf4j", "slf4j-log4j12") withSources())

  val hbase_CDH4 = Seq(
    "org.apache.hbase" % "hbase" % "0.94.15-cdh4.7.1",
    "org.apache.hadoop" % "hadoop-common" % "2.0.0-cdh4.7.1",
    "org.apache.thrift" % "libthrift" % "0.9.0"
  ) map (_
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("org.apache.httpcomponents", "httpcore")
    exclude("org.apache.httpcomponents", "httpclient")
    withSources())

  val hbase_CDH5 = Seq(
    "org.apache.hadoop" % "hadoop-common" % "2.6.0-cdh5.5.1",
    "org.apache.hadoop" % "hadoop-core" % "2.6.0-mr1-cdh5.5.1",
    "org.apache.hbase" % "hbase-common" % "1.0.0-cdh5.5.1",
    "org.apache.hbase" % "hbase-client" % "1.0.0-cdh5.5.1",
    "org.apache.hbase" % "hbase-server" % "1.0.0-cdh5.5.1"
  ) map (_
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("org.apache.httpcomponents", "httpcore")
    exclude("org.apache.httpcomponents", "httpclient")
    withSources())

  val hbase_sep_CDH5 = Seq(
    "com.ngdata" % "hbase-sep-tools" % "1.5-cdh5.5.1",
    "com.ngdata" % "hbase-sep-impl-common" % "1.5-cdh5.5.1",
    "com.ngdata" % "hbase-sep-api" % "1.5-cdh5.5.1",
    "com.ngdata" % "hbase-sep-impl" % "1.5-hbase1.0-cdh5.5.1"
  ) map (_
    exclude("org.slf4j", "slf4j-log4j12")
    exclude("org.apache.httpcomponents", "httpcore")
    exclude("org.apache.httpcomponents", "httpclient")
    withSources())

  val common = Seq(
    "commons-codec" % "commons-codec" % "1.4"
  )
}
