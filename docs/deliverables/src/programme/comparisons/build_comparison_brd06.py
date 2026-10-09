"""Builds the comparison workbook of BDOI's Renewal FRS (BDO ITG, v1.0) and the BIBS Renewal FRS (BRD-6, v2.0).

Sources
  * bdoi_frs_brd06.yaml (this folder): BDOI's FRS extracted from docs/source-documents - the business requirements
    mapping, every FRRN item with its text, the annex tables and the fields of the embedded annexes;
  * comparison_brd06.yaml (this folder): the assessment - structure, pros and cons, the mapping of every FRRN item,
    the BIBS-only content, the conflicts, the annex comparison, the incorporation plan and the recommendation;
  * the BIBS FRS source of BRD-6 (docs/deliverables/src/BRD-06_Renewal/FRS_BRD06_RENEWAL.md): the FRs with their
    titles, rules and acceptance criteria, and the traceability of the 42 BRRN IDs and of every line ID of the main
    BRD, read at every build so the workbook follows the FRS.

Output
  docs/deliverables/out/Programme/Comparisons/BIBS_Comparison_BRD-06_BDOI_FRS_vs_BIBS_FRS_Renewal_v1.0.xlsx

Usage
  python docs/deliverables/src/programme/comparisons/build_comparison_brd06.py           # writes the workbook
  python docs/deliverables/src/programme/comparisons/build_comparison_brd06.py --check   # checks the sources only
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

BDOI = yaml.safe_load((HERE / "bdoi_frs_brd06.yaml").read_text(encoding="utf-8"))
CMP = yaml.safe_load((HERE / "comparison_brd06.yaml").read_text(encoding="utf-8"))
FRS_SRC = REPO / "docs" / "deliverables" / "src" / "BRD-06_Renewal" / "FRS_BRD06_RENEWAL.md"
OUT = brand.OUT_DIR / "Programme" / "Comparisons" / CMP["meta"]["file"]

MATCH = ["Same", "Partial", "Different", "Not in ours"]
PLATFORM = ["Yes", "Partly", "No"]
ACTION = ["Adopt BDOI wording", "Keep ours", "Needs BDOI decision", "Platform change"]
BDOI_COVERAGE = ["Mapped", "Reference only", "Covered (not traced)", "Partly covered (not traced)", "Not covered",
                 "Out of scope"]
OUR_COVERAGE = ["Traced", "Out of scope"]
RECOMMEND = ["Include", "Include as annex", "Keep as supporting document"]
PRO_CON = ["Pro", "Con"]
DECISION = ["Confirm", "Decide"]

# Colours of the values of this workbook (added to the brand colours for this run only).
_COLOURS = {
    "SAME": (brand.SUCCESS_BG, brand.SUCCESS), "DIFFERENT": (brand.DANGER_BG, brand.DANGER),
    "NOT IN OURS": ("EDE7F6", "4527A0"), "YES": (brand.SUCCESS_BG, brand.SUCCESS),
    "PARTLY": (brand.AMBER_BG, brand.AMBER), "NO": (brand.DANGER_BG, brand.DANGER),
    "ADOPT BDOI WORDING": (brand.BG_BLUE, brand.HEADER_BLUE), "KEEP OURS": (brand.SUCCESS_BG, brand.SUCCESS),
    "NEEDS BDOI DECISION": (brand.AMBER_BG, brand.AMBER), "PLATFORM CHANGE": ("EDE7F6", "4527A0"),
    "MAPPED": (brand.SUCCESS_BG, brand.SUCCESS), "TRACED": (brand.SUCCESS_BG, brand.SUCCESS),
    "COVERED (NOT TRACED)": (brand.BG_BLUE, brand.HEADER_BLUE),
    "PARTLY COVERED (NOT TRACED)": (brand.AMBER_BG, brand.AMBER), "NOT COVERED": (brand.DANGER_BG, brand.DANGER),
    "REFERENCE ONLY": (brand.DIRTY_WHITE, brand.MUTED), "OUT OF SCOPE": (brand.DIRTY_WHITE, brand.MUTED),
    "PRO": (brand.SUCCESS_BG, brand.SUCCESS), "CON": (brand.DANGER_BG, brand.DANGER),
    "INCLUDE": (brand.SUCCESS_BG, brand.SUCCESS), "INCLUDE AS ANNEX": (brand.BG_BLUE, brand.HEADER_BLUE),
    "KEEP AS SUPPORTING DOCUMENT": (brand.DIRTY_WHITE, brand.MUTED),
    "CONFIRM": (brand.BG_BLUE, brand.HEADER_BLUE), "DECIDE": (brand.AMBER_BG, brand.AMBER),
}


# ============================================================================================ BIBS FRS source

def parse_frs(text: str) -> dict:
    """FRs, the BRRN trace and the main BRD line-ID trace of the BIBS FRS source."""
    frs = {}
    for m in re.finditer(r"^```fr\n(.*?)^```", text, re.M | re.S):
        d = yaml.safe_load(m.group(1))
        frs[d["id"]] = {"title": d["title"], "rules": len(d.get("rules") or []),
                        "acceptance": len(d.get("acceptance") or []),
                        "validations": len(d.get("validations") or [])}

    def table(after: str, before: str) -> list[list[str]]:
        part = text.split(after, 1)[1].split(before, 1)[0]
        rows = []
        for line in part.splitlines():
            if line.startswith("|") and not line.startswith("|---"):
                rows.append([c.strip() for c in line.strip().strip("|").split("|")])
        return rows[1:]

    brrn = {}
    for r in table("## BRRN requirements", "## Main BRD line IDs"):
        m = re.match(r"(BRRN\.\d{3}) \((.*)\)", r[0])
        brrn[m.group(1)] = {"pages": m.group(2), "frs": r[1]}
    lines = []
    function = ""
    for r in table("## Main BRD line IDs", "## Out-of-scope IDs"):
        function = r[0] or function
        for lid in (x.strip() for x in r[1].split(",")):
            if lid:
                lines.append({"id": lid, "function": function, "pages": r[2], "frs": r[3]})
    return {"frs": frs, "brrn": brrn, "lines": lines}


def ids_of(cell: str) -> list[str]:
    return re.findall(r"FR-RN-\d{3}", cell or "")


# ============================================================================================ sheets

def bdoi_items() -> list[dict]:
    out = []
    for fr in BDOI["frrn"]:
        for item in fr["items"]:
            out.append({"fr": fr["id"], "capability": fr["capability"], **item})
    return out


# Technical words of BDOI's text, shown in business words in square brackets (business content only).
_BUSINESS_WORDS = [(re.compile(r"\bAPIs?\b"), "[interface]"), (re.compile(r"\bhand-?off payload\b", re.I), "[message]"),
                   (re.compile(r"\bpayloads?\b", re.I), "[message]"),
                   (re.compile(r"\bbackend repository/table\b", re.I), "[kept list]"),
                   (re.compile(r"\bdatabases?\b", re.I), "[system]")]


def business(text: str) -> str:
    for pattern, words in _BUSINESS_WORDS:
        text = pattern.sub(words, text)
    return text


def quote(item: dict, limit: int = 300) -> str:
    """A short quote of BDOI's item: its first statements, word for word (technical words in business words)."""
    text = business(" ".join(item.get("text") or []))
    if not text:
        return business(item["title"])
    if len(text) <= limit:
        return text
    cut = text[:limit].rsplit(" ", 1)[0]
    return cut + " ..."


