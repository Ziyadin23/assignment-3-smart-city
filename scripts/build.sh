#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

mkdir -p build/classes
mapfile -d '' sources < <(find src -type f -name '*.java' -print0)
javac --release 17 -Xlint:all -d build/classes "${sources[@]}"
