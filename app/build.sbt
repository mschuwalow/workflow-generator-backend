addCommandAlias("build", "prepare; test")
addCommandAlias("prepare", "fix; fmt")
addCommandAlias("check", "fixCheck; fmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")
addCommandAlias(
  "fixCheck",
  "compile:scalafix --check; test:scalafix --check"
)
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias(
  "fmtCheck",
  "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck"
)

inThisBuild(
  List(
    organization := "com.schuwalow",
    developers := List(
      Developer(
        "mschuwalow",
        "Maxim Schuwalow",
        "mschuwalow@uos.de",
        url("https://github.com/mschuwalow")
      )
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalaVersion := "2.13.4",
    scalafixDependencies ++= Dependencies.ScalaFix
  )
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerSpotifyClientPlugin)
  .settings(
    name := "workflow-generator-backend",
    scalacOptions in ThisBuild := Options
      .scalacOptions(scalaVersion.value, isSnapshot.value),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Dependencies.App
  )

assemblyMergeStrategy in assembly := {
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x                                     =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
