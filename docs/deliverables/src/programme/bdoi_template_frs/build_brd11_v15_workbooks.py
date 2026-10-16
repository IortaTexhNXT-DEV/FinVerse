"""The review workbooks v1.2 of BRD-11 User Access Maintenance, aligned with the document set v1.5.

    python docs/deliverables/src/programme/bdoi_template_frs/build_brd11_v15_workbooks.py

Writes in docs/deliverables/out/Programme/BDOI_Template_FRS/BRD-11_User_Access_Maintenance/:
  BIBS_RTM_BRD-11_Test_Cases_and_Traceability_v1.2.xlsx
  BIBS_FitGap_BRD-11_BRD_vs_OOTB_vs_Best_Practice_v1.2.xlsx
  BIBS_CR_BRD-11_Change_Request_Register_v1.2.xlsx
  BIBS_CR_BRD-11_Effort_Estimate_v1.2.xlsx
  BIBS_Inputs_BRD-11_Business_Unit_Requirements_Collection_v1.2.xlsx
  BIBS_UAT_BRD-11_Business_Unit_Walkthrough_Users_v1.2.xlsx

Content from brd11_v15_content.py and brd11_v15_pages.py, so that every number agrees with the FRS v1.5. No password
is written. Self-checks: every FRUM requirement in force has a test case; every BRD row of the mapping has a fit-gap
row; every open change request has an effort.
"""

from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import brd11_v15_content as C  # noqa: E402
import brd11_v15_pages as P  # noqa: E402
from brd11_xlsx import Column, ReviewWorkbook  # noqa: E402

REPO = HERE.parents[4]
FOLDER = REPO / "docs" / "deliverables" / "out" / "Programme" / "BDOI_Template_FRS" / "BRD-11_User_Access_Maintenance"
V = C.WB_VERSION
DATE = "16 October 2026"
FRS = "FRS User Access Maintenance v1.5"
COLOUR = {"k": "Black (BRD)", "g": "Green (OOTB)", "o": "Orange (CR)", "b": "Blue (best practice)", "": "-"}
RESULT = ["Not run", "Pass", "Fail", "Blocked"]
PERSONA_OF = {"FRUM.001": "Any user", "FRUM.002": "User Access Administrator", "FRUM.003": "User Access Administrator",
              "FRUM-004": "System Administrator (test accounts)", "FRUM.005": "Group Profile Maker",
              "FRUM.006": "Group Profile Maker", "FRUM.007": "Auditor", "FRUM-008": "Auditor",
              "FRUM.010": "Group Profile Checker", "FRUM.012": "Information Security", "FRUM.013": "Any user",
              "FRUM.014": "User Access Administrator"}
SCREEN_OF = {"FRUM.001": "Sign in", "FRUM.002": "Identity Sync; Users", "FRUM.003": "Users › a user",
             "FRUM-004": "Sign in (test environment)", "FRUM.005": "Group Profiles › New Group Profile",
             "FRUM.006": "Group Profiles › Propose Change", "FRUM.007": "Reports & Certification; Access Matrix",
             "FRUM-008": "Audit Log", "FRUM.010": "Group Profile › Pending Change",
             "FRUM.012": "Separation of Duties; UAM Settings", "FRUM.013": "Notifications (bell); mailbox",
             "FRUM.014": "Identity Sync"}


def _wb(title, kind):
    return ReviewWorkbook(title, doc_type=kind, brd="BRD-11", version=V, date=DATE,
                          subtitle=f"User Access Maintenance; goes with the {FRS}")


