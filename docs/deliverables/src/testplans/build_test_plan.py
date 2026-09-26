"""Builds the test plan of one BRD (client deliverable 3): an Excel master and a Word summary.

Usage
  python docs/deliverables/src/testplans/build_test_plan.py docs/deliverables/src/testplans/brd03_cases.yaml
  python docs/deliverables/src/testplans/build_test_plan.py brd03_cases.yaml --check      # checks only
  python docs/deliverables/src/testplans/build_test_plan.py brd03_cases.yaml --no-pdf     # skip TOC pages
  python docs/deliverables/src/testplans/build_test_plan.py brd03_cases.yaml --previews   # page PNGs

Inputs (this folder and docs/deliverables/src/frs)
  * brdnn_cases.yaml    the test data of the BRD (schema below);
  * TP_BRDnn_<NAME>.md  the Word summary, in the bdoi_docx source format, with placeholders that
                        this script fills from the YAML (list below);
  * the FRS source(s) named in meta.frs: the FR list, FR titles, BRD IDs and priorities are read
    from their ```fr blocks, so they are never typed twice.

Outputs (docs/deliverables/out/TestPlans)
  * BIBS_TestPlan_BRD-nn_<Name>_v<version>.xlsx          Cover, README, Document Control, Test
    Conditions, Scenarios, Test Cases, Coverage, Test Data, Roles and Access, FRS Findings;
  * BIBS_TestPlan_BRD-nn_<Name>_Summary_v<version>.docx  from the summary source.

YAML schema (brdnn_cases.yaml)
------------------------------
meta:
  brd: BRD-03                      # BRD code
  name: Product Maintenance        # used in the output file names
  code: PM                         # ID prefix: TC-PM-020.1, SC-PM-01, TD-PM-01, AC-PM-01
  frs: FRS_BRD03_PRODUCT_MAINTENANCE.md    # file under src/frs, or a list of files (volumes)
  summary: TP_BRD03_PRODUCT_MAINTENANCE.md # Word summary source in this folder
  version: "1.0"
  date: 25 September 2026
  built: true                      # BRDs 1-5: message codes must exist in backend/src/main and
                                   # automation references must exist; false (BRDs 6-12): codes
                                   # and automation references are refused
personas:                          # role code -> name, the demo user (built BRDs) and an optional
  MKT_AO: {name: Marketing AO, user: ao, short: MKT AO}   # short column label for the access table
screens:                           # optional aliases for menu paths; a case may also give the path
  REQ: Product Maintenance > Package Requests > (request) Package Request page
data:                              # named test data sets
  - {id: TD-PM-01, name: ..., content: ..., source: "Demo seed V997 (PKR-2026-900003)"}
scenarios:                         # business scenarios (persona end-to-end threads)
  - {id: SC-PM-01, persona: MKT_AO, title: ..., pre: ...}
frs:                               # one entry per FR of the FRS, in FRS order
  FR-PM-020:
    scenario: SC-PM-02             # default scenario of the cases
    persona: MKT_AO                # default persona (role code)
    screen: REQ                    # default screen (alias or menu path)
    data: [TD-PM-01]               # default data sets (optional)
    brd: [BRPM.008]                # optional: narrows the BRD IDs of the FR for its cases
    conditions:                    # test conditions: TC-PM-020.1, TC-PM-020.2, ...
      - A complete NEW request is numbered and moves to FOR_MKT_APPROVAL.
    cases:                         # test cases: TC-PM-020.1-01, ... (condition number + sequence)
      - c: 1                       # condition number (1-based) the case tests
        type: Positive             # Positive | Negative | Boundary | Security-access | Workflow
                                   # | Report-output | Upload-download
        title: Submit a complete NEW generic request
        pre: The user is logged in as ao.        # preconditions (text or list)
        data: [TD-PM-01]           # overrides the FR default
        steps: [Open ..., Click ...]
        expected: The request gets a PKR-yyyy-nnnnnn number ...   # text or list
        msg: [Enter the package or programme name, PKG_REQUEST_INCOMPLETE]   # optional; the code
                                   # only for built BRDs; shown as: Message "<text>" (<CODE>)
        auto: PackageRequestProcessIT#aNewPackageRunsFromDraftToReleasedWithTwoRoundsAndAnAdvisory
                                   # optional; <test file name>#<method or test name>; several
                                   # references are separated by "; "
        neg: true                  # optional polarity override (see below)
        persona / screen / brd / priority / scenario     # optional overrides
access:                            # roles-and-access matrix: one row per action and role
  - action: Create a package request
    screen: NEWREQ
    permission: PKG_REQUEST
    allow: [MKT_AO, MKT_TL, TSU]
    deny: [MBS, MANCOM, AUDITOR]
    allowed: The New Package Request button is shown and the draft is saved.   # optional
    denied: The button is not shown; POST /requests returns HTTP 403 (ACCESS_DENIED).
    auto: ProductMaintenanceApiIT#permissionsAreEnforced                      # optional
findings:                          # FRS defects found while writing the cases (for the FRS owner)
  - {fr: FR-PM-002, issue: ..., proposal: ...}

Polarity. Coverage counts a case as positive (the action succeeds) or negative (the system refuses
it). Positive, Workflow, Report-output, Upload-download and Boundary cases are positive, and
Negative and Security-access cases negative, unless the case sets neg: true / false (for example a
Boundary case one unit outside the limit). Every FR needs at least one positive and one negative
case, every condition at least one case, and every BRD ID of the FRS at least one case.

Placeholders in the Word summary (a line on its own): <!-- tp:counts -->, <!-- tp:coverage -->
(per FR), <!-- tp:brd-coverage -->, <!-- tp:scenarios -->, <!-- tp:data -->, <!-- tp:personas -->,
<!-- tp:access -->, <!-- tp:automation -->, <!-- tp:findings -->.
"""

