"""First issue (v1.0) of the standard document set of two BDOI FRS received on 17-Oct-2026, in BDOI's template:
Operations Remittance (BRD-02, from BDOI's "FRS - BDO Insure Core Modernization - Remittance") and Claims (BRD-07,
from BDOI's "FRS - BDOI BROKERSYS Claims v1.0").

    python docs/deliverables/src/programme/bdoi_template_frs/build_bdoi_v10_packs.py --pack remittance|claims
        [--only FRS,BPS,UXD,RNO,ICD,WB,DSP,DDI,USR,README] [--no-pdf]

The content of each pack is in its module: brd02r_v10_content.py (Remittance) and brd07_v10_content.py (Claims),
with the screens, user stories and developer facts in brd02r_v10_pack.py and brd07_v10_pack.py. The FRS, BPS, UXD,
RNO, ICD and the review workbooks are written here; the DSP, DDI, USR and the "How to read this pack" note by the
shared writers of build_pack.py (pack_dsp, pack_ddi, pack_usr, pack_readme). Each document starts from BDOI's
template D051 through pack_common.PackDoc and is checked for wording before it is saved.

Outputs in docs/deliverables/out/Programme/BDOI_Template_FRS/<folder of the pack>/ (PDFs not kept in git).
Self-checks: every BRD row maps to an FRS entry or names the FRS that holds it; every requirement has acceptance
criteria, a fit, a user story and a test case; every open change request has its effort.
"""

from __future__ import annotations

import argparse
import importlib
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[4] / "tools" / "deliverables"))
import build_brd06_v13 as B6  # noqa: E402  (document class and colour code of the BDOI template sets)
import pack_common as pc  # noqa: E402
import pack_usr  # noqa: E402
from brd11_xlsx import Column, ReviewWorkbook  # noqa: E402
from colour_code import LABELS  # noqa: E402

PACKS = {"remittance": "brd02r_v10_pack", "claims": "brd07_v10_pack"}
COLOUR = {"k": "Black (BRD)", "g": "Green (OOTB)", "o": "Orange (CR)", "b": "Blue (best practice)", "": "-"}
FIT_VALUES = ["Fit", "Partial", "Gap", "Change request", "Integration", "Data Migration", "In another FRS"]
RESULT = ["Not run", "Pass", "Fail", "Blocked"]
K = None  # the pack module (set by main)
C = None  # its content module


class Doc(B6.Doc):
    """A document of a v1.0 pack (the Renewal set's document class with this pack's content)."""

    def __init__(self, key: str, revisions):
        m = C.DOCS[key]
        version = C.ICD_VERSION if key == "ICD" else C.VERSION
        pc.PackDoc.__init__(self, pc.DocMeta(file=m["file"], cover=m["cover"], header_left=m["header_left"],
                                             header_right=C.HEADER_RIGHT, doc_no=m["doc_no"], version=version,
                                             date_us=C.DATE_US, created_us=C.CREATED_US.get(key, C.DATE_US),
                                             revisions=revisions))
        self.key = key
        self.shots = 0

    def legend(self) -> None:
        old = B6.C.LEGEND_NOTE
        B6.C.LEGEND_NOTE = C.LEGEND_NOTE
        try:
            super().legend()
        finally:
            B6.C.LEGEND_NOTE = old

    def personas(self, caption=None) -> None:
        self.table(["Persona", "Does", "Screens", "Walkthrough account"], C.PERSONAS, [1.8, 3.6, 2.6, 1.6],
                   caption=caption or f"Personas of {C.NAME}")


def dot(src: str, name: str, tmp: Path) -> Path:
    path = tmp / f"{name}.dot"
    path.write_text(src, encoding="utf-8")
    png = tmp / f"{name}.png"
    subprocess.run(["dot", "-Tpng", "-Gdpi=170", str(path), "-o", str(png)], check=True)
    return png


def lifecycle(tmp: Path, key: str) -> Path:
    nodes, edges, title = C.LIFECYCLES[key]
    body = "".join(f'{n} [label="{lab}"]; ' for n, lab in nodes)
    body += "".join(f'{a} -> {b} [label="{lab}"]; ' for a, b, lab in edges)
    return dot('digraph g { rankdir=LR; graph [fontname="Arial", nodesep=0.3, ranksep=0.35]; node [shape=box, '
               'style="rounded", fontname="Arial", fontsize=10]; edge [fontname="Arial", fontsize=8];\n' + body + "}",
               f"life_{key}", tmp)


