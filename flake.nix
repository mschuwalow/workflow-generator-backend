{

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    sbt-derivation.url = "github:zaninime/sbt-derivation";
  };

  outputs = { self, nixpkgs, flake-utils, sbt-derivation }:

    flake-utils.lib.simpleFlake {
      inherit self nixpkgs;
      name = "workflow-generator-backend";
      preOverlays = [
        # korolev wants jdk 11 or newer.
        (self: super: { jre = super.adoptopenjdk-jre-hotspot-bin-14; })
        # fix py4j issues with client server.
        (self: super: {
          python38 = super.python38.override {
            packageOverrides = python-self: python-super: {
              py4j = python-super.py4j.overrideAttrs
                (oldAttrs: { patches = [ ./nix/patches/fix-py4j.diff ]; });
            };
          };
        })
        sbt-derivation.overlay
      ];
      overlay = ./nix/overlay.nix;
      shell = ./nix/shell.nix;
      systems = [ "x86_64-darwin" "x86_64-linux" ];
    };
}
