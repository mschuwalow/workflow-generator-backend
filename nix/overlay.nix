final: prev: {
  workflow-generator-backend = rec {
    app = prev.callPackage ./derivations/app.nix { };

    docker = prev.dockerTools.buildImage {
      name = "workflow-generator-backend";
      tag = "latest";
      contents = [ app ];
      config = { Cmd = [ "/bin/workflow-generator-backend" ]; };
    };
  };
}
