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
    Excel or PowerPoint file;
  * development-status wording or an internal engineering reference in the text of a client document in out/ (Word
    body, tables, headers and footers; Excel cells, sheet names, headers and footers; PowerPoint slides, tables and
    notes): built / as built / not built, designed, build waves and steps, work in progress, defects and known issues,
    automated tests and test class names, code, file and API paths, Flyway versions and internal engineering codes.
    Until BDOI's business users sign off each FRS, a client document presents the proposed system only (client
    instruction of 27-Sep-2026). Matching is whole-word and case-insensitive; business words such as "building",
    "built-in" or the design of a product do not match.

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
# Development-status wording and internal engineering references (client instruction of 27-Sep-2026). Each entry is
# (label, pattern); patterns are whole-word and case-insensitive unless they carry their own flags.
BUILD_STATUS: list[tuple[str, re.Pattern]] = [(label, re.compile(pat, re.I)) for label, pat in [
    ("built", r"\b(?:as[- ]|not[- ]|un|re)?built\b(?!-in)"),
    ("designed", r"\b(?:as[- ]|not[- ])?designed\b"),
    # "Rebuild from rule" and "Build Comparative" are button labels of the screens.
    ("build", r"\b(?:re)?builds?\b(?!-up)(?! from (?:the )?rule)(?<!\bBuild(?= Comparative))(?! Comparative\b)"),
    ("build wave or step", r"\b(?:build|delivery)[- ](?:wave|step|phase)s?\b|\bwaves? [0-9]\b"),
    ("internal design document", r"\b(?:build|solution|module) designs?\b|(?-i:\b[A-Z_]+_DESIGN\b)"),
    ("work in progress", r"\bwork[- ]in[- ]progress\b|\bWIP\b"),
    ("defect or known issue", r"\bdefects?\b|\bknown (?:issues?|gaps?|limitations?)\b|\bbugs?\b"),
    ("automated test", r"\b(?:automated|automation|unit|end-to-end|e2e) tests?\b|"
                       r"\btest automation\b|\bautomated by\b|\bautomation references?\b|\bjunit\b|\bplaywright\b|"
                       r"\bselenium\b|\bcypress\b|\bvitest\b|\bmockito\b"),
    ("test class name", r"(?-i:\b[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)*(?:Test|Tests|IT|Spec)\b)"),
    ("code or file path", r"\b[\w.-]+\.(?:java|kt|tsx?|jsx?|py|sql|md|ya?ml|properties|xml|json|dot|sh)\b|"
                          r"(?:^|[\s(`'\"])(?:\.{0,2}/)?(?:src|docs|tools|backend|frontend|deploy|main|test)/[\w./-]+"),
    ("source code", r"\b(?:source code|code ?base|in the code|from the code|the code (?:reads|returns|checks))\b|"
                    r"\b(?:source|code|git) repository\b|\brepository URLs?\b|\b(?:deployed|git) commits?\b|\bcommit (?:hash|id)\b|"
                    r"\bpull requests?\b"),
    ("API path", r"/api/|\b(?:GET|POST|PUT|PATCH|DELETE) (?:\.\.\.)?/|(?:^|[\s(])(?:\.\.\.)?/[a-z][\w-]*(?:/[\w{}.-]+)+|/\{\w+\}|\bendpoints?\b|\bHTTP [1-5][0-9]{2}\b|\bREST API\b"),
    ("Flyway version", r"\bflyway\b|(?-i:\bV[0-9]{3,4}(?:__\w+)?\b)"),
    ("internal code", r"(?-i:\b[a-z]{2,}(?:_[a-z0-9]+)+\b|\b[A-Z][a-z]+(?:[A-Z][a-z0-9]+)+(?:Service|Controller|"
                      r"Repository|Entity|Dto|DTO|Mapper|Job|Listener|Handler|Config|Page|Inbox|Client|Adapter|Port|"
                      r"Gateway)\b|\bST[0-9]+\b|\b[a-z]+(?:-[a-z]+)*-cron\b|\b(?:com\.iortatechnxt|brokerverse)\.[\w.-]+)|"
                      r"\bstub(?:s|bed)?\b|\bmocked\b|\b(?:parked )?seams?\b|\badapters?\b|\bidempotent\b"),
    ("development status", r"\bnot (?:yet )?(?:implemented|developed|coded|wired)\b|\bimplemented (?:in|by) the (?:code|build)\b|"
                           r"\bin development\b|\bunder development\b|\bto be (?:built|developed|coded)\b|"
                           r"\bsprints?\b|\bjira\b|\bbacklog item\b|"
                           r"\bgap(?:s)? to (?:build|close)\b"),
]]
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