from __future__ import annotations

import argparse
import codecs
import re
import sys
from collections import Counter, OrderedDict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_docx import BdoiDocument, load_source, meta_from, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

FRS_DIR = REPO / "docs" / "deliverables" / "src" / "frs"
OUT = brand.OUT_DIR / "TestPlans"

TYPES = ["Positive", "Negative", "Boundary", "Security-access", "Workflow", "Report-output", "Upload-download"]
NEGATIVE_TYPES = {"Negative", "Security-access"}
PRIORITIES = ["High", "Medium", "Low"]
STATUSES = ["Not run", "Pass", "Fail", "Blocked", "N/A"]
PRIORITY_OF = {"must have": "High", "should have": "Medium", "could have": "Low", "nice to have": "Low"}
CASE_KEYS = {"c", "type", "title", "pre", "data", "steps", "expected", "msg", "auto", "neg", "persona",
             "screen", "brd", "priority", "scenario"}
FR_KEYS = {"scenario", "persona", "screen", "data", "brd", "conditions", "cases"}
FILLER = ("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art",
          "best-in-class", "world-class")
CODE_RE = re.compile(r"^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)+$")


# --------------------------------------------------------------------------- model

@dataclass
class Fr:
    id: str
    title: str
    brd: list[str]
    priority: str
    source: str


@dataclass
class Case:
    id: str
    cond: str
    fr: str
    brd: list[str]
    scenario: str
    title: str
    type: str
    negative: bool
    persona: str
    screen: str
    pre: str
    steps: str
    expected: str
    priority: str
    auto: str
    data: list[str] = field(default_factory=list)
    raw_messages: list[str] = field(default_factory=list)


@dataclass
class Plan:
    meta: dict[str, Any]
    path: Path
    frs: "OrderedDict[str, Fr]"
    personas: dict[str, dict[str, Any]]
    screens: dict[str, str]
    data: list[dict[str, Any]]
    scenarios: list[dict[str, Any]]
    conditions: list[dict[str, Any]] = field(default_factory=list)
    cases: list[Case] = field(default_factory=list)
    access: list[dict[str, Any]] = field(default_factory=list)
    findings: list[dict[str, Any]] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    @property
    def built(self) -> bool:
        return bool(self.meta.get("built"))

    @property
    def all_brd_ids(self) -> list[str]:
        """Every BRD ID traced by the FRS: requirement IDs in number order, then other references."""
        seen: list[str] = []
        for fr in self.frs.values():
            for b in fr.brd:
                if b not in seen:
                    seen.append(b)
        return sorted(seen, key=_id_key)


def _text(value: Any) -> str:
    if value is None:
        return ""
    if isinstance(value, (list, tuple)):
        return "\n".join(str(v) for v in value)
    return str(value).strip()


def brd_ids(ref: str) -> list[str]:
    """'BRPM.008 (p.18-19)' -> ['BRPM.008']; page notes in brackets are dropped and several IDs
    separated by ';' or ',' are split."""
    text = re.sub(r"\([^)]*\)", "", str(ref))
    return [part.strip() for part in re.split(r"[;,]", text) if part.strip()]


def _id_key(b: str) -> tuple:
    m = re.match(r"^([A-Z]+)[.\-]?(\d+)(\w*)$", b)
    return (0, m.group(1), int(m.group(2)), m.group(3)) if m else (1, b, 0, "")


def read_frs(files: list[str]) -> "OrderedDict[str, Fr]":
    frs: OrderedDict[str, Fr] = OrderedDict()
    for name in files:
        text = (FRS_DIR / name).read_text(encoding="utf-8")
        for block in re.findall(r"```(?:fr|requirement)\n(.*?)```", text, re.S):
            fr = yaml.safe_load(block)
            refs = fr.get("brd") or []
            refs = refs if isinstance(refs, list) else [refs]
            ids: list[str] = []
            for r in refs:
                for b in brd_ids(r):
                    if b not in ids:
                        ids.append(b)
            frs[fr["id"]] = Fr(fr["id"], str(fr.get("title", "")), ids, str(fr.get("priority", "")), name)
    return frs


def fr_priority(fr: Fr) -> str:
    p = fr.priority.lower()
    for k, v in PRIORITY_OF.items():
        if k in p:
            return v
    for level in PRIORITIES:  # an FRS that states the level directly, e.g. "High (BRD p.21)"
        if re.match(rf"{level.lower()}\b", p):
            return level
    return "High" if "must" in p else "Medium"


def number_of(fr_id: str) -> str:
    """'FR-PM-020' -> 'PM-020'."""
    return fr_id[3:] if fr_id.startswith("FR-") else fr_id


# --------------------------------------------------------------------------- load and check

