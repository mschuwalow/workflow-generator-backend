let
  sources = import ./nix/sources.nix;
  pkgs = import sources.nixpkgs { };
  customPython = pkgs.python38.buildEnv.override {
    extraLibs = with pkgs.python38Packages; [ py4j click ];
  };
in pkgs.mkShell {
  buildInputs = with pkgs; [
    customPython
  ];
}