def journey_flow(tmp: Path) -> Path:
    """Swimlane flow of the end-to-end journey: one cluster per actor, steps in journey order."""
    lanes: dict[str, list[tuple[str, str]]] = {}
    for step, who, what in C.JOURNEY:
        lanes.setdefault(who, []).append((step, what))
    parts = []
    for i, (who, steps) in enumerate(lanes.items()):
        nodes = " ".join(f'S{s.replace(".", "_")} [label="{s}. {_wrap(w)}"];' for s, w in steps)
        parts.append(f'subgraph cluster_{i} {{ label="{who}"; style="rounded,filled"; fillcolor="#F3F6FA"; '
                     f'color="#9DB3CC"; fontname="Arial"; fontsize=10; {nodes} }}')
    order = [s for s, _, _ in C.JOURNEY]
    edges = " ".join(f'S{a.replace(".", "_")} -> S{b.replace(".", "_")};' for a, b in zip(order, order[1:]))
    return dot('digraph g { rankdir=LR; compound=true; graph [fontname="Arial", nodesep=0.25, ranksep=0.3]; '
               'node [shape=box, style="rounded,filled", fillcolor="white", fontname="Arial", fontsize=9, '
               'width=1.6]; edge [color="#555555"];\n' + "\n".join(parts) + "\n" + edges + "}", "journey", tmp)


def _wrap(text: str, width: int = 26) -> str:
    words, lines, line = text.split(), [], ""
    for w in words:
        if len(line) + len(w) + 1 > width and line:
            lines.append(line)
            line = w
        else:
            line = f"{line} {w}".strip()
    lines.append(line)
    return "\\n".join(lines).replace('"', "'")