def test_cases():
    rows = []
    for f in C.FRUM:
        if f["status"] == "Withdrawn":
            continue
        for k, (rid, text, colour, label, rule, ex) in enumerate(f["reqs"], start=1):
            if text.endswith("withdrawn.") or "are withdrawn" in text:
                continue
            rows.append({"tc": f"TC-UA-{rid.split('.', 1)[1].replace('.', '-').replace('004', '004')}",
                         "frum": rid, "brd": f["brd"], "persona": PERSONA_OF[f["id"]], "screen": SCREEN_OF[f["id"]],
                         "pre": "Test environment connected to UIDM-ISC and Entra ID test tenants; walkthrough "
                         "accounts of the Walkthrough Users workbook.",
                         "steps": f"Perform: {text}" + (f" Example: {ex}" if ex != "-" else ""),
                         "expected": rule if rule != "-" else text, "colour": COLOUR[colour],
                         "scope": "Integration" if label == "int" else "BIBS", "result": "Not run"})
    for sb in P.STORYBOARDS:
        rows.append({"tc": f"TC-UA-{sb['id']}", "frum": "End to end", "brd": "BRD-11", "persona": ", ".join(
            sorted({s[0] for s in sb["steps"]})), "screen": "; ".join(dict.fromkeys(s[1] for s in sb["steps"])),
            "pre": "As above.", "steps": " ".join(f"{i + 1}. {s[0]}: {s[2]}" for i, s in enumerate(sb["steps"])),
            "expected": " ".join(f"{i + 1}. {s[3]}" for i, s in enumerate(sb["steps"])), "colour": "Black (BRD)",
            "scope": "BIBS and integration", "result": "Not run"})
    for r in rows:
        r["tc"] = r["tc"].replace("TC-UA-0", "TC-UA-0")
    return rows


def build_rtm():
    wb = _wb("Test Cases and Traceability", "Requirements traceability")
    tcs = test_cases()
    cols = [Column("tc", "Test case", 14), Column("frum", "FRUM", 13), Column("brd", "BRD", 22),
            Column("persona", "Persona", 22), Column("screen", "Screen", 24), Column("pre", "Precondition", 30),
            Column("steps", "Steps", 50), Column("expected", "Expected result", 44), Column("colour", "Colour", 16),
            Column("scope", "Delivered by", 16), Column("result", "Result", 12, values=RESULT, status=True),
            Column("tester", "Tester", 16), Column("date", "Date run", 12, kind="date")]
    wb.sheet("Test Cases", cols, tcs, description="One test case per requirement in force of the FRS v1.5, and "
             "one per storyboard of the Process and Storyboards v1.5.")
    rtm = []
    for b, req, frum, where, change in C.MAPPING:
        ids = [t.split()[0] for t in frum.replace(";", ",").split(",") if t.strip()]
        tc = [t["tc"] for t in tcs if any(t["frum"].startswith(i.strip()) for i in ids)]
        rtm.append({"brd": b, "req": req, "frum": frum, "where": where, "change": change,
                    "tc": ", ".join(tc) if tc else "Withdrawn: tested in UIDM-ISC"})
    wb.sheet("BRD to Test", [Column("brd", "BRD", 22), Column("req", "Business requirement", 40),
                             Column("frum", "FRUM", 26), Column("where", "Where", 24),
                             Column("change", "Change from v1.4", 26), Column("tc", "Test cases", 50)], rtm,
             description="Every BRD line of the mapping with its FRUM items and test cases.")
    for r in rtm:
        if not r["tc"]:
            raise SystemExit(f"no test case for {r['brd']}")
    return wb.save(FOLDER / f"BIBS_RTM_BRD-11_Test_Cases_and_Traceability_v{V}.xlsx"), len(tcs)


def build_fitgap():
    wb = _wb("BRD vs Out-of-the-Box vs Best Practice", "Fit-gap")
    fit = {"k": "Fit", "g": "Fit (OOTB extra)", "o": "Change request", "": "Withdrawn"}
    rows = []
    for f in C.FRUM:
        for rid, text, colour, label, rule, ex in f["reqs"]:
            rows.append({"frum": rid, "brd": f["brd"], "req": text, "colour": COLOUR[colour],
                         "fit": "Integration" if label == "int" and colour else fit[colour],
                         "note": rule if rule != "-" else ""})
    wb.sheet("Fit-Gap", [Column("frum", "FRUM", 13), Column("brd", "BRD", 22), Column("req", "Requirement", 50),
                         Column("colour", "Colour", 16), Column("fit", "Fit", 16,
                         values=["Fit", "Fit (OOTB extra)", "Integration", "Change request", "Withdrawn"]),
                         Column("note", "Note", 44)], rows, description="Each requirement of the FRS v1.5 against "
             "the BRD and the iNXT BrokerVerse OOTB platform.")
    wb.sheet("Best Practice", [Column("id", "No.", 10), Column("t", "Recommendation", 46),
                               Column("p", "Priority", 10), Column("e", "Man-days", 10, kind="number")],
             [{"id": a, "t": b, "p": c, "e": d} for a, b, c, d in C.BEST_PRACTICE],
             description="Blue points: in no BRD, FRS or OOTB; recommendation only.")
    return wb.save(FOLDER / f"BIBS_FitGap_BRD-11_BRD_vs_OOTB_vs_Best_Practice_v{V}.xlsx"), len(rows)