def load(path: Path) -> Plan:
    raw = yaml.safe_load(path.read_text(encoding="utf-8"))
    meta = raw["meta"]
    files = meta["frs"] if isinstance(meta["frs"], list) else [meta["frs"]]
    plan = Plan(meta=meta, path=path, frs=read_frs(files), personas=raw.get("personas") or {},
                screens=raw.get("screens") or {}, data=raw.get("data") or [], scenarios=raw.get("scenarios") or [])
    plan.access = raw.get("access") or []
    plan.findings = raw.get("findings") or []
    code = meta["code"]
    data_ids = {d["id"] for d in plan.data}
    scen_ids = {s["id"] for s in plan.scenarios}
    for s in plan.scenarios:
        if s.get("persona") not in plan.personas:
            plan.errors.append(f"{s['id']}: persona {s.get('persona')} is not in personas")
    entries = raw.get("frs") or {}
    for fr_id in entries:
        if fr_id not in plan.frs:
            plan.errors.append(f"{fr_id}: not an FR of {', '.join(files)}")
    for fr_id in plan.frs:
        if fr_id not in entries:
            plan.errors.append(f"{fr_id}: no test conditions or cases")
    for fr_id, entry in entries.items():
        if fr_id not in plan.frs:
            continue
        fr = plan.frs[fr_id]
        entry = entry or {}
        for k in set(entry) - FR_KEYS:
            plan.errors.append(f"{fr_id}: unknown key '{k}'")
        fr_brd = entry.get("brd") or fr.brd
        for b in fr_brd:
            if b not in fr.brd:
                plan.errors.append(f"{fr_id}: BRD ID {b} is not traced to this FR in the FRS")
        conds = entry.get("conditions") or []
        n = number_of(fr_id)
        for i, text in enumerate(conds, start=1):
            if not isinstance(text, str):
                plan.errors.append(f"{fr_id}: condition {i} is not text (quote it): {text}")
            plan.conditions.append({"id": f"TC-{n}.{i}", "fr": fr_id, "brd": fr_brd, "text": _text(text),
                                    "priority": fr_priority(fr), "cases": []})
        cond_rows = plan.conditions[len(plan.conditions) - len(conds):]
        seq: Counter[int] = Counter()
        for k, c in enumerate(entry.get("cases") or [], start=1):
            where = f"{fr_id} case {k}"
            for key in set(c) - CASE_KEYS:
                plan.errors.append(f"{where}: unknown key '{key}'")
            ci = c.get("c")
            if not isinstance(ci, int) or not 1 <= ci <= len(conds):
                plan.errors.append(f"{where}: 'c' must be a condition number 1..{len(conds)}")
                continue
            seq[ci] += 1
            cid = f"TC-{n}.{ci}-{seq[ci]:02d}"
            ctype = c.get("type")
            if ctype not in TYPES:
                plan.errors.append(f"{cid}: type '{ctype}' is not one of {', '.join(TYPES)}")
            negative = bool(c["neg"]) if "neg" in c else ctype in NEGATIVE_TYPES
            persona = c.get("persona") or entry.get("persona")
            if persona not in plan.personas:
                plan.errors.append(f"{cid}: persona {persona} is not in personas")
            scenario = c.get("scenario") or entry.get("scenario")
            if scenario not in scen_ids:
                plan.errors.append(f"{cid}: scenario {scenario} is not defined")
            screen = c.get("screen") or entry.get("screen") or ""
            screen = plan.screens.get(screen, screen)
            if not screen:
                plan.errors.append(f"{cid}: no screen")
            data = c.get("data", entry.get("data")) or []
            for d in data:
                if d not in data_ids:
                    plan.errors.append(f"{cid}: data set {d} is not defined")
            brd = c.get("brd") or fr_brd
            for b in brd:
                if b not in fr.brd:
                    plan.errors.append(f"{cid}: BRD ID {b} is not traced to {fr_id}")
            steps = c.get("steps") or []
            if not steps:
                plan.errors.append(f"{cid}: no steps")
            for key in ("title", "pre", "steps", "expected"):
                value = c.get(key)
                items = value if isinstance(value, list) else [value]
                if any(v is not None and not isinstance(v, (str, int, float)) for v in items):
                    plan.errors.append(f"{cid}: {key} has an item read as a mapping (quote text with ': ')")
            for key in ("title", "expected"):
                if not c.get(key):
                    plan.errors.append(f"{cid}: no {key}")
            pre = _text(c.get("pre"))
            if data:
                names = "; ".join(f"{d} {next((x['name'] for x in plan.data if x['id'] == d), '')}" for d in data)
                pre = (pre + "\n" if pre else "") + "Data: " + names
            expected = _text(c.get("expected"))
            msg = c.get("msg")
            if msg:
                if not isinstance(msg, list) or not 1 <= len(msg) <= 2:
                    plan.errors.append(f"{cid}: msg must be [message] or [message, CODE]")
                else:
                    shown = f'Message "{str(msg[0]).replace("{", "").replace("}", "")}"'
                    if len(msg) == 2 and msg[1] not in ("-", None):
                        if not plan.built:
                            plan.errors.append(f"{cid}: message codes are quoted only for built BRDs")
                        shown += f" ({msg[1]})"
                    expected = (expected + "\n" if expected else "") + shown + "."
            auto = _text(c.get("auto"))
            if auto and not plan.built:
                plan.errors.append(f"{cid}: automation references are given only for built BRDs")
            priority = c.get("priority") or fr_priority(fr)
            if priority not in PRIORITIES:
                plan.errors.append(f"{cid}: priority {priority} is not one of {PRIORITIES}")
            case = Case(
                id=cid, cond=cond_rows[ci - 1]["id"], fr=fr_id, brd=list(brd), scenario=scenario or "",
                title=_text(c.get("title")), type=ctype or "", negative=negative, persona=persona or "",
                screen=screen, pre=pre,
                steps="\n".join(f"{i}. {_text(s)}" for i, s in enumerate(steps, start=1)),
                expected=expected, priority=priority, auto=auto, data=list(data),
                raw_messages=[str(msg[0])] if msg else [])
            plan.cases.append(case)
            cond_rows[ci - 1]["cases"].append(cid)
        for row in cond_rows:
            if not row["cases"]:
                plan.errors.append(f"{row['id']}: condition has no test case")
    for i, a in enumerate(plan.access, start=1):
        a["id"] = f"AC-{code}-{i:02d}"
        a["screen"] = plan.screens.get(a.get("screen", ""), a.get("screen", ""))
        for r in list(a.get("allow") or []) + list(a.get("deny") or []):
            if r not in plan.personas:
                plan.errors.append(f"{a['id']}: role {r} is not in personas")
        if a.get("auto") and not plan.built:
            plan.errors.append(f"{a['id']}: automation references are given only for built BRDs")
    _check_coverage(plan)
    _check_text(plan)
    if plan.built:
        _check_codes(plan)
        _check_messages(plan)
        _check_automation(plan)
    return plan


