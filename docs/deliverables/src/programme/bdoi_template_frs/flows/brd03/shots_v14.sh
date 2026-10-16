#!/usr/bin/env bash
# Full-page screenshots of Product Maintenance v1.4 (UX design and process storyboards), taken on a local seed
# environment (backend with the seed profile, frontend dev server). shots_v14.sh [step-regex]
# Needs SEED_PASSWORD (the password of the SIT/UAT test accounts), BASE, API and PGDATABASE in the environment.
# Run tools/screenshots/packs/brd03_realistic.sql once on the fresh database first (display names).
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export CHROMIUM="${CHROMIUM:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
export CHROME_ARGS="--no-sandbox --disable-gpu --hide-scrollbars"
export PLAYWRIGHT_MODULE="${PLAYWRIGHT_MODULE:-/opt/node22/lib/node_modules/playwright}"
export OUT_DIR="${OUT_DIR:-$HERE/../../figures/brd03_v14_screens}"
node "$HERE/shots_v14.cjs" "${1:-.}"
# 256-colour palette images (flat screens), as the BRD-11 set.
python3 - "$OUT_DIR" <<'PY'
import sys, pathlib
from PIL import Image
for f in pathlib.Path(sys.argv[1]).glob("*.png"):
    im = Image.open(f)
    if im.mode != "P":
        im.convert("RGB").quantize(256, dither=Image.Dither.NONE).save(f, optimize=True)
PY
