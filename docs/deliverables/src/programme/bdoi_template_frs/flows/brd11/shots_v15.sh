#!/usr/bin/env bash
# Full-page screenshots of User Access Maintenance v1.5 (UX design and process storyboards), taken on a local seed
# environment (backend with the seed profile, frontend dev server). shots_v15.sh [step-regex]
# Needs SEED_PASSWORD (the password of the SIT/UAT test accounts), BASE, API and PGDATABASE in the environment.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export CHROMIUM="${CHROMIUM:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
export CHROME_ARGS="--no-sandbox --disable-gpu --hide-scrollbars"
export PLAYWRIGHT_MODULE="${PLAYWRIGHT_MODULE:-/opt/node22/lib/node_modules/playwright}"
export OUT_DIR="${OUT_DIR:-$HERE/../../figures/brd11_v15_screens}"
exec node "$HERE/shots_v15.cjs" "${1:-.}"
