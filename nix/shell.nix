{ pkgs ? import <nixpkgs> }:
pkgs.mkShell {
  buildInputs = with pkgs;
    [ gnumake nixfmt ] ++ workflow-generator-backend.app.buildInputs;
}
