"""Builds the comparison workbook of BDOI's User Access Maintenance FRS (BDO ITG, v1.1) and the BIBS User Access
Maintenance FRS (BRD-11, v2.1).

Sources
  * brd11_bdoi_frs.yaml (this folder): BDOI's FRS extracted word for word from docs/source-documents (written by
    --extract): the business requirements mapping, every FRUM item with its text, the annex field tables;
  * brd11_comparison.yaml (this folder): the assessment - structure, pros and cons, the mapping of every FRUM item,
    the BIBS-only content, the conflicts, the annex and field comparison, the incorporation plan, the recommendation;
  * the BIBS FRS source of BRD-11 (docs/deliverables/src/BRD-11_User_Access_Maintenance/FRS_BRD11_USER_ACCESS_
    MAINTENANCE.md): the FRs with their titles, rules and acceptance criteria, and the traceability of the 160 BRD
    requirement lines, read at every build so the workbook follows the FRS.

Output
  docs/deliverables/out/Programme/Comparisons/BIBS_Comparison_BRD-11_BDOI_FRS_vs_BIBS_FRS_User_Access_Maintenance_v1.0.xlsx

Usage
  python docs/deliverables/src/programme/comparisons/build_brd11_comparison.py --extract   # re-reads BDOI's FRS
  python docs/deliverables/src/programme/comparisons/build_brd11_comparison.py             # writes the workbook
  python docs/deliverables/src/programme/comparisons/build_brd11_comparison.py --check     # checks the sources only
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

BDOI_FILE = HERE / "brd11_bdoi_frs.yaml"
CMP_FILE = HERE / "brd11_comparison.yaml"
SOURCE_DOCX = REPO / "docs" / "source-documents" / "FRS - BDOI BROKERSYS User Access Maintenance v1.1 (BDOI).docx"
FRS_SRC = REPO / "docs" / "deliverables" / "src" / "BRD-11_User_Access_Maintenance" / "FRS_BRD11_USER_ACCESS_MAINTENANCE.md"

MATCH = ["Same", "Partial", "Different", "Not in ours"]
PLATFORM = ["Yes", "Partly", "No"]
ACTION = ["Adopt BDOI wording", "Keep ours", "Needs BDOI decision", "Platform change"]
BDOI_COVERAGE = ["Mapped", "Mapped (outside the system)", "Mapped to another document", "Not covered"]
OUR_COVERAGE = ["Traced"]
RECOMMEND = ["Include", "Include as annex", "Keep as supporting document"]
PRO_CON = ["Pro", "Con"]
DECISION = ["Confirm", "Decide"]
KIND = ["Between the documents", "Slip in BDOI's FRS"]

_COLOURS = {
    "SAME": (brand.SUCCESS_BG, brand.SUCCESS), "DIFFERENT": (brand.DANGER_BG, brand.DANGER),
    "NOT IN OURS": ("EDE7F6", "4527A0"), "YES": (brand.SUCCESS_BG, brand.SUCCESS),
    "PARTLY": (brand.AMBER_BG, brand.AMBER), "NO": (brand.DANGER_BG, brand.DANGER),
    "ADOPT BDOI WORDING": (brand.BG_BLUE, brand.HEADER_BLUE), "KEEP OURS": (brand.SUCCESS_BG, brand.SUCCESS),
    "NEEDS BDOI DECISION": (brand.AMBER_BG, brand.AMBER), "PLATFORM CHANGE": ("EDE7F6", "4527A0"),
    "MAPPED": (brand.SUCCESS_BG, brand.SUCCESS), "TRACED": (brand.SUCCESS_BG, brand.SUCCESS),
    "MAPPED (OUTSIDE THE SYSTEM)": (brand.AMBER_BG, brand.AMBER),
    "MAPPED TO ANOTHER DOCUMENT": (brand.DIRTY_WHITE, brand.MUTED), "NOT COVERED": (brand.DANGER_BG, brand.DANGER),
    "PRO": (brand.SUCCESS_BG, brand.SUCCESS), "CON": (brand.DANGER_BG, brand.DANGER),
    "INCLUDE": (brand.SUCCESS_BG, brand.SUCCESS), "INCLUDE AS ANNEX": (brand.BG_BLUE, brand.HEADER_BLUE),
    "KEEP AS SUPPORTING DOCUMENT": (brand.DIRTY_WHITE, brand.MUTED),
    "CONFIRM": (brand.BG_BLUE, brand.HEADER_BLUE), "DECIDE": (brand.AMBER_BG, brand.AMBER),
    "BETWEEN THE DOCUMENTS": (brand.AMBER_BG, brand.AMBER), "SLIP IN BDOI'S FRS": (brand.DIRTY_WHITE, brand.MUTED),
}


# ============================================================================================ BDOI's FRS (extract)

def _cell_lines(cell) -> list[dict]:
    """The paragraphs of a cell with their list level (0 = plain paragraph, 1 = first list level, ...)."""
    from docx.oxml.ns import qn  # noqa: PLC0415
    out = []
    for p in cell.paragraphs:
        text = " ".join(p.text.replace("\xa0", " ").split())
        if not text:
            continue
        num = p._p.find(qn("w:pPr") + "/" + qn("w:numPr"))
        level = 0
        if num is not None:
            il = num.find(qn("w:ilvl"))
            level = 1 + (int(il.get(qn("w:val"))) if il is not None else 0)
        bold = p._p.find(".//" + qn("w:b")) is not None
        out.append({"text": text, "level": level, "bold": bold})
    return out


def extract() -> dict:
    """BDOI's FRS, word for word, in the structure the workbook and the incorporation use."""
    import docx  # noqa: PLC0415
    d = docx.Document(str(SOURCE_DOCX))
    t = d.tables
    meta = {r.cells[0].text.strip().rstrip(":"): r.cells[1].text.strip() for r in t[1].rows}
    brm = []
    for row in t[3].rows[1:]:
        brm.append({"id": row.cells[0].text.strip(), "requirement": [x["text"] for x in _cell_lines(row.cells[1])],
                    "bdoi_frs": " ".join(row.cells[2].text.split())})
    frum = []
    for row in t[4].rows[1:]:
        entry = {"id": row.cells[0].text.strip(), "component": " ".join(row.cells[1].text.split()),
                 "capability": " ".join(row.cells[2].text.split()), "scope": [], "items": []}
        current = None
        seen = Counter()
        for line in _cell_lines(row.cells[3]):
            m = re.match(r"(FRUM[.-]\d{3}(?:\.\d{2})+)\s*(.*)", line["text"])
            if m and line["bold"] and line["level"] == 0:
                seen[m.group(1)] += 1
                key = m.group(1) + (" (2nd)" if seen[m.group(1)] > 1 else "")
                current = {"id": key, "title": m.group(2).strip(), "text": []}
                entry["items"].append(current)
            elif current is None:
                entry["scope"].append(line["text"])
            else:
                current["text"].append(("  " * max(line["level"] - 1, 0)) + line["text"])
        frum.append(entry)

    def fields(ti):
        return [[c.text.strip() for c in r.cells] for r in t[ti].rows[1:]]

    words = sum(len(p.text.split()) for p in d.paragraphs) + sum(
        len(c.text.split()) for tb in t for r in tb.rows for c in r.cells)
    return {
        "meta": {"file": SOURCE_DOCX.name, "template": meta.get("Template Reference No."),
                 "fr_document": meta.get("FR Document No."), "prepared_by": meta.get("Prepared by"),
                 "created": meta.get("Creation Date"), "revision": meta.get("Revision No."),
                 "last_updated": meta.get("Last Updated Date"), "tables": len(t), "images": 9,
                 "approx_words": round(words, -2)},
        "brm": brm, "frum": frum,
        "annex_e": fields(8), "annex_f": fields(9), "annex_g": fields(10), "annex_h": fields(11),
        "annex_files": {"C": ["User Access Matrix - Target State082626.xlsx", "List of BDOI Personnel _BDOI for Training.xlsx"],
                        "I": ["Application Security Requirement (ASR).xlsx", "Password Protection Guidelines v2_10Sep2026.pdf"]},
    }


