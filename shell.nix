let
  sources = import ./nix/sources.nix;
  overlays = [
    (self: super: {
      jdk = super.openjdk14;
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
