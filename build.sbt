name := "mining"

organization := "org.ms"

version := "0.0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-feature")


libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-xml" % "1.0.2",
    "com.typesafe.akka" %% "akka-actor" % "2.3.7",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.7",
    "rome" % "rome" % "1.0",
    "org.slf4j" % "slf4j-simple" % "1.7.5",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test",
    "junit" % "junit" % "4.10" % "test",
    "org.scalaj" %% "scalaj-http" % "1.1.0",
    "mysql" % "mysql-connector-java" % "5.1.35",
    "io.spray" %%  "spray-json" % "1.3.2",
    "com.zaxxer" % "HikariCP" % "2.4.3"
)
    					   