def mapping_rows(frs: dict) -> list[dict]:
    items = bdoi_items()
    rows = []
    for item, m in zip(items, CMP["mapping"], strict=True):
        iid, ours, match, platform, action, diff = m
        titles = "; ".join(f"{i} {frs[i]['title']}" for i in ids_of(ours))
        rows.append({"fr": item["fr"], "item": iid, "title": business(item["title"]), "bdoi": quote(item),
                     "ours": ours if ours != "-" else "", "ours_title": titles, "match": match, "diff": diff,
                     "platform": platform, "action": action})
    return rows


def coverage_rows(src: dict) -> list[dict]:
    brm = {r["id"]: r for r in BDOI["brm"]}
    notes = CMP["brrn_notes"]
    rows = []
    for bid in sorted(src["brrn"]):
        n = int(bid.split(".")[1])
        source = ("Addendum 1" if n <= 19 else "Workshop addendum" if n <= 40 else "Walkthrough addendum")
        if bid in ("BRRN.001", "BRRN.020", "BRRN.023", "BRRN.024", "BRRN.028", "BRRN.029", "BRRN.030", "BRRN.033",
                   "BRRN.034", "BRRN.035", "BRRN.036", "BRRN.037"):
            source += " (restated in the Walkthrough addendum)"
        bd = brm.get(bid, {}).get("bdoi_frs", "")
        cov = "Mapped" if "FRRN" in bd else "Reference only" if bd else "Not covered"
        rows.append({"id": bid, "source": source, "req": brm.get(bid, {}).get("requirement", ""),
                     "pages": src["brrn"][bid]["pages"], "bdoi": bd.replace(" / ", ", "), "bdoi_cov": cov,
                     "ours": ", ".join(ids_of(src["brrn"][bid]["frs"])), "ours_cov": "Traced",
                     "note": notes.get(bid, "")})
    fmap = CMP["brd_functions"]
    for line in src["lines"]:
        parts = line["id"].split(".")
        hit = None
        for k in range(len(parts), 1, -1):
            hit = fmap.get(".".join(parts[:k]))
            if hit:
                break
        out = line["frs"].startswith("OUT")
        if out:
            bd, cov, note = "-", "Out of scope", "Legacy reference (BDOIsys, EBIX or QPS), out of scope (BRD p.60)."
        elif hit:
            bd, cov, note = hit
        else:
            bd, cov, note = "-", "Not covered", ""
        rows.append({"id": line["id"], "source": "Main BRD (RMEL Phase 2 Online Dispositioning)",
                     "req": line["function"], "pages": line["pages"], "bdoi": bd, "bdoi_cov": cov,
                     "ours": "" if out else ", ".join(ids_of(line["frs"])),
                     "ours_cov": "Out of scope" if out else "Traced", "note": note})
    return rows


