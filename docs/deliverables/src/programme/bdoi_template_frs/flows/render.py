"""Renders the swimlane process flows of the BDOI-template FRS to PNG (Chromium headless, device scale 2).

    python render.py <folder> [name ...]   # e.g. brd03; default: every *_spec.py of the folder; writes <name>.png

Each FRS keeps its specs and PNGs in a sub-folder (brd02, brd03). The builders call render_png(folder, name); when
Chromium is not installed the PNG committed next to the spec is used.
"""
from __future__ import annotations

import importlib.util
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[5]
LOGO = REPO / "frontend" / "src" / "assets" / "platform" / "iorta-technxt.png"
CHROME = [Path("/opt/pw-browsers/chromium-1194/chrome-linux/chrome"), Path(shutil.which("chromium") or "/nonexistent"),
          Path(shutil.which("google-chrome") or "/nonexistent")]
sys.path.insert(0, str(HERE))
import gen  # noqa: E402


def _spec(folder: str, name: str) -> dict:
    sp = importlib.util.spec_from_file_location(name, HERE / folder / f"{name}_spec.py")
    m = importlib.util.module_from_spec(sp)
    sp.loader.exec_module(m)
    return m.SPEC


def render_png(folder: str, name: str) -> Path:
    """The PNG of flow <folder>/<name> (rendered again when Chromium is available)."""
    out = HERE / folder / f"{name}.png"
    chrome = next((c for c in CHROME if c.exists()), None)
    if chrome is None:
        if not out.exists():
            raise SystemExit(f"{out.name} missing and no Chromium to render it")
        return out
    from PIL import Image
    spec = _spec(folder, name)
    w, h = spec["size"]
    work = Path(tempfile.mkdtemp(prefix="flow_"))
    try:
        shutil.copy(LOGO, work / "logo.png")
        (work / "page.html").write_text(gen.page(spec), encoding="utf-8")
        raw = work / "raw.png"
        subprocess.run(["bash", str(HERE / "shot.sh"), str(chrome), f"{w},{h + 150}", str(raw),
                        (work / "page.html").as_uri()], check=True, capture_output=True, timeout=180)
        Image.open(raw).crop((0, 0, 2 * w, 2 * h)).convert("RGB").save(out, optimize=True)
    finally:
        shutil.rmtree(work, ignore_errors=True)
    return out


if __name__ == "__main__":
    folder = sys.argv[1]
    names = sys.argv[2:] or sorted(p.name[:-8] for p in (HERE / folder).glob("*_spec.py"))
    for n in names:
        print(render_png(folder, n))