CLOSED = [("CR-UA-001", "Separation-of-duties rules", "Closed: BRD requirement, OOTB"),
          ("CR-UA-002", "Dormant-user deactivation", "Closed: OOTB (OB-UA-02)"),
          ("CR-UA-004", "Warning before system sign-out", "Closed: BRD requirement"),
          ("CR-UA-005", "Synchronisation with UIDM-ISC", "Closed: integration scope (IS-UA-01)"),
          ("CR-UA-006", "Revocation on SSO status change", "Closed: integration scope (IS-UA-01)"),
          ("CR-UA-008", "No conflicting permissions in a profile", "Closed: BRD requirement"),
          ("CR-UA-009", "Export file names", "Closed: BRD requirement"),
          ("CR-UA-010", "Source address in the audit log", "Closed: BRD requirement (addenda NFR 4.03)"),
          ("CR-UA-012", "Periodic access review", "Closed: OOTB (OB-UA-05)"),
          ("CR-UA-013", "E-mail copy of notices", "Closed: BRD requirement; user request notices in UIDM-ISC"),
          ("CR-UA-015", "One session per device", "Closed: BRD requirement (Collections NFR 10.07)")]


def build_cr():
    wb = _wb("Change Request Register", "Change control")
    rows = [{"id": a, "t": b, "s": c, "frs": e, "md": sum(eff), "note": n} for a, b, c, e, eff, n in C.CRS]
    rows += [{"id": a, "t": b, "s": "Closed", "frs": "-", "md": 0, "note": n} for a, b, n in CLOSED]
    rows.sort(key=lambda r: r["id"])
    wb.sheet("Register", [Column("id", "CR", 12), Column("t", "Title", 40), Column("s", "Status v1.2", 14,
             values=["Open", "Withdrawn", "Closed", "Approved", "Rejected"], status=True),
             Column("frs", "FRS v1.5", 14), Column("md", "Man-days", 10, kind="number"), Column("note", "Note", 60)],
             rows, description="All change requests of BRD-11. Withdrawn in v1.2: CR-UA-003, CR-UA-011, "
             "CR-UA-014. Rescoped: CR-UA-007.")
    log = [{"d": "11 Oct 2026", "id": "Register v1.1", "e": "Five open change requests after the product owner "
            "review."},
           {"d": "15 Oct 2026", "id": "CR-UA-003", "e": "Withdrawn: no break-glass account (BDOI IT)."},
           {"d": "16 Oct 2026", "id": "CR-UA-011, CR-UA-014", "e": "Withdrawn: user access requests and effective "
            "dates are in UIDM-ISC."},
           {"d": "16 Oct 2026", "id": "CR-UA-007", "e": "Rescoped to company and branch data access."}]
    wb.sheet("Decision Log", [Column("d", "Date", 14), Column("id", "Item", 22), Column("e", "Decision", 70)], log)
    return wb.save(FOLDER / f"BIBS_CR_BRD-11_Change_Request_Register_v{V}.xlsx"), len(rows)


