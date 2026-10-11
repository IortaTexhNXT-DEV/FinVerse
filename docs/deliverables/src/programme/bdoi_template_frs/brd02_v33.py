"""Version 3.3 of the Operations Cashiering FRS in BDOI's template (used by build_brd02_bdoi_frs.py).

  * the swimlane process flows (flows/brd02): the simplified flow is Figure 1 of section 1.4 and the end-to-end flow
    opens Appendix C (Figure 2);
  * section 1.5 'How to Read this Document' with the colour legend;
  * the colour of the points that are not BRD requirements (green: available in the iNXT BrokerVerse OOTB edition; orange: change request);
  * Appendix W: change requests with their effort, points beyond the BRD available OOTB in iNXT BrokerVerse, the BRD line check;
  * Appendix O rows (BRD maintenance and disaster lines, programme NFRs 10.07 and 4.03; peak hours of the BRD) and a
    checklist point; the revision row, the Introduction note and the change summary of the review copy (only the
    changes of version 3.3 are shaded there).
The content is in brd02_v33.yaml.
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
V33 = DATA = yaml.safe_load((HERE / "brd02_v33.yaml").read_text(encoding="utf-8"))
sys.path.insert(0, str(HERE / "flows"))
import render as flows  # noqa: E402

from colour_code import COLOURS  # noqa: E402  (the shared colours of the BDOI-template FRS)

GREEN, ORANGE = COLOURS["green"], COLOURS["orange"]
RPR_AFTER_COLOR = ["spacing", "w", "kern", "position", "sz", "szCs", "highlight", "u", "effect", "bdr", "shd",
                   "fitText", "vertAlign", "rtl", "cs", "em", "lang", "eastAsianLayout", "specVanish", "oMath"]
M = None


def bind(core) -> None:
    """Called by the builder: the content of this round joins the figure list, Appendix O and the checklist."""
    global M
    M = core
    f = V33["figures"]
    for spec in core.FLOWS:
        if spec["id"] == "brd02_fig_e2e":
            spec.update(png=f["simple"]["png"], caption=f["simple"]["caption"], read=f["simple"]["read"],
                        brd=f["simple"]["brd"])
    if not any(s["id"] == f["e2e"]["id"] for s in core.FLOWS):
        pos = next(i for i, s in enumerate(core.FLOWS) if s["where"] == "C")
        core.FLOWS.insert(pos, dict(f["e2e"], where="C", kind="swimlane"))
    rows = core.REV["nfr"]["rows"]
    for i, r in enumerate(rows):
        if r[0] in V33["nfr_fix"]:
            rows[i] = V33["nfr_fix"][r[0]]
    for r in V33["nfr_add"]:
        if r[0] not in {x[0] for x in rows}:
            rows.append(r)
    point = ["Points beyond the BRD and change requests", "1.5; Appendix W",
             "Keep or drop each green point; approve, defer or reject each orange change request",
             "Product Owner; Cashiering Process Owner"]
    if point not in core.REV["checklist"]["rows"]:
        core.REV["checklist"]["rows"].append(point)


def man_days(cr: dict) -> float:
    return round(sum(cr["md"]), 1)


def total_days() -> float:
    return round(sum(man_days(c) for c in V33["crs"]), 1)


@contextlib.contextmanager
def this_round(b):
    old = (b.round13, b.round14)
    b.round13 = b.round14 = True
    try:
        yield
    finally:
        b.round13, b.round14 = old


def begin(b) -> None:
    """The content of version 3.2 is no longer shaded in the review copy."""
    b.round13 = b.round14 = False


def revision(b) -> None:
    t = b.tables[1]
    empty = next((r for r in t.rows[1:] if not r.cells[0].text.strip()), None)
    filled = [r for r in t.rows[1:] if r.cells[0].text.strip()]
    with this_round(b):
        tr = b.clone_row(filled[-1]._tr, V33["revision_row"])
    filled[-1]._tr.addnext(tr)
    if empty is not None:
        empty._tr.getparent().remove(empty._tr)


def introduction(b) -> None:
    p = M.find_par(b, DOC_PREP_START)
    with this_round(b):
        p._p.addnext(b.clone_par(p._p, V33["intro_note"]))


DOC_PREP_START = "Version 3.2 of this document was elaborated"


def how_to_read(b) -> None:
    h = V33["how_to_read"]
    model_h2 = M.find_par(b, "Summary for the Business Unit Review")._p
    anchor = M.find_par(b, "Business Requirements Mapping", "Heading 1")._p
    head = copy.deepcopy(model_h2)
    for el in list(head):
        if el.tag != qn("w:pPr"):
            head.remove(el)
    for pb in head.iter(qn("w:pageBreakBefore")):
        pb.getparent().remove(pb)
    with this_round(b):
        head.append(b.run(h["title"]))
        els = [head, b.para(h["intro"], keep_next=True)]
        tbl = b.table(["Colour", "Meaning", "Cost"], [[x[1], x[2], x[3]] for x in h["legend"]], [1.2, 4.6, 1.6],
                      size=8.5)
        colours = {"green": GREEN, "orange": ORANGE, "grey": "616161", "blue": "1565C0"}
        for tr, x in zip(tbl.findall(qn("w:tr"))[1:], h["legend"]):
            if x[0] in colours:
                _colour(tr.findall(qn("w:tc"))[0], colours[x[0]], bold=True)
        n_g = sum(1 for p in V33["points"] if p["class"] == "ootb")
        els += [tbl, b.para(""), b.para(h["after"]),
                b.para(f"In this version: {len(V33['crs'])} change requests ({total_days():g} man-days in all) and "
                       f"{n_g} point{'s' if n_g != 1 else ''} beyond the BRD available OOTB in iNXT BrokerVerse (Appendix W).")]
    for e in els:
        anchor.addprevious(e)


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


def _text(p) -> str:
    return "".join(t.text or "" for t in p.iter(qn("w:t")))



def colour_points(b) -> Counter:
    found: Counter = Counter()
    for p in list(b.body.iter(qn("w:p"))):
        text = _text(p).strip()
        if not text:
            continue
        for pt in V33["points"]:
            hit = text == pt["match"] if pt.get("exact") else pt["match"] in text
            if not hit:
                continue
            target = p
            if pt.get("scope") == "row":
                while target is not None and target.tag != qn("w:tr"):
                    target = target.getparent()
            if target is None:
                continue
            if pt["class"] != "brd":      # class brd: a BRD requirement offered OOTB, stays black
                _colour(target, GREEN if pt["class"] == "ootb" else ORANGE)
            found[pt["id"]] += 1
    missing = [pt["id"] for pt in V33["points"] if not found[pt["id"]]]
    if missing:
        raise SystemExit(f"colour points not found in the document: {missing}")
    return found


def appendix_w(a) -> None:
    b = a.b
    with this_round(b):
        a.h1("Appendix W – Points beyond the BRD and Change Requests")
        a.p("This appendix lists what this FRS states beyond the Cashiering scope of the Operations BRD: the change "
            "requests (orange in the text) with their effort, and the points that the iNXT BrokerVerse OOTB edition provides (green in the "
            "text). It ends with the check of the non-functional lines of the BRD. The legend is in section 1.5. The "
            "programme-wide non-functional requirements of the other BDOI addenda (Collections NFR 10.07; Claims, "
            "Collections and Reinsurance NFR 4.03) are BRD requirements, not change requests.")
        a.h2("W.1 How the points were classified")
        a.bullets([
            "Every point added in versions 3.2 and 3.3 was compared with the BRD (the BRQID and CSHID IDs of the "
            "Cashiering scope and the non-functional table) and with the programme NFRs of the BDOI addenda.",
            "A point not in the BRD was looked up in the programme Feature List (column 'OOTB platform': the iNXT "
            "BrokerVerse OOTB platform edition): Available or OOTB extra = green; Partial, Not available, BDOI-specific "
            "or not listed = orange (change request), with the effort of the missing part only for a Partial feature.",
            "Effort is in man-days of 8 hours, estimated from the size of the platform components each change touches, "
            "with a range of plus or minus 20%; the detail, the assumptions and the cost (rate cell) are in the "
            "workbook BIBS_CR_BRD-02_Effort_Estimate_v1.0.xlsx. The change requests are also in the change request "
            "register (status 'Candidate - for decision').",
        ])
        a.h2("W.2 Change requests (orange)")
        rows = [[c["cr"], c["title"], c["frs"], c["brd"], c["fl"], f"{man_days(c):g}", c["complexity"],
                 c["priority"]] for c in V33["crs"]]
        rows.append(["Total", f"{len(V33['crs'])} change requests", "", "", "", f"{total_days():g}", "", ""])
        a.table(["CR No.", "Change", "FRS reference", "BRD reference", "OOTB status (Feature List)",
                 "Man-days", "Complexity", "Priority"], rows, [0.75, 1.45, 1.05, 0.85, 1.85, 0.55, 0.65, 0.55],
                size=7.5)
        for tr in a.els[-2].findall(qn("w:tr"))[1:]:
            _colour(tr, ORANGE)
        a.p("Points of BDOI's FRS checked and found to be covered, fully or partly, by a BRD line: they are BRD "
            "requirements (black, in scope), not change requests; their gap against the OOTB platform is delivery "
            "work, with a planning effort.")
        rows = [[x["id"], x["title"], x["brd"], x["frs"], f"{x['md']:g}"] for x in DATA["brd_deliver"]]
        a.table(["No.", "Point", "BRD line that covers it", "FRS reference", "Planning man-days"], rows,
                [0.65, 1.7, 2.6, 1.35, 0.7], size=7.5)
        a.p("Points first listed as green that a BRD line asks for, read with BRD-00 and the programme-wide NFRs "
            "(product owner decision of 11-Oct-2026): BRD requirements (black, in scope) that the iNXT BrokerVerse OOTB "
            "platform offers, so they need no change request and no delivery effort.")
        rows = [[p["id"], p["title"], p["brd_line"], p["frs"], p["fl"], "0"] for p in V33["points"] if p["class"] == "brd"]
        a.table(["No.", "Point", "BRD line that covers it", "FRS reference", "Feature List / OOTB", "Man-days"], rows,
                [0.5, 1.5, 2.4, 1.0, 1.1, 0.5], size=7.5)
        a.h2("W.3 Points beyond the BRD available OOTB in iNXT BrokerVerse (green)")
        rows = [[p["id"], p["title"], p["frs"], p["fl"], "Available; no cost"] for p in V33["points"]
                if p["class"] == "ootb"]
        a.table(["No.", "Point", "FRS reference", "Feature List ID / OOTB status", "Status"], rows,
                [0.5, 2.6, 1.0, 1.9, 1.2], size=8)
        for tr in a.els[-2].findall(qn("w:tr"))[1:]:
            _colour(tr, GREEN)
        a.h2("W.4 Check of the non-functional lines of the BRD")
        a.p("Every BRD ID of the Cashiering scope is mapped in section 2 (Business Requirements Mapping) and traced to "
            "its test cases in Appendix Q and the traceability workbook. The lines below are the non-functional table "
            "of the BRD and the programme NFRs; the gaps found were fixed in this version.")
        a.table(["BRD part", "BRD line", "Where in this FRS", "Result"], V33["brd_lines"], [1.4, 2.8, 2.4, 0.8],
                size=8)
        scope_and_practice(a, "W", [[x[0], x[2]] for x in M.REV["integrations"]["rows"]], [[x[0], x[1]] for x in M.REV["data_setup"]["rows"]])
    b.stats["crs"] = len(V33["crs"])
    b.stats["green"] = sum(1 for p in V33["points"] if p["class"] == "ootb")
    b.stats["brd_gaps"] = sum(1 for r in V33["brd_lines"] if r[3] == "Gap fixed")


GREY, BLUE = COLOURS["grey"], COLOURS["blue"]


def scope_labels(b) -> int:
    """The grey label [Integration scope] / [Data Migration scope] after each heading or row it names."""
    n = 0
    for p in list(b.body.iter(qn("w:p"))):
        text = _text(p).strip()
        for sl in DATA["scope_labels"]:
            if (text == sl["match"]) if sl.get("exact") else text.startswith(sl["match"]):
                p.append(b.run(" " + sl["label"], color=GREY, hl=False))
                n += 1
    if n < len(DATA["scope_labels"]):
        raise SystemExit("a scope label did not find its heading or row")
    return n


def scope_and_practice(a, ann: str, integrations: list, data_loads: list) -> None:
    """{ann}.5 the items covered by the Integration and Data Migration scope; {ann}.6 the best practices (blue)."""
    b = a.b
    a.h2(f"{ann}.5 Covered by the Integration scope and the Data Migration scope")
    a.p("The interfaces with other systems and the data loaded at go-live are delivered under the Integration scope "
        "and the Data Migration scope (BRD-13). They carry a grey label in this document and are not change requests.")
    rows = [[x[0], "Integration scope", x[1]] for x in integrations] + \
           [[x[0], "Data Migration scope", x[1]] for x in data_loads]
    a.table(["Item", "Scope", "What"], rows, [1.6, 1.4, 4.4], size=8)
    for tr in a.els[-2].findall(qn("w:tr"))[1:]:
        for tc in tr.findall(qn("w:tc"))[1:2]:
            _colour(tc, GREY)
    head = b.para(f"{ann}.6 Best-practice recommendations (blue)", style="Heading2", no_num=True, keep_next=True,
                  color=BLUE)
    a.els.append(head)
    a.p("Insurance-broking practices for this area that are not in the BRD, in this FRS or in the iNXT BrokerVerse OOTB "
        "platform. They are recommendations for the Business Unit, with an indicative effort; they are not change "
        "requests and are not in the change request total.", color=BLUE, keep_next=True)
    rows = [[r[0], r[1], r[2], r[3], f"{r[4]:g}"] for r in DATA["best_practice"]]
    a.table(["ID", "Practice", "Why it matters for a broker", "Priority", "Indicative man-days"], rows,
            [0.8, 2.6, 2.6, 0.7, 0.7], size=8)
    for tr in a.els[-2].findall(qn("w:tr"))[1:]:
        _colour(tr, BLUE)
    b.stats["best_practice"] = len(rows)


def change_summary(b) -> None:
    s = b.stats
    points = [
        "Document Revision Log: row 3.3; Introduction: a note on what version 3.3 adds.",
        "Section 1.4: the simplified swimlane process flow (Figure 1); new section 1.5 'How to Read this Document' "
        "with the colour legend.",
        "Appendix C: the end-to-end process flow by actor and system (Figure 2) with the change requests tagged; the "
        "figures after it are numbered from Figure 3.",
        f"Colour of the points not in the BRD: {s['green']} point{'s' if s['green'] != 1 else ''} in green (available OOTB in iNXT BrokerVerse) and the {s['crs']} "
        "change requests in orange where they are stated (Appendices G, J, M and N).",
        f"Appendix O: peak hours aligned with the BRD (08:00-17:30); rows CSH-NFR-15 to CSH-NFR-18 for the BRD lines "
        f"and programme NFRs not yet listed ({s['brd_gaps']} gaps found). Appendix U: one checklist point.",
        f"New Appendix W: change requests with their effort ({total_days():g} man-days), points beyond the BRD "
        "available OOTB in iNXT BrokerVerse, check of the BRD non-functional lines. New workbook BIBS_CR_BRD-02_Effort_Estimate_v1.0"
        ".xlsx; the change request register and the traceability workbook list the change requests.",
    ]
    els = [b.para("Summary of changes in version 3.3", bold=True, size=14, color="014EA9", hl=False, space_after=120),
           b.para("This review copy shows in light yellow the passages added in version 3.3. BDOI's text of version "
                  "3.1 and the content of version 3.2 are not shaded. The clean copy has the same content without "
                  "shading and without this page.", size=9.5, hl=False)]
    n = b.new_num(b.BULLET_NUM)
    for pt in points:
        els.append(b.para(pt, style="ListParagraph", num=(n, 0), size=9.5, hl=False))
    els.append(b.para("Shading used in this copy: ", size=9.5, hl=False))
    els[-1].append(b.run("text added in version 3.3", size=9.5, hl=True))
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
    nums = b.fig_numbers
    if nums.get("brd02_fig_e2e") != 1 or nums.get(V33["figures"]["e2e"]["id"]) != 2:
        out.append(f"figure numbers of the swimlane flows: {nums.get('brd02_fig_e2e')}, "
                   f"{nums.get(V33['figures']['e2e']['id'])}")
    for need in ("Figure 1 – " + V33["figures"]["simple"]["caption"], "Figure 2 – " + V33["figures"]["e2e"]["caption"],
                 "Appendix W – Points beyond the BRD", "BIBS DEV environment: https://dev.brokerverse.cloud"):
        if need not in text:
            out.append(f"missing: {need}")
    for c in V33["crs"]:
        if c["cr"] not in text:
            out.append(f"{c['cr']} not in the document")
    return out
