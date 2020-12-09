import sbt._

object Dependencies {

  object Versions {
    val circe      = "0.13.0"
    val doobie     = "0.9.4"
    val http4s     = "0.21.13"
    val log4j      = "2.14.0"
    val zio        = "1.0.3"
    val zioLogging = "0.5.4"
  }
  import Versions._

  val App =
    List(
      "com.github.pureconfig"   %% "pureconfig"               % "0.14.0",
      "com.h2database"           % "h2"                       % "1.4.200",
      "com.lihaoyi"             %% "sourcecode"               % "0.2.1",
      "dev.zio"                 %% "zio-interop-cats"         % "2.2.0.1",
      "dev.zio"                 %% "zio-logging-slf4j"        % zioLogging,
      "dev.zio"                 %% "zio-logging"              % zioLogging,
      "dev.zio"                 %% "zio-streams"              % zio,
      "dev.zio"                 %% "zio-test-sbt"             % zio     % "test",
      "dev.zio"                 %% "zio-test"                 % zio     % "test",
      "dev.zio"                 %% "zio"                      % zio,
      "io.circe"                %% "circe-core"               % circe,
      "io.circe"                %% "circe-generic"            % circe,
      "io.circe"                %% "circe-literal"            % circe   % "test",
      "net.sf.py4j"              % "py4j"                     % "0.10.9.1",
      "org.apache.logging.log4j" % "log4j-api"                % log4j,
      "org.apache.logging.log4j" % "log4j-core"               % log4j,
      "org.apache.logging.log4j" % "log4j-slf4j-impl"         % log4j,
      "org.flywaydb"             % "flyway-core"              % "7.3.1",
      "org.http4s"              %% "http4s-blaze-server"      % http4s,
      "org.http4s"              %% "http4s-circe"             % http4s,
      "org.http4s"              %% "http4s-dsl"               % http4s,
      "org.scala-lang.modules"  %% "scala-parser-combinators" % "1.1.2",
      "org.tpolecat"            %% "doobie-core"              % doobie,
      "org.tpolecat"            %% "doobie-hikari"            % doobie,
      "org.tpolecat"            %% "doobie-postgres"          % doobie,
      "org.typelevel"           %% "jawn-parser"              % "1.0.1",
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
      compilerPlugin(
        ("org.typelevel" % "kind-projector" % "0.11.1").cross(CrossVersion.full)
      )
    )
}
