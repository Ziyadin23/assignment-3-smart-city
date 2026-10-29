#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."

bash scripts/build.sh
exec java -Xms256m -Xmx256m -cp build/classes benchmark.BenchmarkRunner "$@"