def only_bdoi_rows(mapping: list[dict]) -> list[dict]:
    impact = {
        "Platform change": "BIBS needs a platform change to provide it.",
        "Needs BDOI decision": "The two FRS disagree; BIBS follows the BIBS FRS until BDOI decides.",
        "Adopt BDOI wording": "Missing from the BIBS FRS text; the BIBS platform provides it or most of it.",
        "Keep ours": "BIBS follows the BRD wording.",
    }
    act = {"Platform change": "Add to our FRS; platform change needed", "Needs BDOI decision": "Ask BDOI",
           "Adopt BDOI wording": "Add to our FRS", "Keep ours": "Ask BDOI to align its wording"}
    rows = []
    for m in mapping:
        if m["match"] not in ("Not in ours", "Different"):
            continue
        rows.append({"ref": m["item"], "item": m["title"], "bdoi": m["bdoi"],
                     "ours": (m["ours"] + ": " if m["ours"] else "") + m["diff"],
                     "impact": impact[m["action"]], "action": act[m["action"]]})
    ours_of = {}
    for annex, _item, _bdoi, ours, _diff in CMP["annexes"]:
        ours_of.setdefault(annex, ours)
    for ref, item, impact_text, action in CMP["only_bdoi_annex"]:
        letter = ref.replace("Annex ", "")
        rows.append({"ref": ref, "item": item, "bdoi": BDOI["annex_forms"].get(letter, "Flowchart image"),
                     "ours": ours_of.get(letter, ""), "impact": impact_text, "action": action})
    return rows


