with import <nixpkgs> { };

stdenv.mkDerivation {
  name = "desmo";

  buildInputs = [ boot ];
}