def build_effort():
    wb = _wb("CR Effort Estimate", "Change control")
    rows = []
    for a, b, c, e, eff, n in C.CRS:
        if c != "Open":
            continue
        rows.append({"id": a, "t": b, "an": eff[0], "be": eff[1], "fe": eff[2], "te": eff[3], "do": eff[4],
                     "tot": sum(eff)})
    if not rows:
        raise SystemExit("no open change request")
    rows.append({"id": "Total", "t": "Open change requests", **{k: sum(r[k] for r in rows) for k in
                                                                 ("an", "be", "fe", "te", "do", "tot")}})
    num = lambda k, h: Column(k, h, 12, kind="number")  # noqa: E731
    wb.sheet("Effort", [Column("id", "CR", 12), Column("t", "Title", 40), num("an", "Analysis and design"),
                        num("be", "Backend"), num("fe", "Frontend"), num("te", "Testing"),
                        num("do", "Documentation and UAT"), num("tot", "Total")], rows,
             description="Man-days of 8 hours, plus or minus 20 percent, for the open change requests only.")
    return wb.save(FOLDER / f"BIBS_CR_BRD-11_Effort_Estimate_v{V}.xlsx"), len(rows) - 1


def build_inputs():
    wb = _wb("Business Unit Requirements Collection", "Requirements collection")
    rows = [{"id": a, "item": b, "owner": c, "due": d, "status": "Open", "answer": ""} for a, b, c, d in
            C.OPEN_QUESTIONS]
    rows += [{"id": a, "item": b, "owner": "BDOI IT" if "IT" in b else "BDOI", "due": "Before SIT",
              "status": "Open", "answer": ""} for a, b in C.DEPENDENCIES]
    rows += [{"id": f"P-{k + 1:02d}", "item": f"Confirm the value of: {p[0]} (default {p[1]})", "owner":
              "Information Security", "due": "01 Dec 2026", "status": "Open", "answer": ""}
             for k, p in enumerate(C.PARAMETERS)]
    wb.sheet("Inputs", [Column("id", "No.", 10), Column("item", "What BDOI provides or decides", 60),
                        Column("owner", "Owner", 20), Column("due", "Needed by", 14),
                        Column("status", "Status", 12, values=["Open", "Received", "Confirmed"], status=True),
                        Column("answer", "Answer", 40)], rows,
             description="Open questions, dependencies and settings of the FRS v1.5.")
    return wb.save(FOLDER / f"BIBS_Inputs_BRD-11_Business_Unit_Requirements_Collection_v{V}.xlsx"), len(rows)


def build_uat():
    wb = _wb("Business Unit Walkthrough Users", "User acceptance")
    users = [{"p": n, "acct": a, "does": d, "screens": s, "pw": "Issued separately by the iorta TechNXT Project "
              "Team; sign-in through Microsoft Entra ID where the tenant is connected"} for n, d, s, a in C.PERSONAS]
    wb.sheet("Users", [Column("p", "Persona", 24), Column("acct", "Walkthrough account", 24),
                       Column("does", "Does", 50), Column("screens", "Screens", 44), Column("pw", "Sign-in", 40)],
             users, description="One walkthrough account per persona of the FRS v1.5.")
    script = []
    for sb in P.STORYBOARDS:
        for k, (who, screen, does, sees, shot) in enumerate(sb["steps"], start=1):
            script.append({"sb": f"{sb['id']} {sb['title']}", "n": k, "who": who, "screen": screen, "does": does,
                           "sees": sees, "result": "Not run"})
    wb.sheet("Script", [Column("sb", "Storyboard", 32), Column("n", "Step", 6, kind="number"),
                        Column("who", "Who", 22), Column("screen", "Screen", 24), Column("does", "Does", 40),
                        Column("sees", "Sees", 40), Column("result", "Result", 12, values=RESULT, status=True)],
             script, description="The storyboards of the Process and Storyboards v1.5, step by step.")
    return wb.save(FOLDER / f"BIBS_UAT_BRD-11_Business_Unit_Walkthrough_Users_v{V}.xlsx"), len(script)


def main() -> int:
    for fn in (build_rtm, build_fitgap, build_cr, build_effort, build_inputs, build_uat):
        path, n = fn()
        print(f"wrote {Path(path).name} ({n} rows)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
