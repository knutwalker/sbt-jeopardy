import de.heikoseeberger.sbtheader.license.Apache2_0

enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
sbtPlugin := true

git.baseVersion := "0.1.0"
organization := "de.knutwalker"

name := "sbt-jeopardy"
description := "Plays jeopardy theme music during compile"

licenses := List("Apache 2.0" → url("https://www.apache.org/licenses/LICENSE-2.0.html"))
headers := Map("scala" -> Apache2_0("2016", "Paul Horn"))

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfuture",
  "-Xfatal-warnings",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-M15" % "test"
