import sbt.Keys._


lazy val sesameVersion = "2.8.1"
lazy val bananaVersion = "0.8.1"

lazy val commonSettings = Seq(
  name := "lwm-semantics",
  version := "1.0",
  organization := "lwm",
  version := "0.1.0",
  scalaVersion := "2.11.6"
)

lazy val root = (project in file(".")).
  settings(Defaults.coreDefaultSettings: _*).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= semanticDependencies,
    libraryDependencies ++= testDependencies
  )

lazy val semanticDependencies = Seq(
  "org.w3" %% "banana-rdf" % bananaVersion,
  "org.w3" %% "banana-sesame" % bananaVersion,
  "org.openrdf.sesame" % "sesame-runtime" % sesameVersion
)

lazy val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "org.scalactic" %% "scalactic" % "2.2.4"
)