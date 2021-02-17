import sbt._

object Dependencies {

  object Versions {
    val betterMonadicFor       = "0.3.1"
    val circe                  = "0.13.0"
    val doobie                 = "0.9.4"
    val flyway                 = "7.3.1"
    val http4s                 = "0.21.13"
    val jawn                   = "1.0.1"
    val kindProjector          = "0.11.1"
    val korolev                = "0.17.0"
    val log4j                  = "2.14.0"
    val organizeImports        = "0.4.4"
    val pureConfig             = "0.14.0"
    val py4j                   = "0.10.9.1"
    val scalaParserCombinators = "1.1.2"
    val zio                    = "1.0.4-2"
    val zioInteropCats         = "2.2.0.1"
    val zioLogging             = "0.5.4"
    val zioPrelude             = "1.0.0-RC2"
  }
  import Versions._

  val App =
    List(
      "com.github.pureconfig"   %% "pureconfig"               % pureConfig,
      "dev.zio"                 %% "zio-interop-cats"         % zioInteropCats,
      "dev.zio"                 %% "zio-logging-slf4j"        % zioLogging,
      "dev.zio"                 %% "zio-logging"              % zioLogging,
      "dev.zio"                 %% "zio-streams"              % zio,
      "dev.zio"                 %% "zio-test-sbt"             % zio   % "test",
      "dev.zio"                 %% "zio-test"                 % zio   % "test",
      "dev.zio"                 %% "zio"                      % zio,
      "io.circe"                %% "circe-core"               % circe,
      "io.circe"                %% "circe-generic-extras"     % circe,
      "io.circe"                %% "circe-generic"            % circe,
      "io.circe"                %% "circe-literal"            % circe % "test",
      "net.sf.py4j"              % "py4j"                     % py4j,
      "org.apache.logging.log4j" % "log4j-api"                % log4j,
      "org.apache.logging.log4j" % "log4j-core"               % log4j,
      "org.apache.logging.log4j" % "log4j-slf4j-impl"         % log4j,
      "org.flywaydb"             % "flyway-core"              % flyway,
      "org.fomkin"              %% "korolev-http4s"           % korolev,
      "org.fomkin"              %% "korolev-zio"              % korolev,
      "org.http4s"              %% "http4s-blaze-server"      % http4s,
      "org.http4s"              %% "http4s-circe"             % http4s,
      "org.http4s"              %% "http4s-dsl"               % http4s,
      "org.scala-lang.modules"  %% "scala-parser-combinators" % scalaParserCombinators,
      "org.tpolecat"            %% "doobie-core"              % doobie,
      "org.tpolecat"            %% "doobie-hikari"            % doobie,
      "org.tpolecat"            %% "doobie-postgres-circe"    % doobie,
      "org.tpolecat"            %% "doobie-postgres"          % doobie,
      "org.typelevel"           %% "jawn-parser"              % jawn,
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicFor),
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjector).cross(CrossVersion.full)
    )

  val ScalaFix =
    List(
      "com.github.liancheng" %% "organize-imports" % organizeImports
    )

}
