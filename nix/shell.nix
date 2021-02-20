{ pkgs ? import <nixpkgs> }:
pkgs.mkShell {
  buildInputs = with pkgs;
    [ gnumake nixfmt docker-compose ] ++ workflow-generator-backend.app.buildInputs;
}