def _check_coverage(plan: Plan) -> None:
    for fr_id in plan.frs:
        cases = [c for c in plan.cases if c.fr == fr_id]
        if cases and not any(not c.negative for c in cases):
            plan.errors.append(f"{fr_id}: no positive case")
        if cases and not any(c.negative for c in cases):
            plan.errors.append(f"{fr_id}: no negative case")
    covered = {b for c in plan.cases for b in c.brd}
    for b in plan.all_brd_ids:
        if b not in covered:
            plan.errors.append(f"{b}: BRD ID not covered by any case")


# Restricted tool and vendor names (writing standard). Stored in ROT13 so the names are not spelled out here.
RESTRICTED_NAMES = re.compile(r"\b(" + codecs.decode('pynhqr|tcg-?\\q|pungtcg|bcranv|naguebcvp|trzvav|yynzn|pbcvybg', "rot13") + r")\b")


def _all_text(plan: Plan) -> list[tuple[str, str]]:
    out = [(c.id, " ".join((c.title, c.pre, c.steps, c.expected))) for c in plan.cases]
    out += [(r["id"], r["text"]) for r in plan.conditions]
    out += [(s["id"], _text(s.get("title")) + " " + _text(s.get("pre"))) for s in plan.scenarios]
    out += [(a["id"], " ".join(_text(a.get(k)) for k in ("action", "allowed", "denied"))) for a in plan.access]
    return out


def _check_text(plan: Plan) -> None:
    for ident, text in _all_text(plan):
        low = text.lower()
        for word in FILLER:
            if re.search(rf"\b[{word[0]}{word[0].upper()}]{re.escape(word[1:])}\b", text):
                plan.errors.append(f"{ident}: filler word '{word}' (writing standard)")
        if RESTRICTED_NAMES.search(low):
            plan.errors.append(f"{ident}: names a restricted tool or vendor")


def _source_text(roots: list[Path], suffixes: tuple[str, ...]) -> str:
    parts = []
    for root in roots:
        for p in root.rglob("*"):
            if p.is_file() and p.suffix in suffixes and "node_modules" not in p.parts:
                parts.append(p.read_text(encoding="utf-8", errors="ignore"))
    return "\n".join(parts)


_SOURCE_CACHE: dict[str, str] = {}


def _main_source() -> str:
    if "main" not in _SOURCE_CACHE:
        _SOURCE_CACHE["main"] = _source_text(
            [REPO / "backend" / "src" / "main", REPO / "frontend" / "src"],
            (".java", ".sql", ".yml", ".yaml", ".properties", ".ts", ".tsx"))
    return _SOURCE_CACHE["main"]


def _normalised_source() -> str:
    if "norm" not in _SOURCE_CACHE:
        src = _main_source()
        src = re.sub(r'"\s*\+\s*"', "", src)          # "a " + "b" -> "a b"
        src = re.sub(r"'\s*\+\s*'", "", src)
        _SOURCE_CACHE["norm"] = re.sub(r"\s+", " ", src)
    return _SOURCE_CACHE["norm"]


def message_fragments(message: str) -> list[str]:
    """The literal parts of a message (3 words or more) between the values filled in at run time.
    Values are {marked} text, <placeholders>, 'quoted' labels, tokens with a digit and upper-case
    codes."""
    text = re.sub(r"\{[^}]*\}|<[^>]*>|'[^']*'", " \x00 ", message)
    fragments, current = [], []
    for token in text.split():
        core = token.strip("\"().,;:")
        variable = (token == "\x00" or any(ch.isdigit() for ch in token)
                    or (len(core) > 1 and core.upper() == core and any(ch.isalpha() for ch in core)))
        if variable:
            if len(current) >= 3:
                fragments.append(" ".join(current))
            current = []
        else:
            current.append(token)
    if len(current) >= 3:
        fragments.append(" ".join(current))
    return [f.rstrip(".").strip() for f in fragments]


def _check_messages(plan: Plan) -> None:
    """Each literal part of each quoted message occurs in backend/src/main or frontend/src."""
    src = _normalised_source()
    for c in plan.cases:
        for message in c.raw_messages:
            for fragment in message_fragments(message):
                if fragment and fragment not in src:
                    plan.errors.append(f'{c.id}: message text not found in the code: "{fragment}"')


def _check_codes(plan: Plan) -> None:
    """Every code quoted in a message (the text in brackets after 'Message "...") exists in the code."""
    src = _main_source()
    for c in plan.cases:
        for code in re.findall(r'Message "[^"]*" \(([A-Z0-9_]+)\)', c.expected):
            if code not in src:
                plan.errors.append(f"{c.id}: code {code} does not occur in backend/src/main or frontend/src")


def _test_files() -> dict[str, list[Path]]:
    if "tests" not in _SOURCE_CACHE:
        index: dict[str, list[Path]] = {}
        roots = [REPO / "backend" / "src" / "test", REPO / "frontend" / "src"]
        for root in roots:
            for p in root.rglob("*"):
                if p.is_file() and "node_modules" not in p.parts and (
                        p.suffix == ".java" or re.search(r"\.(test|spec)\.tsx?$", p.name)):
                    index.setdefault(p.name, []).append(p)
                    index.setdefault(p.stem, []).append(p)
        _SOURCE_CACHE["tests"] = index  # type: ignore[assignment]
    return _SOURCE_CACHE["tests"]  # type: ignore[return-value]


