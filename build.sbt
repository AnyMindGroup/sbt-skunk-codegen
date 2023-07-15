ThisBuild / organization := "com.anymindgroup"
ThisBuild / scalaVersion := "2.12.18"

val betterFilesVersion = "3.9.2"

lazy val commonSettings = List(
  credentials += {
    for {
      username <- sys.env.get("ARTIFACT_REGISTRY_USERNAME")
      apiKey   <- sys.env.get("ARTIFACT_REGISTRY_PASSWORD")
    } yield Credentials("https://asia-maven.pkg.dev", "asia-maven.pkg.dev", username, apiKey)
  }.getOrElse(Credentials(Path.userHome / ".ivy2" / ".credentials")),
  version ~= { v => if (v.contains('+')) s"${v.replace('+', '-')}-SNAPSHOT" else v },
)

lazy val root = (project in file("."))
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
  publishTo := {
    val pkgDev = "https://asia-maven.pkg.dev/anychat-staging"
    if (isSnapshot.value)
      Some("https://asia-maven.pkg.dev" at pkgDev + "/maven-snapshot")
    else
      Some("https://asia-maven.pkg.dev" at pkgDev + "/maven-release")
  }
)

lazy val core = (project in file("modules/core"))
  .settings(
    name := "skunk-codegen",
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    Compile / scalacOptions ++= Seq("-Xsource:3", "-release:17"),
    libraryDependencies ++= Seq(
      "com.anymindgroup"     %% "dumbo"        % "0.0.1",
      "com.github.pathikrit" %% "better-files" % betterFilesVersion,
    ),
  )
  .settings(commonSettings)
  .settings(releaseSettings)

lazy val sbtPlugin = (project in file("modules/sbt"))
  .enablePlugins(SbtPlugin)
  .dependsOn(core)
  .settings(commonSettings)
  .settings(releaseSettings)
  .settings(
    name         := "sbt-skunk-codegen",
    scalaVersion := "2.12.18",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
  )
