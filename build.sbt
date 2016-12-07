lazy val commonSettings = Seq(
  version := "0.3.0",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.7"),
  organization := "org.afabl"
)

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "afabl.org",
    paradoxTheme := Some(builtinParadoxTheme("generic"))
  ).enablePlugins(JekyllPlugin).
  enablePlugins(ParadoxSitePlugin)
