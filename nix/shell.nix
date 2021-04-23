{ pkgs }:
with pkgs;
mkShell {
  buildInputs = [ gnumake nixfmt docker-compose sbt yq moreutils ]
    ++ (with workflow-generator-backend; jar.buildInputs ++ runtimeDeps);
}
