name := "maze"

scalaVersion := "2.12.8"

lazy val root =
  project
    .in(file("."))
    .settings(
      libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )