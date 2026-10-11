"""The review workbooks of BRD-11 User Access Maintenance that go with the FRS in BDOI's template v1.3.

    python docs/deliverables/src/programme/bdoi_template_frs/build_brd11_workbooks.py

Builds the FRS v1.3 in memory first (build_brd11_bdoi_frs.build_review), so that every number of the workbooks is
the number of the document, then writes:

  BDOI_Template_FRS/BRD-11_User_Access_Maintenance/
    BIBS_Inputs_BRD-11_Business_Unit_Requirements_Collection_v1.0.xlsx  what the Business Unit provides
    BIBS_FitGap_BRD-11_BRD_vs_OOTB_vs_Best_Practice_v1.0.xlsx           every BRD requirement against the standard
    BIBS_CR_BRD-11_Change_Request_Register_v1.0.xlsx                    change control, candidates, decision log
    BIBS_RTM_BRD-11_Test_Cases_and_Traceability_v1.0.xlsx               traceability and test cases
    BIBS_UAT_BRD-11_Business_Unit_Walkthrough_Users_v1.0.xlsx           walkthrough users and script
  Quality/
    BIBS_Coverage_BRD-11_FRS_Coverage_in_BIBS_v1.0.xlsx                 internal: FRS coverage in the BIBS environment

Sources: brd11_workbooks.yaml and brd11_review.yaml (this folder), the BIBS FRS BRD-11 and its test cases, the
comparison of BRD-11, the programme workbooks (BDOI inputs v1.2, Feature List vs OOTB, conformance register, UAT
walkthrough users). No password is written: the password column says it is issued separately.

Self-checks: every BRD ID has at least one test case and a fit-gap row; every FR item (BDOI's and added) has at least
one test case and a coverage row; every open question and input of Annex Z is in the requirements collection
workbook; every Annex Q observation has a decision row; the numbers agree with the FRS.
"""

from __future__ import annotations

import datetime as dt
import re
import sys
from collections import Counter, OrderedDict, defaultdict
from pathlib import Path

import docx
import openpyxl
import yaml

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import brd_common_final as final  # noqa: E402
import brd11_v13 as v13  # noqa: E402
import build_brd11_bdoi_frs as frs  # noqa: E402
from brd11_xlsx import Column, ReviewWorkbook  # noqa: E402

REPO = frs.REPO
UA = frs.UA
OUT = REPO / "docs" / "deliverables" / "out"
WB = yaml.safe_load((HERE / "brd11_workbooks.yaml").read_text(encoding="utf-8"))
REV = v13.REV
META = WB["meta"]
FOLDER = OUT / META["folder"]
QUALITY = OUT / META["quality"]
DATES = {k: (v if isinstance(v, dt.date) else dt.date.fromisoformat(str(v))) for k, v in META["dates"].items()}
STATUS = ["Open", "Received", "Confirmed"]
RESULT = ["Not run", "Pass", "Fail", "Blocked"]
FRUM_RE = frs.FRUM_RE


def d(x) -> str:
    if isinstance(x, (dt.date, dt.datetime)):
        return x.strftime("%d-%b-%Y")
    return str(x or "")


# ----------------------------------------------------------------------------------------------- the data
class Data:
    def __init__(self):
        self.b = frs.build_review()
        b = self.b
        self.lines = b.lines
        self.trace = b.trace
        self.our = b.our
        self.mapping = b.mapping
        self.fr_to_frum = b.fr_to_frum
        self.cases_yaml = yaml.safe_load((UA / "brd11_cases.yaml").read_text(encoding="utf-8"))
        self.personas = self.cases_yaml["personas"]
        self.items = self.frum_items()
        self.bibs_cases = self.make_bibs_cases()
        self.acc_cases = self.make_acceptance_cases()
        self.steps_by_fr = self.walkthrough_steps()
        self.inputs = {r["id"]: r for r in v13.inputs_tab()}
        self.conformance = self.read_conformance()
        cmp_data = yaml.safe_load(frs.CMP.read_text(encoding="utf-8"))
        self.cmp_platform = {m[0]: (m[3], m[5]) for m in cmp_data["mapping"]}

    def frum_items(self) -> list[dict]:
        src = docx.Document(str(REPO / frs.DOC["meta"]["source"]))
        t = src.tables[4]
        out = []
        for row in t.rows[1:]:
            entry = row.cells[0].text.strip()
            for sid, block in frs.subitem_blocks(row.cells[3]._tc).items():
                head = frs.el_text(block[0]).strip()
                title = re.sub(r"^\s*FRUM[.-]\d{3}(?:\.\d{2})+\s*", "", head).strip() or head
                add = frs.ADD["items"].get(sid, {})
                out.append({"id": sid, "entry": entry, "title": title[:90], "origin": "BDOI FRS v1.1",
                            "refs": add.get("refs", []), "ac": add.get("ac", []),
                            "statements": sum(1 for el in block[1:] if frs.el_text(el).strip())})
        for ns in frs.ADD["new_subitems"]:
            out.append({"id": ns["id"], "entry": ns["row"], "title": ns["title"], "origin": "Added in v1.2",
                        "refs": ns["refs"], "ac": ns["ac"], "statements": len(ns["shall"])})
        for item in frs.ADD["new_items"]:
            for si in item["subitems"]:
                out.append({"id": si["id"], "entry": item["id"], "title": si["title"], "origin": "Added in v1.2",
                            "refs": si["refs"], "ac": si["ac"], "statements": len(si["shall"])})
        s = self.b.stats
        if len(out) != s["bdoi_items"] + s["new_subitems"] + s["new_item_subitems"]:
            raise SystemExit("FR item count differs from the FRS")
        return out

    def persona_user(self, code: str) -> tuple[str, str]:
        p = self.personas.get(code, {"name": code, "user": "-"})
        return frs.clean(p["name"]), p["user"]

    def make_bibs_cases(self) -> list[dict]:
        scen = {s["id"]: s for s in self.cases_yaml["scenarios"]}
        data_names = {x["id"]: x["name"] for x in self.cases_yaml["data"]}
        screens = self.cases_yaml["screens"]
        brd_of = defaultdict(list)
        for rid, t in self.trace.items():
            for fr in t["frs"]:
                brd_of[fr].append(rid)
        out = []
        for fr_id, e in self.cases_yaml["frs"].items():
            n = fr_id.split("-")[-1]
            seq: Counter = Counter()
            prio = self.our[fr_id].get("priority", "Must have")
            for c in e.get("cases") or []:
                ci = c["c"]
                seq[ci] += 1
                persona, user = self.persona_user(c.get("persona", e["persona"]))
                pre = c.get("pre") or scen.get(e.get("scenario"), {}).get("pre", "")
                exp = c["expected"]
                if c.get("msg"):
                    exp += " Message: " + "; ".join(re.sub(r"[{}]", "", m) for m in c["msg"])
                out.append({
                    "id": f"TC-UA-{n}.{ci}-{seq[ci]:02d}", "title": frs.clean(c["title"]), "fr": fr_id,
                    "frum": ", ".join(self.fr_to_frum.get(fr_id, [])),
                    "brd": ", ".join(brd_of.get(fr_id, [])) or ", ".join(
                        x.split(" (")[0] for x in self.our[fr_id]["brd"] if isinstance(x, str)),
                    "persona": persona, "user": user,
                    "screen": frs.clean(screens.get(c.get("screen", e.get("screen")), c.get("screen", e.get("screen")))),
                    "pre": frs.clean(pre), "steps": "\n".join(f"{i}. {frs.clean(s)}" for i, s in enumerate(c["steps"], 1)),
                    "data": "; ".join(f"{x} {data_names.get(x, '')}" for x in (c.get("data") or e.get("data") or [])),
                    "expected": frs.clean(exp),
                    "type": "Negative" if c["type"] == "Negative" else "Positive",
                    "priority": "High" if str(prio).startswith("Must") else "Medium",
                    "source": "Test conditions of this FRS"})
        return out

    def make_acceptance_cases(self) -> list[dict]:
        out = []
        for it in self.items:
            code, screen = WB["entry_persona"][it["entry"]]
            persona, user = self.persona_user(code)
            for n, ac in enumerate(it["ac"], 1):
                neg = re.search(r"\b(refused|cannot|not offered|is not|no forgotten|never|denied)\b", ac, re.I)
                out.append({
                    "id": f"TC-{it['id'].replace('FRUM.', 'FRUM-').replace(' (2nd)', '-2ND')}-AC{n:02d}",
                    "title": f"Acceptance criterion {n} of {it['id']} {it['title']}"[:120], "fr": ", ".join(it["refs"]),
                    "frum": it["id"], "brd": self.brd_of_item(it["id"]), "persona": persona, "user": user,
                    "screen": screen, "pre": "Users, profiles and requests of the SIT/UAT data (TD-UA-01 to TD-UA-03).",
                    "steps": f"1. Sign in as {user} ({persona}).\n2. Open {screen}.\n3. Bring about the situation of "
                             f"the acceptance criterion and observe the result.",
                    "data": "TD-UA-01 Test users per role", "expected": ac,
                    "type": "Negative" if neg else "Positive", "priority": "High",
                    "source": f"Acceptance criterion of the FRS v{REV['meta']['version']}"})
        if len(out) != self.b.stats["ac"]:
            raise SystemExit("acceptance criteria count differs from the FRS")
        return out

    def brd_of_item(self, item: str) -> str:
        return ", ".join(r for r, m in self.mapping.items() if item in FRUM_RE.findall(m))

    def cases_of_item(self, item: str) -> list[str]:
        ids = [c["id"] for c in self.acc_cases if c["frum"] == item]
        ids += [c["id"] for c in self.bibs_cases if item in c["frum"].split(", ")]
        if not ids:   # a heading item (FRUM.003.02, FRUM.006.02): the cases of its sub-items
            for it in self.items:
                if it["id"].startswith(item + "."):
                    ids += self.cases_of_item(it["id"])
        return ids

    def walkthrough_steps(self) -> dict[str, list[str]]:
        scr_frs = {}
        for f in sorted((UA / "pack" / "screens").glob("*.yaml")):
            for s in yaml.safe_load(f.read_text(encoding="utf-8"))["screens"]:
                scr_frs[s["id"]] = s.get("frs", [])
        out = defaultdict(list)
        for w in yaml.safe_load((UA / "pack" / "walkthroughs.yaml").read_text(encoding="utf-8"))["walkthroughs"]:
            for i, s in enumerate(w["steps"], 1):
                for fr in scr_frs.get(s[1], []):
                    out[fr].append(f"{w['id']}.{i}")
        return out

    def read_conformance(self) -> dict[str, list[tuple]]:
        p = OUT / "Programme" / "Quality" / "BIBS_Conformance_BRD-00_End-to-End_Conformance_Register_v1.0.xlsx"
        wb = openpyxl.load_workbook(p, read_only=True)
        out = defaultdict(list)
        for r in wb["FR conformance"].iter_rows(values_only=True):
            if r[0] == "BRD-11":
                out[r[1]].append((r[4], r[7]))
        wb.close()
        return out


# ----------------------------------------------------------------------------------------- 1 requirements collection
def rc_cols():
    return [
        {"key": "id", "header": "ID", "width": 12, "mandatory": True, "format": "Text", "what": "Reference of the item (kept from the BDOI inputs workbook where it exists)"},
        {"key": "item", "header": "Item", "width": 28, "mandatory": True, "what": "What the item is about"},
        {"key": "brd", "header": "BRD ID", "width": 14, "what": "BRD requirement the item serves"},
        {"key": "frs", "header": "FRS reference", "width": 16, "what": "FR item or annex of the FRS v1.3"},
        {"key": "need", "header": "What is needed", "width": 42, "mandatory": True, "what": "What the Business Unit provides or decides"},
        {"key": "fmt", "header": "Format of the answer", "width": 26, "mandatory": True, "what": "How to answer"},
        {"key": "example", "header": "Example", "width": 24, "what": "An example of an answer"},
        {"key": "by", "header": "Provided by (role)", "width": 18, "mandatory": True, "what": "Role that answers"},
        {"key": "due", "header": "Needed by", "width": 12, "mandatory": True, "format": "Date dd-Mmm-yyyy", "what": "Date of the drop plan"},
        {"key": "resp", "header": "BU response", "width": 34, "what": "The answer of the Business Unit"},
        {"key": "status", "header": "Status", "width": 12, "mandatory": True, "values": STATUS, "what": "Open until answered; Received; Confirmed by the project"},
    ]


def input_row(data: Data, ref: str, item: str, brd: str, frsref: str, fmt: str, example: str, default_due) -> dict:
    src = data.inputs.get(ref)
    need = src["need"] if src else ""
    by = src["owner"] if src else ""
    due = src["due"] if src else d(default_due)
    return {"id": ref, "item": item, "brd": brd, "frs": frsref, "need": need, "fmt": fmt, "example": example,
            "by": by, "due": due, "resp": "", "status": "Open"}


def frs_table_text(ref: str) -> str:
    text = (UA / "FRS_BRD11_USER_ACCESS_MAINTENANCE.md").read_text(encoding="utf-8")
    m = re.search(r"^\| " + re.escape(ref) + r" \| ([^|]+)\|", text, re.M)
    return frs.clean(m.group(1).strip()) if m else ""


def build_requirements(data: Data) -> tuple[Path, dict]:
    wb = ReviewWorkbook("Business Unit Requirements Collection", doc_type="Requirements collection", brd="BRD-11",
                        version=META["version"], subtitle="BRD-11 User Access Maintenance - what the Business Unit "
                                                          "provides for the FRS v1.3")
    wb.cover_notes = ["Fill in the column BU response and set the Status of each row; questions go to the sheet "
                      "Questions and comments. Items keep the IDs of the BDOI Requirements and Inputs by BRD v1.2 "
                      "workbook where they exist."]
    cols = rc_cols()
    sheets: "OrderedDict[str, list[dict]]" = OrderedDict()
    owner = {}
    # documents
    rows = []
    docs = yaml.safe_load((UA / "pack" / "documents.yaml").read_text(encoding="utf-8"))["documents"]
    for doc in docs:
        e = REV["documents"]["items"][doc["id"]]
        rows.append({"id": e["rc"], "item": doc["name"], "brd": re.search(r"BRD [\d.]+", e["purpose"]).group(0)
                     if re.search(r"BRD [\d.]+", e["purpose"]) else "BRD p.18", "frs": f"Annex U ({doc['id']})",
                     "need": f"Confirm the layout of the {doc['name']} shown in Annex U, or give the layout. BDOI "
                             f"reference: {e['bdoi']}.",
                     "fmt": "Confirm, or a sample (Excel or PDF) with the fields in order", "example": "Confirmed as in Annex U",
                     "by": "Process Owner (Business Administration)", "due": d(DATES["decision"]), "resp": "", "status": "Open"})
    for doc in REV["documents"]["extra"]:
        rows.append({"id": doc["rc"], "item": doc["name"], "brd": "BRD 1.009" if "Bulk" in doc["name"] else "BRD NFR p.17",
                     "frs": f"Annex U ({doc['id']})", "need": f"Confirm or give the layout: {doc['purpose']}.",
                     "fmt": "Confirm, or the columns in order", "example": "Add the column Data access",
                     "by": "Process Owner (Business Administration)", "due": d(DATES["decision"]), "resp": "", "status": "Open"})
    r = input_row(data, "D-UA-04", "Bulk file layout and limits", "BRD 1.009", "FRUM.011; Annex U (DO-07)",
                  "Maximum rows per file; columns", "500 rows", DATES["decision"])
    r["need"] = r["need"] or frs_table_text("D-UA-04") or "The bulk file layout and limits."
    r["by"] = r["by"] or "Process Owner (Business Administration)"
    rows.append(r)
    rows.append({"id": "RC-DOC-09", "item": "Export file names", "brd": "BRD p.18", "frs": "FRUM.007.02; Annex Q C18",
                 "need": "Confirm BDOI's file names <Report_Name>_MMDDYYYY and Audit Logs_MMDDYYYY for every export.",
                 "fmt": "Confirm / other pattern", "example": "User Access Report_10092026",
                 "by": "Process Owner (Business Administration)", "due": d(DATES["decision"]), "resp": "", "status": "Open"})
    sheets["Documents and print formats"] = rows
    owner["Documents and print formats"] = "Process Owner (Business Administration)"
    # e-mails
    rows = []
    for i in REV["emails"]["items"]:
        rows.append({"id": i["id"], "item": i["name"], "brd": i["brd"], "frs": f"Annex T; {i['frum']}",
                     "need": f"Approve the wording, the trigger and the recipients. Subject: {i['subject']}. Text: {i['body']}",
                     "fmt": "Approve, or the new wording", "example": "Approved", "by": "Process Owner (Business Administration)",
                     "due": d(DATES["decision"]), "resp": "", "status": "Open"})
    rows.append({"id": "RC-EML-21", "item": "Sender and footer of the e-mails", "brd": "BRD NFR p.17 (10)", "frs": "Annex T",
                 "need": "The sender address of each environment and, if required, the standard footer or disclaimer of BDOI e-mails.",
                 "fmt": "Address per environment; footer text", "example": "bibs-noreply@ (domain of BDOI)", "by": "BDOI IT",
                 "due": d(DATES["uat"]), "resp": "", "status": "Open"})
    sheets["E-mail wordings"] = rows
    owner["E-mail wordings"] = "Process Owner (Business Administration)"
    # schedules
    rows = []
    for k, s in enumerate(REV["reports"]["schedules"], 1):
        rows.append({"id": f"RC-SCH-{k:02d}", "item": s[0], "brd": s[5].split(";")[0], "frs": "Annex V",
                     "need": f"Confirm the time ({s[1]}) and who is told: {s[3]}.", "fmt": "Time and recipients, or Confirm",
                     "example": s[1], "by": s[4].split(":")[-1].strip() if ":" in s[4] else "System Administrator",
                     "due": d(DATES["uat"]), "resp": "", "status": "Open"})
    k = len(rows)
    for item, need, fmt, ex, by in [
        ("Approval times and reminders", "Days per approval stage, reminder day and escalation role (proposal of Annex S).",
         "Working days per stage; reminder day; role", "2 days; reminder day 3; Business Administrator", "Process Owner (Business Administration)"),
        ("Second approval time", "Time for the second approval of privileged or out-of-hours changes.", "Working days", "1", "Information Security"),
        ("Implementation time", "Time for the System Administrator to implement an approved group-profile request.", "Working days", "1", "Process Owner (Business Administration)"),
        ("Scheduled access review report", "Whether the User Access Report and the audit log of the month are sent to Information Security, and when (proposal, UA-Q51).", "Yes / No; day and time", "First working day, 08:00", "Information Security"),
        ("Access certification", "Frequency of the access certification and who certifies (UA-Q21).", "Frequency; role", "Quarterly; unit heads", "Information Security")]:
        k += 1
        rows.append({"id": f"RC-SCH-{k:02d}", "item": item, "brd": "BRD 2.002; NFR p.11", "frs": "Annex S; Annex V",
                     "need": need, "fmt": fmt, "example": ex, "by": by, "due": d(DATES["decision"]), "resp": "", "status": "Open"})
    sheets["Schedules and timings"] = rows
    owner["Schedules and timings"] = "Information Security; Process Owner"
    # parameters
    rows = [input_row(data, "CFG-071", "Sign-in, password, session and access-request parameters", "BRD NFR p.14, p.17",
                      "Annex M.7; Annex Y", "Template UA-05 of the upload templates v1.2", "See the rows below", DATES["uat"])]
    for k, p in enumerate(frs.DOC["annex_m"]["parameters"], 1):
        rows.append({"id": f"RC-PAR-{k:02d}", "item": p[0], "brd": "BRD NFR p.13-17", "frs": "Annex M.7",
                     "need": f"Confirm the value of {p[0].lower()}: {p[2]}. Delivered: {p[1]}.", "fmt": "Value",
                     "example": str(p[1]), "by": p[3], "due": d(DATES["uat"]), "resp": "", "status": "Open"})
    for ref, item, fmt, ex in [("UQ12", "Retention of the user-access records", "Years online and in the archive", "5 and 15"),
                               ("UA-Q53", "Retention periods", "Years per record type", "5 / 15"),
                               ("UA-Q52", "Recipients of the operational notices", "E-mail addresses or roles", "BIBS support mailbox")]:
        r = input_row(data, ref, item, "BRD NFR p.11, p.14-15", "Annex V; Annex X", fmt, ex, DATES["uat"])
        r["need"] = r["need"] or frs_table_text(ref)
        r["by"] = r["by"] or "Compliance"
        rows.append(r)
    sheets["Parameters and thresholds"] = rows
    owner["Parameters and thresholds"] = "Information Security"
    # lists
    rows = []
    for ref in ("CLR-UA-05", "UQ05"):
        r = input_row(data, ref, "Business unit groups and user levels", "BRD NFR p.14", "FRUM.002.03; Annex M.8",
                      "List of values (code, name)", "MKT - Marketing", DATES["sit"])
        r["need"] = r["need"] or frs_table_text(ref)
        r["by"] = r["by"] or "User Access Administrator"
        rows.append(r)
    for k, lst in enumerate(frs.DOC["annex_m"]["lists"], 1):
        rows.append({"id": f"RC-LOV-{k:02d}", "item": lst[0], "brd": "BRD NFR p.14", "frs": "Annex M.8; template UA-06",
                     "need": f"Give or confirm the values of the list {lst[0]} (delivered: {lst[1]}). Template: tab UA-06 "
                             f"of the Master Data and Configuration Upload Templates v1.2.",
                     "fmt": "Values in template UA-06", "example": "Resigned", "by": "Process Owner (Business Administration)",
                     "due": d(DATES["sit"]), "resp": "", "status": "Open"})
    sheets["Lists of values"] = rows
    owner["Lists of values"] = "Process Owner (Business Administration)"
    # master data
    rows = []
    for ref, item, tab in [("CFG-110", "Users at go-live", "UA-01"), ("UA-Q27", "List of BIBS users", "UA-01"),
                           ("CFG-111", "Role-to-permission matrix", "UA-02"), ("UA-Q30", "Role-to-permission matrix", "UA-02"),
                           ("UA-Q29", "User Access Matrix and privilege levels", "UA-02"), ("DEP-16", "Matrix and SIT/UAT users", "UA-02"),
                           ("CFG-112", "Approvers, approval rules and limits", "UA-03"), ("UA-Q35", "Authorisation limits", "UA-03"),
                           ("CFG-113", "Separation-of-duties rules", "UA-04"), ("UA-Q49", "Data access by company and branch", "UA-01")]:
        r = input_row(data, ref, item, "BRD p.5; 4.002", "Annex Y; Annex C",
                      f"Template {tab} of the Master Data and Configuration Upload Templates v1.2 (do not copy it here)",
                      f"Filled tab {tab}", DATES["sit"])
        rows.append(r)
    sheets["Master and reference data"] = rows
    owner["Master and reference data"] = "User Access Administrator"
    # approvers and users per role
    rows = []
    for k, (persona, count) in enumerate([("Requestor", "14 users (BRD p.11)"), ("Approver", "8 users (BRD p.11)"),
                                          ("Second Approver", "to name"), ("Business Administrator", "4 users (BRD p.11)"),
                                          ("System Administrator", "1 user (BRD p.11) and the break-glass holders"),
                                          ("Information Security Officer", "to name"), ("Auditor", "to name")], 1):
        rows.append({"id": f"RC-APR-{k:02d}", "item": f"Users of the role {persona}", "brd": "BRD p.11; sections A to D",
                     "frs": "Annex M.1; Annex S", "need": f"Name the users of the role {persona} ({count}) with the unit "
                                                         f"each requests or approves for.",
                     "fmt": "User ID, name, unit", "example": "a013000101, Juan dela Cruz, Marketing",
                     "by": "User Access Administrator", "due": d(DATES["sit"]), "resp": "", "status": "Open"})
    for ref, item in [("UQ01", "Requestors and approvers"), ("UA-Q33", "Users of each user-access role"),
                      ("TD-11", "UAT testers"), ("SO-11-1", "Signature of the FRS"), ("SO-11-2", "Approval of the test plan"),
                      ("SO-11-3", "UAT sign-off certificate")]:
        r = input_row(data, ref, item, "BRD p.11", "Annex S; Signoff Sheet", "Names, or signature", "-", DATES["decision"])
        r["need"] = r["need"] or frs_table_text(ref)
        r["by"] = r["by"] or "Product Owner, MBS"
        rows.append(r)
    sheets["Approvers and users per role"] = rows
    owner["Approvers and users per role"] = "User Access Administrator"
    # integration inputs and open decisions
    integ_refs = {"CLR-UA-01", "CLR-UA-23", "CLR-UA-17", "CLR-UA-04", "UA-Q57"}
    rows_i, rows_o = [], []
    for k, q in enumerate(REV["open_items"]["open"], 1):
        first = q["refs"].split(";")[0].strip()
        brd = re.search(r"BRD (?:NFR )?[\dx][\w.]*(?: \([^)]*\))?|BRD p\.[\d-]+", q["why"])
        row = {"id": first if re.match(r"[A-Z]", first) and not first.startswith("Q.") else f"RC-DEC-{k:02d}",
               "item": re.split(r"[:?]", q["q"])[0][:70], "brd": brd.group(0) if brd else "BRD p.5-18",
               "frs": f"Annex Z.3.{k} ({q['refs']})", "need": q["q"],
               "fmt": q["fmt"], "example": "", "by": q["owner"], "due": d(q["by"]), "resp": "", "status": "Open"}
        (rows_i if first in integ_refs else rows_o).append(row)
    for ref, item in [("UA-Q19", "UIDM-ISC connector and aggregation"), ("UA-Q22", "Gateway in front of BIBS"),
                      ("UA-Q56", "E-mail channel")]:
        rows_i.append(input_row(data, ref, item, "BRD p.5-6; NFR p.13", "Annex N; Annex W", "Product / connector / frequency",
                                "Daily aggregation", DATES["decision"]))
    for drow in REV["open_items"]["dependencies"]:
        rows_i.append({"id": drow[0], "item": "Dependency", "brd": "", "frs": f"Annex Z.2; {drow[2]}", "need": drow[1],
                       "fmt": "Date when it is in place", "example": d(DATES["sit"]), "by": "BDOI IT" if "EIAM" in drow[1] or "relay" in drow[1] else "Process Owner (Business Administration)",
                       "due": d(DATES["sit"]), "resp": "", "status": "Open"})
    sheets["Integration inputs (BDOI IT)"] = rows_i
    owner["Integration inputs (BDOI IT)"] = "BDOI IT"
    sheets["Open decisions"] = rows_o
    owner["Open decisions"] = "Product Owner; Information Security"
    # self-check: every input and open item of Annex Z is here
    present = {r["id"] for rows in sheets.values() for r in rows}
    present_text = " ".join(r["frs"] for rows in sheets.values() for r in rows)
    missing = [ref for ref, kind in data.b.where.items() if kind == "input" and ref not in present]
    missing += [f"Z.3.{k}" for k in range(1, len(REV["open_items"]["open"]) + 1) if f"Annex Z.3.{k} " not in present_text]
    if missing:
        raise SystemExit(f"requirements collection lacks {missing}")
    # Start here
    index = []
    for k, (name, rows) in enumerate(sheets.items(), 1):
        dues = sorted({r["due"] for r in rows if r["due"]}, key=lambda x: dt.datetime.strptime(x, "%d-%b-%Y"))
        index.append([k, name, f"{len(rows)} items", owner[name], dues[0] if dues else "", len(rows),
                      f"=COUNTIF('{name}'!K:K,\"Open\")", "Not started", name])
    index.append([len(index) + 1, "Questions and comments", "Questions of the Business Unit on any item", "Any reviewer",
                  "", 0, "", "Not started", "Questions and comments"])
    wb.index_sheet("Start here", "Start here – BRD-11 User Access Maintenance: what the Business Unit provides",
                   "Fill in the sheets in this order (decisions and lists first, then data). Each sheet is "
                   "self-contained: title block, column guide above the header, an example row to overwrite. "
                   "Open rows: the formula counts the rows still Open.",
                   ["No.", "Sheet", "Content", "Owner", "First due", "Rows", "Rows open", "Status", "Go to"],
                   index, [5, 30, 18, 30, 12, 8, 10, 14, 10], link_col=9, status_col=8,
                   status_values=["Not started", "In progress", "Completed"])
    for name, rows in sheets.items():
        ex = dict(rows[0], resp=rows[0]["example"] or "Confirmed", status="Received")
        wb.template_sheet(name, f"{name} – BRD-11 User Access Maintenance",
                          [("Purpose", f"Items of the kind '{name}' the FRS v1.3 needs from the Business Unit."),
                           ("Who fills it in", owner[name]),
                           ("How it is used", "The project applies each confirmed answer to the FRS, the configuration or "
                                              "the templates; data go through the upload templates v1.2."),
                           ("Due", "The date in the column Needed by (drop plan)."),
                           ("Depends on", "The decisions of the sheet Open decisions where an item names one.")],
                          cols, rows, ex)
    q_cols = [
        {"key": "no", "header": "No.", "width": 6, "mandatory": True, "format": "Number", "what": "Running number"},
        {"key": "ref", "header": "Item or section", "width": 20, "mandatory": True, "what": "ID of the item, or FRS section / annex"},
        {"key": "q", "header": "Question or comment", "width": 60, "mandatory": True, "what": "The question or comment"},
        {"key": "by", "header": "Raised by", "width": 20, "mandatory": True, "what": "Name and role"},
        {"key": "date", "header": "Date", "width": 12, "format": "Date dd-Mmm-yyyy", "what": "Date raised"},
        {"key": "answer", "header": "Answer of the project", "width": 50, "what": "Filled in by iorta TechNXT"},
        {"key": "status", "header": "Status", "width": 12, "values": STATUS, "what": "Open, Received, Confirmed"}]
    wb.template_sheet("Questions and comments", "Questions and comments", [
        ("Purpose", "Any question or comment of the Business Unit on the items or on the FRS v1.3."),
        ("Who fills it in", "Any reviewer; the project answers."), ("How it is used", "Answered within 3 working days."),
        ("Due", "Before the sign-off of the FRS."), ("Depends on", "-")], q_cols, [],
        {"no": 1, "ref": "NT-07", "q": "Can the e-mail also go to the unit head?", "by": "Process Owner", "date": "",
         "answer": "", "status": "Open"})
    path = wb.save(FOLDER / "BIBS_Inputs_BRD-11_Business_Unit_Requirements_Collection_v1.0.xlsx")
    return path, {"rows": sum(len(r) for r in sheets.values()), "sheets": len(sheets)}