def _check_automation(plan: Plan) -> None:
    index = _test_files()
    refs = [(c.id, c.auto) for c in plan.cases] + [(a["id"], _text(a.get("auto"))) for a in plan.access]
    for ident, auto in refs:
        for ref in [r.strip() for r in auto.split(";") if r.strip()]:
            cls, _, method = ref.partition("#")
            files = index.get(cls)
            if not files:
                plan.errors.append(f"{ident}: test class {cls} not found under backend/src/test or frontend/src")
                continue
            if method and not any(method in f.read_text(encoding="utf-8", errors="ignore") for f in files):
                plan.errors.append(f"{ident}: {method} not found in {cls}")


# --------------------------------------------------------------------------- statistics

def fr_stats(plan: Plan) -> list[dict[str, Any]]:
    rows = []
    for fr in plan.frs.values():
        cases = [c for c in plan.cases if c.fr == fr.id]
        rows.append({
            "item": fr.id, "kind": "FR", "title": fr.title, "traces": ", ".join(fr.brd),
            "conditions": sum(1 for r in plan.conditions if r["fr"] == fr.id),
            "positive": sum(1 for c in cases if not c.negative),
            "negative": sum(1 for c in cases if c.negative),
            "total": len(cases),
            "automated": sum(1 for c in cases if c.auto),
            "types": Counter(c.type for c in cases),
        })
    for r in rows:
        r["result"] = "Covered" if r["positive"] and r["negative"] else "GAP"
    return rows


def brd_stats(plan: Plan) -> list[dict[str, Any]]:
    rows = []
    for b in plan.all_brd_ids:
        cases = [c for c in plan.cases if b in c.brd]
        frs = [f.id for f in plan.frs.values() if b in f.brd]
        rows.append({
            "item": b, "kind": "BRD ID", "title": "", "traces": ", ".join(frs),
            "conditions": len({c.cond for c in cases}),
            "positive": sum(1 for c in cases if not c.negative),
            "negative": sum(1 for c in cases if c.negative),
            "total": len(cases), "automated": sum(1 for c in cases if c.auto),
        })
        rows[-1]["result"] = "Covered" if cases else "GAP"
    return rows


def totals(plan: Plan) -> dict[str, Any]:
    types = Counter(c.type for c in plan.cases)
    covered_brd = {b for c in plan.cases for b in c.brd}
    return {
        "frs": len(plan.frs),
        "frs_covered": sum(1 for r in fr_stats(plan) if r["result"] == "Covered"),
        "brd_ids": len(plan.all_brd_ids),
        "brd_covered": sum(1 for b in plan.all_brd_ids if b in covered_brd),
        "conditions": len(plan.conditions),
        "scenarios": len(plan.scenarios),
        "cases": len(plan.cases),
        "positive": sum(1 for c in plan.cases if not c.negative),
        "negative": sum(1 for c in plan.cases if c.negative),
        "types": types,
        "automated": sum(1 for c in plan.cases if c.auto),
        "access_rows": sum(len(a.get("allow") or []) + len(a.get("deny") or []) for a in plan.access),
        "data": len(plan.data),
    }


def persona_label(plan: Plan, code: str) -> str:
    p = plan.personas.get(code) or {}
    label = f"{code} – {p.get('name', '')}".strip(" –")
    if p.get("user"):
        label += f" (demo user {p['user']})"
    return label


# --------------------------------------------------------------------------- Excel

