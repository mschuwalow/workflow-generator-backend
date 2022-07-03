import sbt._

object Dependencies {

  object Versions {
    val betterMonadicFor       = "0.3.1"
    val circe                  = "0.14.2"
    val circeJsonSchema        = "0.2.0"
    val doobie                 = "0.13.4"
    val flyway                 = "8.5.13"
    val http4s                 = "0.21.26"
    val jawn                   = "1.3.2"
    val kindProjector          = "0.13.2"
    val korolev                = "1.2.0"
    val log4j                  = "2.18.0"
    val organizeImports        = "0.6.0"
    val pureConfig             = "0.17.1"
    val py4j                   = "0.10.9.5"
    val scalaParserCombinators = "2.1.1"
    val silencer               = "1.7.0"
    val sttp                   = "2.3.0"
    val tsec                   = "0.4.0"
    val zio                    = "1.0.15"
    val zioInteropCats         = "2.5.1.0"
    val zioKafka               = "0.14.0"
    val zioLogging             = "0.5.14"
    val zioPrelude             = "1.0.0-RC5"
  }
  import Versions._

  val App = List(
    "com.github.pureconfig"        %% "pureconfig"               % pureConfig,
    "com.softwaremill.sttp.client" %% "httpclient-backend-zio"   % sttp,
    "com.softwaremill.sttp.client" %% "circe"                    % sttp,
    "dev.zio"                      %% "zio-interop-cats"         % zioInteropCats,
    "dev.zio"                      %% "zio-kafka"                % zioKafka,
    "dev.zio"                      %% "zio-logging-slf4j"        % zioLogging,
    "dev.zio"                      %% "zio-logging"              % zioLogging,
    "dev.zio"                      %% "zio-prelude"              % zioPrelude,
    "dev.zio"                      %% "zio-streams"              % zio,
    "dev.zio"                      %% "zio-test-sbt"             % zio   % "test",
    "dev.zio"                      %% "zio-test"                 % zio   % "test",
    "dev.zio"                      %% "zio"                      % zio,
    "io.circe"                     %% "circe-core"               % circe,
    "io.circe"                     %% "circe-generic-extras"     % circe,
    "io.circe"                     %% "circe-generic"            % circe,
    "io.circe"                     %% "circe-json-schema"        % circeJsonSchema,
    "io.circe"                     %% "circe-literal"            % circe % "test",
    "io.github.jmcardon"           %% "tsec-http4s"              % tsec,
    "net.sf.py4j"                   % "py4j"                     % py4j,
    "org.apache.logging.log4j"      % "log4j-api"                % log4j,
    "org.apache.logging.log4j"      % "log4j-core"               % log4j,
    "org.apache.logging.log4j"      % "log4j-slf4j-impl"         % log4j,
    "org.flywaydb"                  % "flyway-core"              % flyway,
    "org.fomkin"                   %% "korolev-http4s"           % korolev,
    "org.fomkin"                   %% "korolev-zio"              % korolev,
    "org.http4s"                   %% "http4s-blaze-server"      % http4s,
    "org.http4s"                   %% "http4s-circe"             % http4s,
    "org.http4s"                   %% "http4s-dsl"               % http4s,
    "org.scala-lang.modules"       %% "scala-parser-combinators" % scalaParserCombinators,
    "org.tpolecat"                 %% "doobie-core"              % doobie,
    "org.tpolecat"                 %% "doobie-hikari"            % doobie,
    "org.tpolecat"                 %% "doobie-postgres-circe"    % doobie,
    "org.tpolecat"                 %% "doobie-postgres"          % doobie,
    "org.typelevel"                %% "jawn-parser"              % jawn,
    compilerPlugin("com.olegpy"   %% "better-monadic-for" % betterMonadicFor),
    compilerPlugin("org.typelevel" % "kind-projector"     % kindProjector cross CrossVersion.full)
  )

  val ScalaFix = List(
    "com.github.liancheng" %% "organize-imports" % organizeImports
  )

}
