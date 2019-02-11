import sbt.Keys.scalaVersion
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoPackage

name := "wasted-util"

organization := "io.wasted"

version := scala.io.Source.fromFile("version").mkString.trim

scalaVersion := "2.12.4"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps", "-language:implicitConversions")

libraryDependencies ++= Seq(
  "com.twitter" %% "util-core" % "18.8.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe" % "config" % "1.3.2",
  "com.google.guava" % "guava" % "19.0",
  "io.netty" % "netty-all" % "4.1.28.Final",
  "org.javassist" % "javassist" % "3.21.0-GA",
  "com.google.code.findbugs" % "jsr305" % "2.0.1",
  "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

// For testing
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

enablePlugins(BuildInfoPlugin)

buildInfoKeys ++= Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "io.wasted.util.build"