"""The requirement lines of the Renewal BRD (BRD-06) as printed in the BRD pack of 8-Oct-2026.

    python docs/deliverables/src/programme/bdoi_template_frs/brd06_brd.py      # prints the counts

Shared by the BDOI-format FRS of Renewal (build_brd06_bdoi_frs.py) and its review workbooks
(build_brd06_workbooks.py). The BRD pack (docs/source-documents/Renewal (RN) BRD.pdf, 215 pages: the Walkthrough
addendum, the Workshop addendum, Addendum 1 and the main BRD "RMEL Phase 2 - Online Dispositioning") holds:

  * the 42 BRRN requirements of the addenda (BRRN.001-042); their text is BDOI's text of the Business Requirements
    Mapping of BDOI's FRS, which quotes the addenda;
  * the 1,033 line IDs of the main BRD (BRD 1.001.1 to BRD 6.002.2.25), read from the requirement tables of the PDF
    with their requirement text (the column "Business Requirement"). Two IDs break over a page and are completed here.

The table reading takes a minute; the result is kept in the system temporary folder, keyed by the size and date of the
PDF, so that the FRS and the workbooks read the same lines.
"""

from __future__ import annotations

import json
import re
import sys
import tempfile
from collections import OrderedDict
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
BRD_PDF = REPO / "docs" / "source-documents" / "Renewal (RN) BRD.pdf"
FRS_MD = REPO / "docs" / "deliverables" / "src" / "BRD-06_Renewal" / "FRS_BRD06_RENEWAL.md"

# Lines whose row breaks over a page of the PDF, or whose text the table reading misses: the text as printed.
SPLIT_LINES = {
    "1.010.7.1": "1. Encrypt and add password prior to sending",
    "1.011.1": "a. Override Accounts with Outstanding balance",
    "3.004.5": "e. Edit disposition on accounts tagged as “Not for Renewal” (i.e. account was initially tagged "
               "as “Not for Renewal” then client later on decided to renew, etc.)",
}
PERSONAS = OrderedDict([
    ("1", "Marketing Team Leader"), ("2", "Marketing AO / Admin (Account Broker)"), ("3", "Processing Team Leader"),
    ("4", "Processing Officer / Broker"), ("5", "Business Administrator"), ("6", "System Administrator")])


def _key(rid: str) -> list[int]:
    return [int(x) for x in rid.split(".")]


def _read_pdf() -> list[dict]:
    import pdfplumber  # noqa: PLC0415
    out: list[dict] = []
    seen: set[str] = set()
    with pdfplumber.open(str(BRD_PDF)) as pdf:
        for pno, page in enumerate(pdf.pages, 1):
            if pno < 61:                         # the addenda; the out-of-scope list of p.60 repeats main-BRD IDs
                continue
            for table in page.extract_tables():
                for row in table:
                    cells = [re.sub(r"\s+", " ", (c or "")).strip() for c in row]
                    if not cells or not re.match(r"^BRD\s*\d", cells[0]) or len(cells) < 5:
                        continue
                    rid = re.sub(r"\s+", "", cells[0].replace("BRD", ""))
                    if rid in seen:
                        continue
                    seen.add(rid)
                    out.append({"id": rid, "page": pno, "function": cells[2], "text": cells[3],
                                "priority": cells[-1]})
    return out


def lines() -> list[dict]:
    """Every line ID of the main BRD in BRD order: id (as 1.003.3.1), page, function heading printed on the first
    row of a function, requirement text."""
    stat = BRD_PDF.stat()
    cache = Path(tempfile.gettempdir()) / f"brd06_lines_{stat.st_size}_{int(stat.st_mtime)}.json"
    if cache.exists():
        rows = json.loads(cache.read_text(encoding="utf-8"))
    else:
        rows = _read_pdf()
        cache.write_text(json.dumps(rows), encoding="utf-8")
    have = {r["id"] for r in rows}
    for rid, text in SPLIT_LINES.items():
        if rid not in have:
            rows.append({"id": rid, "page": 0, "function": "", "text": text, "priority": "must have"})
    rows.sort(key=lambda r: _key(r["id"]))
    for r in rows:
        r["text"] = r["text"] or SPLIT_LINES.get(r["id"], "")
    return rows


def frs_trace() -> "OrderedDict[str, dict]":
    """The trace of the BIBS FRS BRD-06 (section 11.2): line ID -> BRD function, pages, FR-RN IDs (OUT for the IDs that
    Addendum 1 takes out of scope)."""
    text = FRS_MD.read_text(encoding="utf-8")
    part = text.split("## Main BRD line IDs", 1)[1].split("## Out-of-scope IDs", 1)[0]
    out: "OrderedDict[str, dict]" = OrderedDict()
    function = ""
    for line in part.splitlines():
        if not line.startswith("|") or line.startswith("|---"):
            continue
        c = [x.strip() for x in line.strip().strip("|").split("|")]
        if len(c) < 4 or not re.match(r"\d", c[1] or ""):
            continue
        function = c[0] or function
        frs = re.findall(r"FR-RN-\d{3}", c[3])
        for lid in (x.strip() for x in c[1].split(",")):
            if lid:
                out[lid] = {"function": function, "pages": c[2], "frs": frs, "out": c[3].startswith("OUT")}
    return out


def brrn_trace() -> "OrderedDict[str, dict]":
    """The trace of the BIBS FRS BRD-06 (section 11.1): BRRN ID -> pages, FR-RN IDs."""
    text = FRS_MD.read_text(encoding="utf-8")
    part = text.split("## BRRN requirements", 1)[1].split("## Main BRD line IDs", 1)[0]
    out: "OrderedDict[str, dict]" = OrderedDict()
    for line in part.splitlines():
        m = re.match(r"\| (BRRN\.\d{3}) \(([^)]*)\) \| ([^|]+)\|", line)
        if m:
            out[m.group(1)] = {"pages": m.group(2), "frs": re.findall(r"FR-RN-\d{3}", m.group(3))}
    return out


def functions(rows: list[dict] | None = None) -> "OrderedDict[str, dict]":
    """The BRD functions (1.003, 2.004 ...) in BRD order with their name (from the BIBS FRS trace), persona and IDs."""
    rows = rows or lines()
    trace = frs_trace()
    out: "OrderedDict[str, dict]" = OrderedDict()
    for r in rows:
        fid = ".".join(r["id"].split(".")[:2])
        name = trace.get(r["id"], {}).get("function", "")
        f = out.setdefault(fid, {"id": fid, "name": "", "persona": PERSONAS.get(fid.split(".")[0], ""), "ids": []})
        if name and not f["name"]:
            f["name"] = re.sub(r"^\d\.\d{2,3}\s+", "", name)
        f["ids"].append(r["id"])
    return out


def check() -> dict:
    rows = lines()
    trace = frs_trace()
    ids = [r["id"] for r in rows]
    if len(ids) != len(set(ids)):
        raise SystemExit("repeated BRD line IDs")
    missing = sorted(set(trace) - set(ids), key=_key)
    extra = sorted(set(ids) - set(trace), key=_key)
    if missing or extra:
        raise SystemExit(f"BRD lines differ from the BIBS FRS trace: missing {missing[:10]}, not traced {extra[:10]}")
    empty = [r["id"] for r in rows if not r["text"]]
    if empty:
        raise SystemExit(f"BRD lines without text: {empty[:10]}")
    return {"lines": len(rows), "functions": len(functions(rows)), "out_of_scope": sum(1 for t in trace.values()
                                                                                     if t["out"]),
            "brrn": len(brrn_trace())}


if __name__ == "__main__":
    print(check())
    sys.exit(0)
