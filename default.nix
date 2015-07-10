with import <nixpkgs> { };

stdenv.mkDerivation {
  name = "desmo";

  buildInputs = [leiningen nodejs chromium];
}