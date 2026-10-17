#!/usr/bin/env bash
# Renders the "Processes at a Glance" panel picture next to this file: bash render_panels.sh [W,H]
# The page is written by panels_gen.py from panels.py, shot by headless Chromium at device scale 2 and cropped.
set -euo pipefail
cd "$(dirname "$0")"
SIZE="${1:-1680,1240}"
W="${SIZE%,*}"; H="${SIZE#*,}"
CHROME="${CHROME:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
python3 panels_gen.py panels.py "$TMP/p.html"
"$CHROME" --headless --no-sandbox --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
  --window-size="$W,$((H + 150))" --screenshot="$TMP/raw.png" "file://$TMP/p.html" >/dev/null 2>&1
python3 -c "import sys;from PIL import Image;W,H=map(int,sys.argv[3].split(','));Image.open(sys.argv[1]).crop((0,0,2*W,2*H)).convert('RGB').save(sys.argv[2],optimize=True)" \
  "$TMP/raw.png" panels.png "$SIZE"
