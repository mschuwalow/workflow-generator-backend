final: prev:
with prev; {

  workflow-generator-backend = rec {

    jar = sbt.mkDerivation rec {
      pname = "workflow-generator-backend-jar";
      version = "0.0.1";

      # depsSha256 = "0000000000000000000000000000000000000000000000000000";
      depsSha256 = "74yc8FitUd7HgXGF0QuDqNMotSdXdMCmYYQLFYxlqN4=";

      src = ../.;

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
      version = "0.0.1";

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
