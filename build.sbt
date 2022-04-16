addCommandAlias("build", "prepare; test")
addCommandAlias("prepare", "fix; fmt")
addCommandAlias("check", "fixCheck; fmtCheck")
addCommandAlias("fix", "all compile:scalafix test:scalafix")
addCommandAlias("fixCheck", "compile:scalafix --check; test:scalafix --check")
addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

resolvers += "jitpack".at("https://jitpack.io")

inThisBuild(
  List(
    organization      := "com.schuwalow",
    developers        := List(
      Developer(
        "mschuwalow",
        "Maxim Schuwalow",
        "mschuwalow@uos.de",
        url("https://github.com/mschuwalow")
      )
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalaVersion      := "2.13.8",
    scalafixDependencies ++= Dependencies.ScalaFix
  )
)

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name           := "workflow-generator-backend",
    scalacOptions  := Options.scalacOptions(optimize = !isSnapshot.value),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Dependencies.App
  )

assembly / assemblyMergeStrategy := {
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x                                    =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}