# ------------------------------------------------------------------------------------------------- 2 fit-gap
def build_fitgap(data: Data) -> tuple[Path, dict]:
    feat = {}
    wbf = openpyxl.load_workbook(OUT / "Programme" / "Feature_List" /
                                 "BIBS_BDOI_FeatureList_vs_OOTB_and_Best_Practice_v1.0.xlsx", read_only=True)
    for r in wbf["Feature Comparison"].iter_rows(min_row=5, values_only=True):
        if r[0] and str(r[0]).startswith("UAM"):
            feat[r[0]] = {"feature": r[3], "ootb": r[8], "assess": r[13]}
    wbf.close()
    groups = WB["fitgap_groups"]

    def group_of(rid):
        key = rid.replace("BRD ", "").replace(" (2nd)", "")
        best = None
        for g in groups:
            for k in g["keys"]:
                if key == k or key.startswith(k + "."):
                    if best is None or len(k) > len(best[1]):
                        best = (g, k)
        if best is None:
            raise SystemExit(f"no fit-gap group for {rid}")
        return best[0]
    rows = []
    for rid, text in data.lines:
        g = group_of(rid)
        frs_ua = data.trace[rid]["frs"]
        rows.append({"id": rid, "text": text, "group": g["name"], "frum": ", ".join(FRUM_RE.findall(data.mapping[rid])),
                     "fr": ", ".join(frs_ua),
                     "features": "; ".join(f"{f} {feat[f]['feature']} (OOTB: {feat[f]['ootb']})" for f in g["features"]),
                     "ootb": g["ootb"], "best": g["best"], "fit": g["fit"], "rec": g["rec"], "impact": g["impact"],
                     "env": env_status(data, frs_ua)})
    nfr_text = {n["id"]: n for n in frs.our_nfr()}
    for nid, n in nfr_text.items():
        num = int(nid.split("-")[-1])
        fit, fl, rec = WB["fitgap_nfr"][num]
        rows.append({"id": nid, "text": frs.clean(n["req"]), "group": "Non-functional (BRD section 6)",
                     "frum": ", ".join(sorted({x for f in n["frs"] for x in data.fr_to_frum.get(f, [])})), "fr": ", ".join(n["frs"]),
                     "features": "; ".join(f"{f} {feat[f]['feature']} (OOTB: {feat[f]['ootb']})" for f in fl) or "-",
                     "ootb": re.sub(r"FR-UA-\d{3}", lambda m: ", ".join(data.fr_to_frum.get(m.group(0), [])[:2]) or "-",
                                    frs.clean(n["approach"])),
                     "best": "BIBS standards for all BRDs (NFR register)",
                     "fit": fit, "rec": rec, "impact": "Annex X", "env": env_status(data, n["frs"]) if n["frs"] else "-"})
    wb = ReviewWorkbook("BRD vs Out-of-the-Box vs Best Practice", doc_type="Fit-gap", brd="BRD-11",
                        version=META["version"], subtitle="BRD-11 User Access Maintenance")
    wb.legend = [("Fit", "Met by the standard system"), ("Configure", "Met by set-up or settings"),
                 ("Extend", "Met by an extension of the standard system"), ("Recommend change", "A change to the BRD text is recommended")]
    counts = Counter(r["fit"] for r in rows)
    by_group = defaultdict(Counter)
    for r in rows:
        by_group[r["group"]][r["fit"]] += 1
    summ = [{"group": "All BRD requirements", "n": len(rows), **{f: counts[f] for f in WB["fit_values"]}}]
    summ += [{"group": g, "n": sum(c.values()), **{f: c[f] for f in WB["fit_values"]}} for g, c in by_group.items()]
    wb.sheet("Summary", [Column("group", "Group", 46, "Function of the BRD"), Column("n", "Requirements", 14, "Count"),
                         Column("Fit", "Fit", 10, "Count"), Column("Configure", "Configure", 12, "Count"),
                         Column("Extend", "Extend", 10, "Count"), Column("Recommend change", "Recommend change", 18, "Count")],
             summ, description=f"Counts by fit: {len(data.lines)} BRD requirement lines and {len(nfr_text)} non-functional rows")
    wb.sheet("Fit-Gap", [
        Column("id", "BRD ID", 18, "BRD requirement ID (UAM-NFR-nn for the rows of section 6)"),
        Column("text", "BRD text", 44, "Requirement as printed in the BRD"),
        Column("group", "Function", 24, "Function of the BRD"),
        Column("frum", "FR item (BDOI format)", 22, "FR items of the FRS v1.3"),
        Column("features", "Feature list reference", 40, "Rows of the BIBS Feature List vs OOTB workbook"),
        Column("ootb", "What BIBS offers out of the box", 46, "Standard capability or configuration"),
        Column("best", "Insurance-broking best practice", 46, "Practice of the Philippine market and international broking"),
        Column("fit", "Fit", 16, "Fit class", values=WB["fit_values"], status=True),
        Column("rec", "Recommendation", 40, "What to do"),
        Column("impact", "Impact", 22, "Where it lands"),
        Column("env", "In the BIBS environment", 20, "Available / Partly available / Planned for")],
        rows, description="One row per BRD requirement")
    path = wb.save(FOLDER / "BIBS_FitGap_BRD-11_BRD_vs_OOTB_vs_Best_Practice_v1.0.xlsx")
    if {r["id"] for r in rows} < {rid for rid, _ in data.lines}:
        raise SystemExit("fit-gap lacks BRD IDs")
    return path, {"rows": len(rows), **counts}