def write_extract() -> Path:
    data = extract()
    head = ("# BDOI's Functional Requirements Specifications for User Access Maintenance (BDO ITG template D051, FR document\n"
            "# PRJ0012475, revision 1.1), extracted word for word from the source document of the same name in\n"
            "# docs/source-documents by build_brd11_comparison.py --extract: the business requirements mapping, every FRUM\n"
            "# item with its text (list levels shown by indentation) and the field tables of Annexes E to H. The second\n"
            "# item numbered FRUM.008.01 (export of the audit trail) is keyed 'FRUM.008.01 (2nd)'.\n")
    BDOI_FILE.write_text(head + yaml.safe_dump(data, sort_keys=False, allow_unicode=True, width=120),
                         encoding="utf-8")
    return BDOI_FILE


# ============================================================================================ BIBS FRS source

def parse_frs(text: str) -> dict:
    frs = {}
    for m in re.finditer(r"^```fr\n(.*?)^```", text, re.M | re.S):
        d = yaml.safe_load(m.group(1))
        frs[d["id"]] = {"title": d["title"], "rules": len(d.get("rules") or []),
                        "acceptance": len(d.get("acceptance") or []),
                        "validations": len(d.get("validations") or [])}
    part = text.split("<!-- TRACE:START -->", 1)[1].split("<!-- TRACE:END -->", 1)[0]
    lines = []
    for line in part.splitlines():
        if line.startswith("| BRD "):
            c = [x.strip() for x in line.strip().strip("|").split("|")]
            if not c[1].startswith("p."):
                continue
            lines.append({"id": c[0], "page": c[1], "req": c[2], "flag": c[3], "frs": c[4], "tests": c[5]})
    return {"frs": frs, "lines": lines}


