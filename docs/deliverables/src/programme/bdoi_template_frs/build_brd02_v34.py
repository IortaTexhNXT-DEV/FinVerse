"""Operations Cashiering document set v3.4 (BRD-02) in BDOI's template: FRS, BPS, UXD, RNO and the Interface
Specification v1.0.

    python docs/deliverables/src/programme/bdoi_template_frs/build_brd02_v34.py [--no-pdf] [--only FRS,BPS,UXD,RNO,ICD]

Each document starts from BDOI's FRS template (template D051, templates/bdoi_d051_frs_template.docx) through
pack_common.PackDoc: cover, document details, headers and footers, styles, revision log and table of contents are
kept; the body is written from brd02_v34_content.py (FRS), brd02_v34_pages.py (process, storyboards, outputs,
interfaces) and brd02_v34_screens.py (screens). The screenshots are in figures/brd02_v34_screens
(flows/brd02/shots_v34.sh), one full page each. The payment file layouts of the ICD are read from the schema
(tools/deliverables/schema_facts.py: the channel profiles the migrations load).

Outputs in docs/deliverables/out/Programme/BDOI_Template_FRS/BRD-02_Operations_Cashiering/ (PDFs not kept in git).
Self-checks: every screenshot named exists; every BRD row of the mapping points to an FRS entry; every screen of the
UX Design has a screenshot; every storyboard step has its screenshot.
"""

from __future__ import annotations

import argparse
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[4] / "tools" / "deliverables"))
import brd02_v34_content as C  # noqa: E402
import brd02_v34_pages as P  # noqa: E402
import brd02_v34_screens as S  # noqa: E402
import build_brd06_v13 as B6  # noqa: E402  (the document class and the colour code of the BDOI template sets)
import pack_common as pc  # noqa: E402
from colour_code import LABELS  # noqa: E402

OUT = pc.OUT_BDOI / "BRD-02_Operations_Cashiering"
SHOTS = HERE / "figures" / "brd02_v34_screens"
FLOWS = HERE / "flows" / "brd02"
COLOUR_NAME = B6.COLOUR_NAME
CREATED = {"FRS": "08/26/2026"}


class Doc(B6.Doc):
    """A document of the Operations Cashiering set (the Renewal set's document class with this set's content)."""

    def __init__(self, key: str, revisions):
        m = C.DOCS[key]
        version = C.ICD_VERSION if key == "ICD" else C.VERSION
        pc.PackDoc.__init__(self, pc.DocMeta(file=m["file"], cover=m["cover"], header_left=m["header_left"],
                                             header_right=C.HEADER_RIGHT, doc_no=m["doc_no"], version=version,
                                             date_us=C.DATE_US, created_us=CREATED.get(key, C.DATE_US),
                                             revisions=revisions))
        self.key = key
        self.shots = 0

    def screen(self, shot: str, caption: str) -> None:
        old = B6.SHOTS
        B6.SHOTS = SHOTS
        try:
            super().screen(shot, caption)
        finally:
            B6.SHOTS = old

    def legend(self) -> None:
        old = B6.C.LEGEND_NOTE
        B6.C.LEGEND_NOTE = C.LEGEND_NOTE
        try:
            super().legend()
        finally:
            B6.C.LEGEND_NOTE = old

    def personas(self, caption="Personas of Operations Cashiering") -> None:
        self.table(["Persona", "Does", "Screens", "Walkthrough account"], C.PERSONAS, [1.8, 3.6, 2.6, 1.6],
                   caption=caption)


def _dot(src: str, name: str, tmp: Path) -> Path:
    dot = tmp / f"{name}.dot"
    dot.write_text(src, encoding="utf-8")
    png = tmp / f"{name}.png"
    subprocess.run(["dot", "-Tpng", "-Gdpi=170", str(dot), "-o", str(png)], check=True)
    return png