def summary_rows(src: dict, mapping: list[dict], coverage: list[dict], only_ours: int, only_bdoi: int) -> list[dict]:
    frs = src["frs"]
    match = Counter(m["match"] for m in mapping)
    plat = Counter(m["platform"] for m in mapping)
    act = Counter(m["action"] for m in mapping)
    conflicts = CMP["conflicts"]
    brrn = [c for c in coverage if c["id"].startswith("BRRN")]
    lines = [c for c in coverage if not c["id"].startswith("BRRN")]
    bcov = Counter(c["bdoi_cov"] for c in lines)
    ours_bracket = sum(1 for c in conflicts if c["matches"].startswith("Ours ("))
    items = sum(len(f["items"]) for f in BDOI["frrn"])
    empty = sum(1 for v in BDOI["annex_forms"].values() if v == "Heading only")
    rows = [
        {"section": "Purpose", "item": "What each document is for", "bdoi": CMP["purpose"]["bdoi"],
         "ours": CMP["purpose"]["ours"]},
        {"section": "Counts", "item": f"BRRN IDs of the addenda ({len(brrn)})",
         "bdoi": f"{sum(1 for c in brrn if c['bdoi_cov'] == 'Mapped')} mapped to FRRN items; "
                 f"{sum(1 for c in brrn if c['bdoi_cov'] == 'Reference only')} to the User Access Matrix only",
         "ours": f"{sum(1 for c in brrn if c['ours_cov'] == 'Traced')} traced to FRs, screens and test cases"},
        {"section": "Counts", "item": f"Line IDs of the main BRD ({len(lines):,})",
         "bdoi": f"None traced. In substance: {bcov['Covered (not traced)']} covered, "
                 f"{bcov['Partly covered (not traced)']} partly covered, {bcov['Not covered']} not covered, "
                 f"{bcov['Out of scope']} out of scope",
         "ours": f"{sum(1 for c in lines if c['ours_cov'] == 'Traced'):,} traced; "
                 f"{sum(1 for c in lines if c['ours_cov'] == 'Out of scope')} out of scope (BRD p.60)"},
        {"section": "Counts", "item": "Functional requirements",
         "bdoi": f"{len(BDOI['frrn'])} FRRN entries with {items} numbered items",
         "ours": f"{len(frs)} FRs with {sum(f['rules'] for f in frs.values())} business rules and "
                 f"{sum(f['validations'] for f in frs.values())} validations"},
        {"section": "Counts", "item": "Acceptance criteria", "bdoi": "None",
         "ours": f"{sum(f['acceptance'] for f in frs.values())} numbered criteria (basis of 236 test conditions "
                 f"and 406 test cases)"},
        {"section": "Counts", "item": "Annexes and supporting chapters",
         "bdoi": f"{len(BDOI['annex_forms'])} annexes A to Z ({empty} with a heading only) and one process flow",
         "ours": "20 screen specifications, 5 walkthroughs, about 150 messages, 19 notifications, 5 document "
                 "outputs, 7 upload screens, 40 clarifications, 89 user stories"},
        {"section": "Counts", "item": "Length",
         "bdoi": f"About {BDOI['meta']['approx_words_main']:,} words, plus about "
                 f"{BDOI['meta']['approx_words_embedded']:,} in embedded annexes",
         "ours": "About 69,000 words and 78 figures"},
        {"section": "Comparison", "item": f"BDOI's {len(mapping)} FRRN items against the BIBS FRS",
         "bdoi": ", ".join(f"{k}: {match[k]}" for k in MATCH), "ours": ""},
        {"section": "Comparison", "item": "Only in ours / only in BDOI's / conflicting",
         "bdoi": f"Only in BDOI's: {only_bdoi} items (FR items not in ours or different, and the annexes)",
         "ours": f"Only in ours: {only_ours} items; conflicting: {len(conflicts)} points, of which "
                 f"{ours_bracket} where the BIBS FRS follows the BRD or the platform"},
        {"section": "Comparison", "item": "Does the BIBS platform provide BDOI's items today?",
         "bdoi": ", ".join(f"{k}: {plat[k]}" for k in PLATFORM), "ours": ""},
        {"section": "Comparison", "item": "Proposed actions on BDOI's items",
         "bdoi": ", ".join(f"{k}: {act[k]}" for k in ACTION), "ours": ""},
    ]
    hl = CMP["headline"]
    for i in range(5):
        rows.append({"section": "Headline pros", "item": f"Pro {i + 1}", "bdoi": hl["bdoi_pros"][i],
                     "ours": hl["ours_pros"][i]})
    for i in range(5):
        rows.append({"section": "Headline cons", "item": f"Con {i + 1}", "bdoi": hl["bdoi_cons"][i],
                     "ours": hl["ours_cons"][i]})
    rec = CMP["recommendation"]
    rows.append({"section": "Recommendation", "item": "A. " + rec["title"], "bdoi": rec["text"],
                 "ours": "Recommended"})
    for alt in rec["alternatives"]:
        rows.append({"section": "Alternatives", "item": alt["name"], "bdoi": alt["tradeoff"], "ours": ""})
    rows.append({"section": "ID scheme", "item": "Recommended", "bdoi": CMP["id_scheme"]["recommended"],
                 "ours": ""})
    rows.append({"section": "ID scheme", "item": "Alternative", "bdoi": CMP["id_scheme"]["alternative"],
                 "ours": ""})
    rows.append({"section": "Note", "item": "Schedules compared",
                 "bdoi": "The schedules of the two Renewal FRS are compared on sheet 8 (extraction, Renewal Advice "
                         "notices, NRNS, closing letters, billing runs, hold cover triggers). Neither Renewal FRS sets "
                         "a weekly report schedule.", "ours": ""})
    return rows


