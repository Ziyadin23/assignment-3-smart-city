#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

bash scripts/build.sh
mkdir -p build/test-classes
mapfile -d '' test_sources < <(find tests -type f -name '*.java' -print0)
javac --release 17 -Xlint:all -cp build/classes -d build/test-classes "${test_sources[@]}"

for test_source in "${test_sources[@]}"; do
    if [[ "$test_source" == *Test.java ]]; then
        test_class="${test_source#tests/}"
        test_class="${test_class%.java}"
        java -ea -cp build/classes:build/test-classes "${test_class//\//.}"
    fi
done