def _lifecycle(tmp: Path) -> Path:
    return _dot(
        'digraph g { rankdir=LR; graph [fontname="Arial", nodesep=0.3, ranksep=0.35]; node [shape=box, '
        'style="rounded", fontname="Arial", fontsize=10]; edge [fontname="Arial", fontsize=8];\n'
        'C [label="Created"]; F [label="For Posting"]; R [label="Returned"]; P [label="Posted\\n(AR/OR Issued,\\n'
        'Cancelled or Reinstated)"]; X [label="Record\\nCancelled"];\n'
        'C -> F [label="Submit for Posting"]; F -> P [label="Post (second user)"]; F -> R [label="Return"]; '
        'R -> F [label="Edit and submit"]; C -> X [label="Cancel"]; }', "lifecycle", tmp)


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
            caption="Scope of Operations Cashiering in BIBS")
    d.h2("Document Set")
    d.table(["Document", "Content"], C.DOC_SET, [3.2, 6.0], caption="Operations Cashiering document set")
    d.h2("How to Read This Document")
    d.legend()
    d.h2("References")
    d.bullets(C.REFERENCES)

    d.h1("Business Requirements Mapping")
    d.para("Every Cashiering requirement of the Operations BRD maps to entries of section 3; the RTM workbook traces "
           "each one to the requirement rows, user stories, screens and test cases.")
    d.table(["BRD Req No.", "Business requirement", "FRS entries", "Note"], C.MAPPING, [1.7, 4.0, 2.3, 1.3],
            caption="Business requirements mapping")
    d.table(["BRD non-functional line", "BRD value", "Programme NFR"], C.BRD_NFR, [2.4, 4.2, 2.6],
            caption="Non-functional lines of the BRD and the programme NFR that states them")

    d.h1("Functional Requirements")
    d.para("Each item keeps BDOI's FRS.CSH number (slips corrected, section 11); sub-items are stated in their item's "
           "row. Status gives the change from version 3.3. The Example column holds the first acceptance criterion; "
           "all the criteria are in the User Stories v1.0 and the test cases of the RTM workbook.")
    d.table(["Entry", "Component", "Capability", "Status", "BRD"],
            [[f["id"], f["component"], f["capability"], f["status"], f["brd"]] for f in C.FRUM],
            [1.6, 2.0, 2.4, 1.1, 2.2], caption="Functional requirements at a glance")
    for f in C.FRUM:
        d.h2(f"{f['id']} {f['capability']}")
        d.para(f"Component: {f['component']}. Status: {f['status']}. BRD: {f['brd']}.", italic=True)
        rows, cols = [], []
        for rid, text, colour, label, rule, ex in f["reqs"]:
            lab = {"int": "  " + LABELS["integration"], "dm": "  " + LABELS["migration"]}.get(label, "")
            rows.append([rid, text + lab, rule, ex])
            cols.append(colour)
        d.ctable(["ID", "Requirement", "Rule or setting", "Example"], rows, [1.9, 4.2, 1.5, 2.2], colours=cols,
                 colour_cols={1, 2})

    d.h1("Business Rules")
    d.ctable(["ID", "Rule", "Colour"], [[a, b, COLOUR_NAME[c]] for a, b, c in C.BUSINESS_RULES], [1.3, 6.8, 1.2],
             caption="Business rules", colours=[c for *_, c in C.BUSINESS_RULES], colour_cols={1, 2})
    d.h1("Validations and Messages")
    d.para("The main messages of the module. The full list with the message codes of the services is in the "
           "Developer Specification; the field checks of each screen are in the UX Design.")
    d.table(["Screen or step", "Condition", "Message"], C.MESSAGES, [2.2, 3.1, 3.9],
            caption="Validations and messages")
    d.h1("Personas and Permissions")
    d.personas()
    d.table(C.PERSONA_HEADS, C.PERMISSIONS, [3.4] + [0.95] * 6, caption="What each persona may do (Y = yes); the "
            "rules of section 4 apply whatever the profile grants")
    d.h1("Parameters and Lists of Values")
    d.para("Settings are changed on System Parameters and applied after another user approves the change; the "
           "minimal balance rules and the payment file layouts on Cashiering Setup. Keys, types and ranges are in "
           "the Data Dictionary, sheet Parameters.")
    d.ctable(["Setting", "Default", "Range", "Area"], [r[:4] for r in C.PARAMETERS], [4.0, 2.4, 1.6, 1.2],
             caption="Cashiering settings", colours=[r[4] for r in C.PARAMETERS])
    d.table(["List", "Values"], C.LOVS, [3.0, 6.2], caption="Lists of values (codes in the Data Dictionary)")
    d.h1("Statuses")
    d.figure(_lifecycle(tmp), "Statuses of an AR / OR creation, cancellation or reinstatement record", width_cm=16.0)
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
    d.ctable(["CR", "Title", "Status", "FRS", "Man-days", "Note"],
             [[a, b, c, e, f"{sum(eff):g}", n] for a, b, c, e, eff, n in C.CRS], [1.3, 2.9, 0.9, 1.4, 0.9, 1.8],
             caption="Change requests", colours=["o" if c == "Open" else "" for _, _, c, *__ in C.CRS])
    d.ctable(["No.", "Not in the BRD, offered by the OOTB platform", "FRS"], C.OOTB_POINTS, [1.3, 5.6, 2.3],
             caption="Green points (no cost)", colours=["g"] * len(C.OOTB_POINTS), colour_cols={1})
    d.table(["No.", "Integration and data migration scope", "FRS"], C.INTEGRATION_POINTS, [1.3, 5.6, 2.3],
            caption="Grey label points (not change requests)")
    d.ctable(["No.", "Best-practice recommendation", "Priority", "Man-days"],
             [[a, b, c, str(e)] for a, b, c, e in C.BEST_PRACTICE], [1.3, 5.0, 1.4, 1.4],
             caption="Blue points (recommendations)", colours=["b"] * len(C.BEST_PRACTICE), colour_cols={1})
    d.h1("Changes from Version 3.3")
    d.table(["Item", "Change"], C.CHANGES_V33, [3.0, 6.2], caption="Changes from version 3.3")
    d.table(["Number in BDOI's FRS v3.1", "Number in this version", "Why"], C.RENUMBERED, [3.4, 2.8, 3.0],
            caption="Numbering slips corrected")
    return d


