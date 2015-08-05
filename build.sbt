name := "mining"

organization := "org.ms"

version := "0.0.1"



scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-feature")


libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.3.7",
    					    "com.typesafe.akka" %% "akka-testkit" % "2.3.7",
    					    "rome" % "rome" % "1.0",
    					    "org.scalaj" %% "scalaj-http" % "1.1.0" )
    					   
libraryDependencies ++= Seq("org.slf4j" % "slf4j-api" % "1.7.5",
    					    "org.slf4j" % "slf4j-simple" % "1.7.5",
    					    "org.slf4j" % "slf4j-nop" % "1.7.5")
    					    

libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "2.1.0",
                            "com.h2database"     %  "h2"    % "1.3.166") 
                            
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"