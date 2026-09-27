"""Checks the client pack for duplicates, stale versions, incomplete release sets and restricted words.

    python tools/deliverables/check_pack.py          # exit code 1 when an error is found

Errors:
  * the same file content (SHA-256) twice in docs/deliverables or docs/design/screenshots: every image and document
    lives in one place (the pack screenshots in the source folder of their BRD, the app-wide screen catalogue in
    docs/design/screenshots, nothing copied between them);
  * an older version of a document next to a newer one in the same folder of out/ (only current documents remain);
  * a BRD folder of an issued sign-off release set (brand.SIGNOFF_SETS) without one of the standard files 00 to 05
    (brand.READING_ORDER); for the other BRDs a missing standard file is only a warning until their set is issued;
  * a restricted word in a source of docs/deliverables/src, in a README of out/, or in the text of a generated Word,
    Excel or PowerPoint file.

Working files that are never committed are skipped: print copies (`_print/`), page previews (`_previews/`), Python
caches and Office lock files.
"""

from __future__ import annotations

import codecs
import hashlib
import re
import sys
import zipfile
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand  # noqa: E402

ROOTS = [brand.DELIVERABLES, brand.REPO_ROOT / "docs" / "design" / "screenshots"]
SKIP_PARTS = {"_print", "_previews", "__pycache__", ".pytest_cache"}
TEXT_SUFFIXES = {".md", ".yaml", ".yml", ".py", ".dot", ".txt", ".csv"}
OFFICE_SUFFIXES = {".docx", ".xlsx", ".pptx"}
# The words are kept encoded so that this file does not contain them itself.
RESTRICTED = re.compile(r"\b(" + codecs.decode(
    "qrzb|qhzzl|snxr|fnzcyr qngn|cebgbglcr|fnaqobk|yberz vcfhz|gbqb|svkzr|pynhqr|naguebcvp|pungtcg|bcranv|"
    "pbcvybg|tvguho", "rot13") + r")\b", re.I)
NAME_RE = re.compile(r"(?:(?P<order>\d\d)_)?BIBS_(?P<type>[A-Za-z_]+?)_(?P<brd>BRD-\d\d)_(?P<name>.+?)"
                     r"_v(?P<ver>\d+(?:\.\d+)*)\.(?P<ext>\w+)$")


def files(root: Path):
    for p in sorted(root.rglob("*")):
        if p.is_file() and not SKIP_PARTS.intersection(p.parts) and not p.name.startswith(("~$", ".")):
            yield p


def rel(p: Path) -> str:
    return p.relative_to(brand.REPO_ROOT).as_posix()


def duplicates() -> list[str]:
    seen: dict[str, list[Path]] = defaultdict(list)
    for root in ROOTS:
        for p in files(root):
            seen[hashlib.sha256(p.read_bytes()).hexdigest()].append(p)
    return [f"duplicate content: {', '.join(rel(p) for p in ps)}" for ps in seen.values() if len(ps) > 1]


def _version(v: str) -> tuple[int, ...]:
    return tuple(int(x) for x in v.split("."))


def stale_versions() -> list[str]:
    out: list[str] = []
    groups: dict[tuple, list[tuple[str, Path]]] = defaultdict(list)
    for p in files(brand.OUT_DIR):
        m = NAME_RE.match(p.name)
        if m:
            groups[(p.parent, m["type"], m["brd"], m["name"], m["ext"])].append((m["ver"], p))
    for key, versions in groups.items():
        if len(versions) > 1:
            versions.sort(key=lambda x: _version(x[0]))
            newest = versions[-1]
            for ver, p in versions[:-1]:
                out.append(f"older version {ver} next to {newest[0]}: {rel(p)}")
    return out


def release_sets() -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    for brd, name in brand.BRD_NAMES.items():
        folder = brand.out_dir(brd, "FRS")
        present = {NAME_RE.match(p.name)["order"] for p in folder.glob("*") if NAME_RE.match(p.name)}
        missing = [f"{order} {kind}" for kind, order in brand.READING_ORDER.items() if order not in present]
        if not missing:
            continue
        if brd in brand.SIGNOFF_SETS:
            errors.append(f"{brd} {name}: standard file(s) missing in {rel(folder)}: {', '.join(missing)}")
        else:
            warnings.append(f"{brd} {name}: sign-off set not yet issued ({len(missing)} of the standard files 00-05 "
                            "to come)")
    return errors, warnings


def office_text(p: Path) -> str:
    try:
        with zipfile.ZipFile(p) as z:
            parts = [z.read(n).decode("utf-8", "ignore") for n in z.namelist() if n.endswith(".xml")]
    except zipfile.BadZipFile:
        return ""
    return re.sub(r"<[^>]+>", " ", " ".join(parts))


def restricted_words() -> list[str]:
    out: list[str] = []
    candidates = list(files(brand.DELIVERABLES / "src")) + list(files(brand.OUT_DIR))
    for p in candidates:
        if p.suffix in TEXT_SUFFIXES:
            text = p.read_text(encoding="utf-8", errors="ignore")
        elif p.suffix in OFFICE_SUFFIXES:
            text = office_text(p)
        else:
            continue
        hits = sorted({m.group(0).lower() for m in RESTRICTED.finditer(text)})
        if hits:
            out.append(f"restricted word(s) {', '.join(hits)} in {rel(p)}")
    return out


def main() -> int:
    errors = duplicates() + stale_versions()
    set_errors, warnings = release_sets()
    errors += set_errors + restricted_words()
    for w in warnings:
        print(f"warning: {w}")
    for e in errors:
        print(f"error: {e}")
    print(f"check_pack: {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