def env_status(data: Data, frs_ua: list[str]) -> str:
    res = [x[1] for f in frs_ua for x in data.conformance.get(f, [])]
    if not res:
        return "Planned for"
    if all(x in ("Conformant", "Partly conformant") for x in res):
        return "Available"
    return "Partly available"


# ------------------------------------------------------------------------------------------------- 3 change requests
def build_cr(data: Data) -> tuple[Path, dict]:
    wb = ReviewWorkbook("Change Request Register", doc_type="Change control", brd="BRD-11", version=META["version"],
                        subtitle="BRD-11 User Access Maintenance - after the sign-off of the FRS v1.3")
    steps = [{"step": s[0], "what": s[1], "who": s[2], "record": s[3]} for s in REV["change_control"]["steps"]]
    wb.sheet("How to use", [Column("step", "Step", 12, "Step of the process"), Column("what", "What happens", 60, ""),
                            Column("who", "Who", 36, ""), Column("record", "Record in this workbook", 36, "")],
             steps, description="Raise -> assess -> approve -> schedule -> test -> close (FRS Annex AA)")
    rows = []
    for k, c in enumerate(WB["cr_candidates"], 1):
        rows.append({"no": f"CR-UA-{k:03d}", "date": "09-Oct-2026", "by": "iorta TechNXT Project Team", "brd": c[1],
                     "frs": c[0], "desc": f"{c[2]}: {c[3]}", "reason": c[4], "type": c[5], "scope": c[6],
                     "schedule": c[7], "cost": c[8], "other": c[9], "tests": c[10], "priority": c[11],
                     "status": "Candidate - for decision", "decision": "", "approved": "", "ddate": "", "drop": "",
                     "linked": ", ".join(data.cases_of_item(c[0].split(",")[0])[:5]) if FRUM_RE.match(c[0]) else ""})
    wb.sheet("Register", [
        Column("no", "CR No.", 12, "Change request number"), Column("date", "Date raised", 12, "dd-Mmm-yyyy"),
        Column("by", "Raised by", 18, "Name and role"), Column("brd", "BRD ID", 16, "BRD requirement"),
        Column("frs", "FRS reference", 16, "FR item or annex"), Column("desc", "Description", 46, "The change"),
        Column("reason", "Reason", 36, "Why"), Column("type", "Type", 18, "Kind of change", values=WB["cr_types"]),
        Column("scope", "Impact on scope", 22, ""), Column("schedule", "Impact on schedule", 14, ""),
        Column("cost", "Impact on cost", 12, ""), Column("other", "Impact on other BRDs", 18, ""),
        Column("tests", "Impact on tests", 18, ""), Column("priority", "Priority", 10, "", values=["High", "Medium", "Low"]),
        Column("status", "Status", 20, "", values=WB["cr_status"], status=True),
        Column("decision", "Decision", 24, "Decision of the approvers"), Column("approved", "Approved by", 18, ""),
        Column("ddate", "Decision date", 12, ""), Column("drop", "Target drop", 12, ""),
        Column("linked", "Linked test cases", 30, "Test cases of the Test Cases and Traceability workbook")],
        rows, description="Candidate changes known at v1.3 (status Candidate - for decision) and new requests")
    form_cols = [
        {"key": "field", "header": "Field", "width": 30, "mandatory": True, "what": "Part of the assessment"},
        {"key": "value", "header": "Assessment", "width": 70, "mandatory": True, "what": "Filled in by the project"},
        {"key": "by", "header": "Assessed by", "width": 20, "what": "Name"}]
    fields = ["CR No.", "Summary of the change", "BRD IDs and FR items affected", "Screens, messages, e-mails affected",
              "Other BRDs affected (Annex W)", "Effort (mandays)", "Schedule impact (drop)", "Cost",
              "Test cases to add or change", "Training and documents", "Risks", "Recommendation"]
    wb.template_sheet("Impact assessment form", "Impact assessment form", [
        ("Purpose", "The assessment of one change request before the decision."),
        ("Who fills it in", "iorta TechNXT Project Team with the System Analyst (BDO ITG)"),
        ("How it is used", "Copied for each change; its conclusion goes to the Register."),
        ("Due", "Within 5 working days of the request."), ("Depends on", "Register")],
        form_cols, [{"field": f, "value": "", "by": ""} for f in fields],
        {"field": "CR No.", "value": "CR-UA-001", "by": "Project analyst"})
    dec = [{"no": q["no"], "section": q["section"], "topic": q["topic"], "proposal": q["proposal"],
            "by": q["decide_by"], "decision": "", "dby": "", "ddate": "", "cr": "", "status": "Open"} for q in data.b.q_rows]
    if len(dec) != data.b.stats["observations"]:
        raise SystemExit("decision log differs from Annex Q")
    wb.sheet("Decisions log", [
        Column("no", "Annex Q No.", 14, "Row of Annex Q of the FRS v1.3"), Column("section", "Section", 8, ""),
        Column("topic", "Topic", 26, ""), Column("proposal", "Proposed in the FRS", 60, ""),
        Column("by", "Decision by", 26, ""), Column("decision", "Decision", 30, "Adopt proposal / Keep BDOI text / Other"),
        Column("dby", "Decided by (name)", 18, ""), Column("ddate", "Date", 12, ""),
        Column("cr", "CR No. (if a change follows)", 14, ""),
        Column("status", "Status", 12, "", values=["Open", "Decided", "Withdrawn"], status=True)],
        dec, description="One row per observation of Annex Q, for the decision of the Business Unit")
    path = wb.save(FOLDER / "BIBS_CR_BRD-11_Change_Request_Register_v1.0.xlsx")
    return path, {"candidates": len(rows), "decisions": len(dec)}