def ids_of(cell: str) -> list[str]:
    return re.findall(r"FR-UA-\d{3}", cell or "")


# Technical words of BDOI's text, shown in business words in square brackets (business content only).
_BUSINESS_WORDS = [(re.compile(r"\bvia APIs?\b"), "through an [interface]"), (re.compile(r"\bAPIs?\b"), "[interface]")]


def business(text: str) -> str:
    for pattern, words in _BUSINESS_WORDS:
        text = pattern.sub(words, text)
    return text


def quote(item: dict, limit: int = 320) -> str:
    text = business(" ".join(x.strip() for x in item.get("text") or []))
    if not text:
        return business(item["title"])
    if len(text) <= limit:
        return text
    return text[:limit].rsplit(" ", 1)[0] + " ..."


# ============================================================================================ rows of the sheets

def bdoi_items(bdoi: dict) -> list[dict]:
    return [{"fr": fr["id"], "capability": fr["capability"], **item} for fr in bdoi["frum"] for item in fr["items"]]


def mapping_rows(bdoi: dict, cmp: dict, frs: dict) -> list[dict]:
    rows = []
    for item, m in zip(bdoi_items(bdoi), cmp["mapping"], strict=True):
        iid, ours, match, platform, action, diff = m
        titles = "; ".join(f"{i} {frs[i]['title']}" for i in ids_of(ours))
        rows.append({"fr": item["fr"], "item": iid, "title": business(item["title"]), "bdoi": quote(item),
                     "ours": ours if ours != "-" else "", "ours_title": titles, "match": match, "diff": diff,
                     "platform": platform, "action": action})
    return rows


def bdoi_row_of(brd_id: str, bdoi: dict) -> dict | None:
    """BDOI's mapping row that holds a BRD line: the row of the same function (first two levels; for 3.002 and 3.003
    the first three)."""
    parts = brd_id.replace("BRD ", "").replace(" (2nd)", "").split(".")
    key = ".".join(parts[:3] if parts[:2] in (["3", "002"], ["3", "003"]) else parts[:2])
    for r in bdoi["brm"]:
        rp = r["id"].replace("BRD ", "").split(".")
        if ".".join(rp[:3] if rp[:2] in (["3", "002"], ["3", "003"]) else rp[:2]) == key:
            return r
    return None


def coverage_of(bdoi_frs: str) -> str:
    if "UIDM" in bdoi_frs:
        return "Mapped (outside the system)"
    if re.search(r"FRUM[.-]\d{3}", bdoi_frs):
        return "Mapped"
    if re.search(r"FRID\.\d{3}", bdoi_frs):
        return "Mapped to another document"
    return "Not covered"


