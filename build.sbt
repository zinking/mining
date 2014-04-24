name := "mining"

organization := "org.ms"

version := "0.0.1"



scalaVersion := "2.10.2"

scalacOptions ++= Seq("-deprecation", "-feature")


libraryDependencies += "org.scalatest" %% "scalatest" % "1.9.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.2.3",
    					    "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
    					    "rome" % "rome" % "1.0",
    					    "org.scalaj" %% "scalaj-http" % "0.3.14" )
    					   
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
    					    "org.slf4j" % "slf4j-simple" % "1.7.5",
    					    "org.slf4j" % "slf4j-nop" % "1.7.5")

libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "2.0.1",
                            "com.h2database"     %  "h2"    % "1.3.166") 
  					   
    					   
//The following configuration is in the format of sbt 0.12 
//0.13 please check http://www.scala-sbt.org/0.13.1/docs/Detailed-Topics/Java-Sources.html for details
// Include only src/main/java in the compile configuration
unmanagedSourceDirectories in Compile <<= Seq(scalaSource in Compile).join

// Include only src/test/java in the test configuration
unmanagedSourceDirectories in Test <<= Seq(scalaSource in Test).join

unmanagedClasspath in Runtime <<= (unmanagedClasspath in Runtime, baseDirectory) map { (cp, bd) => cp :+ Attributed.blank(bd / "config") }