# ------------------------------------------------------------------------------------------------- 4 RTM
def answered_rows(data: Data) -> list[dict]:
    drop0 = {}
    p = REPO / "docs" / "deliverables" / "src" / "programme" / "bdoi_inputs" / "drop0_requirements.yaml"
    for r in (yaml.safe_load(p.read_text(encoding="utf-8")) or {}).get("BRD-11", []):
        if isinstance(r, dict) and r.get("ref"):
            drop0[r["ref"]] = r.get("need", "")
    out = []
    for ref, (clause, answer) in REV["open_items"]["answered"].items():
        q = (data.inputs.get(ref) or {}).get("need") or drop0.get(ref) or frs_table_text(ref)
        out.append({"ref": ref, "q": frs.clean(q) if q else "-", "clause": clause, "answer": answer,
                    "kept": "Annex Q decision" if re.search(r"decision C\d\d", answer) else "-"})
    return out


def build_rtm(data: Data) -> tuple[Path, dict]:
    cases = data.acc_cases + data.bibs_cases
    case_ids_by_fr = defaultdict(list)
    for c in data.bibs_cases:
        case_ids_by_fr[c["fr"]].append(c["id"])
    trace_rows = []
    for rid, text in data.lines:
        frum = FRUM_RE.findall(data.mapping[rid])
        fr_ua = data.trace[rid]["frs"]
        ids = sorted({i for f in fr_ua for i in case_ids_by_fr[f]} | {c["id"] for c in data.acc_cases if c["frum"] in frum})
        screens = "; ".join(dict.fromkeys(frs.clean(data.our[f].get("screens", "")) for f in fr_ua))
        steps = sorted({s for f in fr_ua for s in data.steps_by_fr.get(f, [])}, key=lambda x: (x.split(".")[0], int(x.split(".")[1])))
        trace_rows.append({"id": rid, "text": text, "frum": ", ".join(frum), "fr": ", ".join(fr_ua), "n": len(ids),
                           "cases": ", ".join(ids), "screens": screens, "steps": ", ".join(steps) or "-"})
    for n in frs.our_nfr():
        if n["frs"]:
            ids = sorted({i for f in n["frs"] for i in case_ids_by_fr[f]})
            trace_rows.append({"id": n["id"], "text": frs.clean(n["req"]), "frum": v13_frum(data, n["frs"]),
                               "fr": ", ".join(n["frs"]), "n": len(ids), "cases": ", ".join(ids),
                               "screens": "; ".join(dict.fromkeys(frs.clean(data.our[f].get("screens", "")) for f in n["frs"])),
                               "steps": ", ".join(sorted({s for f in n["frs"] for s in data.steps_by_fr.get(f, [])})) or "-"})
    no_case = [r["id"] for r in trace_rows if r["n"] == 0]
    if no_case:
        raise SystemExit(f"BRD IDs without a test case: {no_case}")
    item_rows = []
    for it in data.items:
        ids = data.cases_of_item(it["id"])
        if not ids:
            raise SystemExit(f"{it['id']} has no test case")
        item_rows.append({"id": it["id"], "title": it["title"], "origin": it["origin"], "ac": len(it["ac"]),
                          "n": len(ids), "cases": ", ".join(ids[:12]) + (" ..." if len(ids) > 12 else "")})
    expected_total = frs.bibs_case_count() + data.b.stats["ac"]
    if len(cases) != expected_total:
        raise SystemExit("test case count differs from the FRS")
    wb = ReviewWorkbook("Test Cases and Traceability", doc_type="Requirements traceability", brd="BRD-11",
                        version=META["version"], subtitle="BRD-11 User Access Maintenance - FRS in BDOI's template v1.3")
    summary = [
        {"what": "BRD requirement IDs", "n": len(data.lines), "proof": f"{sum(1 for r in trace_rows[:len(data.lines)] if r['n'])} with at least one test case (sheet Traceability)"},
        {"what": "Non-functional rows with a function", "n": len(trace_rows) - len(data.lines), "proof": "Each with at least one test case"},
        {"what": "FR items (BDOI's and added)", "n": len(data.items), "proof": f"{sum(1 for r in item_rows if r['n'])} with at least one test case (sheet FR items)"},
        {"what": "Acceptance criteria", "n": data.b.stats["ac"], "proof": "One acceptance test case each"},
        {"what": "Test cases", "n": len(cases), "proof": f"{len(data.bibs_cases)} cases of the test conditions keyed to the FR items; {len(data.acc_cases)} acceptance cases"},
        {"what": "Positive / negative", "n": f"{sum(1 for c in cases if c['type'] == 'Positive')} / {sum(1 for c in cases if c['type'] == 'Negative')}", "proof": "-"},
        {"what": "Questions answered in the BDOI FRS", "n": len(REV["open_items"]["answered"]), "proof": "Sheet Answered in the BDOI FRS"},
    ]
    wb.sheet("Summary", [Column("what", "Item", 40, ""), Column("n", "Count", 14, ""), Column("proof", "Proof", 80, "")],
             summary, description="Counts and the proof that every BRD ID and every FR item is covered")
    wb.sheet("Traceability", [
        Column("id", "BRD ID", 18, "BRD requirement ID"), Column("text", "BRD requirement", 44, ""),
        Column("frum", "FR item (BDOI format)", 26, "FR items of the FRS v1.3"),
        Column("n", "Test cases", 10, "Count"),
        Column("cases", "Test case IDs", 60, "Sheet Test cases"), Column("screens", "Screen", 30, "Screen of the FR"),
        Column("steps", "Walkthrough steps", 26, "Steps of the walkthrough users workbook")],
        trace_rows, description="BRD ID -> FR item -> test cases -> screen -> walkthrough step")
    wb.sheet("FR items", [Column("id", "FR item", 16, ""), Column("title", "Title", 44, ""), Column("origin", "Origin", 16, ""),
                          Column("ac", "Acceptance criteria", 12, ""), Column("n", "Test cases", 10, ""),
                          Column("cases", "Test case IDs", 70, "")],
             item_rows, description="Every FR item of the FRS v1.3 with its test cases")
    case_cols = [
        Column("id", "Test case ID", 26, "TC-UA-nnn.c-ss: test condition c and its case ss; TC-FRUM-...-ACnn: acceptance criterion"),
        Column("title", "Title", 36, ""), Column("brd", "BRD ID", 22, ""), Column("frum", "FR item", 18, ""),
        Column("persona", "Persona", 18, ""),
        Column("user", "Sign-in ID", 12, "The password is issued separately by the iorta TechNXT Project Team"),
        Column("screen", "Screen", 22, ""), Column("pre", "Preconditions", 30, ""), Column("steps", "Steps", 50, ""),
        Column("data", "Test data", 22, ""), Column("expected", "Expected result", 50, ""),
        Column("type", "Type", 10, "", values=["Positive", "Negative"]),
        Column("priority", "Priority", 10, "", values=["High", "Medium", "Low"]),
        Column("result", "Result", 10, "", values=RESULT, status=True), Column("tester", "Tested by", 14, ""),
        Column("date", "Date", 12, ""), Column("obs", "Observation ref", 14, ""), Column("remarks", "Remarks", 24, ""),
        Column("source", "Source", 22, "")]
    for c in cases:
        c.update({"result": "Not run", "tester": "", "date": "", "obs": "", "remarks": ""})
    wb.sheet("Test cases", case_cols, cases, description="Test cases of the FRS v1.3 with the execution columns")
    ans = answered_rows(data)
    wb.sheet("Answered in the BDOI FRS", [
        Column("ref", "Reference", 14, "CLR-UA, UQ, IQ, UA-Q, A-UA"), Column("q", "Question or assumption", 60, ""),
        Column("clause", "BDOI FRS clause", 30, ""), Column("answer", "Answer in BDOI's FRS", 60, ""),
        Column("kept", "Still a decision?", 16, "Where BDOI's answer differs from the BRD, the decision stays in Annex Q")],
        ans, description="Open items dropped from the FRS because BDOI's FRS answers them")
    crit = [{"kind": "Entry", "no": i, "criterion": c} for i, c in enumerate(WB["uat_criteria"]["entry"], 1)]
    crit += [{"kind": "Exit", "no": i, "criterion": c} for i, c in enumerate(WB["uat_criteria"]["exit"], 1)]
    wb.sheet("UAT entry and exit", [Column("kind", "Entry / exit", 12, ""), Column("no", "No.", 6, ""),
                                    Column("criterion", "Criterion", 100, ""),
                                    ], crit, description="UAT entry and exit criteria of BRD-11")
    path = wb.save(FOLDER / "BIBS_RTM_BRD-11_Test_Cases_and_Traceability_v1.0.xlsx")
    return path, {"trace": len(trace_rows), "cases": len(cases), "answered": len(ans), "items": len(item_rows)}


