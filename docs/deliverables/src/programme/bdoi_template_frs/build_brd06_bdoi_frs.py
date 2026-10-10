"""Renewal FRS in BDOI's template, version 1.1 (full BRD coverage and Business Unit review edition).

    python docs/deliverables/src/programme/bdoi_template_frs/build_brd06_bdoi_frs.py [--no-pdf]

Starts from a copy of BDOI's own FRS (docs/source-documents/FRS - BDOI BROKERSYS Renewal v1.0 (BDOI).docx, BDO ITG
template D051, project PRJ0012475) and keeps its template: styles, cover box, headers and footers, table formats, fonts
and the table of contents (marked to update when the document is opened). BDOI's text is kept word for word; the
additions of version 1.1 come from:

  brd06_frs_additions.yaml  statements and acceptance criteria under BDOI's 143 items, new sub-items, FRRN.045-049
  brd06_document.yaml       cover, revision log, introduction, mapping additions, rows of BDOI's annexes, Annex AC,
                            the slips and observations of Annex AO and the glossary
  brd06_review.yaml         the Business Unit review edition: summary, Annexes AD to AK, AN, AP, AQ, AP.5
  brd06_flows.yaml          the figures (process flows, life-cycles, menu, approvals, context), drawn with
                            brd11_figures.py and placed by brd06_review_edition.py
  ../comparisons/comparison_brd06.yaml   the conflicts of the comparison workbook (Annex AP.1)
  brd06_brd.py              the 1,033 requirement lines of the Renewal BRD, read from the BRD PDF

and from the BIBS release set BRD-06 (FRS v2.0, screens, screenshots, notifications, walkthroughs, test cases).

Outputs in docs/deliverables/out/Programme/BDOI_Template_FRS/BRD-06_Renewal/:
  BIBS_FRS-BDOI_BRD-06_Renewal_v1.1.docx                       clean copy
  BIBS_FRS-BDOI_BRD-06_Renewal_v1.1.pdf                        the clean copy as PDF (not kept in git)
  BIBS_FRS-BDOI_BRD-06_Renewal_v1.1_Changes_Highlighted.docx   every addition of version 1.1 shaded light yellow,
                                                               change summary in front

Self-checks (the build fails when one does not hold): every BRRN ID and every line ID of the BRD is in the Business
Requirements Mapping and maps to at least one FRRN item; every FR-RN of the BIBS FRS is in Annex AM with its FRRN
items; every conflict of the comparison is in Annex AO; BDOI's original FRRN IDs and texts are all still present; every
notice and e-mail of the platform for Renewal is in Annex AE with its text found in the platform; every document of
the BIBS set is in Annex AF; every earlier question is classified once and none that BDOI's FRS answers is kept in
Annex AN; no internal code or restricted word in the added text.
"""

from __future__ import annotations

import argparse
import copy
import re
import shutil
import subprocess
import sys
import tempfile
from collections import Counter, OrderedDict, defaultdict
from pathlib import Path

import docx
import yaml
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import brd06_brd  # noqa: E402
import build_brd11_bdoi_frs as base  # noqa: E402  (the helpers of the BDOI-template builders)
from build_brd11_bdoi_frs import (cell_shade, el_text, last_content, ppr_insert, rpr_shade,  # noqa: E402
                                  set_update_fields, shd_el, strip_ids, subsequence, texts_of, to_pdf)

REPO = HERE.parents[4]
SRC = REPO / "docs" / "deliverables" / "src"
RN = SRC / "BRD-06_Renewal"
CMP = SRC / "programme" / "comparisons" / "comparison_brd06.yaml"
ADD = yaml.safe_load((HERE / "brd06_frs_additions.yaml").read_text(encoding="utf-8"))
DOC = yaml.safe_load((HERE / "brd06_document.yaml").read_text(encoding="utf-8"))
OUT = REPO / "docs" / "deliverables" / "out" / DOC["meta"]["out_folder"]
HL_FILL = base.HL_FILL
HEAD_FILL = base.HEAD_FILL
FRRN_RE = re.compile(r"FRRN\.\d{2,3}(?:\.\d{2})*(?: \(2nd\))?")
ITEM_HEAD_RE = re.compile(r"\s*(FRRN\.\d{2,3}(?:\.\d{2})*)\b")

