#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd -- "$project_root"

if (($# > 1)); then
    printf 'Usage: %s [public-server-url]\n' "$0" >&2
    exit 2
fi
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
    printf 'Usage: %s [public-server-url]\n' "$0"
    printf 'Rebuilds the hosted RuneLite client and shareable LocalScape.jar. Run ./dev setup once first.\n'
    exit 0
fi

./dev build-client
./dev configure
./dev build-launcher "$@"