# ================================================================================================ FRS
def write_frs(tmp: Path) -> Doc:
    d = Doc("FRS", C.REVISIONS)
    i = C.FRS_INTRO
    d.h1("Introduction")
    d.h2("Document Purpose")
    d.para(i["purpose"])
    d.h2("Intended Audience")
    d.para(i["audience"])
    d.h2("Project Background")
    for t in i["background"]:
        d.para(t)
    d.h2("Scope")
    n = max(len(i["in_scope"]), len(i["out_scope"]))
    d.table(["In scope (BIBS)", "Out of scope or in another FRS"],
            [[(i["in_scope"] + [""] * n)[k], (i["out_scope"] + [""] * n)[k]] for k in range(n)], [4.6, 4.6],
            caption=f"Scope of {C.NAME} in BIBS")
    d.h2("Document Set")
    d.table(["Document", "Content"], C.DOC_SET, [3.2, 6.0], caption=f"{C.NAME} document set")
    d.h2("How to Read This Document")
    d.legend()
    d.h2("References")
    d.bullets(C.REFERENCES)

    d.h1("Business Requirements Mapping")
    d.para(C.MAPPING_NOTE)
    d.table(["BRD Req No.", "Business requirement", "FRS entries", "Note"], C.MAPPING, [1.7, 4.0, 2.3, 1.3],
            caption="Business requirements mapping")
    d.table(["BRD non-functional line", "BRD value", "Programme NFR"], C.BRD_NFR, [2.4, 4.2, 2.6],
            caption="Non-functional lines of the BRD and the programme NFR that states them")

    d.h1("Functional Requirements")
    d.para(C.FRUM_NOTE)
    d.table(["Entry", "Component", "Capability", "Source", "BRD"],
            [[f["id"], f["component"], f["capability"], f["status"], f["brd"]] for f in C.FRUM],
            [1.6, 2.0, 2.4, 1.3, 2.0], caption="Functional requirements at a glance")
    for f in C.FRUM:
        d.h2(f"{f['id']} {f['capability']}")
        d.para(f"Component: {f['component']}. Source: {f['status']}. BRD: {f['brd']}.", italic=True)
        rows, cols = [], []
        for r in f["reqs"]:
            lab = {"int": "  " + LABELS["integration"], "dm": "  " + LABELS["migration"]}.get(r["label"], "")
            rows.append([r["id"], r["text"] + lab, r["rule"], r["ac"][0]])
            cols.append(r["colour"])
        d.ctable(["ID", "Requirement", "Rule or setting", "Example"], rows, [1.9, 4.2, 1.5, 2.2], colours=cols,
                 colour_cols={1, 2})

    d.h1("Business Rules")
    d.ctable(["ID", "Rule", "Colour"], [[a, b, B6.COLOUR_NAME[c]] for a, b, c in C.BUSINESS_RULES],
             [1.3, 6.8, 1.2], caption="Business rules", colours=[c for *_, c in C.BUSINESS_RULES],
             colour_cols={1, 2})
    d.h1("Validations and Messages")
    d.para("The main messages of the module. The full list with the message codes is in the Developer "
           "Specification; the field checks of each screen are in the UX Design.")
    d.table(["Screen or step", "Condition", "Message"], C.MESSAGES, [2.2, 3.1, 3.9], caption="Validations and messages")
    d.h1("Personas and Permissions")
    d.personas()
    d.table(C.PERSONA_HEADS, C.PERMISSIONS, [3.4] + [5.8 / (len(C.PERSONA_HEADS) - 1)] * (len(C.PERSONA_HEADS) - 1),
            caption="What each persona may do (Y = yes); the rules of section 4 apply whatever the profile grants")
    d.para(C.ACCESS_NOTE)
    d.h1("Parameters and Lists of Values")
    d.para(C.PARAMETER_NOTE)
    d.ctable(["Setting", "Default", "Range", "Area"], [r[:4] for r in C.PARAMETERS], [4.0, 2.4, 1.6, 1.2],
             caption=f"{C.SHORT} settings", colours=[r[4] for r in C.PARAMETERS])
    d.table(["List", "Values"], C.LOVS, [3.0, 6.2], caption="Lists of values (codes in the Data Dictionary)")
    d.h1("Statuses")
    for key, (_, _, title) in C.LIFECYCLES.items():
        d.figure(lifecycle(tmp, key), title, width_cm=16.0)
    for name, rows in C.STATUSES.items():
        d.h2(name)
        d.table(["Status", "Meaning", "Next"], rows, [2.4, 3.6, 3.2], caption=f"Statuses: {name}")
    d.h1("Assumptions, Dependencies and Open Questions")
    d.table(["ID", "Assumption"], C.ASSUMPTIONS, [1.3, 7.9], caption="Assumptions")
    d.table(["ID", "Dependency"], C.DEPENDENCIES, [1.3, 7.9], caption="Dependencies")
    d.table(["ID", "Question", "Owner", "Answer by"], C.OPEN_QUESTIONS, [1.2, 5.0, 1.8, 1.2],
            caption="Open questions")
    d.h1("Change Control Summary")
    d.para(f"Change requests of the register v{C.WB_VERSION}. Effort in man-days (analysis and design, backend, "
           "frontend, testing, documentation and UAT support).")
    if C.CRS:
        d.ctable(["CR", "Title", "Status", "FRS", "Man-days", "Note"],
                 [[a, b, c, e, f"{sum(eff):g}", n] for a, b, c, e, eff, n in C.CRS], [1.3, 2.9, 0.9, 1.4, 0.9, 1.8],
                 caption="Change requests", colours=["o" if c == "Open" else "" for _, _, c, *__ in C.CRS])
    else:
        d.para("No change request: every requirement of BDOI's FRS is asked by the BRD, BRD-00 or a programme "
               "document.")
    if C.OOTB_POINTS:
        d.ctable(["No.", "Not in the BRD, offered by the OOTB platform", "FRS"], C.OOTB_POINTS, [1.3, 5.6, 2.3],
                 caption="Green points (no cost)", colours=["g"] * len(C.OOTB_POINTS), colour_cols={1})
    d.table(["No.", "Integration and data migration scope", "FRS"], C.INTEGRATION_POINTS, [1.3, 5.6, 2.3],
            caption="Grey label points (not change requests)")
    d.ctable(["No.", "Best-practice recommendation", "Priority", "Man-days"],
             [[a, b, c, str(e)] for a, b, c, e in C.BEST_PRACTICE], [1.3, 5.0, 1.4, 1.4],
             caption="Blue points (recommendations)", colours=["b"] * len(C.BEST_PRACTICE), colour_cols={1})
    d.h1("Review of BDOI's FRS")
    d.para(C.REVIEW_NOTE)
    d.table(["Item", "Finding", "How this version treats it"], C.REVIEW, [2.2, 3.6, 3.4],
            caption="Findings of the review of BDOI's FRS")
    d.table(["Point", "Agreed programme position", "Effect on this FRS"], C.CONFLICTS, [2.2, 3.6, 3.4],
            caption="Points to align with programme decisions and other FRS")
    if C.RENUMBERED:
        d.table(["Number in BDOI's FRS", "Number in this version", "Why"], C.RENUMBERED, [3.4, 2.8, 3.0],
                caption="Numbering slips corrected")
    for title, paras, table in getattr(C, "FRS_ANNEXES", []):
        d.h1(title)
        for t in paras:
            d.para(t)
        if table:
            heads, rows, widths, cap = table
            d.table(heads, rows, widths, caption=cap)
    return d