def coverage_rows(src: dict, bdoi: dict, cmp: dict) -> list[dict]:
    notes = cmp["coverage_notes"]
    rows = []
    for line in src["lines"]:
        r = bdoi_row_of(line["id"], bdoi)
        bd = r["bdoi_frs"] if r else "-"
        rows.append({"id": line["id"], "page": line["page"], "req": line["req"], "flag": line["flag"],
                     "bdoi_row": r["id"] if r else "-", "bdoi": bd, "bdoi_cov": coverage_of(bd) if r else "Not covered",
                     "ours": ", ".join(ids_of(line["frs"])), "ours_cov": "Traced",
                     "note": notes.get(line["id"].replace(" (2nd)", ""), notes.get(r["id"] if r else "", ""))})
    for n in cmp["nfr_coverage"]:
        rows.append({"id": n[0], "page": n[1], "req": n[2], "flag": "Yes" if "Other BU" in n[2] else "-",
                     "bdoi_row": "-", "bdoi": n[3], "bdoi_cov": n[4], "ours": n[5], "ours_cov": "Traced",
                     "note": n[6]})
    return rows


def only_bdoi_rows(mapping: list[dict], cmp: dict) -> list[dict]:
    impact = {
        "Platform change": "BIBS needs a change to provide it.",
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
    for ref, item, bdoi_text, ours, impact_text, action in cmp["only_bdoi_extra"]:
        rows.append({"ref": ref, "item": item, "bdoi": bdoi_text, "ours": ours, "impact": impact_text,
                     "action": action})
    return rows


def summary_rows(src, bdoi, cmp, mapping, coverage, only_ours, only_bdoi) -> list[dict]:
    frs = src["frs"]
    match = Counter(m["match"] for m in mapping)
    plat = Counter(m["platform"] for m in mapping)
    act = Counter(m["action"] for m in mapping)
    lines = [c for c in coverage if c["id"].startswith("BRD ")]
    bcov = Counter(c["bdoi_cov"] for c in lines)
    conflicts = cmp["conflicts"]
    between = [c for c in conflicts if c["kind"] == "Between the documents"]
    slips = [c for c in conflicts if c["kind"] == "Slip in BDOI's FRS"]
    items = sum(len(f["items"]) for f in bdoi["frum"])
    rows = [
        {"section": "Purpose", "item": "What each document is for", "bdoi": cmp["purpose"]["bdoi"],
         "ours": cmp["purpose"]["ours"]},
        {"section": "Counts", "item": f"BRD requirement lines ({len(lines)})",
         "bdoi": f"{len(bdoi['brm'])} mapping rows, one per BRD function. In substance: "
                 f"{bcov['Mapped']} lines mapped to FRUM items, {bcov['Mapped (outside the system)']} marked "
                 f"\"c/o UIDM\" (done outside the system), {bcov['Mapped to another document']} mapped to FRID items "
                 f"of another document, {bcov['Not covered']} not covered",
         "ours": f"{sum(1 for c in lines if c['ours_cov'] == 'Traced')} traced line by line to FRs, screens and "
                 f"test cases"},
        {"section": "Counts", "item": "Non-functional requirements of the BRD (41 rows; 11 'Other BU' items)",
         "bdoi": "Session times, lock-out, SSO, password expiry and history, audit; no capacity, retention, bulk, "
                 "notification, workflow or escalation item",
         "ours": "41 rows traced (UAM-NFR-01 to 41) with the BIBS approach; 25 of them specified as FRs"},
        {"section": "Counts", "item": "Functional requirements",
         "bdoi": f"{len(bdoi['frum'])} FRUM entries with {items} numbered items (FRUM.008.01 used twice)",
         "ours": f"{len(frs)} FRs with {sum(f['rules'] for f in frs.values())} business rules and "
                 f"{sum(f['validations'] for f in frs.values())} validations"},
        {"section": "Counts", "item": "Acceptance criteria", "bdoi": "None",
         "ours": f"{sum(f['acceptance'] for f in frs.values())} numbered criteria (basis of the BRD-11 test plan)"},
        {"section": "Counts", "item": "Annexes and supporting chapters",
         "bdoi": "9 annexes (A to I): 4 process flows, 3 report samples, the audit log sample, 4 field tables; the "
                 "User Access Matrix and the password guidelines are external files",
         "ours": "19 screen specifications, 4 walkthroughs, the messages and notifications catalogues, 5 reports, "
                 "the bulk upload, configuration inputs, 27 clarifications, user stories"},
        {"section": "Counts", "item": "Length",
         "bdoi": f"About {bdoi['meta']['approx_words']:,} words, 13 tables, 9 images",
         "ours": "About 56,000 words with the screen specifications, and 6 figures"},
        {"section": "Comparison", "item": f"BDOI's {len(mapping)} FRUM items against the BIBS FRS",
         "bdoi": ", ".join(f"{k}: {match[k]}" for k in MATCH), "ours": ""},
        {"section": "Comparison", "item": "Only in ours / only in BDOI's / conflicting",
         "bdoi": f"Only in BDOI's: {len(only_bdoi)} items", "ours": f"Only in ours: {len(only_ours)} items; "
         f"conflicting: {len(between)} points between the documents and {len(slips)} slips in BDOI's FRS"},
        {"section": "Comparison", "item": "Does the BIBS platform provide BDOI's items today?",
         "bdoi": ", ".join(f"{k}: {plat[k]}" for k in PLATFORM), "ours": ""},
        {"section": "Comparison", "item": "Proposed actions on BDOI's items",
         "bdoi": ", ".join(f"{k}: {act[k]}" for k in ACTION), "ours": ""},
    ]
    hl = cmp["headline"]
    for i in range(5):
        rows.append({"section": "Headline pros", "item": f"Pro {i + 1}", "bdoi": hl["bdoi_pros"][i],
                     "ours": hl["ours_pros"][i]})
    for i in range(5):
        rows.append({"section": "Headline cons", "item": f"Con {i + 1}", "bdoi": hl["bdoi_cons"][i],
                     "ours": hl["ours_cons"][i]})
    rec = cmp["recommendation"]
    rows.append({"section": "Recommendation", "item": "A. " + rec["title"], "bdoi": rec["text"], "ours": "Recommended"})
    for alt in rec["alternatives"]:
        rows.append({"section": "Alternatives", "item": alt["name"], "bdoi": alt["tradeoff"], "ours": ""})
    rows.append({"section": "ID scheme", "item": "Recommended", "bdoi": cmp["id_scheme"]["recommended"], "ours": ""})
    rows.append({"section": "ID scheme", "item": "Alternative", "bdoi": cmp["id_scheme"]["alternative"], "ours": ""})
    for d in cmp["decisions"]:
        rows.append({"section": "Decisions for BDOI", "item": d[0], "bdoi": d[1], "ours": d[2]})
    return rows


# ============================================================================================ checks and build

def check(src: dict, bdoi: dict, cmp: dict) -> list[str]:
    problems = []
    items = bdoi_items(bdoi)
    if len(items) != len(cmp["mapping"]):
        problems.append(f"mapping has {len(cmp['mapping'])} rows for {len(items)} BDOI items")
    for item, m in zip(items, cmp["mapping"]):
        if item["id"] != m[0]:
            problems.append(f"mapping row {m[0]} stands where BDOI has {item['id']} ({item['title']})")
        if m[2] not in MATCH or m[3] not in PLATFORM or m[4] not in ACTION:
            problems.append(f"{m[0]}: value outside the lists ({m[2]}, {m[3]}, {m[4]})")
        for fid in ids_of(m[1]):
            if fid not in src["frs"]:
                problems.append(f"{m[0]}: {fid} is not an FR of the BIBS FRS")
    ids = set()
    for c in cmp["conflicts"]:
        if c["id"] in ids:
            problems.append(f"conflict id {c['id']} twice")
        ids.add(c["id"])
        if c["kind"] not in KIND:
            problems.append(f"conflict {c['id']}: kind {c['kind']} outside the list")
        for fid in ids_of(c.get("ours", "")):
            if fid not in src["frs"]:
                problems.append(f"conflict {c['id']}: {fid} is not an FR of the BIBS FRS")
        for key in ("topic", "bdoi", "ours", "brd", "platform", "matches", "recommendation", "impact", "decide_by"):
            if not c.get(key):
                problems.append(f"conflict {c['id']}: {key} missing")
    for row in cmp["only_ours"]:
        for fid in ids_of(row[1]):
            if fid not in src["frs"]:
                problems.append(f"only in ours: {fid} is not an FR of the BIBS FRS")
        if row[3] not in RECOMMEND:
            problems.append(f"only in ours: recommendation {row[3]} outside the list")
    covered = {fid for row in cmp["only_ours"] for fid in ids_of(row[1])} | \
              {fid for m in cmp["mapping"] for fid in ids_of(m[1])}
    missing = sorted(set(src["frs"]) - covered)
    if missing:
        problems.append(f"FRs of the BIBS FRS neither mapped nor listed as only in ours: {missing}")
    if len(src["lines"]) != 160:
        problems.append(f"{len(src['lines'])} BRD lines read from the BIBS FRS (160 expected)")
    for line in src["lines"]:
        if bdoi_row_of(line["id"], bdoi) is None:
            problems.append(f"{line['id']}: no BDOI mapping row of its function")
    return problems


def build(src: dict, bdoi: dict, cmp: dict) -> Path:
    brand.STATUS_COLOURS.update(_COLOURS)
    meta = cmp["meta"]
    out = brand.OUT_DIR / "Programme" / "Comparisons" / meta["file"]
    wb = BdoiWorkbook(meta["title"], doc_type="Comparison workbook", brd="BRD-11", version=meta["version"],
                      date=meta["date"], subtitle=meta["subtitle"])
    wb.legend = [("Same", "The BIBS FRS says the same"), ("Partial", "The BIBS FRS covers part of it"),
                 ("Different", "The BIBS FRS says something else"),
                 ("Not in ours", "The BIBS FRS does not have it"), ("Yes", "The BIBS platform provides it today"),
                 ("Partly", "The BIBS platform provides part of it"), ("No", "It needs a platform change"),
                 ("Adopt BDOI wording", "Take BDOI's text into the merged FRS"),
                 ("Keep ours", "Keep the BIBS text (BRD wording or same meaning)"),
                 ("Needs BDOI decision", "BDOI decides between the two"),
                 ("Platform change", "BIBS needs a change to provide BDOI's item")]
    wb.cover_notes = [f"BDOI's FRS: {bdoi['meta']['file']} (template {bdoi['meta']['template']}, FR document "
                      f"{bdoi['meta']['fr_document']}, revision {bdoi['meta']['revision']}, prepared by "
                      f"{bdoi['meta']['prepared_by']}, created {meta['bdoi_created']}). BIBS FRS: "
                      f"02_BIBS_FRS_BRD-11_User_Access_Maintenance_v{meta['ours_version']} ({meta['ours_date']}). "
                      f"BRD: User Access Maintenance BRD, version of 8-Oct-2026 (v1 of 15-Apr-2025, 160 requirement "
                      f"lines).",
                      "Quotes of BDOI's FRS are its own words, shortened with \"...\" where long; its technical words "
                      "are shown in business words in square brackets.",
                      "\"BIBS platform today\" says what the BIBS screens and rules provide now, as seen on the SIT "
                      "environment of 8-Oct-2026."]
    mapping = mapping_rows(bdoi, cmp, src["frs"])
    coverage = coverage_rows(src, bdoi, cmp)
    only_bdoi = only_bdoi_rows(mapping, cmp)
    only_ours = cmp["only_ours"]

    wb.sheet("Summary and recommendation", [
        Column("section", "Section", 16, "Part of the summary"),
        Column("item", "Item", 34, "What is summarised"),
        Column("bdoi", "BDOI's FRS (v1.1) / text", 80, "BDOI's FRS; for the recommendation and decisions, the text"),
        Column("ours", "BIBS FRS (v2.1)", 70, "The BIBS FRS of BRD-11"),
    ], summary_rows(src, bdoi, cmp, mapping, coverage, only_ours, only_bdoi),
        description="What each document is for, the counts, the headline pros and cons, the recommended way forward "
                    "and the decisions BDOI must take")
    wb.sheet("Structure", [
        Column("bdoi", "BDOI section", 34, "Section of BDOI's FRS ('-' when it has none)"),
        Column("ours", "BIBS chapter(s)", 38, "Chapters of the BIBS FRS"),
        Column("bdoi_has", "What BDOI's section contains", 44, "Content of BDOI's section"),
        Column("ours_has", "What the BIBS chapter contains", 44, "Content of the BIBS chapters"),
        Column("depth", "Depth", 22, "Relative depth"),
        Column("assessment", "Assessment", 50, "Assessment and what to do in the merged FRS"),
    ], cmp["structure"], description="Section by section: BDOI's ITG template D051 against the chapters of the BIBS FRS")
    wb.sheet("Pros and cons", [
        Column("doc", "Document", 12, "BDOI's FRS or the BIBS FRS (ours)"),
        Column("group", "Group", 28, "Aspect assessed"),
        Column("kind", "Pro / Con", 10, "Strength or weakness", values=PRO_CON, status=True),
        Column("point", "Point", 100, "The strength or weakness, with its evidence"),
    ], [dict(zip(("doc", "group", "kind", "point"), r)) for r in cmp["pros_cons"]],
        description="Strengths and weaknesses of each document, grouped by aspect")
    wb.sheet("BRD coverage", [
        Column("id", "BRD ID", 17, "Requirement ID of the BRD as printed; NFR rows: the analyst's ID of the BIBS FRS"),
        Column("page", "Page", 7, "Page of the BRD"),
        Column("req", "Requirement", 50, "Activity and requirement of the BRD"),
        Column("flag", "Mandatory", 10, "The BRD's Mandatory flag ('-' = not flagged)"),
        Column("bdoi_row", "BDOI mapping row", 15, "Row of BDOI's Business Requirements Mapping holding the line"),
        Column("bdoi", "BDOI FR IDs", 20, "Functional requirements BDOI maps the row to"),
        Column("bdoi_cov", "BDOI coverage", 20, "Coverage in BDOI's FRS", values=BDOI_COVERAGE, status=True),
        Column("ours", "BIBS FR IDs", 20, "FRs of the BIBS FRS"),
        Column("ours_cov", "BIBS coverage", 11, "Coverage in the BIBS FRS", values=OUR_COVERAGE, status=True),
        Column("note", "Note", 50, "Difference between the two"),
    ], coverage, description="Every BRD requirement line (160) and the non-functional rows that carry a function, in "
                             "BDOI's FRS and in the BIBS FRS")
    wb.sheet("Requirement mapping", [
        Column("fr", "BDOI FR", 11, "BDOI functional requirement entry"),
        Column("item", "BDOI item", 16, "Numbered item of the entry, as numbered by BDOI"),
        Column("title", "BDOI item title", 28, "Title of the item"),
        Column("bdoi", "BDOI says (short quote)", 52, "BDOI's text, word for word, shortened"),
        Column("ours", "BIBS FR", 16, "Matching FRs of the BIBS FRS"),
        Column("ours_title", "BIBS FR title", 30, "Titles of the matching FRs"),
        Column("match", "Match", 12, "How far the BIBS FRS says the same", values=MATCH, status=True),
        Column("diff", "Difference in plain words", 56, "What differs"),
        Column("platform", "BIBS platform today", 12, "Does BIBS provide it today", values=PLATFORM, status=True),
        Column("action", "Proposed action", 18, "What to do in the merged FRS", values=ACTION, status=True),
    ], mapping, description=f"Every numbered item of BDOI's {len(bdoi['frum'])} FRUM entries against the BIBS FRS and "
                            "the BIBS platform")
    wb.sheet("Only in ours", [
        Column("kind", "Kind", 16, "FR, rule, screens, messages and the other content"),
        Column("item", "BIBS content", 70, "Content of the BIBS FRS that BDOI's FRS does not have"),
        Column("where", "Where in BDOI's template", 34, "FRUM entry, new FRUM item or new annex"),
        Column("rec", "Recommendation", 22, "How to carry it into the merged FRS", values=RECOMMEND, status=True),
    ], [dict(zip(("kind", "item", "where", "rec"), r)) for r in only_ours],
        description="BIBS content that BDOI's FRS does not have, and where it goes in BDOI's template")
    wb.sheet("Only in BDOI's", [
        Column("ref", "BDOI ref", 16, "FRUM item or annex"),
        Column("item", "Item", 34, "Item of BDOI's FRS"),
        Column("bdoi", "BDOI's FRS", 46, "BDOI's text (short quote) or the form of the annex"),
        Column("ours", "BIBS FRS today", 46, "What the BIBS FRS says, or the difference"),
        Column("impact", "Impact", 36, "Impact on BIBS"),
        Column("action", "Action", 26, "Add to our FRS, platform change needed, ask BDOI"),
    ], only_bdoi, description="Items of BDOI's FRS missing from the BIBS FRS or different in it, with the annexes")
    wb.sheet("Conflicts", [
        Column("id", "#", 6, "Conflict number (C = between the documents, S = slip in BDOI's FRS)"),
        Column("topic", "Topic", 22, "Point of conflict"),
        Column("kind", "Kind", 14, "Between the documents or a slip within BDOI's FRS", values=KIND, status=True),
        Column("bdoi", "BDOI's FRS says", 44, "BDOI's text"),
        Column("ours", "BIBS FRS says", 40, "The BIBS FRS"),
        Column("brd", "BRD says", 36, "The User Access Maintenance BRD"),
        Column("platform", "BIBS platform", 24, "What BIBS does today"),
        Column("matches", "Matches the BRD / platform", 22, "Which side matches the BRD and the platform"),
        Column("decision", "BDOI to", 10, "Confirm the BRD rule or decide", values=DECISION, status=True),
        Column("recommendation", "Our recommendation", 44, "The project team's recommendation"),
        Column("decide_by", "Decision by", 22, "Who decides for BDOI"),
    ], [{**c, "decision": "Confirm" if c["matches"].startswith("Ours (") or c["kind"] != KIND[0] else "Decide"}
        for c in cmp["conflicts"]],
        description="Where the two documents say different things, and the slips within BDOI's FRS: which side "
                    "matches the BRD and the platform, and the decision needed from BDOI")
    wb.sheet("Annex and field comparison", [
        Column("annex", "BDOI annex", 12, "Annex of BDOI's FRS"),
        Column("item", "Item", 26, "Report, matrix, log, field or rule"),
        Column("bdoi", "BDOI's FRS", 50, "Content of the annex"),
        Column("ours", "BIBS FRS and platform", 46, "What the BIBS FRS and platform have"),
        Column("diff", "Difference", 46, "Difference and what to do"),
    ], [dict(zip(("annex", "item", "bdoi", "ours", "diff"), r)) for r in cmp["annexes"]],
        description="Annexes B to I of BDOI's FRS (reports, User Access Matrix, audit log, report fields, password "
                    "guidelines) and the process flows of Annex A against the BIBS reports, matrix, audit log and "
                    "password policy, field by field")
    plan = [dict(zip(("step", "section", "content", "form", "effort", "who"), r)) for r in cmp["plan"]]
    plan.append({"step": "ID", "section": "ID scheme", "content": cmp["id_scheme"]["recommended"],
                 "form": "Alternative: " + cmp["id_scheme"]["alternative"], "effort": "", "who": "BDO ITG"})
    plan.append({"step": "ITG", "section": "What BDO ITG reviews", "content": "; ".join(cmp["itg_review"]),
                 "form": "", "effort": "", "who": "BDO ITG"})
    wb.sheet("Incorporation plan", [
        Column("step", "Step", 6, "Order of work"),
        Column("section", "BDOI section", 28, "Section of BDOI's FRS that receives the content"),
        Column("content", "Content to insert from ours", 60, "BIBS content and how it is placed"),
        Column("form", "Form in BDOI's style", 40, "How it looks in BDOI's FRS"),
        Column("effort", "Effort (days)", 11, "Estimated person-days of the project team (BDOI's effort apart)"),
        Column("who", "Who", 28, "Who does it and who reviews"),
    ], plan, description="How the BIBS content is placed into BDOI's FRS (version 1.2): section, ID scheme, effort, "
                         "order and BDO ITG's review")
    return wb.save(out)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--check", action="store_true", help="check the sources and the written file only")
    ap.add_argument("--extract", action="store_true", help="re-read BDOI's FRS into brd11_bdoi_frs.yaml")
    args = ap.parse_args()
    if args.extract:
        print("wrote", write_extract().relative_to(REPO))
        return 0
    bdoi = yaml.safe_load(BDOI_FILE.read_text(encoding="utf-8"))
    cmp = yaml.safe_load(CMP_FILE.read_text(encoding="utf-8"))
    src = parse_frs(FRS_SRC.read_text(encoding="utf-8"))
    problems = check(src, bdoi, cmp)
    for p in problems:
        print("ERROR:", p)
    if problems:
        return 1
    out = brand.OUT_DIR / "Programme" / "Comparisons" / cmp["meta"]["file"]
    if args.check:
        if not out.exists():
            print("ERROR: not written yet:", out.relative_to(REPO))
            return 1
        print("OK:", out.relative_to(REPO))
        return 0
    path = build(src, bdoi, cmp)
    print(f"{path.relative_to(REPO)} ({path.stat().st_size:,} bytes)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