def v13_frum(data: Data, frs_ua: list[str]) -> str:
    return ", ".join(sorted({x for f in frs_ua for x in data.fr_to_frum.get(f, [])}, key=frs.frum_key))


# ------------------------------------------------------------------------------------------------- 5 UAT users
PERSONA_MENU = {"Requestor": 1, "Approver": 2, "Second approver": 3, "Business Administrator": 4,
                "System Administrator": 5, "Information Security Officer": 6, "Auditor": 7}


def build_uat(data: Data) -> tuple[Path, dict]:
    src = OUT / "Programme" / "UAT" / "BIBS_UAT_BRD-00_Walkthrough_Users_and_Sign-in_v1.0.xlsx"
    w = openpyxl.load_workbook(src, read_only=True)
    menus = next(f for f in v13.FLOWS if f["id"] == "brd11_fig_menus")["rows"]
    users = []
    ws = w["BRD-11 User Access Maintenance"]
    head = None
    for r in ws.iter_rows(values_only=True):
        if r and r[0] == "Persona":
            head = list(r)
            continue
        if head and r and r[0]:
            row = dict(zip(head, r))
            col = PERSONA_MENU.get(row["Persona"])
            if col:
                menu = "; ".join(m[0] for m in menus if m[col])
            elif "New Business" in row["Persona"]:
                menu = "Home › My Approvals; User Access › Access Requests, Group Profile Requests, Bulk Request, User Access Matrix; Help › My Profile"
            else:
                menu = "The menu of the Marketing Account Officer (no User Access entry); Help › My Profile"
            users.append({"persona": row["Persona"], "name": row["User name"], "signin": row["Sign-in ID"],
                          "profiles": row["Group profile"], "unit": row["Company / branch / unit"],
                          "does": row["What this user does in the walkthrough"], "start": row["Menu path to start"],
                          "menus": menu, "steps": row["Walkthrough steps"], "password": META["password_text"]})
    script = []
    for r in w["Walkthrough Script"].iter_rows(values_only=True):
        if r and r[0] == "BRD-11":
            script.append({"wt": r[1], "step": r[2], "persona": r[3], "signin": r[4], "action": r[5],
                           "expected": r[6], "result": "Not run", "tester": "", "date": "", "remarks": ""})
    w.close()
    if not users or not script:
        raise SystemExit("walkthrough users or script not found")
    wb = ReviewWorkbook("Business Unit Walkthrough Users", doc_type="UAT walkthrough", brd="BRD-11",
                        version=META["version"], subtitle="BRD-11 User Access Maintenance - sign-in IDs for the walkthrough")
    wb.cover_notes = [f"Environment: {META['environment']}. Passwords: {META['password_text']}; no password is "
                      f"written in this workbook."]
    wb.sheet("Users", [
        Column("persona", "Persona", 24, "Business role of the FRS"), Column("name", "User name", 22, "As shown on the screens"),
        Column("signin", "Sign-in ID", 14, "User ID to type on the Login page"),
        Column("profiles", "Group profiles", 26, "Role held"), Column("unit", "Company / branch / unit", 30, ""),
        Column("does", "What the user does in the walkthrough", 40, ""), Column("start", "Menu path to start", 30, ""),
        Column("menus", "Menus the user should see", 50, "User Access Maintenance entries of the user's menu"),
        Column("steps", "Walkthrough steps", 24, "Steps of the sheet Walkthrough script"),
        Column("password", "Password", 26, "Never written here")],
        users, description=f"{META['environment']}. Password: {META['password_text']}")
    wb.sheet("Walkthrough script", [
        Column("wt", "Walkthrough", 30, ""), Column("step", "Step", 8, ""), Column("persona", "Persona", 22, ""),
        Column("signin", "Sign-in ID", 14, ""), Column("action", "Action", 56, ""),
        Column("expected", "Expected result", 56, ""), Column("result", "Result", 10, "", values=RESULT, status=True),
        Column("tester", "Done by", 14, ""), Column("date", "Date", 12, ""), Column("remarks", "Remarks", 24, "")],
        script, description="The walkthrough of BRD-11 for the Business Unit (FRS Annex O)")
    path = wb.save(FOLDER / "BIBS_UAT_BRD-11_Business_Unit_Walkthrough_Users_v1.0.xlsx")
    return path, {"users": len(users), "steps": len(script)}


