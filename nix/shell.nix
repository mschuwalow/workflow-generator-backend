{ pkgs }:
pkgs.mkShell {
  buildInputs = with pkgs; [ gnumake nixfmt docker-compose sbt yq moreutils fd ]
    ++ (with workflow-generator-backend; jar.buildInputs ++ runtimeDeps);
}
