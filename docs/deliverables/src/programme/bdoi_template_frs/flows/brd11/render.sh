#!/usr/bin/env bash
# Renders the User Access Maintenance process-flow pictures of the FRS (section 1.8 and Annex N) next to this file:
#   bash render.sh simple 1600,1150 1600,1000
#   bash render.sh e2e 1800,1450 1800,1290
# Arguments: picture (spec simple.py / e2e.py), browser window size, page size (CSS pixels). The page is written by
# gen.py, shot by headless Chromium at twice the size and cropped to the page. CHROME overrides the browser path.
set -euo pipefail
cd "$(dirname "$0")"
CHROME="${CHROME:-/opt/pw-browsers/chromium-1194/chrome-linux/chrome}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
python3 gen.py "$1.py" "$TMP/$1.html"
"$CHROME" --headless --no-sandbox --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
  --window-size="$2" --screenshot="$TMP/$1_raw.png" "file://$TMP/$1.html" >/dev/null 2>&1
python3 -c "import sys;from PIL import Image;W,H=map(int,sys.argv[3].split(','));Image.open(sys.argv[1]).crop((0,0,2*W,2*H)).save(sys.argv[2],optimize=True)" \
  "$TMP/$1_raw.png" "$1.png" "$3"
ls -la "$1.png" | awk '{print $5}'
