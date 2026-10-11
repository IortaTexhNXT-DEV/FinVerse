"""Checks the client pack for duplicates, stale versions, incomplete release sets and restricted words.

    python tools/deliverables/check_pack.py          # exit code 1 when an error is found

Errors:
  * the same file content (SHA-256) twice in docs/deliverables or docs/design/screenshots: every image and document
    lives in one place (the pack screenshots in the source folder of their BRD, the app-wide screen catalogue in
    docs/design/screenshots, nothing copied between them);
  * an older version of a document next to a newer one in the same folder of out/ (only current documents remain);
  * a BRD folder of an issued sign-off release set (brand.SIGNOFF_SETS) without one of the standard files 00 to 05
    (brand.READING_ORDER); for the other BRDs a missing standard file is only a warning until their set is issued;
  * a BRD retired by an FRS in BDOI's format (brand.BDOI_FRS_SETS) whose old-format set folder is still in out/, or
    whose BDOI-format FRS is missing from Programme/BDOI_Template_FRS (the only documents of such a BRD);
  * a set of brand.UX_SETS without its UX screen documents 07 to 09 (brand.READING_ORDER_UX: deck, register, image
    package); for the other issued sets they are a warning until the set is re-issued;
  * a drop-level set (brand.DROP_SETS, for example the Drop 0 closure set) without one of its files, or with a file
    that is not one of them in the version of the set;
  * a restricted word in a source of docs/deliverables/src, in a README of out/, or in the text of a generated Word,
    Excel or PowerPoint file;
  * development-status wording or an internal engineering reference in the text of a client document in out/ (Word
    body, tables, headers and footers; Excel cells, sheet names, headers and footers; PowerPoint slides, tables and
    notes): built / as built / not built, designed, build waves and steps, work in progress, defects and known issues,
    automated tests and test class names, code, file and API paths, Flyway versions and internal engineering codes;
    a word is accepted in one folder only when it is on BUILD_STATUS_ALLOWED with its reason (the name of the backlog
    tool in the user story backlog of out/Programme/Backlog).
    Until BDOI's business users sign off each FRS, a client document presents the proposed system only (client
    instruction of 27-Sep-2026). Matching is whole-word and case-insensitive; business words such as "building",
    "built-in" or the design of a product do not match;
  * a technical term in a client document of a business sign-off set (client decision of 28-Sep-2026: the sets hold
    business content only; technical content goes into the Technical Specification of the set, reviewed by BDOI IT):
    APIs and endpoints, JSON, SQL, databases, schemas, table names and the data model, payloads, Flyway and migration
    scripts, http addresses and /api/ paths, file-transfer protocols. Checked in the Word, Excel and PowerPoint files
    of the issued sign-off sets and of the drop-level sets in out/, and in the sources of the client documents of
    every BRD in src/ (FRS, test plan, Start Here, handbook, test cases, pack data, drop closure sources), so that the
    next sets start clean. A phrase is accepted only when it is on the allow-list TECHNICAL_ALLOWED with its reason (a
    platform text quoted word for word whose wording fix is requested, or the name of a BDO system). The other
    documents of out/ (sets not yet re-issued, programme documents) are reported as warnings: they are rebuilt from
    the checked sources when their set is issued, and the programme alignment pack is addressed to BDOI IT.

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
    ("build wave or step", r"\b(?:build|delivery)[- ](?:wave|step|phase)s?\b|\bwaves? [0-9]\b|\bwaves? (?-i:[A-Z]{1,4}[0-9])|"
                           r"(?-i:\bDM[0-3](?:-[A-C])?\b|\b(?:A1-(?:GL|FRBS|OPSX|DSB|PRQ)|C1-[ABC]|CL1-[AB])\b)"),
    ("internal design document", r"\b(?:build|solution|module) designs?\b|(?-i:\b[A-Z_]+_DESIGN\b)"),
    ("work in progress", r"\bwork[- ]in[- ]progress\b|\bWIP\b"),
    ("defect or known issue", r"\bdefects?\b|\bknown (?:issues?|gaps?|limitations?)\b|\bbugs?\b"),
    ("automated test", r"\b(?:automated|automation|unit|end-to-end|e2e) tests?\b|"
                       r"\btest automation\b|\bautomated by\b|\bautomation references?\b|\bjunit\b|\bplaywright\b|"
                       r"\bselenium\b|\bcypress\b|\bvitest\b|\bmockito\b"),
    ("test class name", r"(?-i:\b[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)*(?:Test|Tests|IT|Spec)\b)"),
    ("code or file path", r"\b[\w.-]+\.(?:java|kt|tsx?|jsx?|py|sql|md|ya?ml|properties|xml|json|dot|sh)\b|"
                          r"(?:^|[\s(`'\"])(?:\.{0,2}/)?(?:src|docs|tools|backend|frontend|deploy|main|test)/[\w./-]+"),
    ("source code", r"\b(?:source code|code ?base|in the code(?! maps?\b| lists?\b| of\b)|from the code(?! maps?\b| lists?\b| of\b)|the code (?:reads|returns|checks))\b|"
                    r"\b(?:source|code|git) repository\b|\brepository URLs?\b|\b(?:deployed|git) commits?\b|\bcommit (?:hash|id)\b|"
                    r"\bpull requests?\b"),
    ("API path", r"/api/|\b(?:GET|POST|PUT|PATCH|DELETE) (?:\.\.\.)?/|(?:^|[\s(])(?:\.\.\.)?/[a-z][\w-]*(?:/[\w{}.-]+)+|/\{\w+\}|\bendpoints?\b|\bHTTP [1-5][0-9]{2}\b|\bREST API\b"),
    ("Flyway version", r"\bflyway\b|(?-i:\bV[0-9]{3,4}(?:__\w+)?\b)"),
    ("internal code", r"(?-i:\b[a-z]{2,}(?:_[a-z0-9]+)+\b|\b[A-Z][a-z]+(?:[A-Z][a-z0-9]+)+(?:Service|Controller|"
                      r"Repository|Entity|Dto|DTO|Mapper|Job|Listener|Handler|Config|Page|Inbox|Client|Adapter|Port|"
                      r"Gateway)\b|\b[A-Z][a-z]+(?:[A-Z][a-z]+){2,}\b|\b[A-Z][a-z]+(?:[A-Z][a-z]+)*(?:Booked|Posted|Changed|Created|"
                      r"Updated|Deleted|Completed|Requested|Raised)\b|\bST[0-9]+\b|\b[a-z]+(?:-[a-z]+)*-cron\b|(?<![.@/])\b(?:com\.iortatechnxt|brokerverse)\.[\w.-]+)|"
                      r"\bstub(?:s|bed)?\b|\bmocked\b|\b(?:parked )?seams?\b|\badapters?\b|\bidempotent\b"),
    ("development status", r"\bnot (?:yet )?(?:implemented|developed|coded|wired)\b|\bimplemented (?:in|by) the (?:code|build)\b|"
                           r"\bin development\b|\bunder development\b|\bto be (?:built|developed|coded)\b|"
                           r"\bsprints?\b|\bjira\b|\bbacklog item\b|"
                           r"\bgap(?:s)? to (?:build|close)\b"),
]]
# Words of BUILD_STATUS a folder of out/ may contain, each with its reason: (folder, label, pattern of the word,
# or of the text up to and including the word when the pattern names its context).
BUILD_STATUS_ALLOWED: list[tuple[Path, str, re.Pattern, str]] = [
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "Backlog", "development status", re.compile(r"^jira$", re.I),
     "The user story backlog and its import guide name the tool BDOI chose for the working backlog (decision of the "
     "BIBS Product Owner of 9-Oct-2026); the other development-status words, sprint among them, stay refused there"),
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "Comparisons", "built", re.compile(r"\bYear Built$"),
     "The comparison of BDOI's Product Maintenance FRS quotes BDOI's vessel and aircraft field 'Year Built' (Annex B, C "
     "and the quotation slip templates) word for word; every other form of the word stays refused there"),
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "BDOI_Template_FRS", "built", re.compile(r"\bYear Built$"),
     "BDOI's Product Maintenance FRS v1.1 keeps BDOI's own Annex B and C, whose vessel and aircraft field is 'Year "
     "Built'; every other form of the word stays refused there"),
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "BDOI_Template_FRS", "built",
     re.compile(r"\bBilling Built$"),
     "BDOI's Renewal FRS names its CLPC billing files \"<Product Line>_CLPC Billing Built in_MMDDYYYY.csv\" "
     "(FRRN.027) word for word; every other form of the word stays refused there"),
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "BDOI_Template_FRS", "build",
     re.compile(r"subject to further design, build$"),
     "BDOI's Signoff Sheet (template D051) is kept word for word: the Application Owner confirms the requirements "
     "'subject to further design, build and test activities'; every other use of the word stays refused there"),
    (brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "BDOI_Template_FRS", "API path", re.compile(r"^/LIM/FIRE$"),
     "BDOI's Operations Cashiering FRS v3.1 keeps BDOI's Trade payment file sample word for word, whose Remarks read "
     "'(part of 900,000 /LIM/FIRE)'; every other path stays refused there"),
]
# Technical terms that a business sign-off set does not contain (client decision of 28-Sep-2026). (label, pattern);
# whole-word and case-insensitive.
TECHNICAL: list[tuple[str, re.Pattern]] = [(label, re.compile(pat, re.I)) for label, pat in [
    ("API or endpoint", r"\bAPIs?\b|\bend-?points?\b|/api/"),
    ("JSON", r"\bJSONB?\b"),
    ("SQL", r"\b(?:No)?SQL\b"),
    ("database", r"\bdatabases?\b|\bDBAs?\b"),
    ("schema or data model", r"\bschemas?\b|\bdata model\b|\btable names?\b|\bcolumn names? of the (?:database|data model|tables?)\b"),
    ("payload", r"\bpayloads?\b"),
    ("Flyway or migration script", r"\bflyway\b|\bmigration scripts?\b"),
    ("http address", r"\bhttps?\b|://"),
    ("file-transfer protocol", r"\bS?FTPS?\b"),
]]
# Phrases a client document may contain although they hold a technical term, each with its reason.
TECHNICAL_ALLOWED: list[tuple[re.Pattern, str]] = [(re.compile(pat), why) for pat, why in [
    (r"Negative List Database System", "Name of the BDO system NLDS (BRD-10), not a technical term"),
]]
# Sources of the client documents in src/ (per BRD folder and the drop closure folders).
CLIENT_SOURCES = ("FRS_*.md", "TP_*.md", "START_HERE_*.md", "HANDBOOK_*.md", "*_cases.yaml", "pack/*.yaml",
                  "pack/screens/*.yaml")
# Configuration keys of the sources that hold paths of the toolkit, not client text.
_CONFIG_KEY = re.compile(r"^\s*(?:menu_suites|foreign_packs|plugin|source|screens_dir|screenshot_dir|frontend_dirs|"
                         r"test_plan|frs|summary|signoff|output|bulk_screen):")
NAME_RE = re.compile(r"(?:(?P<order>\d\d)_)?BIBS_(?P<type>[A-Za-z_]+?)_(?P<brd>BRD-\d\d)_(?P<name>.+?)"
                     r"_v(?P<ver>\d+(?:\.\d+)*)\.(?P<ext>\w+)$")
# A document of a drop-level set (brand.DROP_SETS): <nn>_BIBS_<Drop-n>_<Name>_v<version>.<ext>.
DROP_NAME_RE = re.compile(r"(?:(?P<order>\d\d)_)?BIBS_(?P<drop>Drop-\d)_(?P<name>.+?)_v(?P<ver>\d+(?:\.\d+)*)\.(?P<ext>\w+)$")


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
    return [f"duplicate content: {', '.join(rel(p) for p in ps)}" for ps in seen.values()
            if len(ps) > 1 and not _menus_of_several_sets(ps)]


def _menus_of_several_sets(paths: list[Path]) -> bool:
    """Identical persona menu screenshots (screenshots/ux-nav-*) of different BRD sets: the same role has
    the same menu, and each sign-off set carries its own images so that it stands alone. Two identical
    menu images within one set stay an error (the set keeps one through ux.yaml navigation.shared)."""
    sets = [p.parent.parent for p in paths if p.parent.name == "screenshots" and p.name.startswith("ux-nav-")]
    return len(sets) == len(paths) and len(set(sets)) == len(sets)


def _version(v: str) -> tuple[int, ...]:
    return tuple(int(x) for x in v.split("."))


def stale_versions() -> list[str]:
    out: list[str] = []
    groups: dict[tuple, list[tuple[str, Path]]] = defaultdict(list)
    for p in files(brand.OUT_DIR):
        m = NAME_RE.match(p.name)
        if m:
            groups[(p.parent, m["type"], m["brd"], m["name"], m["ext"])].append((m["ver"], p))
            continue
        m = DROP_NAME_RE.match(p.name)
        if m:
            groups[(p.parent, "Drop", m["drop"], m["name"], m["ext"])].append((m["ver"], p))
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
        if brd in brand.BDOI_FRS_SETS:  # retired set: only the BDOI-format pack remains
            frs = brand.bdoi_frs_dir(brd) / brand.BDOI_FRS_SETS[brd]["frs"]
            if not frs.exists():
                errors.append(f"{brd} {name}: FRS in BDOI's format missing: {rel(frs)}")
            left = [p.name for p in folder.glob("*") if p.is_file() and not p.name.startswith(("~$", "."))]
            if left:
                errors.append(f"{brd} {name}: old-format set retired (brand.BDOI_FRS_SETS) but {rel(folder)} still "
                              f"holds {', '.join(sorted(left)[:4])}")
            continue
        present = {NAME_RE.match(p.name)["order"] for p in folder.glob("*") if NAME_RE.match(p.name)}
        missing = [f"{order} {kind}" for kind, order in brand.READING_ORDER.items() if order not in present]
        ux_missing = [f"{order} {kind} ({ext})" for (kind, ext), order in brand.READING_ORDER_UX.items()
                      if order not in present]
        if brd in brand.SIGNOFF_SETS and ux_missing:
            if brd in brand.UX_SETS:
                errors.append(f"{brd} {name}: UX screen document(s) missing in {rel(folder)}: {', '.join(ux_missing)}")
            else:
                warnings.append(f"{brd} {name}: UX screen documents 07-09 to come with the next issue of the set")
        if not missing:
            continue
        if brd in brand.SIGNOFF_SETS:
            errors.append(f"{brd} {name}: standard file(s) missing in {rel(folder)}: {', '.join(missing)}")
        else:
            warnings.append(f"{brd} {name}: sign-off set not yet issued ({len(missing)} of the standard files 00-05 "
                            "to come)")
    # drop-level sets (the closure set of a drop): every file of the set in its folder, in the version of the set
    for drop, spec in brand.DROP_SETS.items():
        folder = brand.drop_set_dir(drop)
        for order, (name, ext, _) in spec["files"].items():
            file = folder / brand.drop_output_name(drop, name, str(spec["version"]), ext)
            if not file.exists():
                errors.append(f"{drop} closure set: {order} {name} missing in {rel(folder)} ({file.name})")
        for p in folder.glob("*"):
            if p.is_file() and not p.name.startswith(("~$", ".")) and p.suffix in OFFICE_SUFFIXES:
                m = DROP_NAME_RE.match(p.name)
                if not m or m["ver"] != str(spec["version"]) or m["order"] not in spec["files"]:
                    errors.append(f"{drop} closure set: {rel(p)} is not a file of the set v{spec['version']}")
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


def package_lines(p: Path) -> list[str]:
    """The text of the CSV and text files inside a ZIP of a set (the register of the 09 image package), one entry per
    line; the images are not read."""
    out: list[str] = []
    try:
        with zipfile.ZipFile(p) as z:
            for name in z.namelist():
                if name.lower().endswith((".csv", ".txt")):
                    out.extend(z.read(name).decode("utf-8-sig", "ignore").splitlines())
    except zipfile.BadZipFile:
        return []
    return out


def document_lines(p: Path) -> list[str]:
    """The visible text of a client document of out/: an Office file, or the text files of a ZIP package."""
    return package_lines(p) if p.suffix == ".zip" else office_lines(p)


def extract_field_names() -> set[str]:
    """The column names of the BRD-13 load templates, the control file and the code map files (read by
    build_dm_pack.py from the layouts of the Migration Console). They are the agreed interface of the files BDOI
    extracts from its legacy systems (the header row of each template), so they are business names, not internal
    codes, even though they are written in lower case with underscores."""
    import importlib.util  # noqa: PLC0415
    path = brand.SRC_DIR / "BRD-13_Data_Migration" / "build_dm_pack.py"
    if not path.exists():
        return set()
    if "build_dm_pack" not in sys.modules:
        spec = importlib.util.spec_from_file_location("build_dm_pack", path)
        module = importlib.util.module_from_spec(spec)
        sys.modules["build_dm_pack"] = module
        spec.loader.exec_module(module)
    return set(sys.modules["build_dm_pack"].extract_field_names())


def build_status() -> list[str]:
    """Development-status wording or internal references in the text of the client documents in out/."""
    out: list[str] = []
    allowed = extract_field_names()
    for p in files(brand.OUT_DIR):
        if p.suffix not in OFFICE_SUFFIXES | {".zip"}:
            continue
        hits: dict[str, set[str]] = defaultdict(set)
        for line in document_lines(p):
            for label, pattern in BUILD_STATUS:
                for m in pattern.finditer(line):
                    if label == "internal code" and m.group(0) in allowed:
                        continue
                    if any(label == lab and folder in p.parents
                           and (word.match(m.group(0).strip()) or word.search(line[:m.end()]))
                           for folder, lab, word, _ in BUILD_STATUS_ALLOWED):
                        continue
                    hits[label].add(m.group(0).strip())
        if hits:
            detail = "; ".join(f"{label}: {', '.join(sorted(words)[:6])}" for label, words in sorted(hits.items()))
            out.append(f"build-status wording in {rel(p)} ({detail})")
    return out


def technical_hits(lines: list[str]) -> dict[str, set[str]]:
    """The technical terms of TECHNICAL in the lines, after the phrases of TECHNICAL_ALLOWED are taken out."""
    hits: dict[str, set[str]] = defaultdict(set)
    for line in lines:
        for pattern, _ in TECHNICAL_ALLOWED:
            line = pattern.sub(" ", line)
        for label, pattern in TECHNICAL:
            for m in pattern.finditer(line):
                hits[label].add(m.group(0).strip())
    return hits


def source_lines(p: Path) -> list[str]:
    """The client text of a source: without the comment lines of YAML (and of the front matter of a Word source),
    HTML comments and the configuration keys that hold toolkit paths."""
    text = re.sub(r"<!--.*?-->", " ", p.read_text(encoding="utf-8", errors="ignore"), flags=re.S)
    out: list[str] = []
    in_front = p.suffix == ".md" and text.startswith("---")
    for i, line in enumerate(text.splitlines()):
        if p.suffix == ".md" and in_front and i > 0 and line.strip() == "---":
            in_front = False
            continue
        is_yaml = p.suffix in (".yaml", ".yml") or in_front
        if is_yaml and (line.lstrip().startswith("#") or _CONFIG_KEY.match(line)):
            continue
        out.append(line)
    return out


def client_sources() -> list[Path]:
    out: list[Path] = []
    for folder in sorted(brand.SRC_DIR.glob("BRD-*")) + sorted(brand.SRC_DIR.glob("Drop-*")):
        patterns = CLIENT_SOURCES + ("*.md", "*.yaml") if folder.name.startswith("Drop-") else CLIENT_SOURCES
        for pattern in patterns:
            out += [p for p in sorted(folder.glob(pattern)) if p not in out]
    return out


def technical_terms() -> tuple[list[str], list[str]]:
    """Technical terms in the client documents of the sign-off sets and their sources (errors) and in the other
    documents of out/ (warnings)."""
    errors: list[str] = []
    warnings: list[str] = []
    set_dirs = {brand.out_dir(b, "FRS").resolve() for b in brand.SIGNOFF_SETS}
    set_dirs |= {brand.drop_set_dir(d).resolve() for d in brand.DROP_SETS}

    def detail(hits: dict[str, set[str]]) -> str:
        return "; ".join(f"{label}: {', '.join(sorted(words)[:6])}" for label, words in sorted(hits.items()))

    for p in files(brand.OUT_DIR):
        if p.suffix not in OFFICE_SUFFIXES | {".zip"}:
            continue
        hits = technical_hits(document_lines(p))
        if hits:
            msg = f"technical term in {rel(p)} ({detail(hits)})"
            (errors if p.parent.resolve() in set_dirs else warnings).append(msg)
    for p in client_sources():
        hits = technical_hits(source_lines(p))
        if hits:
            errors.append(f"technical term in the client source {rel(p)} ({detail(hits)})")
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
        elif p.suffix == ".zip":
            text = "\n".join(package_lines(p))
        else:
            continue
        hits = sorted({m.group(0).lower() for m in RESTRICTED.finditer(text)})
        if hits:
            out.append(f"restricted word(s) {', '.join(hits)} in {rel(p)}")
    return out


def main() -> int:
    errors = duplicates() + stale_versions()
    set_errors, warnings = release_sets()
    tech_errors, tech_warnings = technical_terms()
    errors += set_errors + restricted_words() + build_status() + tech_errors
    warnings += tech_warnings
    for w in warnings:
        print(f"warning: {w}")
    for e in errors:
        print(f"error: {e}")
    print(f"check_pack: {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
