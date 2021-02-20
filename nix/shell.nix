{ pkgs ? import <nixpkgs> }:
pkgs.mkShell {
  buildInputs = with pkgs;
    [ gnumake nixfmt docker-compose sbt ]
    ++ (with workflow-generator-backend; jar.buildInputs ++ runtimeDeps);
}
