let
  sources = import ./nix/sources.nix;
  overlays = [
    (self: super: {
      jdk = super.openjdk14;
    })
    (self: super: {
      python38 = super.python38.override {
        packageOverrides = python-self: python-super: {
          py4j = python-super.py4j.overrideAttrs (oldAttrs: {
            patches = [ ./patches/fix-py4j.diff ];
          });
        };
      };
    })
  ];
  pkgs = import sources.nixpkgs { inherit overlays; };
  customPython = pkgs.python38.buildEnv.override {
    extraLibs = with pkgs.python38Packages; [ py4j click ];
  };
in pkgs.mkShell {
  buildInputs = with pkgs; [
    customPython
    sbt-extras
  ];
}