# ================================================================================================ BPS
def write_bps(tmp: Path) -> Doc:
    d = Doc("BPS", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para(f"This document shows how Operations Cashiering works end to end. The requirements are in the FRS "
           f"v{C.VERSION}; the screens in the UX Design v{C.VERSION}; the reports, receipts and notices in the "
           f"Reports, Notifications and Outputs v{C.VERSION}.")
    d.para("Each storyboard step is illustrated by a full-page screenshot taken on a test environment. The people, "
           "clients and insurers are fictitious.")
    d.personas("Personas")
    d.h1("End-to-end Journey")
    d.para(f"{len(P.JOURNEY)} steps from the payment to the deposit and the reports.")
    d.figure(FLOWS / "csh_simple.png", "Operations Cashiering: simplified process flow (swimlanes by actor)",
             width_cm=16.0)
    d.table(["Step", "Who", "What happens"], P.JOURNEY, [0.9, 2.2, 6.1], caption=f"The {len(P.JOURNEY)} steps")
    d.h1("To-be Processes")
    for p in P.PROCESSES:
        d.h2(f"{p['id']} {p['name']}")
        d.table(["Item", "Value"], [["Journey steps", p["steps"]], ["Trigger", p["trigger"]], ["Actors", p["actors"]],
                                    ["Outcome", p["outcome"]], ["Controls", p["controls"]]], [2.0, 7.2])
    d.h2("Statuses of a record")
    d.figure(_lifecycle(tmp), "Statuses of an AR / OR creation, cancellation or reinstatement record", width_cm=16.0)
    d.h1("Storyboards")
    for sb in P.STORYBOARDS:
        d.h2(f"{sb['id']} {sb['title']}")
        d.para(sb["summary"])
        d.para(f"Test data: {sb['data']}.", italic=True)
        d.table(["Step", "Who", "Screen", "Does", "Sees"],
                [[str(k + 1), w, s, a, r] for k, (w, s, a, r, _) in enumerate(sb["steps"])],
                [0.7, 1.9, 1.8, 2.6, 2.2], caption=f"{sb['id']} steps")
        for k, (w, s, a, r, shot) in enumerate(sb["steps"]):
            d.screen(shot, f"{sb['id']} step {k + 1}: {w}, {s}")
    return d


# ================================================================================================ UXD
def write_uxd(tmp: Path) -> Doc:
    d = Doc("UXD", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para("This document describes every Operations Cashiering screen of BIBS: purpose, users, access, rules and "
           "states, fields with their type, whether they are required, default, rule and message, and the actions. "
           f"The requirements and rules are in the FRS v{C.VERSION}. Each screen state is shown by one full-page "
           "screenshot on its own page, taken on a test environment with fictitious data.")
    d.para("Codes used: Y required, N optional, Cond. required on a condition, - display only. Dates show as "
           "dd-MMM-yyyy, times in Manila time, amounts with two decimals.")
    d.h1("Menu by Persona")
    d.para("The screens are under Finance › Cashiering; the Report Centre under Reports. A persona sees only its "
           "entries.")
    d.table(["Screen", "Purpose", "Personas"],
            [[a, b, ", ".join(C.PERSONA_CODES[x] for x in c.split())] for a, b, c in C.MENU],
            [2.6, 4.0, 2.6], caption="Cashiering menu by persona")
    d.h1("Screens")
    for s in S.SCREENS:
        d.h2(f"{s['id']} {s['name']}")
        d.table(["Item", "Value"], [["Purpose", s["purpose"]], ["Access", s["access"]],
                                    ["Requirements", s["frrn"]], ["Outcome", s["outcome"] or "-"]], [2.2, 7.0])
        if s["rules"]:
            d.para("Rules and states:")
            d.bullets(s["rules"])
        if s["fields"]:
            d.table(["Field", "Type", "Req.", "Default", "Rule", "Message"], s["fields"],
                    [2.3, 1.1, 0.6, 1.2, 2.6, 1.6], caption=f"{s['id']} fields")
        if s["actions"]:
            d.table(["Action", "Who", "Available when", "What happens", "Result"], s["actions"],
                    [1.6, 1.7, 1.8, 2.7, 1.4], caption=f"{s['id']} actions")
        for shot, cap, _ in s["shots"]:
            d.screen(shot, f"{s['id']}: {cap}")
    return d


# ================================================================================================ RNO
def write_rno(tmp: Path) -> Doc:
    d = Doc("RNO", C.REVISION_COMPANION)
    d.h1("Introduction")
    d.para("This document lists the reports, the receipts and other documents, the exports, the notices and alerts "
           "and the scheduled jobs of Operations Cashiering. Times are Manila time.")
    d.h1("Reports")
    d.table(["ID", "Report", "FRS", "Parameters", "Content", "Output"], P.REPORTS, [1.2, 2.2, 1.4, 1.1, 2.4, 0.9],
            caption="Cashiering reports")
    d.para(P.REPORT_NOTE)
    d.screen("ux-28-01-list", "Report Centre with the Cashiering reports (FRS.CSH.09.01)")
    d.screen("ux-29-01-record", "Daily Cash Reconciliation Reports run on screen (FRS.CSH.09.02.10)")
    d.h1("Official Receipts and Other Documents")
    d.table(["ID", "Document", "Format and file name", "Kept", "FRS"], P.DOCUMENTS, [0.9, 2.2, 3.4, 1.4, 1.3],
            caption="Receipts and documents produced")
    d.screen("sb1-06", "An issued AR on screen, with its application by component")
    d.h1("Exports")
    d.table(["Output", "Format", "Where", "Content"], P.EXPORTS, [2.6, 1.2, 2.6, 2.8], caption="Exports")
    d.h1("Notices, E-mails and Alerts")
    d.para("Notices appear under the bell; each user may add the e-mail channel on Notification Settings "
           "(FRS.CSH.10.04.03). Alerts appear on the Alerts screen. Text in angle brackets is filled in.")
    d.table(["ID", "Event", "To", "Text", "Channel"], P.NOTICES, [0.8, 2.4, 1.8, 3.2, 1.0],
            caption="Notices and alerts")
    d.h1("Scheduled Jobs")
    d.table(["Job", "When", "What it does", "Alerts"], P.JOBS, [2.6, 1.8, 3.8, 1.0], caption="Jobs")
    d.para("Every run is recorded with its start, end, outcome and counts, and can be started at once from "
           "Operations › Jobs by a user with the job permission.")
    return d


# ================================================================================================ ICD
def write_icd(tmp: Path) -> Doc:
    import schema_facts as sf  # noqa: PLC0415
    d = Doc("ICD", [(C.DATE, C.ICD_VERSION, "First issue: payment channel files, MFT intake, automatic matching, "
                     "collector requests and the hand-offs of Cashiering.", C.AUTHOR)])
    d.h1("Introduction")
    d.para("This document specifies how Operations Cashiering exchanges data with the payment channels and with the "
           "other BIBS modules. The files from systems outside BIBS are Integration scope (FRS v"
           f"{C.VERSION}, grey label); the hand-offs between BIBS modules are listed for completeness.")
    d.table(["ID", "Interface", "Direction", "Means", "Frequency", "Content"], P.INTERFACES,
            [1.0, 1.8, 1.9, 1.3, 1.3, 2.0], caption="Interfaces")
    d.h1("IF-CSH-01 to IF-CSH-04 Payment Files")
    d.bullets([
        "A file arrives by upload on Payment Files (several files of several types at once) or in the MFT folder of "
        "its type; the job CASH_MFT_INTAKE takes the files of the folders and processes them as uploads.",
        "Each file is checked for its name convention, layout, size (at most 50 MB) and duplicate content; a refused "
        "MFT file raises the alert AL-03.",
        "Each row is staged with its upload reference, matched and applied on its own (BR-CSH-17): every payment gets "
        "an AR For Printing and a result Applied, Excess, Pre-booked or Unbooked/Unmatched.",
        "The run report, the validation report, the failed rows and the raw file are downloadable from Payment Files; "
        "Process Again reprocesses a file after correction of its rows.",
        "The layouts are kept on Cashiering Setup › Payment File Layouts (field, position or column, date format) "
        "without a change to the system; the channel simulator of SIT and UAT writes test files in these layouts."])
    profiles = sf.rows("select file_type, name, name_pattern, name_example, file_format, delimiter, header_line, "
                       "date_format, field_map, control_map, max_mb, source_system from csh_channel_profile "
                       "order by file_type")
    if not profiles:
        raise SystemExit("no payment file layouts in the schema")
    d.table(["File type", "Source", "File name (example)", "Format", "Dates", "Size"],
            [[p["name"], p["source_system"], p["name_example"], ("Excel" if p["file_format"] == "EXCEL" else
              "Text or Excel" if p["file_format"] == "ANY" else "Text") + (", header line" if p["header_line"]
             else ""), p["date_format"].replace("MM/dd/yyyy", "MM/DD/YYYY"), f"{p['max_mb']} MB"]
             for p in profiles], [1.8, 1.4, 2.6, 1.6, 1.1, 0.7], caption="Payment file types")
    for p in profiles:
        d.h2(f"{p['name']} file")
        fields = [f.split("=", 1) for f in p["field_map"].split(";") if "=" in f]
        d.table(["No.", "Field", "Position or column"], [[str(k), a.strip(), b.strip()] for k, (a, b) in
                                                         enumerate(fields, 1)], [0.9, 4.0, 4.3])
        if p["control_map"]:
            d.para("Control record: " + ", ".join(f"{a} in position {b}" for a, b in
                                                  (x.split("=") for x in p["control_map"].split(";"))) + ".")
    d.h1("Automatic Matching")
    d.table(["Rule", "Detail"], [
        ["Order", "Bills Payment, CLPC, PDC and Trade: the reference is matched, first match wins, on the QPS, ISYS, "
                  "Ebix, PN, loan application or account number; then the paid amount against the outstanding."],
        ["Direct Credit", "IDENTIFY (setting CASH_DIRECT_CREDIT_MODE): BP file name and transaction number with the "
                          "paid amount identify a payment already received; NEW_PAYMENT treats every row as new."],
        ["Commission Schedule", "Reference and paid amount equal to the outstanding commission; one OR per insurer or "
                                "payee; LINE_LEVEL processes the valid lines."],
        ["Outcome", "Applied (booked account), Pre-booked (account not yet booked), Excess (above the outstanding by "
                    "more than 10.00), Unbooked/Unmatched (no match)."],
        ["Re-matching", "PREBOOKED_REMATCH every 2 hours and PAYMENT_AUTOMATCH hourly apply the waiting payments once "
                        "their account is booked."]], [2.4, 6.8], caption="Matching rules")
    d.h1("IF-CSH-05 Collector Requests")
    d.para("A collector of Collections asks to apply, refund or transfer an unapplied payment. The request is "
           "queued on Incoming Requests (status Queued), accepted by a cashier, which creates the disposition, or "
           "rejected with a reason; the collector sees the status (Accepted, Rejected, Applied). A request is "
           "decided once.")
    d.table(["Field", "Content"], [["Request number", "Given by BIBS"], ["Unapplied payment", "Reference of the "
                                   "unapplied payment"], ["Action", "Apply, Refund or Transfer"],
                                   ["Invoice", "Target invoice of an apply request"],
                                   ["Amount, payee, unit", "Amount; refund payee; receiving marketing unit"],
                                   ["Remarks", "Reason of the request"]], [3.0, 6.2], caption="Collector request")
    d.h1("IF-CSH-06 to IF-CSH-08 Module Hand-offs")
    d.table(["Hand-off", "What passes", "When"], [
        ["Disbursement", "Refund request of an approved Refund disposition; the status comes back (a returned "
                         "refund can be reversed). The approval of an insurer settlement issues its OR.",
         "Approval"],
        ["Payment Requests, Accounting Controls", "Refund validation and payment reversal requests on Incoming "
                                                  "Requests.", "On request"],
        ["New Business", "Payment confirmation of an account (applied or pre-booked).", "Posting"],
        ["Remittance", "Paid premium per invoice for the extraction; remittance returns come back as unapplied "
                       "payments; a cancelled remitted AR becomes an AR Insurer Refund.", "Posting; extraction"],
        ["General ledger", "Journal of each posting, cancellation and reinstatement, with the receipt or record "
                           "number as reference; the bank accounts are those of Post to Bank Account.", "Posting"]],
        [2.2, 5.4, 1.6], caption="Hand-offs")
    d.h1("Open Points")
    d.table(["ID", "Point", "Owner"], [[a, b, "BDO IT"] for a, b in C.DEPENDENCIES[:3]]
            + [["Q-CSH-03", "Direct Credit file: identification or new payments.", "Cashiering Process Owner"]],
            [1.4, 5.8, 2.0], caption="Open points")
    return d


WRITERS = {"FRS": write_frs, "BPS": write_bps, "UXD": write_uxd, "RNO": write_rno, "ICD": write_icd}


def checks() -> None:
    entries = {f["id"] for f in C.FRUM}
    for row in C.MAPPING:
        if row[2] == "-":
            continue
        if not all(e.strip() in entries for e in row[2].split(",")):
            raise SystemExit(f"mapping row {row[0]} points to an unknown entry")
    missing = [s["id"] for s in S.SCREENS if not s["shots"]]
    if missing:
        raise SystemExit(f"screens without a screenshot: {missing}")
    shots = [sh for s in S.SCREENS for sh, _, _ in s["shots"]] + [st[4] for sb in P.STORYBOARDS for st in sb["steps"]]
    absent = [s for s in shots if not (SHOTS / f"{s}.png").exists()]
    if absent:
        raise SystemExit(f"screenshots missing: {absent}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--no-pdf", action="store_true")
    ap.add_argument("--only", default="FRS,BPS,UXD,RNO,ICD")
    args = ap.parse_args()
    checks()
    with tempfile.TemporaryDirectory() as t:
        tmp = Path(t)
        for key in args.only.split(","):
            d = WRITERS[key](tmp)
            target = OUT / C.DOCS[key]["file"]
            n = pc.finish(d, target, tmp, pdf=not args.no_pdf)
            print(f"wrote {target.name} ({n} TOC lines, {d.shots} screenshots)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