# ============================================================================================ checks and build

def check(src: dict) -> list[str]:
    problems = []
    items = bdoi_items()
    if len(items) != len(CMP["mapping"]):
        problems.append(f"mapping has {len(CMP['mapping'])} rows for {len(items)} BDOI items")
    for item, m in zip(items, CMP["mapping"]):
        if item["id"] != m[0]:
            problems.append(f"mapping row {m[0]} stands where BDOI has {item['id']} ({item['title']})")
        if m[2] not in MATCH or m[3] not in PLATFORM or m[4] not in ACTION:
            problems.append(f"{m[0]}: value outside the lists ({m[2]}, {m[3]}, {m[4]})")
        for fid in ids_of(m[1]):
            if fid not in src["frs"]:
                problems.append(f"{m[0]}: {fid} is not an FR of the BIBS FRS")
    for c in CMP["conflicts"]:
        for fid in ids_of(c["ours"]):
            if fid not in src["frs"]:
                problems.append(f"conflict {c['topic']}: {fid} is not an FR of the BIBS FRS")
    for row in CMP["only_ours"]:
        for fid in ids_of(row[1]):
            if fid not in src["frs"]:
                problems.append(f"only in ours: {fid} is not an FR of the BIBS FRS")
        if row[3] not in RECOMMEND:
            problems.append(f"only in ours: recommendation {row[3]} outside the list")
    if len(src["brrn"]) != 42:
        problems.append(f"{len(src['brrn'])} BRRN IDs traced in the BIBS FRS (42 expected)")
    if len({r['id'] for r in BDOI['brm']}) != 42:
        problems.append("BDOI's mapping does not list 42 BRRN IDs")
    if len(src["lines"]) != 1033:
        problems.append(f"{len(src['lines'])} main BRD line IDs read from the BIBS FRS (1,033 expected)")
    return problems