# ================================================================================================ BPS
def write_bps(tmp: Path) -> Doc:
    d = Doc("BPS", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para(f"This document shows how {C.NAME} works end to end. The requirements are in the FRS v{C.VERSION}; the "
           f"screens in the UX Design v{C.VERSION}; the reports, documents and notices in the Reports, Notifications "
           f"and Outputs v{C.VERSION}.")
    d.para(C.BPS_NOTE)
    d.personas("Personas")
    d.h1("End-to-end Journey")
    d.para(f"{len(C.JOURNEY)} steps from {C.JOURNEY_SPAN}.")
    d.figure(journey_flow(tmp), f"{C.NAME}: process flow (swimlanes by actor)", width_cm=16.5)
    d.table(["Step", "Who", "What happens"], C.JOURNEY, [0.9, 2.2, 6.1], caption=f"The {len(C.JOURNEY)} steps")
    d.h1("To-be Processes")
    for p in C.PROCESSES:
        d.h2(f"{p['id']} {p['name']}")
        d.table(["Item", "Value"], [["Journey steps", p["steps"]], ["Trigger", p["trigger"]], ["Actors", p["actors"]],
                                    ["Outcome", p["outcome"]], ["Controls", p["controls"]],
                                    ["Requirements", p["frs"]]], [2.0, 7.2])
    d.h2("Statuses")
    for key, (_, _, title) in C.LIFECYCLES.items():
        d.figure(lifecycle(tmp, key), title, width_cm=16.0)
    d.h1("Storyboards")
    d.para("Each storyboard is a walkthrough of the to-be process with the walkthrough accounts of the Walkthrough "
           "Users workbook and fictitious clients and insurers. The screens are described in the UX Design.")
    for sb in C.STORYBOARDS:
        d.h2(f"{sb['id']} {sb['title']}")
        d.para(sb["summary"])
        d.para(f"Test data: {sb['data']}.", italic=True)
        d.table(["Step", "Who", "Screen", "Does", "Sees"],
                [[str(k + 1), w, s, a, r] for k, (w, s, a, r) in enumerate(sb["steps"])],
                [0.7, 1.9, 1.8, 2.6, 2.2], caption=f"{sb['id']} steps")
    return d


# ================================================================================================ UXD
def write_uxd(tmp: Path) -> Doc:
    d = Doc("UXD", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para(f"This document describes every {C.NAME} screen of BIBS: purpose, users, access, rules and states, fields "
           "with their type, whether they are required, default, rule and message, and the actions. The "
           f"requirements and rules are in the FRS v{C.VERSION}.")
    d.para(C.UXD_NOTE)
    d.para("Codes used: Y required, N optional, Cond. required on a condition, - display only. Dates show as "
           "dd-MMM-yyyy, times in Manila time, amounts with two decimals.")
    d.h1("Menu by Persona")
    d.para(C.MENU_NOTE)
    d.table(["Screen", "Purpose", "Personas"],
            [[a, b, ", ".join(C.PERSONA_CODES[x] for x in c.split())] for a, b, c in C.MENU],
            [2.6, 4.0, 2.6], caption=f"{C.SHORT} menu by persona")
    d.h1("Screens")
    for s in K.UX_SCREENS:
        d.h2(f"{s['id']} {s['name']}")
        d.table(["Item", "Value"], [["Purpose", s["purpose"]], ["Access", s["access"]], ["Requirements", s["frrn"]],
                                    ["Outcome", s["outcome"] or "-"], ["Availability", s["state"]]], [2.2, 7.0])
        if s["rules"]:
            d.para("Rules and states:")
            d.bullets(s["rules"])
        if s["fields"]:
            d.table(["Field", "Type", "Req.", "Default", "Rule", "Message"], s["fields"],
                    [2.3, 1.1, 0.6, 1.2, 2.6, 1.6], caption=f"{s['id']} fields")
        if s["actions"]:
            d.table(["Action", "Who", "Available when", "What happens", "Result"], s["actions"],
                    [1.6, 1.7, 1.8, 2.7, 1.4], caption=f"{s['id']} actions")
    return d


# ================================================================================================ RNO
def write_rno(tmp: Path) -> Doc:
    d = Doc("RNO", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para(f"This document lists the reports, the documents and files produced, the exports, the notices and alerts "
           f"and the scheduled jobs of {C.NAME}. Times are Manila time.")
    d.h1("Reports")
    d.table(["ID", "Report", "FRS", "Parameters", "Content", "Output"], C.REPORTS, [1.2, 2.2, 1.4, 1.4, 2.3, 0.9],
            caption=f"{C.SHORT} reports")
    d.para(C.REPORT_NOTE)
    for title, heads, rows, widths in getattr(C, "REPORT_DETAILS", []):
        d.h2(title)
        d.table(heads, rows, widths, caption=title)
    d.h1("Documents and Files Produced")
    d.table(["ID", "Document", "Format and file name", "Kept", "FRS"], C.DOCUMENTS, [0.9, 2.2, 3.4, 1.4, 1.3],
            caption="Documents and files produced")
    d.h1("Exports")
    d.table(["Output", "Format", "Where", "Content"], C.EXPORTS, [2.6, 1.2, 2.6, 2.8], caption="Exports")
    d.h1("Notices, E-mails and Alerts")
    d.para("Notices appear under the bell; alerts on the Alerts screen. Text in angle brackets is filled in.")
    d.table(["ID", "Event", "To", "Text", "Channel"], C.NOTICES, [0.8, 2.4, 1.8, 3.2, 1.0], caption="Notices and alerts")
    d.h1("Scheduled Jobs")
    d.table(["Job", "When", "What it does", "Alerts"], C.JOBS, [2.6, 1.8, 3.8, 1.0], caption="Jobs")
    d.para("Every run is recorded with its start, end, outcome and counts, and can be started at once from the Job "
           "Monitor by a user with the job permission.")
    return d


# ================================================================================================ ICD
def write_icd(tmp: Path) -> Doc:
    d = Doc("ICD", C.ICD_REVISIONS)
    C.write_icd(d, tmp)
    return d


WRITERS = {"FRS": write_frs, "BPS": write_bps, "UXD": write_uxd, "RNO": write_rno, "ICD": write_icd}


# ================================================================================================ workbooks
def _wb(title, kind):
    return ReviewWorkbook(title, doc_type=kind, brd=C.BRD, version=C.WB_VERSION, date=C.DATE_LONG,
                          subtitle=f"{C.NAME}; goes with the FRS {C.NAME} v{C.VERSION}")


def _save(wb, key):
    return pc.save_workbook(wb, pc.OUT_BDOI / K.FOLDER / K.file_name(key))


def _stories():
    return pack_usr.stories(K)


def stories_of(rid: str, stories) -> list[dict]:
    return [st for st in stories if rid in [x.strip() for x in st["frum"].split(",")]]


def test_cases(stories):
    screens = K.screens_by_id()
    rows = []
    for f in C.FRUM:
        for r in f["reqs"]:
            sts = stories_of(r["id"], stories)
            rows.append({"tc": "TC-" + r["id"].replace(".", "-"), "frum": r["id"], "brd": f["brd"],
                         "persona": sts[0]["persona"] if sts else "-",
                         "screen": f"{sts[0]['screen']} {screens[sts[0]['screen']]['name']}" if sts else "-",
                         "pre": "Test environment with the walkthrough accounts of the Walkthrough Users workbook.",
                         "steps": f"Check: {r['text']}",
                         "expected": " ".join(f"({k}) {a}" for k, a in enumerate(r["ac"], 1))
                         + ("" if r["rule"] == "-" else f" Rule: {r['rule']}."), "acs": len(r["ac"]),
                         "colour": COLOUR[r["colour"]], "fit": r["fit"],
                         "scope": {"int": "Integration", "dm": "Data Migration"}.get(r["label"], "BIBS"),
                         "result": "Not run"})
    for sb in C.STORYBOARDS:
        rows.append({"tc": f"TC-{C.CODE}-{sb['id']}", "frum": "End to end", "brd": C.BRD,
                     "persona": ", ".join(sorted({s[0] for s in sb["steps"]})),
                     "screen": "; ".join(dict.fromkeys(s[1] for s in sb["steps"])), "pre": "As above.",
                     "steps": " ".join(f"{i + 1}. {s[0]}: {s[2]}" for i, s in enumerate(sb["steps"])),
                     "expected": " ".join(f"{i + 1}. {s[3]}" for i, s in enumerate(sb["steps"])),
                     "colour": "Black (BRD)", "fit": "-", "scope": "BIBS", "result": "Not run"})
    return rows


def build_rtm():
    stories = _stories()
    screens = K.screens_by_id()
    wb = _wb("Test Cases and Traceability", "Requirements traceability")
    tcs = test_cases(stories)
    for t in tcs:
        t["story"] = ", ".join(st["id"] for st in stories_of(t["frum"], stories)) or "-"
    wb.sheet("Test Cases", [Column("tc", "Test case", 16), Column("frum", "Requirement", 18),
                            Column("story", "User story", 16), Column("brd", "BRD", 22), Column("persona", "Persona", 22),
                            Column("screen", "Screen", 26), Column("pre", "Precondition", 28),
                            Column("steps", "Steps", 50), Column("expected", "Expected result (acceptance criteria)", 60),
                            Column("colour", "Colour", 16), Column("fit", "Fit", 14), Column("scope", "Delivered by", 14),
                            Column("result", "Result", 12, values=RESULT, status=True), Column("tester", "Tester", 16),
                            Column("date", "Date run", 12, kind="date")], tcs,
             description=f"One test case per requirement of the FRS {C.NAME} v{C.VERSION}, with every acceptance "
             f"criterion as an expected result, and one per storyboard of the Process and Storyboards v{C.VERSION}.")
    rtm = []
    for b, req, frum, note in C.MAPPING:
        ids = [t.strip() for t in frum.split(",") if t.strip() and t.strip() != "-"]
        tc = [t["tc"] for t in tcs if any(t["frum"].startswith(i + ".") for i in ids)]
        if ids and not tc:
            raise SystemExit(f"BRD {b} without test case")
        rtm.append({"brd": b, "req": req, "frum": frum, "note": note, "tc": ", ".join(tc) or "-"})
    wb.sheet("BRD to Test", [Column("brd", "BRD", 14), Column("req", "Business requirement", 50),
                             Column("frum", "FRS entries", 28), Column("note", "Note", 30),
                             Column("tc", "Test cases", 60)], rtm,
             description=f"Every {C.SHORT} requirement of the BRD with its FRS entries and test cases.")
    trace = []
    for f in C.FRUM:
        for r in f["reqs"]:
            sts = stories_of(r["id"], stories)
            tc = [t["tc"] for t in tcs if t["frum"] == r["id"]]
            if not (sts and tc):
                raise SystemExit(f"trace incomplete for {r['id']}")
            trace.append({"brd": f["brd"], "frum": r["id"], "req": r["text"], "story": ", ".join(s["id"] for s in sts),
                          "screen": "; ".join(dict.fromkeys(f"{s['screen']} {screens[s['screen']]['name']}"
                                                            for s in sts)), "tc": ", ".join(tc)})
    wb.sheet("Trace", [Column("brd", "BRD", 22), Column("frum", "Requirement", 18), Column("req", "Requirement", 50),
                       Column("story", "User story", 16), Column("screen", "Screen", 32),
                       Column("tc", "Test case", 18)], trace,
             description="BRD to FRS requirement to user story (User Stories v1.0) to screen (UX Design "
             f"v{C.VERSION}) to test case, one row per requirement.")
    if sum(t.get("acs", 0) for t in tcs) != sum(len(r["ac"]) for f in C.FRUM for r in f["reqs"]):
        raise SystemExit("acceptance criteria missing from the test cases")
    return _save(wb, "RTM"), len(tcs)


def build_fitgap():
    wb = _wb("BRD vs Out-of-the-Box vs Best Practice", "Fit-gap")
    rows = []
    for f in C.FRUM:
        for r in f["reqs"]:
            rows.append({"frum": r["id"], "brd": f["brd"], "req": r["text"], "colour": COLOUR[r["colour"]],
                         "fit": r["fit"], "today": r["today"], "note": "" if r["rule"] == "-" else r["rule"]})
    wb.sheet("Fit-Gap", [Column("frum", "Requirement", 18), Column("brd", "BRD", 20), Column("req", "Requirement", 50),
                         Column("colour", "Colour", 16), Column("fit", "Fit", 16, values=FIT_VALUES),
                         Column("today", "What the BIBS platform offers and the work to deliver", 60),
                         Column("note", "Rule or setting", 30)], rows,
             description=f"Each requirement of the FRS {C.NAME} v{C.VERSION} against the BRD and the BIBS platform "
             "(iNXT BrokerVerse with the BDOI configuration). Fit: offered as asked. Partial: offered in part, the "
             "rest is delivery work. Gap: not offered, delivery work. Change request: not in the BRD and not offered.")
    summary = []
    for v in FIT_VALUES:
        n = sum(1 for r in rows if r["fit"] == v)
        if n:
            summary.append({"fit": v, "n": n, "pct": round(100 * n / len(rows))})
    summary.append({"fit": "Total", "n": len(rows), "pct": 100})
    wb.sheet("Summary", [Column("fit", "Fit", 18), Column("n", "Requirements", 14, kind="number"),
                         Column("pct", "Percent", 10, kind="number")], summary,
             description="Requirements of the FRS by fit.")
    wb.sheet("Beyond the BRD", [Column("id", "No.", 12), Column("t", "Point", 50), Column("frs", "FRS", 22),
                                Column("k", "Colour", 16)],
             [{"id": a, "t": b, "frs": c, "k": "Green (OOTB)"} for a, b, c in C.OOTB_POINTS]
             + [{"id": a, "t": b, "frs": e, "k": "Orange (CR)"} for a, b, _, e, _, _ in C.CRS]
             or [{"id": "-", "t": "No point beyond the BRD.", "frs": "-", "k": "-"}],
             description="Points not in the BRD, one group: green when OOTB, orange when a change request.")
    wb.sheet("Integration and DM", [Column("id", "No.", 12), Column("t", "Item", 56), Column("frs", "FRS", 24)],
             [{"id": a, "t": b, "frs": c} for a, b, c in C.INTEGRATION_POINTS],
             description="Covered by the Integration scope or the Data Migration scope; not change requests.")
    wb.sheet("Best Practice", [Column("id", "No.", 12), Column("t", "Recommendation", 56), Column("p", "Priority", 10),
                               Column("e", "Man-days", 10, kind="number")],
             [{"id": a, "t": b, "p": c, "e": d} for a, b, c, d in C.BEST_PRACTICE],
             description="Blue points: in no BRD, FRS or OOTB; recommendation only, not in the CR total.")
    return _save(wb, "FIT"), len(rows)


def build_cr():
    wb = _wb("Change Request Register", "Change control")
    rows = [{"id": a, "t": b, "s": c, "frs": e, "md": sum(eff), "note": n} for a, b, c, e, eff, n in C.CRS]
    if not rows:
        rows = [{"id": "-", "t": "No change request in this version.", "s": "Closed", "frs": "-", "md": 0,
                 "note": "Every requirement of BDOI's FRS is asked by the BRD, BRD-00 or a programme document."}]
    wb.sheet("Register", [Column("id", "CR", 12), Column("t", "Title", 44), Column("s", f"Status v{C.WB_VERSION}", 14,
             values=["Open", "Withdrawn", "Closed", "Approved", "Rejected"], status=True),
             Column("frs", "Reference", 18), Column("md", "Man-days", 10, kind="number"), Column("note", "Note", 50)],
             rows, description=f"Change requests of {C.BRD} {C.NAME}.")
    wb.sheet("Decision Log", [Column("d", "Date", 14), Column("id", "Item", 22), Column("e", "Decision", 70)],
             [{"d": C.DATE, "id": f"Register v{C.WB_VERSION}", "e": C.CR_DECISION}])
    return _save(wb, "CRR"), len(C.CRS)


def build_effort():
    wb = _wb("CR Effort Estimate", "Change control")
    rows = [{"id": a, "t": b, "an": e[0], "be": e[1], "fe": e[2], "te": e[3], "do": e[4], "tot": sum(e)}
            for a, b, c, _, e, _ in C.CRS if c == "Open"]
    n = len(rows)
    rows.append({"id": "Total", "t": "Open change requests" if rows else "No open change request",
                 **{k: sum(r[k] for r in rows) for k in ("an", "be", "fe", "te", "do", "tot")}})
    num = lambda k, h: Column(k, h, 12, kind="number")  # noqa: E731
    wb.sheet("Effort", [Column("id", "CR", 12), Column("t", "Title", 44), num("an", "Analysis and design"),
                        num("be", "Backend"), num("fe", "Frontend"), num("te", "Testing"),
                        num("do", "Documentation and UAT"), num("tot", "Total")], rows,
             description="Man-days of 8 hours, plus or minus 20 percent, for the open change requests only.")
    wb.sheet("Delivery Work", [Column("id", "No.", 10), Column("t", "Delivery work in the BRD scope", 60),
                               Column("frs", "Requirements", 30), Column("md", "Man-days", 10, kind="number")],
             [{"id": a, "t": b, "frs": c, "md": e} for a, b, c, e in C.DELIVERY_WORK],
             description="Partial and gap requirements of the BRD scope: delivery work of the programme, not change "
             "requests; indicative man-days for planning.")
    return _save(wb, "CRE"), n


def build_inputs():
    wb = _wb("Business Unit Requirements Collection", "Requirements collection")
    rows = [{"id": a, "item": b, "owner": c, "due": d, "status": "Open", "answer": ""} for a, b, c, d in
            C.OPEN_QUESTIONS]
    rows += [{"id": a, "item": b, "owner": "BDOI IT" if "IT" in b else "BDOI", "due": "Before SIT",
              "status": "Open", "answer": ""} for a, b in C.DEPENDENCIES]
    rows += [{"id": f"P-{k + 1:02d}", "item": f"Confirm the value of: {p[0]} (default {p[1]})",
              "owner": C.OWNER, "due": C.ANSWER_BY, "status": "Open", "answer": ""}
             for k, p in enumerate(C.PARAMETERS)]
    wb.sheet("Inputs", [Column("id", "No.", 10), Column("item", "What BDOI provides or decides", 64),
                        Column("owner", "Owner", 22), Column("due", "Needed by", 14),
                        Column("status", "Status", 12, values=["Open", "Received", "Confirmed"], status=True),
                        Column("answer", "Answer", 40)], rows,
             description=f"Open questions, dependencies and settings of the FRS {C.NAME} v{C.VERSION}.")
    return _save(wb, "INP"), len(rows)


def build_uat():
    wb = _wb("Business Unit Walkthrough Users", "User acceptance")
    users = [{"p": n, "acct": a, "does": d, "screens": s, "pw": "Issued separately by the iorta TechNXT Project Team"}
             for n, d, s, a in C.PERSONAS]
    wb.sheet("Users", [Column("p", "Persona", 24), Column("acct", "Walkthrough account", 26),
                       Column("does", "Does", 50), Column("screens", "Screens", 44), Column("pw", "Sign-in", 30)],
             users, description=f"One walkthrough account per persona of the FRS {C.NAME}; the people are fictitious.")
    script = [{"sb": f"{sb['id']} {sb['title']}", "n": k, "who": who, "screen": screen, "does": does, "sees": sees,
               "result": "Not run"} for sb in C.STORYBOARDS
              for k, (who, screen, does, sees) in enumerate(sb["steps"], start=1)]
    wb.sheet("Script", [Column("sb", "Storyboard", 34), Column("n", "Step", 6, kind="number"),
                        Column("who", "Who", 22), Column("screen", "Screen", 24), Column("does", "Does", 42),
                        Column("sees", "Sees", 40), Column("result", "Result", 12, values=RESULT, status=True)],
             script, description=f"The storyboards of the Process and Storyboards v{C.VERSION}, step by step.")
    return _save(wb, "WTU"), len(script)


def write_workbooks() -> None:
    for fn in (build_rtm, build_fitgap, build_cr, build_effort, build_inputs, build_uat):
        path, n = fn()
        print(f"wrote {Path(path).name} ({n} rows)")


def checks() -> None:
    problems = K.check()
    for f in C.FRUM:
        for r in f["reqs"]:
            if not r["ac"]:
                problems.append(f"{r['id']}: no acceptance criterion")
            if r["fit"] not in FIT_VALUES:
                problems.append(f"{r['id']}: fit {r['fit']} unknown")
    entries = {f["id"] for f in C.FRUM}
    for row in C.MAPPING:
        if row[2] != "-" and not all(e.strip() in entries for e in row[2].split(",")):
            problems.append(f"mapping row {row[0]} points to an unknown entry")
    if problems:
        raise SystemExit("content: " + "; ".join(problems))


def _cover_drop() -> None:
    """The cover of BDOI's template names Drop 0; every document of the pack names the drop of its BRD."""
    from docx.oxml.ns import qn  # noqa: PLC0415
    orig = pc.PackDoc._cover

    def cover(self, _orig=orig):
        _orig(self)
        for el in list(self.doc.element.body)[:3]:
            for node in el.iter(qn("w:t")):
                if node.text and "Drop 0" in node.text:
                    node.text = node.text.replace("Drop 0", C.DROP)
    pc.PackDoc._cover = cover


def main() -> int:
    global K, C
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--pack", required=True, choices=sorted(PACKS))
    ap.add_argument("--only", default="FRS,BPS,UXD,RNO,ICD,WB,DSP,DDI,USR,README")
    ap.add_argument("--no-pdf", action="store_true")
    args = ap.parse_args()
    K = importlib.import_module(PACKS[args.pack])
    C = K.C
    _cover_drop()
    checks()
    out = pc.OUT_BDOI / K.FOLDER
    with tempfile.TemporaryDirectory() as t:
        tmp = Path(t)
        for key in args.only.split(","):
            if key in WRITERS:
                d = WRITERS[key](tmp)
                target = out / C.DOCS[key]["file"]
                n = pc.finish(d, target, tmp, pdf=not args.no_pdf)
                print(f"wrote {target.name} ({n} TOC lines)")
            elif key == "WB":
                write_workbooks()
            elif key == "DSP":
                import pack_dsp  # noqa: PLC0415
                target, n, facts = pack_dsp.write(K, out, tmp, pdf=not args.no_pdf)
                print(f"wrote {target.name} ({n} TOC lines; {facts})")
            elif key == "DDI":
                import pack_ddi  # noqa: PLC0415
                orig = pack_ddi.save_workbook

                def save(wb, target, _orig=orig):
                    K.extra_ddi_sheets(wb)
                    return _orig(wb, target)
                pack_ddi.save_workbook = save
                allowed = pack_ddi.sd.allowed_values

                def upper_allowed(definition, _orig=allowed):
                    # the allowed values of a check written as codes (capitals), as the data dictionary names columns
                    vals = _orig(definition)
                    return [v.upper() if v and v == v.lower() and "_" in v else v for v in vals] if vals else vals
                pack_ddi.sd.allowed_values = upper_allowed
                try:
                    target, counts = pack_ddi.write(K, out)
                finally:
                    pack_ddi.save_workbook = orig
                    pack_ddi.sd.allowed_values = allowed
                print(f"wrote {target.name} ({counts})")
            elif key == "USR":
                target, counts, _ = pack_usr.write(K, out, K.FIX_VERSION)
                print(f"wrote {target.name} ({counts})")
            elif key == "README":
                import pack_readme  # noqa: PLC0415
                target, n = pack_readme.write(K, out, tmp, pdf=not args.no_pdf)
                print(f"wrote {target.name} ({n} TOC lines)")
            else:
                raise SystemExit(f"unknown document {key}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