# Parts of an Office file that hold visible text: Word body, headers, footers, foot- and endnotes, comments; Excel
# shared and inline strings, sheet names, headers and footers, comments; PowerPoint slides and notes.
_TEXT_PARTS = re.compile(r"^(?:word/(?:document|header\d*|footer\d*|footnotes|endnotes|comments)\.xml|"
                         r"xl/(?:sharedStrings|workbook|worksheets/sheet\d+|comments\d*)\.xml|"
                         r"ppt/(?:slides/slide\d+|notesSlides/notesSlide\d+)\.xml|docProps/core\.xml)$")
# Paragraph-like elements: Word and PowerPoint paragraphs, Excel string items and cells, header/footer strings.
_BLOCK = re.compile(r"<(?:w:p|a:p|si|c|oddHeader|oddFooter|evenHeader|evenFooter|firstHeader|firstFooter|"
                    r"dc:title|dc:subject|cp:keywords|dc:description)[\s>/]")
_TEXT_RUN = re.compile(r"<(w:t|a:t|t|oddHeader|oddFooter|evenHeader|evenFooter|firstHeader|firstFooter|dc:title|"
                       r"dc:subject|cp:keywords|dc:description)(?:\s[^>]*)?>([^<]*)</\1>")
_SHEET_NAME = re.compile(r"<sheet [^>]*name=\"([^\"]*)\"")


def office_lines(p: Path) -> list[str]:
    """The visible text of a Word, Excel or PowerPoint file, one entry per paragraph, cell or string: Word body
    paragraphs and tables (nested ones too), headers, footers and notes; Excel cells, sheet names, headers and footers;
    PowerPoint shapes, tables and speaker notes. Runs of a paragraph are joined, so a word split across runs is still
    found."""
    import html  # noqa: PLC0415
    out: list[str] = []
    try:
        with zipfile.ZipFile(p) as z:
            for name in z.namelist():
                if not _TEXT_PARTS.match(name):
                    continue
                xml = z.read(name).decode("utf-8", "ignore")
                if name == "xl/workbook.xml":
                    out.extend(html.unescape(n) for n in _SHEET_NAME.findall(xml))
                    continue
                for block in _BLOCK.split(xml):
                    text = "".join(m.group(2) for m in _TEXT_RUN.finditer(block))
                    if text.strip():
                        out.append(html.unescape(text))
    except zipfile.BadZipFile:
        return []
    return out


def extract_field_names() -> set[str]:
    """The field names of the BRD-13 extract layouts (dm_layouts.yaml). They are the agreed interface of the files
    BDOI extracts from its legacy systems (the header row of each template), so they are business names, not
    internal codes, even though they are written in lower case with underscores."""
    import yaml  # noqa: PLC0415
    path = brand.SRC_DIR / "BRD-13_Data_Migration" / "migration" / "dm_layouts.yaml"
    if not path.exists():
        return set()
    names: set[str] = set()
    for layout in (yaml.safe_load(path.read_text(encoding="utf-8")).get("layouts") or {}).values():
        for key in ("key",):
            names.update(str(k) for k in layout.get(key) or [])
        for line in str(layout.get("fields") or "").splitlines():
            if line.strip():
                names.add(line.split("|", 1)[0].strip())
    return names


def build_status() -> list[str]:
    """Development-status wording or internal references in the text of the client documents in out/."""
    out: list[str] = []
    allowed = extract_field_names()
    for p in files(brand.OUT_DIR):
        if p.suffix not in OFFICE_SUFFIXES:
            continue
        hits: dict[str, set[str]] = defaultdict(set)
        for line in office_lines(p):
            for label, pattern in BUILD_STATUS:
                for m in pattern.finditer(line):
                    if label == "internal code" and m.group(0) in allowed:
                        continue
                    hits[label].add(m.group(0).strip())
        if hits:
            detail = "; ".join(f"{label}: {', '.join(sorted(words)[:6])}" for label, words in sorted(hits.items()))
            out.append(f"build-status wording in {rel(p)} ({detail})")
    return out


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
    errors += set_errors + restricted_words() + build_status()
    for w in warnings:
        print(f"warning: {w}")
    for e in errors:
        print(f"error: {e}")
    print(f"check_pack: {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