# ----------------------------------------------------------------------------------------------------------- wording
CODES = {
    "MKT_TL": "Marketing Team Lead", "MKT_AO": "Account Officer", "PROCESSING_TL": "Processing Team Lead",
    "PROCESSOR": "Processing Officer", "BUSINESS_ADMIN": "Business Administrator", "CONTACT_CENTER": "Contact Center",
    "SYSADMIN": "System Administrator", "NB_BOOKING": "New Business booking user",
    "RNW_VIEW": "the view function", "RNW_EXTRACT": "the extraction function", "RNW_ASSIGN": "the assignment function",
    "RNW_DISPOSE": "the disposition function", "RNW_REVIEW": "the review function",
    "RNW_OVERRIDE": "the override function", "RNW_PROCESS_ASSIGN": "the processing assignment function",
    "RNW_PROCESS": "the processing function", "RNW_UPLOAD": "the upload function",
    "RNW_INSURER": "the insurer function", "RNW_RA_GENERATE": "the Renewal Advice function",
    "RNW_RA_SEND": "the sending function", "RNW_ACCEPT": "the acceptance function",
    "RNW_FOLLOWUP": "the follow-up function", "RNW_LAMD_UPLOAD": "the LAMD upload function",
    "RNW_VALIDATE": "the validation function", "RNW_REPORT_VIEW": "the report function",
    "RNW_EXPORT": "the export function", "RNW_SETUP": "the set-up function",
    "RNW_TEMPLATE_MAINTAIN": "the template function", "RNW_PACKAGE_REMAP": "the package function",
    "RNW_TSU_QUEUE": "the Renewal TSU queue function",
    "MASTER_AUTHORIZE": "the authorisation function", "LOV_MANAGE": "the list maintenance function",
    "ACCESS_REQUEST": "the access request function", "ATTACHMENT_MANAGE": "the document function",
    "RA_ACCEPTANCE": "acceptance e-mail", "SIGNED_RA": "signed Renewal Advice", "LAMD_REPORT": "LAMD report",
    "RENEWAL_ADVICE": "Renewal Advice", "RENEWAL_LETTER": "renewal letter", "FOR_DISPOSITION": "For Disposition",
    "FOR_TL_REVIEW": "Review in Progress", "FOR_PROCESSING": "For Processing", "IN_PROCESSING": "In Processing",
    "WITH_INSURER": "With Insurer", "RA_READY": "RA Ready", "RA_GENERATED": "RA Generated", "RA_SENT": "RA Sent",
    "LETTER_PENDING": "Letter Pending", "TRANSFER_PENDING": "Transfer Pending", "NB_PATH": "New Business path",
    "FOR_PLACEMENT_BOOKING": "For Placement and Booking", "EXPIRED_UNRENEWED": "expired without renewal",
    "NOT_RENEWED": "not renewed", "UNASSIGNED": "Unassigned", "EXTRACTED": "Extracted", "EVALUATING": "Evaluating",
    "PACKAGE_REMAP": "the package rule", "ENDORSEMENT_PENDING": "the endorsement rule",
    "OUTSTANDING_PREMIUM": "the outstanding premium rule", "FINANCIAL_IMPACT": "the financial-impact rule",
    "TSI_THRESHOLD": "the TSI rule", "RISK_CODE_DEFINED": "the risk code rule", "REFERENCE_MATCH": "the reference rule",
    "PN_PRESENT": "the PN rule", "KYC_DUE": "the KYC rule", "RNW_CASE": "the renewal workflow",
}
CODE_RE = base.CODE_RE
PHRASES = [
    (re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+"), "a test e-mail address"),
    (re.compile(r"Seed data:\s*"), "SIT/UAT data: "), (re.compile(r"\bseed (data|profile)\b", re.I), r"SIT/UAT \1"),
    (re.compile(r"\bseed\b", re.I), "SIT/UAT"),
    (re.compile(r"\(([A-Z][A-Z0-9]*_[A-Z0-9_]+)(?:,? (?:or|and|/) [A-Z][A-Z0-9]*_[A-Z0-9_]+)*\)"), ""),
    (re.compile(r"\bBIBS\b"), "the system"), (re.compile(r"\bin-app\b", re.I), "in the system"),
    (re.compile(r"\bcandidates?\b"), lambda m: "renewal accounts" if m.group(0).endswith("s") else "renewal account"),
    (re.compile(r"\bTL\b"), "Team Lead"), (re.compile(r"\bstage\b"), "status"),
    (re.compile(r"\b[Aa] platform change\b"), "a change to the system"),
    (re.compile(r"\bplatform change\b"), "change to the system"),
    (re.compile(r"\bchapter \d+\b"), "this FRS"), (re.compile(r"\bsection \d+(?:\.\d+)?\b"), "this FRS"),
]
RESTRICTED = base.RESTRICTED
RESTRICTED_CS = base.RESTRICTED_CS


def clean(text) -> str:
    s = "" if text is None else str(text)
    for code, word in sorted(CODES.items(), key=lambda kv: -len(kv[0])):
        s = re.sub(r"\b" + re.escape(code) + r"\b", word, s)
    for pat, rep in PHRASES:
        s = pat.sub(rep, s)
    s = re.sub(r"\s+([,.;:])", r"\1", re.sub(r"\(\s*\)", "", s))
    return re.sub(r"\s{2,}", " ", s).strip()


def frrn_key(fid: str):
    nums = [int(x) for x in re.findall(r"\d+", fid.replace(" (2nd)", ""))]
    return nums + [1 if "(2nd)" in fid else 0]


# ------------------------------------------------------------------------------------------------------- the sources
def our_frs() -> "OrderedDict[str, dict]":
    text = (RN / "FRS_BRD06_RENEWAL.md").read_text(encoding="utf-8")
    out = OrderedDict()
    for block in re.findall(r"^```fr\n(.*?)^```", text, re.S | re.M):
        fr = yaml.safe_load(block)
        out[fr["id"]] = fr
    return out


def trace_groups() -> list[dict]:
    """The rows of section 11.2 of the BIBS FRS: function, line IDs, pages, FR-RN."""
    text = (RN / "FRS_BRD06_RENEWAL.md").read_text(encoding="utf-8")
    part = text.split("## Main BRD line IDs", 1)[1].split("## Out-of-scope IDs", 1)[0]
    rows, function = [], ""
    for line in part.splitlines():
        if not line.startswith("|") or line.startswith("|---"):
            continue
        c = [x.strip() for x in line.strip().strip("|").split("|")]
        if len(c) < 4 or not re.match(r"\d", c[1] or ""):
            continue
        function = c[0] or function
        rows.append({"function": function, "ids": [x.strip() for x in c[1].split(",") if x.strip()],
                     "pages": c[2], "frs": re.findall(r"FR-RN-\d{3}", c[3]), "out": c[3].startswith("OUT")})
    return rows


def test_cases() -> dict[str, tuple[list[str], int]]:
    data = yaml.safe_load((RN / "brd06_cases.yaml").read_text(encoding="utf-8"))
    out = {}
    for fr_id, entry in (data.get("frs") or {}).items():
        n = fr_id.split("-")[-1]
        conds = [f"TC-RN-{n}.{i}" for i in range(1, len(entry.get("conditions") or []) + 1)]
        out[fr_id] = (conds, len(entry.get("cases") or []))
    return out


def tc_text(fr_id, cases) -> str:
    conds, n = cases.get(fr_id, ([], 0))
    if not conds:
        return "-"
    return (f"{conds[0]} to {conds[-1].split('-')[-1]}" if len(conds) > 1 else conds[0]) + f" ({n} cases)"


def all_items() -> list[tuple[str, list[str]]]:
    out = [(i, v.get("refs") or []) for i, v in ADD["items"].items()]
    out += [(ns["id"], ns["refs"]) for ns in ADD["new_subitems"]]
    out += [(si["id"], si["refs"]) for it in ADD["new_items"] for si in it["subitems"]]
    return out


def fr_to_frrn_map() -> dict[str, list[str]]:
    out: dict[str, list[str]] = defaultdict(list)
    for fid, refs in all_items():
        for r in refs:
            if fid not in out[r]:
                out[r].append(fid)
    return {k: sorted(v, key=frrn_key) for k, v in out.items()}


def line_items_map() -> dict[str, list[str]]:
    """FR-RN -> the FRRN items written against a BRD line (the items where the FR-RN is the first reference, or the
    items named in brd06_document.yaml line_items)."""
    first: dict[str, list[str]] = defaultdict(list)
    for fid, refs in all_items():
        if refs:
            first[refs[0]].append(fid)
    out = {k: sorted(v, key=frrn_key) for k, v in first.items()}
    out.update(DOC["line_items"])
    return out


# ------------------------------------------------------------------------------------------------------- the builder
class Builder(base.Builder):
    """The BDOI-template builder of BRD-11 on BDOI's Renewal FRS; every addition of version 1.1 is shaded in the
    review copy."""

    def __init__(self, highlight: bool):  # noqa: D107  (no super(): another source document)
        self.highlight = highlight
        self.round13 = True
        self.doc = docx.Document(str(REPO / DOC["meta"]["source"]))
        self.body = self.doc.element.body
        self.tables = list(self.doc.tables)
        self.numbering = self.doc.part.numbering_part.element
        self.stats: Counter = Counter()
        self.log: list[dict] = []
        self.fr_to_frum = fr_to_frrn_map()
        self.added_text: list[str] = []
        self.q_rows: list[dict] = []
        self.fig_no = 0

    @property
    def hl(self) -> bool:
        return self.highlight

    def new_num(self, like_num_id: int = 48) -> int:
        return super().new_num(like_num_id)

    def figure(self, png: Path, caption: str, read: str, brd: str, max_h=8.4) -> list:
        self.fig_no += 1
        self.stats["figures_added"] += 1
        return [self.picture(png, max_w=7.0, max_h=max_h),
                self.para(f"Figure {self.fig_no} - {caption}", italic=True, size=8.5, jc="center", space_after=40),
                self.para(f"How to read it: {read} Traces to: {brd}.", size=8.5, space_after=160)]


def find_par(b: Builder, start: str, style: str | None = None):
    return base.find_par(b, start, style)


# ------------------------------------------------------------------------------------------------------- the edits
def edit_cover(b: Builder):
    cell = b.tables[0]._tbl
    old, new = "v" + DOC["meta"]["version_old"], "v" + DOC["meta"]["version_new"]
    for t in cell.iter(qn("w:t")):
        if (t.text or "").rstrip().endswith(old):
            t.text = t.text.replace(old, new)
            if b.highlight:
                r = t.getparent()
                rpr = r.find(qn("w:rPr"))
                if rpr is None:
                    rpr = OxmlElement("w:rPr")
                    r.insert(0, rpr)
                rpr_shade(rpr)
            break
    else:
        raise SystemExit("cover: version not found")
    for p in cell.iter(qn("w:p")):
        if "Prepared by:" in el_text(p):
            p.addnext(b.para(DOC["cover"]["drafting"], bold=True, size=14, color="014EA9", jc="center"))
            return
    raise SystemExit("cover: 'Prepared by' not found")


def edit_revision_log(b: Builder):
    t = b.tables[1]
    log = DOC["revision_log"]
    for cell, v in zip(t.rows[2].cells, [log["date"], log["version"], " ".join(log["description"].split()), log["by"]]):
        cell.paragraphs[0]._p.append(b.run(v, size=9))
        if b.highlight:
            cell_shade(cell._tc)


def edit_introduction(b: Builder):
    p = find_par(b, "The System Analyst or equivalent BDO ITG personnel")
    p._p.addnext(b.clone_par(find_par(b, "This document once approved")._p, DOC["introduction"]["preparation"]))
    ov = [p for p in b.doc.paragraphs if p.text.strip().startswith("In addition, the platform improves")][0]
    ov._p.addnext(b.clone_par(ov._p, " ".join(DOC["introduction"]["overview"].split())))


def bdoi_mapping_cells(t) -> "OrderedDict[str, object]":
    rows = OrderedDict()
    for row in t.rows[1:]:
        rows[row.cells[0].text.replace("\n", "").strip()] = row
    return rows


def edit_mapping(b: Builder, lines: list[dict], functions, trace: dict, brrn: dict) -> dict:
    t = b.tables[2]
    rows = bdoi_mapping_cells(t)
    b.stats["bdoi_rows"] = len(rows)
    for rid, change in DOC["mapping"].items():
        row = rows[rid]
        cell = row.cells[2]._tc
        before = re.sub(r"\s+", " ", row.cells[2].text.replace("\n", " / ")).strip(" /")
        model = next(p for p in cell.findall(qn("w:p")) if p.find(qn("w:r")) is not None)
        cell.findall(qn("w:p"))[-1].addnext(b.clone_par(model, "Added in v1.1: " + ", ".join(change["add"])))
        b.log.append({"id": rid, "before": before, "after": f"{before}; added in v1.1: {', '.join(change['add'])}",
                      "why": change["why"], "brd": re.sub(r"\s+", " ", row.cells[1].text).strip()[:300]})
    # the main BRD lines, by function, after the BRRN rows
    template = t.rows[-1]._tr
    anchor = template
    lmap = line_items_map()
    head_rows = set()
    for fid, f in functions.items():
        tr = b.clone_row(template, [f"BRD {fid}", f"{f['name']} ({f['persona']})", "Lines below"], font_size=8)
        for tc in tr.findall(qn("w:tc")):
            for r in tc.iter(qn("w:r")):
                rp = r.find(qn("w:rPr"))
                if rp is None:
                    rp = OxmlElement("w:rPr")
                    r.insert(0, rp)
                fonts = rp.find(qn("w:rFonts"))
                (fonts.addnext(OxmlElement("w:b")) if fonts is not None else rp.insert(0, OxmlElement("w:b")))
            if not b.highlight:
                cell_shade(tc)
                tc.find(qn("w:tcPr")).find(qn("w:shd")).set(qn("w:fill"), "E7E6E6")
        anchor.addnext(tr)
        anchor = tr
        head_rows.add(f"BRD {fid}")
        by_id = {r["id"]: r for r in lines}
        for lid in f["ids"]:
            tinfo = trace[lid]
            items = sorted({x for fr in tinfo["frs"] for x in lmap.get(fr, [])}, key=frrn_key) \
                or DOC["out_of_scope"].get(lid, [])
            cell3 = ", ".join(items)
            if tinfo["out"]:
                cell3 = "Out of scope (BRD Addendum 1, p.60); replaced by " + cell3
            tr = b.clone_row(template, [f"BRD {lid}", by_id[lid]["text"], cell3], font_size=8)
            anchor.addnext(tr)
            anchor = tr
            b.stats["leaf_rows"] += 1
    mapping = {}
    for row in t.rows[1:]:
        key = row.cells[0].text.replace("\n", "").strip()
        if key not in head_rows:
            mapping[key] = re.sub(r"\s+", " ", row.cells[2].text).strip()
    ids = list(brrn) + [f"BRD {r['id']}" for r in lines]
    missing = [i for i in ids if i not in mapping]
    unmapped = [i for i in ids if i in mapping and not FRRN_RE.search(mapping[i])]
    if missing or unmapped:
        raise SystemExit(f"mapping check failed: missing {missing[:10]}, unmapped {unmapped[:10]}")
    b.stats["brd_ids"] = len(ids)
    b.stats["brrn"] = len(brrn)
    b.stats["lines"] = len(lines)
    b.stats["out_of_scope"] = sum(1 for r in lines if trace[r["id"]]["out"])
    b.stats["mapping_changes"] = len(DOC["mapping"])
    b.stats["functions"] = len(functions)
    return mapping


def subitem_blocks(cell_tc, seen: Counter | None = None):
    """The items of one cell of BDOI's Functional Requirements table: id -> its elements; a repeated number is keyed
    '<id> (2nd)'."""
    blocks = OrderedDict()
    current = None
    seen = Counter() if seen is None else seen
    for el in cell_tc:
        if el.tag == qn("w:tcPr"):
            continue
        text = el_text(el) if el.tag == qn("w:p") else ""
        m = ITEM_HEAD_RE.match(text)
        bold = el.tag == qn("w:p") and el.find(".//" + qn("w:b")) is not None
        if m and bold and el.find(qn("w:pPr") + "/" + qn("w:numPr")) is None:
            seen[m.group(1)] += 1
            current = m.group(1) + (" (2nd)" if seen[m.group(1)] > 1 else "")
            blocks[current] = [el]
        elif current:
            blocks[current].append(el)
    return blocks


def block_num(block, default=None):
    ids = Counter()
    for el in block:
        n = el.find(qn("w:pPr") + "/" + qn("w:numPr") + "/" + qn("w:numId")) if el.tag == qn("w:p") else None
        lvl = el.find(qn("w:pPr") + "/" + qn("w:numPr") + "/" + qn("w:ilvl")) if el.tag == qn("w:p") else None
        if n is not None and (lvl is None or lvl.get(qn("w:val")) == "0"):
            ids[int(n.get(qn("w:val")))] += 1
    return ids.most_common(1)[0][0] if ids else default


def subitem_elements(b: Builder, sid, title, shall, ac):
    out = [b.para(""), b.para(f"{sid} {title}", bold=True, keep_next=True)]
    if shall:
        out += b.items(shall, b.new_num())
    out += b.acceptance(ac)
    return out


def edit_functional_requirements(b: Builder):
    t = b.tables[3]
    rows = OrderedDict()
    for row in t.rows[1:]:
        rows[row.cells[0].text.strip()] = row
    b.stats["bdoi_entries"] = len(rows)
    all_blocks = OrderedDict()
    seen: Counter = Counter()
    for row in rows.values():
        all_blocks.update(subitem_blocks(row.cells[3]._tc, seen))
    b.stats["bdoi_items"] = len(all_blocks)
    if set(all_blocks) != set(ADD["items"]):
        raise SystemExit(f"items differ: {sorted(set(all_blocks) ^ set(ADD['items']))}")
    b.bdoi_item_ids = list(all_blocks)
    for sid, add in ADD["items"].items():
        block = all_blocks[sid]
        num = block_num(block) or b.new_num()
        els = b.items(add.get("shall") or [], num) + b.acceptance(add["ac"])
        b.insert_after(last_content(block), els)
        b.stats["elaborated"] += 1
        b.stats["with_statements"] += bool(add.get("shall"))
    for ns in ADD["new_subitems"]:
        cell = rows[ns["row"]].cells[3]._tc
        prev = [s for s in all_blocks if s.replace(" (2nd)", "").startswith(ns["row"] + ".")]
        if prev and frrn_key(ns["id"]) <= max(frrn_key(p) for p in prev):
            raise SystemExit(f"{ns['id']} does not continue BDOI's numbering")
        for el in subitem_elements(b, ns["id"], ns["title"], ns["shall"], ns["ac"]):
            cell.append(el)
        b.stats["new_subitems"] += 1
    template = rows["FRRN.044"]._tr
    last = t.rows[-1]._tr
    for item in ADD["new_items"]:
        tr = strip_ids(copy.deepcopy(template))
        tcs = tr.findall(qn("w:tc"))
        for tc in tcs:
            for el in list(tc):
                if el.tag != qn("w:tcPr"):
                    tc.remove(el)
            if b.hl:
                cell_shade(tc)
        tcs[0].append(b.para(item["id"]))
        tcs[1].append(b.para(item["component"]))
        tcs[2].append(b.para(item["capability"]))
        first = True
        for si in item["subitems"]:
            els = subitem_elements(b, si["id"], si["title"], si["shall"], si["ac"])
            for el in (els[1:] if first else els):
                tcs[3].append(el)
            first = False
            b.stats["new_item_subitems"] += 1
        last.addnext(tr)
        last = tr
        b.stats["new_items"] += 1


def edit_signoff(b: Builder):
    cell = b.tables[4].rows[0].cells[0]
    for p in cell.paragraphs:
        if p.text.strip() == "System Analyst":
            new = strip_ids(copy.deepcopy(p._p))
            for r in new.findall(qn("w:r")):
                new.remove(r)
            new.append(b.run(DOC["signoff"]["drafting"]))
            p._p.addnext(new)
            return
    raise SystemExit("sign-off: 'System Analyst' not found")


def edit_annex_rows(b: Builder):
    b.annex_log = []
    for annex, spec in DOC["annex_rows"].items():
        t = b.tables[spec["table"]]
        last = t.rows[-1]._tr
        for vals in spec["rows"]:
            tr = b.clone_row(last, vals)
            last.addnext(tr)
            last = tr
            b.stats["annex_rows"] += 1
        b.annex_log.append((annex, spec))


# ------------------------------------------------------------------------------------------------- the new annexes
class Annexes:
    def __init__(self, b: Builder, mapping: dict, lines, trace: dict, our: dict, brrn: dict):
        self.b = b
        self.mapping = mapping
        self.lines = lines
        self.trace = trace
        self.our = our
        self.brrn = brrn
        self.els: list = []
        self.h1_ppr = find_par(b, "Annex Z", "Heading 1")._p.find(qn("w:pPr"))
        self.screen_titles: dict[str, str] = {}
        self.work = Path(tempfile.mkdtemp(prefix="rnwimg_"))

    def refs(self, text: str) -> str:
        def one(m):
            items = line_items_map().get(m.group(0)) or self.b.fr_to_frum.get(m.group(0))
            return ", ".join(items[:2]) if items else m.group(0)
        return re.sub(r"FR-RN-\d{3}", one, clean(text))

    def frrn_of(self, frs) -> str:
        lm = line_items_map()
        return ", ".join(sorted({x for fr in frs for x in lm.get(fr, [])}, key=frrn_key))

    def h1(self, text):
        p = OxmlElement("w:p")
        ppr = strip_ids(copy.deepcopy(self.h1_ppr))
        for r in ppr.findall(qn("w:rPr")):
            ppr.remove(r)
        if ppr.find(qn("w:pageBreakBefore")) is None:
            ppr_insert(ppr, OxmlElement("w:pageBreakBefore"))
        p.append(ppr)
        p.append(self.b.run(text))
        self.els.append(p)
        self.b.stats["annexes"] += 1

    def h2(self, text):
        self.els.append(self.b.para(text, bold=True, size=11, color="014EA9", keep_next=True, space_after=80))

    def p(self, text, **kw):
        self.els.append(self.b.para(text, **kw))

    def bullets(self, texts):
        n = self.b.new_num()
        for t in texts:
            self.els.append(self.b.para(t, style="ListParagraph", num=(n, 0)))

    def table(self, header, rows, widths, size=8.0):
        self.els.append(self.b.table(header, rows, widths, size=size))
        self.els.append(self.b.para("", space_after=0))

    def kv(self, rows, widths=(1.6, 5.8), size=8.5):
        self.table(["Item", "Detail"], rows, list(widths), size=size)

    def shot(self, png: Path, max_h=4.6):
        from PIL import Image  # noqa: PLC0415
        target = self.work / png.name
        with Image.open(png) as im:
            im = im.convert("RGB")
            if im.width > 1500:
                im = im.resize((1500, int(im.height * 1500 / im.width)))
            im.save(target, optimize=True)
        self.els.append(self.b.picture(target, max_w=6.6, max_h=max_h))

    # AA - screens
    def screens(self, review):
        self.h1("Annex AA – Screen Specifications")
        self.p("This annex specifies the Renewal screens of the system: the purpose of each screen, how the user reaches "
               "it, the FR items it serves, one screenshot, its fields with type, mandatory marker, format, list or "
               "source and validation, and its actions. Mandatory: Y = mandatory, N = optional, Cond. = mandatory when "
               "the condition stated applies. The screenshots were taken on the SIT environment with SIT/UAT data; "
               "the labels of the screens follow the BRD (for example Expiry List for the RMEL) and adopt BDOI's names "
               "once decided (Annex AO). The screen standards (layout, dates dd-Mmm-yyyy, amounts with two decimals, "
               "tables with row action menus) are those of the programme and are not repeated here.")
        k = 0
        for f in sorted((RN / "pack" / "screens").glob("*.yaml")):
            for s in yaml.safe_load(f.read_text(encoding="utf-8"))["screens"]:
                k += 1
                self.screen_titles[s["id"]] = clean(s["title"])
                self.h2(f"AA.{k} {clean(s['title'])} ({s['id']})")
                self.p("Purpose: " + self.refs(" ".join(str(s["purpose"]).split())))
                if s.get("entry"):
                    self.p("Access: " + clean("; ".join(s["entry"])))
                frrn = self.frrn_of(s.get("frs", []))
                if frrn:
                    self.p("FR items: " + frrn)
                shots = s.get("shots") or []
                if shots:
                    n = int(s["id"].split("-")[-1])
                    png = RN / "screenshots" / f"scr-rn-{n:02d}-01-{shots[0]['state']}.png"
                    if not png.exists():
                        raise SystemExit(f"screenshot missing: {png}")
                    self.shot(png)
                    self.p(f"Figure AA.{k}: {clean(shots[0].get('caption', s['title']))}", italic=True, size=8,
                           jc="center")
                    self.b.stats["screenshots"] += 1
                rows = []
                for line in s.get("fields", []):
                    c = [x.strip() for x in str(line).split(" | ")]
                    c += ["-"] * (10 - len(c))
                    label = c[1] if c[0] in ("-", "") else f"{c[0]}: {c[1]}"
                    fmt = "; ".join(x for x in (c[3], c[5]) if x not in ("-", ""))
                    val = "; ".join(x for x in (c[8], c[9]) if x not in ("-", ""))
                    rows.append([clean(label), clean(c[2]), clean(c[4]), self.refs(fmt) or "-", self.refs(val) or "-"])
                if rows:
                    self.table(["Field", "Type", "Mandatory", "Format / list / source", "Validation and message"],
                               rows, [1.6, 0.9, 0.8, 2.3, 1.9], size=7.5)
                acts = []
                for line in s.get("actions", []):
                    c = [x.strip() for x in str(line).split(" | ")]
                    c += ["-"] * (6 - len(c))
                    acts.append([clean(c[0]), clean(c[1]), clean(c[2]), self.refs(c[3]), clean(c[4]),
                                 re.sub(r"\s*\(NT-\d+\)", "", clean(c[5]))])
                if acts:
                    self.table(["Action", "Who", "When", "What happens", "Status after", "Notice"], acts,
                               [1.1, 1.1, 1.1, 2.3, 0.9, 1.0], size=7.5)
                self.b.stats["screens"] += 1
        review.menus(self)

    # AB - messages
    def messages(self):
        self.h1("Annex AB – Messages and Validations")
        self.p("The messages the user sees when a check fails or an action is not allowed, word for word as on the "
               "screens; <name> parts are filled in by the system. BDOI's own messages of section Functional "
               "Requirements prevail where they cover the same case; the messages below complete them.")
        rows, seen = [], set()
        for fr_id, fr in self.our.items():
            frrn = self.frrn_of([fr_id]) or "-"
            screen = clean(str(fr.get("screens", "")).split(";")[0])
            for v in fr.get("validations") or []:
                msg = clean(v[1])
                if not msg or msg in seen or msg == "-":
                    continue
                seen.add(msg)
                rows.append([screen, clean(v[0]), msg, frrn])
        for f in sorted((RN / "pack" / "screens").glob("*.yaml")):
            for s in yaml.safe_load(f.read_text(encoding="utf-8"))["screens"]:
                for line in s.get("fields", []):
                    c = [x.strip() for x in str(line).split(" | ")]
                    if len(c) >= 10 and c[9] not in ("-", ""):
                        for msg in (clean(m) for m in re.split(r";\s+(?=[A-Z])", c[9])):
                            if msg and msg not in seen:
                                seen.add(msg)
                                rows.append([clean(s["title"]), clean(f"{c[1]}: {c[8]}"), msg,
                                             self.frrn_of(s.get("frs", [])) or "-"])
        rows = [[str(i)] + r for i, r in enumerate(rows, 1)]
        self.table(["No.", "Screen", "When", "Message shown", "FR item"], rows, [0.4, 1.5, 2.0, 2.5, 1.1], size=7.5)
        self.b.stats["messages"] = len(rows)

    # AC - business rules
    def rules(self):
        m = DOC["annex_ac"]
        self.h1("Annex AC – Business Rules and Configurable Parameters")
        self.p("The rules that apply across the Renewal requirements, the mapping of BDOI's statuses to the stages of "
               "the system, and the values the Business Administrator maintains without a change to the system. A "
               "change of a rule set, parameter, list or risk code takes effect from its effective date, after a "
               "second authorised user approves it (FRRN.040).")
        self.h2("AC.1 Personas")
        self.table(["Persona", "Responsibilities", "BRD"], m["personas"], [2.0, 4.3, 1.1])
        self.h2("AC.2 Proposed role-to-function matrix")
        self.p(m["matrix_note"])
        self.table(["Function"] + m["matrix_roles"], m["matrix"], [2.6] + [0.6] * len(m["matrix_roles"]), size=7.5)
        self.h2("AC.3 Dispositions and their paths")
        self.table(["Disposition", "Path after posting", "BRD"], m["dispositions"], [1.4, 4.9, 1.1])
        self.p(m["reasons_note"])
        self.h2("AC.4 Sanitation rules of the system and their default effect")
        self.p("The rules of the system that run with BDOI's sanitation rules of Annex C; the severity decides the "
               "classification. KYC is for information only.")
        self.table(["Rule", "BRD", "Default effect when it fails"], m["checks"], [3.0, 1.9, 2.5])
        self.h2("AC.5 BDOI's statuses and the stages of the system")
        self.table(["BDOI status (this FRS)", "Stage of the system", "Meaning"], m["status_map"], [2.1, 2.4, 2.9])
        self.h2("AC.6 Status transitions")
        self.table(["From", "Action", "To", "Who", "Condition"], m["transitions"], [1.4, 1.3, 2.0, 1.4, 1.3], size=7.5)
        self.h2("AC.7 Configurable parameters")
        self.table(["Parameter", "Value as delivered", "BRD", "Value decided by"], m["parameters"],
                   [2.2, 2.6, 1.2, 1.4], size=7.5)
        self.h2("AC.8 Lists of values")
        self.table(["List", "Values"], m["lists"], [2.0, 5.4])
        self.b.stats["parameters"] = len(m["parameters"])
        self.b.stats["lists"] = len(m["lists"])

    # AL - walkthroughs
    def walkthroughs(self):
        cases = yaml.safe_load((RN / "brd06_cases.yaml").read_text(encoding="utf-8"))
        personas = {k: clean(v["name"]) for k, v in cases["personas"].items()}
        data = yaml.safe_load((RN / "pack" / "walkthroughs.yaml").read_text(encoding="utf-8"))["walkthroughs"]
        self.h1("Annex AL – Process Walkthroughs")
        self.p("End-to-end walkthroughs of Renewal, step by step: who acts, on which screen, what the user does, what "
               "the user sees and the result. The screens are specified in Annex AA; the users who run the "
               "walkthrough with the Business Unit are in the Business Unit Walkthrough Users workbook.")
        total = 0
        for w in data:
            self.h2(f"{w['id']} {clean(w['title'])}")
            self.p(self.refs(" ".join(str(w["summary"]).split())))
            if w.get("data"):
                self.p(clean(w["data"]), italic=True, size=9)
            rows = []
            for i, s in enumerate(w["steps"], 1):
                rows.append([f"{w['id']}.{i}", personas.get(s[0], clean(s[0])),
                             f"{self.screen_titles.get(s[1], s[1])} ({s[1]})", self.refs(s[2]), self.refs(s[3]),
                             clean(s[4])])
            total += len(rows)
            self.table(["Step", "Persona", "Screen", "What the user does", "What the user sees", "Result"], rows,
                       [0.6, 1.0, 1.2, 2.0, 1.7, 0.9], size=7.5)
            if w.get("downstream"):
                self.p("Afterwards:", bold=True, keep_next=True)
                self.bullets([self.refs(x) for x in w["downstream"]])
        self.b.stats["walk_steps"] = total
        self.b.stats["walkthroughs"] = len(data)

    # AM - traceability
    def traceability(self):
        cases = test_cases()
        self.h1("Annex AM – Requirements Traceability")
        self.p("AM.1 traces the 42 BRRN requirements of the addenda, and AM.2 the 1,033 requirement lines of the main "
               "BRD (grouped as in the BIBS FRS), to the FR items of this FRS (FRRN), to the reference requirements of "
               "the BIBS FRS BRD-06 Renewal v2.0 (FR-RN) and to the test conditions of the BIBS test plan BRD-06 "
               "(TC-RN-nnn.n). AM.3 lists every reference requirement with the FR items that incorporate it. The test "
               "cases of each FR item, its acceptance criteria included, are in the Test Cases and Traceability "
               "workbook.")
        self.h2("AM.1 BRRN requirements")
        rows = []
        for rid, info in self.brrn.items():
            rows.append([f"{rid} ({info['pages']})", ", ".join(FRRN_RE.findall(self.mapping[rid])),
                         ", ".join(info["frs"]), "; ".join(tc_text(r, cases) for r in info["frs"])])
        self.table(["BRD ID", "FR items (this FRS)", "BIBS reference", "Test conditions"], rows,
                   [1.5, 3.6, 1.5, 3.4], size=7.5)
        n = len(rows)
        self.h2("AM.2 Requirement lines of the main BRD")
        rows = []
        lm = line_items_map()
        for g in trace_groups():
            frrn = ", ".join(sorted({x for fr in g["frs"] for x in lm.get(fr, [])}, key=frrn_key)
                             or sorted({x for i in g["ids"] for x in DOC["out_of_scope"].get(i, [])}))
            ids = g["ids"]
            span = ids[0] if len(ids) == 1 else f"{ids[0]} to {ids[-1]} ({len(ids)} lines)"
            rows.append([clean(g["function"]), span, g["pages"],
                         ("Out of scope (p.60); " if g["out"] else "") + frrn, ", ".join(g["frs"]),
                         "; ".join(tc_text(r, cases) for r in g["frs"])])
        self.table(["BRD function", "Line IDs", "Pages", "FR items (this FRS)", "BIBS reference", "Test conditions"],
                   rows, [1.6, 1.9, 0.8, 2.3, 1.2, 2.2], size=7)
        n += len(rows)
        if sum(len(g["ids"]) for g in trace_groups()) != len(self.lines):
            raise SystemExit("Annex AN.2 does not hold every line of the BRD")
        self.h2("AM.3 BIBS reference requirement to FR items")
        rows = []
        self.listed_refs = set()
        for fr_id, fr in self.our.items():
            frrn = ", ".join(self.b.fr_to_frum.get(fr_id, []))
            if not frrn:
                raise SystemExit(f"{fr_id} is not incorporated in any FRRN item")
            brd = ", ".join(x.split(" (")[0] for x in fr["brd"] if isinstance(x, str))
            rows.append([fr_id, clean(fr["title"]), clean(brd)[:160], frrn, tc_text(fr_id, cases)])
            self.listed_refs.add(fr_id)
        self.table(["Reference", "Title", "BRD", "FR items (this FRS)", "Test conditions"], rows,
                   [0.9, 2.6, 2.4, 2.4, 1.7], size=7)
        self.b.stats["trace_rows"] = n + len(rows)

    # AO - observations
    def observations(self, review):
        cmp_data = yaml.safe_load(CMP.read_text(encoding="utf-8"))
        self.h1("Annex AO – Observations and Points for BDOI Decision")
        self.p("Every point where BDOI's FRS and the BRD of 8-Oct-2026 or the BIBS reference FRS differ, the slips "
               "noticed in BDOI's text, the changes made to the Business Requirements Mapping, the rows added to "
               "BDOI's annexes and the other observations of this version. BDOI's text is kept in the body of the "
               "document; this annex gives both readings, the BRD text, what version 1.1 proposes, the impact and who "
               "decides. Every point starts as Open; a point that BIBS offers both ways, chosen by a system setting, "
               "reads Configurable - BDOI to confirm the setting. The Business Unit records its decision in the "
               "Change Request Register workbook (sheet Decisions log).")
        header = ["No.", "Topic", "BDOI FRS text", "BRD text", "Proposed in v1.1", "Impact", "Decision by", "Status"]
        widths = [0.7, 1.1, 1.9, 1.5, 2.3, 1.0, 1.0, 0.5]
        no = 0
        section = ""

        def row(ref, topic, bdoi, brd, prop, impact, who, status="Open"):
            nonlocal no
            no += 1
            out = [f"AO-{no:02d}" + (f" ({ref})" if ref else ""), topic, bdoi, brd, prop, impact, who, status]
            self.b.q_rows.append(dict(zip(["no", "topic", "bdoi", "brd", "proposal", "impact", "decide_by", "status"],
                                          out), section=section))
            return out
        section = "AO.1"
        self.h2("AO.1 Conflicts between BDOI's FRS, the BRD and the BIBS reference FRS")
        rows = []
        self.cmp_ids = []
        for k, c in enumerate(cmp_data["conflicts"], 1):
            cid = f"C{k:02d}"
            self.cmp_ids.append(cid)
            prop = (f"{clean(c['recommendation'])} (BIBS reference FRS: {clean(c['ours'])} The system today: "
                    f"{clean(c['platform'])}.)")
            if c.get("setting"):
                prop += f" Available in BIBS: {clean(c['setting'])}"
            rows.append(row(cid, f"{clean(c['topic'])} ({c['kind']})", clean(c["bdoi"]), clean(c["brd"]), prop,
                            f"Matches: {clean(c['matches'])}", DOC["conflict_owner"].get(cid, "Product Owner, Renewal"),
                            status=c.get("status", "Open")))
        self.table(header, rows, widths, size=7)
        self.listed_conflicts = set(self.cmp_ids)
        section = "AO.2"
        self.h2("AO.2 Slips noticed in BDOI's FRS (proposed corrections; BDOI's text is not changed)")
        rows = [row(s["ref"], s["topic"], s["bdoi"], "-", s["proposal"], s["impact"], "System Analyst (BDO ITG)")
                for s in DOC["slips"]]
        self.table(header, rows, widths, size=7)
        section = "AO.3"
        self.h2("AO.3 Changes to the Business Requirements Mapping")
        rows = []
        for ch in self.b.log:
            rows.append(row(ch["id"], "Mapping extended", f"Mapped to: {ch['before']}", ch["brd"],
                            f"Mapped to: {ch['after']}. {ch['why']}", "Coverage of the BRD requirement.",
                            "System Analyst (BDO ITG); Product Owner, Renewal"))
        s = self.b.stats
        rows.append(row("", "BRD lines added", f"{s['bdoi_rows']} rows, one per BRRN requirement",
                        f"{s['lines']} requirement lines of the main BRD",
                        f"The {s['lines']} lines are added under the heading of their BRD function ({s['functions']} "
                        f"functions), each with its BRD text and the FRRN items that cover it; {s['out_of_scope']} are "
                        f"out of scope by Addendum 1 (p.60). BDOI's rows are kept as they are.",
                        "Coverage of every BRD ID.", "System Analyst (BDO ITG)"))
        self.table(header, rows, widths, size=7)
        section = "AO.4"
        self.h2("AO.4 Rows added to BDOI's annexes and other observations")
        rows = []
        for annex, spec in self.b.annex_log:
            what = "; ".join(r[0] for r in spec["rows"])
            rows.append(row(f"Annex {annex}", f"Row added to Annex {annex}", f"Annex {annex} as issued", "-",
                            f"Row added: {what}. {spec['why']}", f"Annex {annex}.",
                            "System Analyst (BDO ITG); Product Owner, Renewal"))
        for o in DOC["observations"]:
            rows.append(row("", o["topic"], o["bdoi"], o["brd"], o["proposal"], o["impact"], o["by"]))
        self.table(header, rows, widths, size=7)
        section = "AO.5"
        self.h2("AO.5 Observations of the Business Unit review")
        rows = [row("", o["topic"], o["bdoi"], o["brd"], o["proposal"], o["impact"], o["by"])
                for o in review.REV["observations"]]
        self.table(header, rows, widths, size=7)
        self.b.stats["observations"] = no
        self.b.stats["conflicts"] = len(self.cmp_ids)
        self.b.stats["slips"] = len(DOC["slips"])

    # AR - glossary
    def glossary(self, review):
        self.h1("Annex AR – Glossary")
        self.p("Terms and abbreviations used in this document.")
        terms = sorted(DOC["glossary"] + review.REV["glossary_add"], key=lambda g: g[0].lower())
        self.table(["Term", "Meaning"], terms, [2.0, 5.4], size=9)
        self.b.stats["glossary"] = len(terms)


def section_break(b: Builder, landscape: bool):
    return base.section_break(b, landscape)


def add_annexes(b: Builder, a: Annexes, review, figs):
    new = review.NewAnnexes(a, figs)
    a.screens(new)
    a.messages()
    a.rules()
    new.workflow()
    new.emails()
    new.documents()
    new.reports()
    new.integrations()
    new.nfr()
    new.data_setup()
    a.walkthroughs()
    a.els.append(section_break(b, landscape=False))
    a.traceability()
    new.open_items()
    a.observations(review)
    a.els.append(section_break(b, landscape=True))
    new.change_control()
    new.checklist()
    a.glossary(review)
    final = b.body.find(qn("w:sectPr"))
    for el in a.els:
        final.addprevious(el)
    shutil.rmtree(a.work, ignore_errors=True)


CHANGE_POINTS = [
    "Cover: version 1.1 and \"Drafting support: iorta TechNXT Project Team\"; Document Revision Log: row 1.1; Signoff Sheet: \"iorta TechNXT Project Team (drafting support)\" under Prepared by. BDOI's signatories are unchanged.",
    "Introduction: the Project Overview of the BRD of 8-Oct-2026, the preparation sentence, References, and the Summary for the Business Unit Review (scope on a page, the process from end to end, key numbers, the {decisions} decisions, how to review).",
    "Business Requirements Mapping: FR items added to {mapping_changes} of BDOI's 42 BRRN rows, and the {lines} requirement lines of the main BRD added under their {functions} BRD functions, each with its FRRN items.",
    "Functional Requirements: BDOI's {bdoi_items} items kept word for word and elaborated ({with_statements} with added statements, all with numbered acceptance criteria); {new_subitems} new sub-items; new entries FRRN.045 to FRRN.049 ({new_item_subitems} sub-items); {ac} acceptance criteria in all.",
    "Process Flow: {flows} figures added (process flows by persona, life-cycles of the renewal account, letters, hold cover and uploads); BDOI's flowchart is kept.",
    "Annexes A to Z: rows added to Annexes B, M and V (Annex AP.4).",
    "New annexes: AA Screen Specifications ({screens} screens, menu by persona); AB Messages and Validations ({messages}); AC Business Rules and Configurable Parameters; AD Workflow and Approvals; AE E-mail and Notification Texts ({emails}); AF Document Prints and Output Formats ({documents}); AG Reports and Schedules ({reports} reports, {schedules} runs); AH Integrations ({integrations}); AJ Non-functional Requirements ({nfr_rows}); AK Data Set-up and Migration at Go-live ({data_rows}); AL Process Walkthroughs ({walk_steps} steps); AM Requirements Traceability; AN Assumptions, Dependencies and Open Questions ({open_kept} open, {answered} answered by BDOI's FRS and not repeated); AO Observations and Points for BDOI Decision ({observations}); AP Change Control after Sign-off; AQ Business Unit Review Checklist ({checklist} points); AR Glossary ({glossary} terms).",
]


def change_summary(b: Builder, review):
    s = dict(b.stats)
    s["decisions"] = len(review.REV["summary"]["decisions"])
    s["flows"] = sum(1 for f in review.FLOWS if f["where"] == "flow")
    els = [b.para("Summary of changes in version 1.1", bold=True, size=14, color="014EA9", hl=False, space_after=120),
           b.para("This review copy shows in light yellow every passage added in version 1.1. BDOI's text of version "
                  "1.0 is not shaded and is unchanged; the points for decision are in Annex AO and the open questions "
                  "in Annex AN.", size=9.5, hl=False)]
    n = b.new_num()
    for pt in CHANGE_POINTS:
        els.append(b.para(pt.format(**s), style="ListParagraph", num=(n, 0), size=9.5, hl=False))
    els.append(b.para("The first decisions BDOI must take are listed in the Summary for the Business Unit Review "
                      "(third classification, paid-off and RMU loans, assignment, transfer, extraction and "
                      "initiation).", size=9.5, hl=False))
    els.append(b.para("Shading used in this copy: ", size=9.5, hl=False))
    els[-1].append(b.run("text added in version 1.1", size=9.5, hl=True))
    brk = OxmlElement("w:p")
    ppr = OxmlElement("w:pPr")
    ppr.append(copy.deepcopy(b.doc.sections[0]._sectPr))
    brk.append(ppr)
    els.append(brk)
    first = b.body[0]
    for e in els:
        first.addprevious(e)


# ------------------------------------------------------------------------------------------------------- self-checks
def check_bdoi_text(path: Path):
    """BDOI's FRRN IDs and texts are all still present, in order, in the same table cells, and every body paragraph
    of the source is still there, in order."""
    src = docx.Document(str(REPO / DOC["meta"]["source"]))
    new = docx.Document(str(path))
    ids_src = Counter(FRRN_RE.findall("\n".join(texts_of(src.element.body))))
    ids_new = Counter(FRRN_RE.findall("\n".join(texts_of(new.element.body))))
    missing = [i for i, n in ids_src.items() if ids_new[i] < n]
    problems = [f"FRRN ID missing: {missing}"] if missing else []
    new_tables = list(new.tables)
    j = 0
    old_v, new_v = "v" + DOC["meta"]["version_old"], "v" + DOC["meta"]["version_new"]
    for ti, ts in enumerate(src.tables):
        key = [x.replace(old_v, new_v) for x in texts_of(ts._tbl)[:1]]
        while j < len(new_tables) and texts_of(new_tables[j]._tbl)[:1] != key:
            j += 1
        if j >= len(new_tables):
            problems.append(f"table {ti} of the source not found")
            break
        tn = new_tables[j]
        j += 1
        new_rows = list(tn.rows)
        k = 0
        for ri, row in enumerate(ts.rows):
            first = [x.replace(old_v, new_v) for x in texts_of(row.cells[0]._tc)]
            while k < len(new_rows) and not subsequence(first, texts_of(new_rows[k].cells[0]._tc)):
                k += 1
            if k >= len(new_rows):
                problems.append(f"table {ti} row {ri}: not found")
                break
            for ci, cell in enumerate(row.cells):
                old = [x.replace(old_v, new_v) for x in texts_of(cell._tc)]
                it = iter(texts_of(new_rows[k].cells[ci]._tc))
                if not all(any(o == n for n in it) for o in old):
                    problems.append(f"table {ti} row {ri} cell {ci}: BDOI text changed")
            k += 1
    body_src = [p.text.strip() for p in src.paragraphs if p.text.strip()]
    body_new = [p.text.strip() for p in new.paragraphs if p.text.strip()]
    it = iter(body_new)
    lost = [o for o in body_src if not any(o == n for n in it)]
    if lost:
        problems.append(f"body paragraphs changed: {lost[:3]}")
    if problems:
        raise SystemExit("BDOI text check failed:\n  " + "\n  ".join(problems))
    return sum(ids_src.values())


def check_words(b: Builder):
    hits = set()
    for t in b.added_text:
        for m in list(RESTRICTED.finditer(t)) + list(RESTRICTED_CS.finditer(t)):
            if m.group(0).lower() == "built" and re.search(r"Billing Built in|Built-In", t, re.I):
                continue
            hits.add(m.group(0))
        for m in CODE_RE.finditer(t):
            hits.add(m.group(0))
    allowed = {"HOLD_COVER_", "RMEL_", "LAMD", "HOME_CLPC", "MTR_"}
    hits = {h for h in hits if not any(h.startswith(a) for a in allowed) and h not in DOC["allowed_codes"]}
    if hits:
        raise SystemExit(f"restricted words or internal codes in the added text: {sorted(hits)}")


# ----------------------------------------------------------------------------------------------------------- main
def build(highlight: bool, our, lines, functions, trace, brrn, figs: dict):
    import brd06_review_edition as review  # noqa: PLC0415
    b = Builder(highlight)
    unknown = [r for r in b.fr_to_frum if r not in our]
    if unknown:
        raise SystemExit(f"unknown BIBS references: {unknown}")
    where = review.classify_items()
    b.stats["classified"] = review.check_classification(where)
    b.stats["emails_checked"] = review.check_emails()
    edit_cover(b)
    edit_revision_log(b)
    edit_introduction(b)
    mapping = edit_mapping(b, lines, functions, trace, brrn)
    edit_functional_requirements(b)
    edit_signoff(b)
    edit_annex_rows(b)
    review.add_process_flows(b, figs)
    a = Annexes(b, mapping, lines, trace, our, brrn)
    a.where = where
    add_annexes(b, a, review, figs)
    missing_fr = [f for f in our if f not in a.listed_refs]
    if missing_fr:
        raise SystemExit(f"Annex AM lacks {missing_fr}")
    cmp_n = len(yaml.safe_load(CMP.read_text(encoding="utf-8"))["conflicts"])
    if len(a.listed_conflicts) != cmp_n:
        raise SystemExit("Annex AO lacks conflicts of the comparison")
    b.key_numbers = review.key_numbers(b)
    review.add_summary(b, figs)
    if highlight:
        change_summary(b, review)
    set_update_fields(b)
    check_words(b)
    b.mapping = mapping
    b.where = where
    return b


def sources():
    our = our_frs()
    lines = brd06_brd.lines()
    functions = brd06_brd.functions(lines)
    trace = brd06_brd.frs_trace()
    brrn = brd06_brd.brrn_trace()
    brd06_brd.check()
    return our, lines, functions, trace, brrn


def build_review(figs_dir: Path | None = None) -> Builder:
    """The clean copy built in memory (not saved): the numbers, Annex AO rows and lists the workbooks use."""
    import brd06_review_edition as review  # noqa: PLC0415
    our, lines, functions, trace, brrn = sources()
    work = Path(tempfile.mkdtemp(prefix="rnwfig_")) if figs_dir is None else figs_dir
    try:
        figs = review.render_figures(work)
        b = build(False, our, lines, functions, trace, brrn, figs)
    finally:
        if figs_dir is None:
            shutil.rmtree(work, ignore_errors=True)
    b.lines, b.trace, b.our, b.brrn, b.functions = lines, trace, our, brrn, functions
    return b


def main(argv=None) -> int:
    import brd06_review_edition as review  # noqa: PLC0415
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF")
    args = ap.parse_args(argv)
    our, lines, functions, trace, brrn = sources()
    OUT.mkdir(parents=True, exist_ok=True)
    work = Path(tempfile.mkdtemp(prefix="rnwfig_"))
    results = {}
    try:
        figs = review.render_figures(work)
        for highlight, name in ((False, DOC["meta"]["clean"]), (True, DOC["meta"]["highlighted"])):
            b = build(highlight, our, lines, functions, trace, brrn, figs)
            path = OUT / name
            b.doc.save(str(path))
            n_ids = check_bdoi_text(path)
            results[name] = (b, n_ids)
            print(f"wrote {path.relative_to(REPO)} ({path.stat().st_size // 1024} KB)")
    finally:
        shutil.rmtree(work, ignore_errors=True)
    b, n_ids = results[DOC["meta"]["clean"]]
    if not args.no_pdf:
        pdf = OUT / DOC["meta"]["clean"].replace(".docx", ".pdf")
        to_pdf(OUT / DOC["meta"]["clean"], pdf)
        pages = subprocess.run(["pdfinfo", str(pdf)], capture_output=True, text=True).stdout
        print(f"wrote {pdf.relative_to(REPO)} ({pdf.stat().st_size // 1024} KB); "
              + next((x for x in pages.splitlines() if x.startswith("Pages")), ""))
    s = b.stats
    print(f"BRD IDs mapped: {s['brd_ids']} ({s['brrn']} BRRN, {s['lines']} lines of {s['functions']} functions, "
          f"{s['out_of_scope']} out of scope); {s['mapping_changes']} BDOI rows given FRRN items")
    print(f"FRRN: {s['bdoi_entries']} entries and {s['bdoi_items']} items of BDOI kept ({n_ids} FRRN IDs checked); "
          f"{s['elaborated']} items elaborated ({s['with_statements']} with statements); {s['new_subitems']} new "
          f"sub-items; {s['new_items']} new entries ({s['new_item_subitems']} sub-items); {s['requirements_added']} "
          f"statements and {s['ac']} acceptance criteria")
    print(f"Figures {s['figures_added']}; annexes {s['annexes']}: AA {s['screens']} screens; AB {s['messages']} "
          f"messages; AE {s['emails']} texts ({s['emails_checked']} checked); AF {s['documents']}; AG {s['reports']} "
          f"reports, {s['schedules']} runs; AH {s['integrations']}; AJ {s['nfr_rows']}; AK {s['data_rows']}; "
          f"AL {s['walk_steps']} steps; AM {s['trace_rows']} rows; AN {s['open_kept']} open, {s['answered']} answered, "
          f"{s['assumptions']} assumptions, {s['dependencies']} dependencies; AO {s['observations']} observations "
          f"({s['conflicts']} conflicts, {s['slips']} slips); AQ {s['checklist']}; AR {s['glossary']}; "
          f"{s['classified']} references classified")
    for row in b.key_numbers:
        print("  ", " | ".join(row))
    return 0


if __name__ == "__main__":
    sys.exit(main())
