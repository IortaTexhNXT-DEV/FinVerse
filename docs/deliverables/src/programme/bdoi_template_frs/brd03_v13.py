"""Version 1.3 of the Product Maintenance FRS in BDOI's template (used by build_bdoi_frs_pm.py).

  * the swimlane process flows: the simplified flow replaces the persona chart of section 1.4.2 (Figure 1) and the
    end-to-end flow opens section 4 (Figure 2); both drawn by flows/brd03 (spec, generator, renderer, PNG);
  * section 1.5 'How to Read this Document' with the colour legend;
  * the colour of the points that are not BRD requirements (green: available in BIBS; orange: change request);
  * Annex Y: change requests with their effort, points beyond the BRD available in BIBS, the check of the BRD lines;
  * rows added to Annex T (BRD non-functional lines and programme NFRs 10.07 and 4.03) and to Annex X;
  * the Document Revision Log row, the Introduction note and the change summary of the review copy (only the
    changes of version 1.3 are shaded there).
The content is in brd03_v13.yaml.
"""

from __future__ import annotations

import contextlib
import copy
import sys
from collections import Counter
from pathlib import Path

import yaml
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

HERE = Path(__file__).resolve().parent
V13 = yaml.safe_load((HERE / "brd03_v13.yaml").read_text(encoding="utf-8"))
sys.path.insert(0, str(HERE / "flows" / "brd03"))
import render as flows  # noqa: E402

GREEN, ORANGE = "2E7D32", "E65100"
CORE = None
V12M = None
RPR_AFTER_COLOR = ["spacing", "w", "kern", "position", "sz", "szCs", "highlight", "u", "effect", "bdr", "shd",
                   "fitText", "vertAlign", "rtl", "cs", "em", "lang", "eastAsianLayout", "specVanish", "oMath"]


def bind(core, v12) -> None:
    global CORE, V12M
    CORE, V12M = core, v12


def man_days(cr: dict) -> float:
    return round(sum(cr["md"]), 1)


def total_days() -> float:
    return round(sum(man_days(c) for c in V13["crs"]), 1)


@contextlib.contextmanager
def this_round(b):
    """Passages written inside are the additions of version 1.3: shaded in the review copy."""
    old = b.hl
    b.hl = b.highlight
    try:
        yield
    finally:
        b.hl = old


# ============================================================================================== before the build
def begin(b) -> None:
    """Version 1.2 content is no longer shaded; the rows of this round join Annex T and Annex X."""
    V12M.SHADE = False
    nfr = V12M.V12["nfr"]["items"]
    for row in V13["nfr_add"]:
        if row[0] not in {r[0] for r in nfr}:
            nfr.append(row)
    items = V12M.V12["checklist"]["items"]
    for row in V13["checklist_add"]:
        if row not in items:
            items.append(row)


def revision(b) -> None:
    t = b.doc.tables[1]
    last = t.rows[-1]._tr
    new = CORE.strip_ids(copy.deepcopy(last))
    for p in new.iter(qn("w:p")):
        for r in p.findall(qn("w:r")):
            p.remove(r)
    last.addnext(new)
    row = t.rows[-1]
    log = V13["revision"]
    with this_round(b):
        for cell, v in zip(row.cells, [log[k] for k in ("date", "version", "description", "by")]):
            cell.paragraphs[0]._p.append(b.run(v, size=9))


def introduction(b) -> None:
    p = CORE.find_par(b, "Version 1.2 adds")
    with this_round(b):
        p._p.addnext(CORE.clone_par(b, p._p, V13["intro_note"]))


# ============================================================================================== figures
def _picture(b, name: str, max_h: float):
    return b.picture(flows.render_png(name), max_w=6.7, max_h=max_h)


def summary_figure(b) -> None:
    """The simplified swimlane flow replaces the persona chart of section 1.4.2 (Figure 1)."""
    f = V13["figures"]["simple"]
    cap = CORE.find_par(b, "Figure 1 –")._p
    pic = cap.getprevious()
    if pic.find(".//" + qn("w:drawing")) is None:
        raise SystemExit("Figure 1: picture not found before its caption")
    lead = CORE.find_par(b, "Each column is a persona")._p
    with this_round(b):
        lead.addprevious(b.para(f["lead"]))
        lead.getparent().remove(lead)
        pic.addprevious(_picture(b, f["spec"], 5.6))
        pic.getparent().remove(pic)
        cap.addprevious(V12M.caption(b, f"Figure 1 – {f['caption']}"))
        cap.getparent().remove(cap)


def e2e_figure(b) -> None:
    """The end-to-end flow opens section 4 (its caption, Figure 2, is written with the others by v12)."""
    f = V13["figures"]["e2e"]
    h1 = CORE.find_par(b, "Process Flow", "Heading 1")._p
    model = CORE.find_par(b, "New Request Process Flow")._p
    head = CORE.strip_ids(copy.deepcopy(model))
    for r in head.findall(qn("w:r")):
        head.remove(r)
    for pb in head.iter(qn("w:pageBreakBefore")):
        pb.getparent().remove(pb)
    with this_round(b):
        head.append(b.run(f["title"]))
        pic = _picture(b, f["spec"], 6.4)
        lead = b.para(f["lead"])
    h1.addnext(head)
    head.addnext(pic)
    b.e2e_lead = lead           # placed after the caption once the captions are written