# ------------------------------------------------------------------------------------------------- 6 coverage
def build_coverage(data: Data) -> tuple[Path, dict]:
    item_rows, ac_rows = [], []
    for it in data.items:
        refs = it["refs"]
        conf = [x for f in refs for x in data.conformance.get(f, [])]
        evidence = []
        if conf:
            evidence.append("Conformance register: " + "; ".join(
                f"{f} {Counter(x[1] for x in data.conformance.get(f, [])).most_common()}" for f in refs))
        steps = sorted({s for f in refs for s in data.steps_by_fr.get(f, [])})
        if steps:
            evidence.append("Walkthrough " + ", ".join(steps[:8]))
        if it["id"] in WB["coverage_items"]:
            status, note = WB["coverage_items"][it["id"]]
        elif it["id"] in data.cmp_platform:
            plat, cnote = data.cmp_platform[it["id"]]
            status = {"Yes": "Yes", "Partly": "Partly", "No": "No"}.get(plat, "Partly")
            note = "Comparison of BDOI's FRS with the platform: " + frs.clean(cnote)
        elif not refs:
            status, note = "Partly", "Heading item: see its sub-items."
        elif all(x[1] in ("Conformant", "Partly conformant") for x in conf):
            status, note = "Yes", "Shown working in the end-to-end run; criteria without an automated check are confirmed in UAT."
        else:
            status, note = "Partly", "Not reached by the end-to-end run; tested in UAT."
        cases = data.cases_of_item(it["id"])
        item_rows.append({"group": it["entry"], "id": it["id"], "title": it["title"], "origin": it["origin"],
                          "refs": ", ".join(refs) or "-", "status": status,
                          "evidence": f"{len(cases)} test cases; " + " | ".join(evidence), "note": note})
        for n, ac in enumerate(it["ac"], 1):
            key = f"{it['id']}#{n}"
            if key in WB["coverage_acs"]:
                ast, anote = WB["coverage_acs"][key]
            elif status == "No":
                ast, anote = "No", note
            else:
                ast, anote = "Yes", "Available; confirmed with the acceptance test case in UAT."
            ac_rows.append({"group": it["entry"], "id": it["id"], "no": n, "ac": ac, "status": ast,
                            "evidence": next(c["id"] for c in data.acc_cases if c["frum"] == it["id"] and c["id"].endswith(f"AC{n:02d}")),
                            "note": anote})

    def pct(rows, key="status"):
        c = Counter(r[key] for r in rows)
        n = len(rows)
        return c, (round(100 * c["Yes"] / n, 1) if n else 0), (round(100 * (c["Yes"] + c["Partly"]) / n, 1) if n else 0)
    summ = []
    groups = list(dict.fromkeys(r["group"] for r in item_rows))
    for g in ["All"] + groups:
        ir = [r for r in item_rows if g == "All" or r["group"] == g]
        ar = [r for r in ac_rows if g == "All" or r["group"] == g]
        ci, yi, pi = pct(ir)
        ca, ya, pa = pct(ar)
        summ.append({"group": "All FR items" if g == "All" else g, "items": len(ir), "iy": ci["Yes"], "ip": ci["Partly"],
                     "in": ci["No"], "ipct": yi, "ippct": pi, "acs": len(ar), "ay": ca["Yes"], "ap": ca["Partly"],
                     "an": ca["No"], "apct": ya, "appct": pa})
    wb = ReviewWorkbook("FRS Coverage in BIBS", doc_type="Internal coverage", brd="BRD-11", version=META["version"],
                        subtitle="BRD-11 User Access Maintenance - FRS in BDOI's template v1.3 (internal)",
                        classification="Internal - iorta TechNXT")
    wb.legend = [("Yes", "Available in the BIBS environment"), ("Partly", "Partly available (reason in the note)"),
                 ("No", "Planned for a later release (reason in the note)")]
    num = lambda k, h, w=10: Column(k, h, w, "", kind="number")  # noqa: E731
    wb.sheet("Summary", [Column("group", "FR group", 22, "FR entry"), num("items", "FR items"), num("iy", "Yes"),
                         num("ip", "Partly"), num("in", "No"), num("ipct", "% items available", 14),
                         num("ippct", "% items available or partly", 16), num("acs", "Acceptance criteria", 14),
                         num("ay", "Yes"), num("ap", "Partly"), num("an", "No"), num("apct", "% criteria available", 14),
                         num("appct", "% criteria available or partly", 16)],
             summ, description="Coverage of the FRS v1.3 in the BIBS environment, by item count and by acceptance criterion")
    st = ["Yes", "Partly", "No"]
    wb.sheet("FR items", [Column("group", "FR group", 12, ""), Column("id", "FR item", 16, ""), Column("title", "Title", 40, ""),
                          Column("origin", "Origin", 14, ""), Column("refs", "BIBS reference FR", 22, ""),
                          Column("status", "Available", 12, "", values=st, status=True),
                          Column("evidence", "Evidence", 60, ""), Column("note", "Note", 60, "")],
             item_rows, description="Every FR item and sub-item of the FRS v1.3")
    wb.sheet("Acceptance criteria", [Column("group", "FR group", 12, ""), Column("id", "FR item", 16, ""),
                                     Column("no", "AC", 6, ""), Column("ac", "Acceptance criterion", 60, ""),
                                     Column("status", "Available", 12, "", values=st, status=True),
                                     Column("evidence", "Test case", 28, ""), Column("note", "Note", 50, "")],
             ac_rows, description="Every acceptance criterion of the FRS v1.3")
    path = wb.save(QUALITY / "BIBS_Coverage_BRD-11_FRS_Coverage_in_BIBS_v1.0.xlsx")
    return path, {"items": len(item_rows), "acs": len(ac_rows), "all": summ[0]}


def main() -> int:
    data = Data()
    FOLDER.mkdir(parents=True, exist_ok=True)
    results = {}
    for name, fn in (("requirements", build_requirements), ("fitgap", build_fitgap), ("cr", build_cr),
                     ("rtm", build_rtm), ("uat", build_uat), ("coverage", build_coverage)):
        path, stats = fn(data)
        results[name] = stats
        print(f"wrote {path.relative_to(REPO)} ({path.stat().st_size // 1024} KB): {stats}")
        if name != "coverage":  # the internal coverage workbook may keep the earlier references
            final.check_no_references([path])
    return 0


if __name__ == "__main__":
    sys.exit(main())
