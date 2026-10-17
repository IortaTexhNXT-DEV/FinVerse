#!/usr/bin/env bash
# Headless Chromium screenshot used by render.py: shot.sh <chrome> <width,height> <out.png> <page url>
set -euo pipefail
"$1" --headless --no-sandbox --disable-gpu --hide-scrollbars --force-device-scale-factor=2 \
  --window-size="$2" --screenshot="$3" "$4" >/dev/null 2>&1