def build_xlsx(plan: Plan, control: list[dict[str, Any]]) -> Path:
    m = plan.meta
    brd_label = f"BRD-{int(m['brd'].split('-')[1])}"
    wb = BdoiWorkbook(f"Test Plan {brd_label} {m['name']}", doc_type="Test plan", brd=m["brd"],
                      version=str(m["version"]), date=str(m["date"]),
                      subtitle=f"Test conditions, scenarios and test cases for {brd_label} {m['name']}")
    t = totals(plan)
    wb.legend = [("Pass", "The case ran and the actual result matches the expected result"),
                 ("Fail", "The actual result differs; a defect ID is recorded"),
                 ("Blocked", "The case cannot run (environment, data or a defect elsewhere)"),
                 ("N/A", "Not applicable in this cycle, with the reason in Actual result")]
    frs = ", ".join(m["frs"]) if isinstance(m["frs"], list) else m["frs"]
    wb.cover_notes = [
        f"Source: FRS {brd_label} ({frs}). {t['frs']} FRs, {t['brd_ids']} BRD IDs, {t['conditions']} test "
        f"conditions, {t['scenarios']} scenarios, {t['cases']} test cases ({t['positive']} positive, "
        f"{t['negative']} negative), {t['automated']} with an automation reference.",
        "Status starts as Not run. Testers fill Status, Actual result, Tester, Date and Defect ID during execution.",
    ]

    wb.sheet("Document Control", [
        Column("version", "Version", 10, "Document version"),
        Column("date", "Date", 14, "Date of the version"),
        Column("author", "Author", 30, "Who prepared the version"),
        Column("reviewer", "Reviewer", 30, "Who reviewed it"),
        Column("approver", "Approver", 30, "Who approved it"),
        Column("change", "Change", 60, "What changed"),
    ], rows=[{k: str(r.get(k, "")) for k in ("version", "date", "author", "reviewer", "approver", "change")}
             for r in control], description="Version history of this test plan (same as the Word summary)",
        freeze_first_column=False)

    wb.sheet("Test Conditions", [
        Column("id", "TC ID", 14, "Test condition ID: TC-<FR number>.<n>"),
        Column("fr", "FR ID", 12, "Functional requirement of the FRS"),
        Column("brd", "BRD ID(s)", 16, "BRD requirement IDs the FR meets"),
        Column("text", "Test condition", 70, "What must be shown to be true"),
        Column("priority", "Priority", 10, "From the BRD priority of the FR (Must have = High)", values=PRIORITIES),
        Column("cases", "Test cases", 22, "IDs of the test cases that test the condition"),
    ], rows=[{**r, "brd": ", ".join(r["brd"]), "cases": ", ".join(r["cases"])} for r in plan.conditions],
        description="One row per test condition, derived from the acceptance criteria, rules and validations of each FR")

    scen_rows = []
    for s in plan.scenarios:
        cases = [c for c in plan.cases if c.scenario == s["id"]]
        conds = list(OrderedDict.fromkeys(c.cond for c in cases))
        scen_rows.append({"id": s["id"], "conds": ", ".join(conds), "persona": persona_label(plan, s["persona"]),
                          "title": _text(s.get("title")), "pre": _text(s.get("pre")), "n": len(cases)})
    wb.sheet("Scenarios", [
        Column("id", "SC ID", 10, "Scenario ID"),
        Column("conds", "TC IDs", 34, "Test conditions exercised by the scenario's cases"),
        Column("persona", "Persona / role", 26, "Main persona: BIBS role code and name"),
        Column("title", "Business scenario", 60, "The business thread the persona follows"),
        Column("pre", "Preconditions", 44, "State and data needed before the scenario starts"),
        Column("n", "Cases", 8, "Number of test cases in the scenario", kind="number"),
    ], rows=scen_rows, description="Business scenarios by persona; each test case belongs to one scenario")

    cases_ws = wb.sheet("Test Cases", [
        Column("id", "TC-case ID", 16, "Test case ID: <condition ID>-<nn>"),
        Column("scenario", "Scenario", 10, "Scenario ID"),
        Column("fr", "FR ID", 11, "Functional requirement"),
        Column("brd", "BRD ID", 12, "BRD requirement ID(s)"),
        Column("title", "Test case", 30, "What the case checks"),
        Column("type", "Type", 13, "Kind of test", values=TYPES),
        Column("persona", "Persona", 22, "Role code and name (and demo user for built BRDs)"),
        Column("screen", "Screen (menu path)", 26, "Where the tester starts"),
        Column("pre", "Preconditions and test data", 36, "State before the first step and the named data set"),
        Column("steps", "Steps", 52, "Numbered steps"),
        Column("expected", "Expected result", 50,
               "What the system must do, with the exact message text and, for built BRDs, its code"),
        Column("priority", "Priority", 9, "High / Medium / Low", values=PRIORITIES),
        Column("auto", "Automation reference", 30,
               "Existing automated test (class#method) that covers the case; blank when none"),
        Column("status", "Status", 11, "Execution status", values=STATUSES, status=True),
        Column("actual", "Actual result", 30, "What happened, filled by the tester"),
        Column("tester", "Tester", 14, "Who ran the case"),
        Column("date", "Date", 12, "Date of the run", kind="date"),
        Column("defect", "Defect ID", 12, "Defect raised when the case fails"),
    ], rows=[{
        "id": c.id, "scenario": c.scenario, "fr": c.fr, "brd": ", ".join(c.brd), "title": c.title,
        "type": c.type, "persona": persona_label(plan, c.persona), "screen": c.screen, "pre": c.pre,
        "steps": c.steps, "expected": c.expected, "priority": c.priority, "auto": c.auto, "status": "Not run",
    } for c in plan.cases], description="Test cases with steps and expected results; execution columns start blank")

    cov = []
    for r in fr_stats(plan) + brd_stats(plan):
        cov.append({**r, "types": ""})
    wb.sheet("Coverage", [
        Column("item", "FR / BRD ID", 13, "FR of the FRS, then BRD requirement ID"),
        Column("kind", "Kind", 8, "FR or BRD ID"),
        Column("title", "Title", 40, "FR title"),
        Column("traces", "Traces", 26, "BRD IDs of the FR, or FRs of the BRD ID"),
        Column("conditions", "Conditions", 11, "Test conditions", kind="number"),
        Column("positive", "Positive", 10, "Cases where the action succeeds", kind="number"),
        Column("negative", "Negative", 10, "Cases where BIBS refuses the action", kind="number"),
        Column("total", "Total", 8, "All cases", kind="number"),
        Column("automated", "Automated", 11, "Cases with an automation reference", kind="number"),
        Column("result", "Coverage", 11, "Covered: at least one positive and one negative case (FR) or one case "
               "(BRD ID); GAP otherwise", values=["Covered", "GAP"], status=True),
    ], rows=cov, description="Every FR and every BRD ID with its count of positive and negative cases")

    data_rows = []
    for d in plan.data:
        used = [c.id for c in plan.cases if d["id"] in c.data]
        data_rows.append({"id": d["id"], "name": d.get("name", ""), "content": _text(d.get("content")),
                          "source": _text(d.get("source")), "n": len(used)})
    wb.sheet("Test Data", [
        Column("id", "Data set", 11, "Data set ID"),
        Column("name", "Name", 28, "Short name used in the cases"),
        Column("content", "Content", 64, "Records and values in the set"),
        Column("source", "Source", 40, "Demo seed that provides it (built BRDs) or how to prepare it; "
               "non-production data is masked"),
        Column("n", "Cases", 8, "Number of cases that use it", kind="number"),
    ], rows=data_rows, description="Named test data sets; UAT uses masked copies of production-like data")

    acc_rows = []
    for a in plan.access:
        k = 0
        for role in a.get("allow") or []:
            k += 1
            acc_rows.append({"id": f"{a['id']}.{k}", "action": a.get("action"), "screen": a["screen"],
                             "permission": a.get("permission", ""), "role": persona_label(plan, role),
                             "access": "Allowed",
                             "expected": _text(a.get("allowed")) or "The action is offered and completes.",
                             "auto": _text(a.get("auto")), "status": "Not run"})
        for role in a.get("deny") or []:
            k += 1
            acc_rows.append({"id": f"{a['id']}.{k}", "action": a.get("action"), "screen": a["screen"],
                             "permission": a.get("permission", ""), "role": persona_label(plan, role),
                             "access": "Denied", "expected": _text(a.get("denied")),
                             "auto": _text(a.get("auto")), "status": "Not run"})
    access_ws = wb.sheet("Roles and Access", [
        Column("id", "Case ID", 11, "Access case ID: AC-<code>-<nn>.<n>"),
        Column("action", "Action", 30, "Screen or action checked"),
        Column("screen", "Screen (menu path)", 28, "Where it is done"),
        Column("permission", "Permission", 22, "BIBS permission that grants it"),
        Column("role", "Role", 28, "Role tested (log in as a user who holds only this role)"),
        Column("access", "Access", 10, "Expected access", values=["Allowed", "Denied"]),
        Column("expected", "Expected result", 50, "What the user sees"),
        Column("auto", "Automation reference", 28, "Existing automated test, if any"),
        Column("status", "Status", 11, "Execution status", values=STATUSES, status=True),
        Column("actual", "Actual result", 28, "Filled by the tester"),
        Column("tester", "Tester", 14, "Who ran the case"),
        Column("date", "Date", 12, "Date of the run", kind="date"),
        Column("defect", "Defect ID", 12, "Defect raised when the case fails"),
    ], rows=acc_rows, description="Who can and cannot see or do each action (one row per action and role)")

    # The two execution sheets are wide: they print on A3 landscape, fitted to the page width.
    for ws in (cases_ws, access_ws):
        ws.page_setup.paperSize = ws.PAPERSIZE_A3

    if plan.findings:
        wb.sheet("FRS Findings", [
            Column("id", "ID", 10, "Finding ID"),
            Column("fr", "FR ID", 12, "FR concerned"),
            Column("issue", "Finding", 70, "Ambiguous or untestable point found while writing the cases"),
            Column("proposal", "Proposed resolution", 60, "What the test team proposes to the FRS owner"),
        ], rows=[{"id": f"TF-{plan.meta['code']}-{i:02d}", **f} for i, f in enumerate(plan.findings, start=1)],
            description="FRS points raised for the FRS owner; the affected cases assume the proposed resolution")

    name = brand.output_name("TestPlan", m["brd"], m["name"], str(m["version"]), "xlsx")
    return wb.save(OUT / name)


