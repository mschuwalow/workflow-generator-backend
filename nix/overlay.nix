self: super:
with super;
with lib; {

  workflow-generator-backend = rec {

    jar = sbt.mkDerivation rec {
      pname = "workflow-generator-backend-jar";
      version = "latest";

      # depsSha256 = "0000000000000000000000000000000000000000000000000000";
      depsSha256 = "0LkktGh/zvlnRxlkyS99RhU1hssXCYo2j5NgrJgO0Sk=";

      src = sources.sourceByRegex ../. [ "^build.sbt$" "^project.*" "^src.*" ];

      buildPhase = ''
        sbt 'set test in assembly := {}' assembly
      '';

      installPhase = ''
        cp target/scala-*/*-assembly-*.jar $out
      '';
    };

    launcher = writeShellScriptBin "launch-app" ''
      ${jre}/bin/java -jar ${jar}
    '';

    runtimeDeps = [
      (python38.buildEnv.override {
        extraLibs = with python38Packages; [ py4j click ];
      })
    ];

    app = stdenv.mkDerivation {
      pname = "workflow-generator-backend";
      version = "latest";

      buildInputs = [ makeWrapper ];

      unpackPhase = "true";

      installPhase = ''
        mkdir -p "$out/bin"
        makeWrapper ${launcher}/bin/launch-app "$out/bin/workflow-generator-backend" \
          --prefix PATH : ${lib.makeBinPath runtimeDeps}
      '';

    };

    docker = dockerTools.buildImage {
      name = "workflow-generator-backend";
      tag = "latest";
      contents = [ app ];
      config = { Cmd = [ "/bin/workflow-generator-backend" ]; };
    };
  };
}
