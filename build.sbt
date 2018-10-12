name := "magicscrapper"
version := "0.1"
scalaVersion := "2.12.6"
mainClass := Some("app.gathererscrapping.GathererScrapper")

libraryDependencies += "org.jsoup" % "jsoup" % "1.11.3"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.17"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.5"