def after_flows(b) -> None:
    cap = CORE.find_par(b, f"Figure 2 – {V13['figures']['e2e']['caption']}")._p
    cap.addnext(b.e2e_lead)


# ============================================================================================== section 1.5
def how_to_read(b) -> None:
    h = V13["how_to_read"]
    anchor = CORE.find_par(b, "Business Requirements Mapping", "Heading 1")._p
    with this_round(b):
        els = [V12M.heading(b, h["title"], 2), b.para(h["intro"], keep_next=True)]
        tbl = b.table(["Colour", "Meaning", "Cost"], [[x[1], x[2], x[3]] for x in h["legend"]], [1.2, 4.3, 1.4],
                      size=8.5)
        colours = {"green": GREEN, "orange": ORANGE}
        for tr, x in zip(tbl.findall(qn("w:tr"))[1:], h["legend"]):
            if x[0] in colours:
                _colour(tr.findall(qn("w:tc"))[0], colours[x[0]], bold=True)
        els += [tbl, b.para("", space_after=0), b.para(h["after"])]
        n_cr, n_g = len(V13["crs"]), sum(1 for p in V13["points"] if p["class"] == "ootb")
        els.append(b.para(f"In this version: {n_cr} change requests ({total_days():g} man-days in all) and {n_g} "
                          "points available in BIBS beyond the BRD; every line of the BRD is covered (Annex Y.4)."))
    for e in els:
        anchor.addprevious(e)


# ============================================================================================== colour
def _colour(el, colour: str, bold: bool = False) -> int:
    n = 0
    for r in el.iter(qn("w:r")):
        if r.find(qn("w:t")) is None:
            continue
        rpr = r.find(qn("w:rPr"))
        if rpr is None:
            rpr = OxmlElement("w:rPr")
            r.insert(0, rpr)
        for c in rpr.findall(qn("w:color")):
            rpr.remove(c)
        c = OxmlElement("w:color")
        c.set(qn("w:val"), colour)
        if bold and rpr.find(qn("w:b")) is None:
            rpr.insert(1 if rpr.find(qn("w:rFonts")) is not None else 0, OxmlElement("w:b"))
        for child in rpr:
            if child.tag.split("}")[1] in RPR_AFTER_COLOR:
                child.addprevious(c)
                break
        else:
            rpr.append(c)
        n += 1
    return n


def _ancestor(el, tag):
    while el is not None and el.tag != qn(tag):
        el = el.getparent()
    return el


def colour_points(b) -> Counter:
    found: Counter = Counter()
    for p in list(b.body.iter(qn("w:p"))):
        text = V12M._text(p).strip()
        if not text:
            continue
        for pt in V13["points"]:
            hit = text == pt["match"] if pt.get("exact") else pt["match"] in text
            if not hit:
                continue
            target = _ancestor(p, "w:tr") if pt.get("scope") == "row" else p
            if target is None:
                continue
            _colour(target, GREEN if pt["class"] == "ootb" else ORANGE)
            found[pt["id"]] += 1
    missing = [pt["id"] for pt in V13["points"] if not found[pt["id"]]]
    if missing:
        raise SystemExit(f"colour points not found in the document: {missing}")
    return found