def build(src: dict) -> Path:
    brand.STATUS_COLOURS.update(_COLOURS)
    meta = CMP["meta"]
    wb = BdoiWorkbook(meta["title"], doc_type="Comparison workbook", brd="BRD-06", version=meta["version"],
                      date=meta["date"], subtitle=meta["subtitle"])
    wb.legend = [("Same", "The BIBS FRS says the same"), ("Partial", "The BIBS FRS covers part of it"),
                 ("Different", "The BIBS FRS says something else"),
                 ("Not in ours", "The BIBS FRS does not have it"), ("Yes", "The BIBS platform provides it"),
                 ("Partly", "The BIBS platform provides part of it"), ("No", "It needs a platform change"),
                 ("Adopt BDOI wording", "Take BDOI's text into the merged FRS"),
                 ("Keep ours", "Keep the BIBS text (BRD wording or same meaning)"),
                 ("Needs BDOI decision", "BDOI decides between the two"),
                 ("Platform change", "BIBS needs a change to provide BDOI's item")]
    wb.cover_notes = [f"BDOI's FRS: {BDOI['meta']['file']} (v{meta['bdoi_version']}, {meta['bdoi_date']}). "
                      f"BIBS FRS: 02_BIBS_FRS_BRD-06_Renewal_v{meta['ours_version']} ({meta['ours_date']}).",
                      "Quotes of BDOI's FRS are its own words, shortened with \"...\" where long; its technical words "
                      "are shown in business words in square brackets."]
    mapping = mapping_rows(src["frs"])
    coverage = coverage_rows(src)
    only_bdoi = only_bdoi_rows(mapping)
    only_ours = CMP["only_ours"]

    wb.sheet("Summary and recommendation", [
        Column("section", "Section", 16, "Part of the summary"),
        Column("item", "Item", 34, "What is summarised"),
        Column("bdoi", "BDOI's FRS (v1.0) / text", 80, "BDOI's FRS; for the recommendation, its text"),
        Column("ours", "BIBS FRS (v2.0)", 70, "The BIBS FRS of BRD-6"),
    ], summary_rows(src, mapping, coverage, len(only_ours), len(only_bdoi)),
        description="What each document is for, the counts, the headline pros and cons and the recommended way "
                    "forward")
    wb.sheet("Structure comparison", [
        Column("bdoi", "BDOI section", 34, "Section of BDOI's FRS ('-' when it has none)"),
        Column("ours", "BIBS chapter(s)", 38, "Chapters of the BIBS FRS"),
        Column("bdoi_has", "What BDOI's section contains", 44, "Content of BDOI's section"),
        Column("ours_has", "What the BIBS chapter contains", 44, "Content of the BIBS chapters"),
        Column("depth", "Depth", 22, "Relative depth"),
        Column("assessment", "Assessment", 50, "Assessment and what to do in the merged FRS"),
    ], CMP["structure"], description="Section by section: BDOI's ITG template against the chapters of the BIBS FRS")
    wb.sheet("Pros and cons", [
        Column("doc", "Document", 12, "BDOI's FRS or the BIBS FRS (ours)"),
        Column("group", "Group", 28, "Aspect assessed"),
        Column("kind", "Pro / Con", 10, "Strength or weakness", values=PRO_CON, status=True),
        Column("point", "Point", 100, "The strength or weakness, with its evidence"),
    ], [dict(zip(("doc", "group", "kind", "point"), r)) for r in CMP["pros_cons"]],
        description="Strengths and weaknesses of each document, grouped by aspect")
    wb.sheet("BRD coverage", [
        Column("id", "BRD ID", 15, "Requirement ID of the Renewal BRD pack of 8-Oct-2026"),
        Column("source", "Source", 24, "Part of the BRD pack"),
        Column("req", "Requirement", 50, "BRRN: BDOI's quote of the requirement; main BRD: the BRD function"),
        Column("pages", "Pages", 14, "Pages of the BRD pack of 8-Oct-2026"),
        Column("bdoi", "BDOI FR IDs", 20, "FRRN items of BDOI's FRS"),
        Column("bdoi_cov", "BDOI coverage", 18, "Coverage in BDOI's FRS", values=BDOI_COVERAGE, status=True),
        Column("ours", "BIBS FR IDs", 20, "FRs of the BIBS FRS"),
        Column("ours_cov", "BIBS coverage", 13, "Coverage in the BIBS FRS", values=OUR_COVERAGE, status=True),
        Column("note", "Note", 50, "Difference between the two"),
    ], coverage, description="Every BRD requirement ID of the Renewal BRD: the 42 BRRN IDs of the addenda and every "
                             "line ID of the main BRD")
    wb.sheet("Requirement mapping", [
        Column("fr", "BDOI FR", 11, "BDOI functional requirement entry"),
        Column("item", "BDOI item", 14, "Numbered item of the entry, as numbered by BDOI"),
        Column("title", "BDOI item title", 28, "Title of the item"),
        Column("bdoi", "BDOI says (short quote)", 52, "BDOI's text, word for word, shortened"),
        Column("ours", "BIBS FR", 16, "Matching FRs of the BIBS FRS"),
        Column("ours_title", "BIBS FR title", 30, "Titles of the matching FRs"),
        Column("match", "Match", 12, "How far the BIBS FRS says the same", values=MATCH, status=True),
        Column("diff", "Difference in plain words", 56, "What differs"),
        Column("platform", "BIBS platform today", 12, "Does BIBS provide it today", values=PLATFORM, status=True),
        Column("action", "Proposed action", 18, "What to do in the merged FRS", values=ACTION, status=True),
    ], mapping, description="Every numbered item of BDOI's 44 FRRN entries against the BIBS FRS and the BIBS "
                            "platform")
    wb.sheet("Only in ours", [
        Column("kind", "Kind", 16, "FR, rule, screens, messages and the other content"),
        Column("item", "BIBS content", 70, "Content of the BIBS FRS that BDOI's FRS does not have"),
        Column("where", "Where in BDOI's template", 34, "Section, FRRN entry or new annex"),
        Column("rec", "Recommendation", 22, "How to carry it into the merged FRS", values=RECOMMEND, status=True),
    ], [dict(zip(("kind", "item", "where", "rec"), r)) for r in only_ours],
        description="BIBS content that BDOI's FRS does not have, and where it goes in BDOI's template")
    wb.sheet("Only in BDOI's", [
        Column("ref", "BDOI ref", 14, "FRRN item or annex"),
        Column("item", "Item", 34, "Item of BDOI's FRS"),
        Column("bdoi", "BDOI's FRS", 46, "BDOI's text (short quote) or the form of the annex"),
        Column("ours", "BIBS FRS today", 46, "What the BIBS FRS says, or the difference"),
        Column("impact", "Impact", 36, "Impact on BIBS"),
        Column("action", "Action", 26, "Add to our FRS, platform change needed, ask BDOI"),
    ], only_bdoi, description="Items of BDOI's FRS missing from the BIBS FRS or different in it, with every annex A "
                              "to Z")
    wb.sheet("Conflicts", [
        Column("no", "#", 5, "Conflict number"),
        Column("topic", "Topic", 22, "Point of conflict"),
        Column("kind", "Kind", 13, "Rule, status, approval, schedule, naming or integration"),
        Column("bdoi", "BDOI's FRS says", 44, "BDOI's text"),
        Column("ours", "BIBS FRS says", 40, "The BIBS FRS"),
        Column("brd", "BRD says", 36, "The Renewal BRD of 8-Oct-2026"),
        Column("platform", "BIBS platform", 24, "What BIBS does today"),
        Column("matches", "Matches the BRD / platform", 22, "Which side matches the BRD and the platform"),
        Column("decision", "BDOI to", 10, "Confirm the BRD rule or decide", values=DECISION, status=True),
        Column("recommendation", "Our recommendation", 44, "The project team's recommendation"),
    ], [{"no": i, **c, "decision": "Confirm" if c["matches"].startswith("Ours (") else "Decide"}
        for i, c in enumerate(CMP["conflicts"], start=1)],
        description="Where the two documents say different things, which side matches the BRD and the platform, "
                    "and the decision needed from BDOI")
    wb.sheet("Annex comparison", [
        Column("annex", "BDOI annex", 12, "Annex of BDOI's FRS"),
        Column("item", "Item", 26, "Rule, layout or template"),
        Column("bdoi", "BDOI's FRS", 56, "Content of the annex"),
        Column("ours", "BIBS FRS and platform", 44, "What the BIBS FRS and platform have"),
        Column("diff", "Difference", 44, "Difference"),
    ], [dict(zip(("annex", "item", "bdoi", "ours", "diff"), r)) for r in CMP["annexes"]],
        description="Each annex A to Z of BDOI's FRS against the BIBS FRS and the BIBS platform")
    plan = [dict(zip(("step", "section", "content", "form", "effort", "who"), r)) for r in CMP["plan"]]
    plan.append({"step": "ID", "section": "ID scheme", "content": CMP["id_scheme"]["recommended"],
                 "form": "Alternative: " + CMP["id_scheme"]["alternative"], "effort": "", "who": "BDO ITG"})
    plan.append({"step": "ITG", "section": "What BDO ITG reviews", "content": "; ".join(CMP["itg_review"]),
                 "form": "", "effort": "", "who": "BDO ITG"})
    wb.sheet("Incorporation plan", [
        Column("step", "Step", 6, "Order of work"),
        Column("section", "BDOI section", 28, "Section of BDOI's FRS that receives the content"),
        Column("content", "Content to insert from ours", 60, "BIBS content and how it is placed"),
        Column("form", "Form in BDOI's style", 40, "How it looks in BDOI's FRS"),
        Column("effort", "Effort (days)", 11, "Estimated person-days of the project team (BDOI's effort apart)"),
        Column("who", "Who", 28, "Who does it and who reviews"),
    ], plan, description="How the BIBS content is placed into BDOI's FRS format: section, ID scheme, effort, order "
                         "and BDO ITG's review")
    return wb.save(OUT)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="check the sources and the written file only")
    args = ap.parse_args()
    src = parse_frs(FRS_SRC.read_text(encoding="utf-8"))
    problems = check(src)
    for p in problems:
        print("ERROR:", p)
    if problems:
        return 1
    if args.check:
        if not OUT.exists():
            print("ERROR: not written yet:", OUT.relative_to(REPO))
            return 1
        print("OK:", OUT.relative_to(REPO))
        return 0
    path = build(src)
    print(f"{path.relative_to(REPO)} ({path.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
