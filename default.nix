with import <nixpkgs> { };

stdenv.mkDerivation {
  name = "lollipop";

  buildInputs = [leiningen nodejs chromium];
}