# ============================================================================================== Annex Y
def annex_y(a) -> None:
    b = a.b
    with this_round(b):
        a.h1("Annex Y – Points beyond the BRD and Change Requests")
        a.p("This annex lists what this FRS states beyond the Product Maintenance BRD: the change requests (orange in "
            "the text) with their effort, and the points that BIBS already provides (green in the text). It ends "
            "with the check of every line of the BRD outside its requirement table. The legend is in section 1.5. "
            "The programme-wide non-functional requirements of the other BDOI addenda (Collections NFR 10.07; "
            "Claims, Collections and Reinsurance NFR 4.03) are BRD requirements, not change requests.")
        a.h2("Y.1 How the points were classified")
        a.bullets([
            "Every point added in versions 1.1 to 1.3 was compared with the BRD (BRPM.001 to BRPM.024, PMADD01 to "
            "PMADD08, the summary, envisioned process, key capabilities and non-functional table) and with the "
            "programme NFRs of the BDOI addenda.",
            "A point not in the BRD was checked in the BIBS platform (screens, services and scheduled runs) and in "
            "the pack fit-gap workbook: available = green; not available, or available only in part = orange "
            "(change request).",
            "Effort is in man-days of 8 hours, estimated from the size of the platform components each change touches, "
            "with a range of plus or minus 20%; the detail, the assumptions and the cost (rate cell) are in the "
            "workbook BIBS_CR_BRD-03_Effort_Estimate_v1.0.xlsx. The change requests are also in the change "
            "request register (status 'Candidate - for decision').",
        ])
        a.h2("Y.2 Change requests (orange)")
        rows = [[c["cr"], c["title"], c["frs"], c["brd"], c["ootb"], f"{man_days(c):g}", c["complexity"],
                 c["priority"]] for c in V13["crs"]]
        rows.append(["Total", f"{len(V13['crs'])} change requests", "", "", "", f"{total_days():g}", "", ""])
        a.table(["CR No.", "Change", "FRS reference", "BRD reference", "In BIBS today", "Man-days", "Complexity",
                 "Priority"], rows, [0.75, 1.45, 1.05, 0.85, 1.85, 0.55, 0.65, 0.55], size=7.5)
        for tr in a.els[-2].findall(qn("w:tr"))[1:]:
            _colour(tr, ORANGE)
        a.h2("Y.3 Points beyond the BRD available in BIBS (green)")
        rows = [[p["id"], p["title"], p["frs"], p["where"], "Available; no cost"] for p in V13["points"]
                if p["class"] == "ootb"]
        a.table(["No.", "Point", "FRS reference", "Where in BIBS", "Status"], rows, [0.5, 2.6, 1.0, 1.9, 1.2],
                size=8)
        for tr in a.els[-2].findall(qn("w:tr"))[1:]:
            _colour(tr, GREEN)
        a.h2("Y.4 Check of the BRD lines outside the requirement table")
        a.p("Every requirement ID of the BRD is mapped in section 2 (Business Requirements Mapping) and traced to its "
            "test cases in the traceability workbook. The lines below are the other statements of the BRD and the "
            "programme NFRs; the gaps found were fixed in this version.")
        a.table(["BRD part", "BRD line", "Where in this FRS", "Result"], V13["brd_lines"], [1.3, 2.9, 2.2, 0.8],
                size=8)
    b.stats["crs"] = len(V13["crs"])
    b.stats["green"] = sum(1 for p in V13["points"] if p["class"] == "ootb")
    b.stats["brd_gaps"] = sum(1 for r in V13["brd_lines"] if r[3] == "Gap fixed")


# ============================================================================================== review copy
def change_summary(b) -> None:
    s = b.stats
    points = [
        "Cover and Document Revision Log: version 1.3; the entries of versions 1.0 to 1.2 unchanged.",
        "Introduction: a note on what version 1.3 adds; section 1.4.2 shows the simplified swimlane process flow "
        "(Figure 1); new section 1.5 'How to Read this Document' with the colour legend.",
        "Process Flow: the end-to-end process flow by actor and system opens section 4 (Figure 2), with the change "
        "requests tagged; the figures after it are numbered from Figure 3.",
        f"Colour of the points not in the BRD: {s['green']} points in green (available in BIBS) and the "
        f"{s['crs']} change requests in orange where they are stated (FRPM items, Annexes M, P and T).",
        f"Annex T: rows T-18 to T-21 for the BRD lines and programme NFRs not yet listed ({s['brd_gaps']} gaps "
        "found in the BRD check). Annex X: one checklist point for the points beyond the BRD.",
        f"New Annex Y: change requests with their effort ({total_days():g} man-days), points beyond the BRD "
        "available in BIBS, check of the BRD lines. New workbook BIBS_CR_BRD-03_Effort_Estimate_v1.0.xlsx; the "
        "change request register lists the change requests.",
    ]
    els = [b.para("Version 1.3 – summary of changes", bold=True, size=14, color="014EA9", hl=False, space_after=120),
           b.para("This review copy is the Product Maintenance FRS with the changes of version 1.3. The passages "
                  "added in version 1.3 are shaded light yellow; BDOI's text and the content of versions 1.1 and 1.2 "
                  "are not shaded. The clean copy has the same content without shading and without this page.",
                  size=9.5, hl=False)]
    n = b.new_num()
    for pt in points:
        els.append(b.para(pt, style="ListParagraph", num=(n, 0), size=9.5, hl=False))
    els.append(b.para("Shading used in this copy: ", size=9.5, hl=False))
    els[-1].append(b.run("text added in version 1.3", size=9.5, hl=True))
    brk = OxmlElement("w:p")
    ppr = OxmlElement("w:pPr")
    ppr.append(copy.deepcopy(b.doc.sections[0]._sectPr))
    brk.append(ppr)
    els.append(brk)
    first = b.body[0]
    for e in els:
        first.addprevious(e)


def self_checks(b) -> list[str]:
    text = " ".join(t.text or "" for t in b.body.iter(qn("w:t")))
    out = []
    for need in ("Figure 1 – " + V13["figures"]["simple"]["caption"],
                 "Figure 2 – " + V13["figures"]["e2e"]["caption"], "Annex Y – Points beyond the BRD",
                 "BIBS DEV environment: https://dev.brokerverse.cloud"):
        if need not in text:
            out.append(f"missing: {need}")
    ids = [c["cr"] for c in V13["crs"]]
    if len(set(ids)) != len(ids):
        out.append("duplicate CR numbers")
    for c in V13["crs"]:
        if c["cr"] not in text:
            out.append(f"{c['no']} not in the document")
    return out
