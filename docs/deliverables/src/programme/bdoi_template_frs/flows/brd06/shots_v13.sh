#!/usr/bin/env bash
# Full-page screenshots of Renewal v1.3 (UX design and process storyboards), taken on a local seed environment
# (backend with the seed profile on a fresh database, frontend dev server). shots_v13.sh [step-regex]
# Needs SEED_PASSWORD (the password of the SIT/UAT test accounts), BASE, API and PGDATABASE in the environment.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export CHROMIUM="${CHROMIUM:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
export CHROME_ARGS="--no-sandbox --disable-gpu --hide-scrollbars"
export PLAYWRIGHT_MODULE="${PLAYWRIGHT_MODULE:-/opt/node22/lib/node_modules/playwright}"
export OUT_DIR="${OUT_DIR:-$HERE/../../figures/brd06_v13_screens}"
export MANIFEST="$(mktemp)"
trap 'rm -f "$MANIFEST"' EXIT
python3 "$HERE/../../brd06_v13_screens.py" --manifest > "$MANIFEST"
node "$HERE/shots_v13.cjs" "${1:-.}"
# 256-colour palette images (flat screens), as the BRD-11 and BRD-03 sets.
python3 - "$OUT_DIR" <<'PY'
import sys, pathlib
from PIL import Image
for f in pathlib.Path(sys.argv[1]).glob("*.png"):
    im = Image.open(f)
    if im.mode != "P":
        im.convert("RGB").quantize(256, dither=Image.Dither.NONE).save(f, optimize=True)
# A storyboard step that shows exactly a screen state of the UX design uses that image (brd06_v13_pages.SAME_AS).
seen = {f.read_bytes(): f for f in sorted(pathlib.Path(sys.argv[1]).glob("ux-*.png"))}
for f in sorted(pathlib.Path(sys.argv[1]).glob("sb*.png")):
    if f.read_bytes() in seen:
        print("same as", seen[f.read_bytes()].name, ":", f.name)
        f.unlink()
PY
