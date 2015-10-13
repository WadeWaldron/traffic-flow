name := """TrafficFlow"""

scalaVersion := "2.11.7"

val akkaVersion = "2.3.13"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion withSources(),  
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion withSources(),  
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0" withSources(), 
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)