# --------------------------------------------------------------------------- Word summary

def _cell(v: Any) -> str:
    return str(v).replace("|", "\\|").replace("\n", " ")


def _table(opts: str, headers: list[str], rows: list[list[Any]]) -> list[str]:
    lines = [f"<!-- table: {opts} -->", "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    lines += ["| " + " | ".join(_cell(v) for v in r) + " |" for r in rows]
    return lines + [""]


def _paired(opts: str, headers: list[str], rows: list[list[Any]], min_rows: int = 40) -> list[str]:
    """A long narrow table set as two halves side by side, so it takes half the pages."""
    if len(rows) < min_rows:
        return _table(opts, headers, rows)
    half = (len(rows) + 1) // 2
    left, right = rows[:half], rows[half:]
    right = right + [[""] * len(headers)] * (half - len(right))
    m = re.search(r"widths=([\d.,]+)", opts)
    if m:
        opts = opts.replace(m.group(0), "widths=" + m.group(1) + "," + m.group(1))
    return _table(opts, headers + headers, [a + b for a, b in zip(left, right)])


def placeholder(plan: Plan, name: str) -> list[str]:
    t = totals(plan)
    if name == "counts":
        types = t["types"]
        rows = [
            ["Functional requirements (FR) covered", f"{t['frs_covered']} of {t['frs']}"],
            ["BRD IDs covered", f"{t['brd_covered']} of {t['brd_ids']}"],
            ["Test conditions", t["conditions"]],
            ["Business scenarios", t["scenarios"]],
            ["Test cases", t["cases"]],
            ["of which positive / negative", f"{t['positive']} / {t['negative']}"],
        ]
        rows += [[f"of type {k}", types.get(k, 0)] for k in TYPES if types.get(k)]
        rows += [["Test cases with an automation reference", t["automated"]],
                 ["Roles-and-access checks (action x role)", t["access_rows"]],
                 ["Named test data sets", t["data"]]]
        return _table('widths=10,5 caption="Test plan in numbers"', ["Item", "Count"], rows)
    if name == "coverage":
        rows = [[r["item"], r["title"], r["traces"], r["conditions"], r["positive"], r["negative"], r["total"],
                 r["automated"]] for r in fr_stats(plan)]
        size = "7.5" if len(rows) > 50 else "8"
        return _table(f'widths=2.3,6.4,3.2,1.5,1.5,1.5,1.3,1.6 caption="Coverage by FR" size={size}',
                      ["FR", "Title", "BRD IDs", "Cond.", "Pos.", "Neg.", "Total", "Auto."], rows)
    if name == "brd-coverage":
        stats = brd_stats(plan)
        if len(stats) >= 40:
            rows = [[r["item"], r["traces"].replace("FR-", ""), r["positive"], r["negative"]] for r in stats]
            return _paired('widths=2.3,3.6,1.1,1.1 caption="Coverage by BRD ID (FRs without the FR- prefix)" '
                           'size=7.5', ["BRD ID", "FRs", "Pos.", "Neg."], rows)
        rows = [[r["item"], r["traces"], r["conditions"], r["positive"], r["negative"], r["total"]]
                for r in stats]
        return _table('widths=2.6,8,1.8,1.6,1.6,1.6 caption="Coverage by BRD ID" size=8',
                      ["BRD ID", "FRs", "Conditions", "Positive", "Negative", "Total"], rows)
    if name == "scenarios":
        rows = []
        for s in plan.scenarios:
            n = sum(1 for c in plan.cases if c.scenario == s["id"])
            rows.append([s["id"], persona_label(plan, s["persona"]).split(" (")[0], _text(s.get("title")), n])
        return _table('widths=1.8,4.2,9.6,1.4 caption="Business scenarios" size=8.5',
                      ["ID", "Persona", "Scenario", "Cases"], rows)
    if name == "data":
        rows = [[d["id"], d.get("name", ""), _text(d.get("source"))] for d in plan.data]
        return _table('widths=2,6,9 caption="Named test data sets" size=8.5', ["Data set", "Name", "Source"], rows)
    if name == "personas":
        rows = []
        for code, p in plan.personas.items():
            n = sum(1 for c in plan.cases if c.persona == code)
            row = [code, p.get("name", "")]
            if plan.built:
                row.append(p.get("user", "-") or "-")
            rows.append(row + [n])
        headers = ["Role", "Persona"] + (["Demo user"] if plan.built else []) + ["Cases"]
        widths = "3.4,8,2.6,1.6" if plan.built else "3.6,10.4,1.6"
        return _table(f'widths={widths} caption="Personas used by the cases" size=8.5', headers, rows)
    if name == "access":
        used = {r for a in plan.access for r in (a.get("allow") or []) + (a.get("deny") or [])}
        roles = [r for r in plan.personas if r in used]
        rows = []
        for a in plan.access:
            cells = ["Y" if r in (a.get("allow") or []) else ("N" if r in (a.get("deny") or []) else "") for r in roles]
            rows.append([a.get("action", "")] + cells)
        w = ",".join(["4.4"] + ["1.45"] * len(roles))
        return _table(f'widths={w} caption="Roles-and-access checks (Y = allowed, N = refused, blank = not tested)" '
                      'size=7.5', ["Action"] + [str(plan.personas[r].get("short") or r.replace("_", " "))
                                                for r in roles], rows)
    if name == "automation":
        classes: Counter[str] = Counter()
        for c in plan.cases:
            for ref in [r.strip() for r in c.auto.split(";") if r.strip()]:
                classes[ref.partition("#")[0]] += 1
        rows = [[k, v] for k, v in sorted(classes.items(), key=lambda x: (-x[1], x[0]))]
        if not rows:
            return ["No automated test exists yet for this BRD; every case is run manually.", ""]
        return _paired('widths=6,1.6 caption="Automated tests referenced by the cases" size=8',
                       ["Test class", "Cases"], rows, min_rows=24)
    if name == "findings":
        if not plan.findings:
            return ["No FRS finding was raised while the cases were written.", ""]
        rows = [[f"TF-{plan.meta['code']}-{i:02d}", f.get("fr", ""), _text(f.get("issue")), _text(f.get("proposal"))]
                for i, f in enumerate(plan.findings, start=1)]
        return _table('widths=1.8,2.2,7.2,5.8 caption="FRS findings for the FRS owner" size=8.5',
                      ["ID", "FR", "Finding", "Proposed resolution"], rows)
    raise ValueError(f"unknown placeholder tp:{name}")


def expand(plan: Plan, lines: list[str]) -> list[str]:
    out: list[str] = []
    for line in lines:
        m = re.fullmatch(r"\s*<!--\s*tp:([\w-]+)\s*-->\s*", line)
        out.extend(placeholder(plan, m.group(1)) if m else [line])
    return out


def build_docx(plan: Plan, pdf: bool, keep_pdf: bool) -> tuple[Path, Path | None]:
    src = HERE / plan.meta["summary"]
    front, lines = load_source(src)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(plan, lines))
    m = plan.meta
    target = OUT / brand.output_name("TestPlan", m["brd"], f"{m['name']} Summary", str(m["version"]), "docx")
    return doc.publish(target, pdf=pdf, keep_pdf=keep_pdf)


