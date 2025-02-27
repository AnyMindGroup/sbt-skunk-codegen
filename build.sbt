lazy val scala212 = "2.12.19"
lazy val scala213 = "2.13.15"
lazy val scala3   = "3.3.4"
lazy val allScala = Seq(scala212, scala213, scala3)

ThisBuild / organization         := "com.anymindgroup"
ThisBuild / organizationName     := "AnyMind Group"
ThisBuild / organizationHomepage := Some(url("https://anymindgroup.com"))
ThisBuild / licenses             := Seq(License.Apache2)
ThisBuild / homepage             := Some(url("https://github.com/AnyMindGroup/sbt-skunk-codegen"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/AnyMindGroup/sbt-skunk-codegen"),
    "scm:git@github.com:AnyMindGroup/sbt-skunk-codegen.git",
  )
)
ThisBuild / description := "SBT plugin for generating source code from Postgres database schema."
ThisBuild / developers := List(
  Developer("rolang", "Roman Langolf", "@rolang", url("https://github.com/rolang")),
  Developer("dutch3883", "Panuwach Boonyasup", "@dutch3883", url("https://github.com/dutch3883")),
  Developer("qhquanghuy", "Huy Nguyen", "@qhquanghuy", url("https://github.com/qhquanghuy")),
  Developer("alialiusefi", "Ali Al-Yousefi", "@alialiusefi", url("https://github.com/alialiusefi"))
)
ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeCentralHost

lazy val betterFilesVersion = "3.9.2"
lazy val commonSettings = List(
  libraryDependencies ++= {
    if (scalaVersion.value == scala3)
      Seq()
    else
      Seq(compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"))
  },
  version ~= { v => if (v.contains('+')) s"${v.replace('+', '-')}-SNAPSHOT" else v },
  Test / scalacOptions --= Seq("-Xfatal-warnings"),
)

lazy val sbtSkunkCodegen = (project in file("."))
  .dependsOn(core, sbtPlugin)
  .aggregate(core, sbtPlugin)
  .settings(noPublishSettings)

val noPublishSettings = List(
  publish         := {},
  publishLocal    := {},
  publishArtifact := false,
  publish / skip  := true,
)

val releaseSettings = List(
  publishTo := sonatypePublishToBundle.value
)

lazy val core = (project in file("modules/core"))
  .settings(
    name               := "skunk-codegen",
    scalaVersion       := scala213,
    crossScalaVersions := allScala,
    javacOptions ++= Seq("-source", "17"),
    Compile / scalacOptions ++= {
      if (scalaVersion.value == scala3)
        Seq("-source:future")
      else if (scalaVersion.value == scala213)
        Seq(
          "-Ymacro-annotations",
          "-Xsource:3",
          "-Wconf:cat=scala3-migration:s",
        ) // https://github.com/scala/scala/pull/10439
      else
        Seq("-Xsource:3")
    },
    libraryDependencies ++= Seq(
      "dev.rolang"           %% "dumbo"        % "0.0.9",
      "com.github.pathikrit" %% "better-files" % betterFilesVersion,
    ),
  )
  .settings(commonSettings)
  .settings(releaseSettings)

lazy val sbtPlugin = (project in file("modules/sbt"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .aggregate(core)
  .settings(commonSettings)
  .settings(releaseSettings)
  .settings(
    name                             := "sbt-skunk-codegen",
    sbtPluginPublishLegacyMavenStyle := false,
    scalaVersion                     := scala212,
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
  )