# --------------------------------------------------------------------------- CLI

def report(plan: Plan) -> str:
    t = totals(plan)
    types = ", ".join(f"{k} {t['types'][k]}" for k in TYPES if t["types"].get(k))
    return (f"{plan.meta['brd']} {plan.meta['name']}: FRs {t['frs_covered']}/{t['frs']}, BRD IDs "
            f"{t['brd_covered']}/{t['brd_ids']}, conditions {t['conditions']}, scenarios {t['scenarios']}, "
            f"cases {t['cases']} (positive {t['positive']}, negative {t['negative']}; {types}), automated "
            f"{t['automated']}, access checks {t['access_rows']}, findings {len(plan.findings)}")


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Build a BIBS test plan (Excel + Word summary) from its YAML")
    ap.add_argument("yaml", nargs="+", help="brdnn_cases.yaml file(s)")
    ap.add_argument("--check", action="store_true", help="check the YAML and the coverage only")
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF pass (TOC page numbers stay blank)")
    ap.add_argument("--keep-pdf", action="store_true", help="keep the PDF of the Word summary")
    ap.add_argument("--previews", action="store_true", help="render page previews of the Word summary")
    args = ap.parse_args(argv)
    failed = False
    for y in args.yaml:
        path = Path(y)
        if not path.exists():
            path = HERE / y
        plan = load(path)
        for w in plan.warnings:
            print(f"{path.name}: warning: {w}")
        for e in plan.errors:
            print(f"{path.name}: {e}")
        print(report(plan))
        if plan.errors:
            failed = True
            continue
        if args.check:
            continue
        front, _ = load_source(HERE / plan.meta["summary"])
        print(f"xlsx: {build_xlsx(plan, list(front.get('control') or []))}")
        keep = args.keep_pdf or args.previews
        docx_path, pdf_path = build_docx(plan, pdf=not args.no_pdf, keep_pdf=keep)
        print(f"docx: {docx_path}")
        if pdf_path and args.previews:
            import render

            sheets = render.previews(pdf_path)
            print(f"previews: {sheets[0].parent} ({len(sheets)} files)")
        if pdf_path and not args.keep_pdf:
            pdf_path.unlink(missing_ok=True)
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
