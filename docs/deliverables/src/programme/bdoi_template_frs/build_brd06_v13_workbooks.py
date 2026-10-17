"""The review workbooks v1.1 of BRD-06 Renewal, aligned with the document set v1.3.

    python docs/deliverables/src/programme/bdoi_template_frs/build_brd06_v13_workbooks.py

Writes in docs/deliverables/out/Programme/BDOI_Template_FRS/BRD-06_Renewal/: the Test Cases and Traceability (test
cases with every acceptance criterion; the sheets BRD to Test, BRD Lines and Trace BRD, FRRN, user story, screen,
test case), the Fit-Gap, the Change Request Register and Effort Estimate, the Business Unit Requirements Collection
and the Walkthrough Users, all v1.1.

Content from brd06_v13_content.py, brd06_v13_pages.py and brd06_pack.py, so that every number agrees with the FRS
v1.3. No password is written. Self-checks: every FRRN requirement has a user story and a test case; every BRD row of
the mapping has test cases; every open change request has an effort.
"""

from __future__ import annotations

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE.parents[4] / "tools" / "deliverables"))
import brd06_pack as K  # noqa: E402
import brd06_v13_content as C  # noqa: E402
import brd06_v13_pages as P  # noqa: E402
import pack_common as pc  # noqa: E402
import pack_usr  # noqa: E402
from brd11_xlsx import Column, ReviewWorkbook  # noqa: E402

FOLDER = pc.OUT_BDOI / K.FOLDER
V = C.WB_VERSION
DATE = "16 October 2026"
FRS = f"FRS Renewal v{C.VERSION}"
COLOUR = {"k": "Black (BRD)", "g": "Green (OOTB)", "o": "Orange (CR)", "b": "Blue (best practice)", "": "-"}
RESULT = ["Not run", "Pass", "Fail", "Blocked"]


def _wb(title, kind):
    return ReviewWorkbook(title, doc_type=kind, brd="BRD-06", version=V, date=DATE,
                          subtitle=f"Renewal; goes with the {FRS}")


def _save(wb, key):
    return pc.save_workbook(wb, FOLDER / K.file_name(key))


STORIES = pack_usr.stories(K)
SCREENS = K.screens_by_id()


def stories_of(rid: str) -> list[dict]:
    return [st for st in STORIES if rid in [x.strip() for x in st["frum"].split(",")]]


def test_cases():
    rows = []
    for f in C.FRUM:
        for rid, text, colour, label, rule, ex in f["reqs"]:
            sts = stories_of(rid)
            persona = sts[0]["persona"] if sts else "-"
            screen = f"{sts[0]['screen']} {SCREENS[sts[0]['screen']]['name']}" if sts else "-"
            rows.append({"tc": "TC-RN-" + rid.split(".", 1)[1].replace(".", "-"), "frum": rid, "brd": f["brd"],
                         "persona": persona, "screen": screen,
                         "pre": "Test environment with the walkthrough accounts of the Walkthrough Users workbook.",
                         "steps": f"Check: {text}",
                         "expected": " ".join(f"({k}) {a}" for k, a in enumerate(C.ACCEPTANCE[rid], 1))
                         + ("" if rule == "-" else f" Rule: {rule}."), "acs": len(C.ACCEPTANCE[rid]), "colour": COLOUR[colour],
                         "scope": "Integration" if label == "int" else "BIBS", "result": "Not run"})
    for sb in P.STORYBOARDS:
        rows.append({"tc": f"TC-RN-{sb['id']}", "frum": "End to end", "brd": "BRD-06",
                     "persona": ", ".join(sorted({s[0] for s in sb["steps"]})),
                     "screen": "; ".join(dict.fromkeys(s[1] for s in sb["steps"])), "pre": "As above.",
                     "steps": " ".join(f"{i + 1}. {s[0]}: {s[2]}" for i, s in enumerate(sb["steps"])),
                     "expected": " ".join(f"{i + 1}. {s[3]}" for i, s in enumerate(sb["steps"])),
                     "colour": "Black (BRD)", "scope": "BIBS", "result": "Not run"})
    return rows


def build_rtm():
    wb = _wb("Test Cases and Traceability", "Requirements traceability")
    tcs = test_cases()
    for t in tcs:
        t["story"] = ", ".join(st["id"] for st in stories_of(t["frum"])) or "-"
    wb.sheet("Test Cases", [Column("tc", "Test case", 14), Column("frum", "FRRN", 14), Column("story", "User story", 18),
                            Column("brd", "BRD", 22), Column("persona", "Persona", 22), Column("screen", "Screen", 26),
                            Column("pre", "Precondition", 28), Column("steps", "Steps", 50),
                            Column("expected", "Expected result (acceptance criteria)", 60), Column("colour", "Colour", 16),
                            Column("scope", "Delivered by", 14),
                            Column("result", "Result", 12, values=RESULT, status=True), Column("tester", "Tester", 16),
                            Column("date", "Date run", 12, kind="date")], tcs,
             description=f"One test case per requirement of the {FRS}, with every acceptance criterion as an expected "
             f"result, and one per storyboard of the Process and Storyboards v{C.VERSION}.")
    rtm = []
    for b, req, frum, where, change in C.MAPPING:
        ids = [t.strip() for t in frum.replace(";", ",").split(",") if t.strip()]
        tc = [t["tc"] for t in tcs if any(t["frum"].startswith(i) for i in ids)]
        rtm.append({"brd": b, "req": req, "frum": frum, "where": where, "change": change,
                    "tc": ", ".join(tc) or "-"})
    wb.sheet("BRD to Test", [Column("brd", "BRD", 20), Column("req", "Business requirement", 40),
                             Column("frum", "FRRN", 28), Column("where", "Where", 18),
                             Column("change", "Change from v1.2", 24), Column("tc", "Test cases", 50)], rtm,
             description="Every BRD line of the mapping with its FRRN items and test cases.")
    lines = []
    for ln in C.BRD_LINES:
        tc = [t["tc"] for t in tcs if any(t["frum"] == i or t["frum"].startswith(i + ".") for i in ln["frrn"])]
        if not tc:
            raise SystemExit(f"BRD line {ln['id']} without test case")
        lines.append({"brd": ln["id"], "req": ln["text"], "frum": ", ".join(ln["frrn"]), "tc": ", ".join(tc[:12])
                      + (f" and {len(tc) - 12} more" if len(tc) > 12 else "")})
    wb.sheet("BRD Lines", [Column("brd", "BRD line", 16), Column("req", "Requirement line of the BRD", 60),
                           Column("frum", "FRRN", 34), Column("tc", "Test cases", 50)], lines,
             description="The 1,033 requirement lines of the main Renewal BRD with their FRRN items and test cases.")
    trace = []
    for f in C.FRUM:
        for rid, text, *_ in f["reqs"]:
            sts = stories_of(rid)
            tc = [t["tc"] for t in tcs if t["frum"] == rid]
            if not (sts and tc):
                raise SystemExit(f"trace incomplete for {rid}")
            trace.append({"brd": f["brd"], "frum": rid, "req": text, "story": ", ".join(s["id"] for s in sts),
                          "screen": "; ".join(dict.fromkeys(f"{s['screen']} {SCREENS[s['screen']]['name']}"
                                                            for s in sts)), "tc": ", ".join(tc)})
    wb.sheet("Trace", [Column("brd", "BRD", 22), Column("frum", "FRRN", 14), Column("req", "Requirement", 50),
                       Column("story", "User story", 18), Column("screen", "Screen", 32),
                       Column("tc", "Test case", 16)], trace,
             description="BRD to FRRN requirement to user story (User Stories v1.0) to screen (UX Design "
             f"v{C.VERSION}) to test case, one row per requirement.")
    if sum(t.get("acs", 0) for t in tcs) != sum(len(v) for v in C.ACCEPTANCE.values()):
        raise SystemExit("acceptance criteria missing from the test cases")
    return _save(wb, "RTM"), len(tcs)


def build_fitgap():
    wb = _wb("BRD vs Out-of-the-Box vs Best Practice", "Fit-gap")
    fit = {"k": "Fit", "g": "Fit (OOTB extra)", "o": "Change request"}
    rows = []
    for f in C.FRUM:
        for rid, text, colour, label, rule, ex in f["reqs"]:
            rows.append({"frum": rid, "brd": f["brd"], "req": text, "colour": COLOUR[colour],
                         "fit": "Integration" if label == "int" else fit[colour], "note": "" if rule == "-" else rule})
    wb.sheet("Fit-Gap", [Column("frum", "FRRN", 14), Column("brd", "BRD", 22), Column("req", "Requirement", 50),
                         Column("colour", "Colour", 16), Column("fit", "Fit", 16, values=["Fit", "Fit (OOTB extra)",
                         "Integration", "Change request"]), Column("note", "Rule or setting", 40)], rows,
             description=f"Each requirement of the {FRS} against the BRD and the iNXT BrokerVerse OOTB platform.")
    wb.sheet("Beyond the BRD", [Column("id", "No.", 10), Column("t", "Point", 50), Column("frs", "FRS", 20),
                                Column("k", "Colour", 16)],
             [{"id": a, "t": b, "frs": c, "k": "Green (OOTB)"} for a, b, c in C.OOTB_POINTS]
             + [{"id": a, "t": b, "frs": e, "k": "Orange (CR)"} for a, b, _, e, _, _ in C.CRS],
             description="Points not in the BRD, one group: green when OOTB, orange when a change request.")
    wb.sheet("Integration and DM", [Column("id", "No.", 10), Column("t", "Item", 56), Column("frs", "FRS", 24)],
             [{"id": a, "t": b, "frs": c} for a, b, c in C.INTEGRATION_POINTS],
             description="Covered by the Integration scope or the Data Migration scope; not change requests.")
    wb.sheet("Best Practice", [Column("id", "No.", 10), Column("t", "Recommendation", 56), Column("p", "Priority", 10),
                               Column("e", "Man-days", 10, kind="number")],
             [{"id": a, "t": b, "p": c, "e": d} for a, b, c, d in C.BEST_PRACTICE],
             description="Blue points: in no BRD, FRS or OOTB; recommendation only, not in the CR total.")
    return _save(wb, "FIT"), len(rows)


def build_cr():
    wb = _wb("Change Request Register", "Change control")
    rows = [{"id": a, "t": b, "s": c, "frs": e, "md": sum(eff), "note": n} for a, b, c, e, eff, n in C.CRS]
    wb.sheet("Register", [Column("id", "CR", 12), Column("t", "Title", 44), Column("s", f"Status v{V}", 14,
             values=["Open", "Withdrawn", "Closed", "Approved", "Rejected"], status=True),
             Column("frs", "Reference", 16), Column("md", "Man-days", 10, kind="number"), Column("note", "Note", 50)],
             rows, description="Change requests of BRD-06 Renewal.")
    wb.sheet("Decision Log", [Column("d", "Date", 14), Column("id", "Item", 22), Column("e", "Decision", 70)],
             [{"d": "11 Oct 2026", "id": "Register v1.0", "e": "24 candidate changes after the product owner "
               "review (green, orange and BRD gaps)."},
              {"d": "16 Oct 2026", "id": "Register v1.1", "e": "Aligned with the FRS v1.3: the green point and the BRD "
               "gaps are delivered and leave the register; integration points are Integration scope; four change "
               "requests stay open (CR-RN-005, 008 and 015 delivered; CR-RN-024 not delivered)."}])
    return _save(wb, "CRR"), len(rows)


def build_effort():
    wb = _wb("CR Effort Estimate", "Change control")
    rows = [{"id": a, "t": b, "an": e[0], "be": e[1], "fe": e[2], "te": e[3], "do": e[4], "tot": sum(e)}
            for a, b, c, _, e, _ in C.CRS if c == "Open"]
    if not rows:
        raise SystemExit("no open change request")
    rows.append({"id": "Total", "t": "Open change requests",
                 **{k: sum(r[k] for r in rows) for k in ("an", "be", "fe", "te", "do", "tot")}})
    num = lambda k, h: Column(k, h, 12, kind="number")  # noqa: E731
    wb.sheet("Effort", [Column("id", "CR", 12), Column("t", "Title", 44), num("an", "Analysis and design"),
                        num("be", "Backend"), num("fe", "Frontend"), num("te", "Testing"),
                        num("do", "Documentation and UAT"), num("tot", "Total")], rows,
             description="Man-days of 8 hours, plus or minus 20 percent, for the open change requests only.")
    return _save(wb, "CRE"), len(rows) - 1


def build_inputs():
    wb = _wb("Business Unit Requirements Collection", "Requirements collection")
    rows = [{"id": a, "item": b, "owner": c, "due": d, "status": "Open", "answer": ""} for a, b, c, d in
            C.OPEN_QUESTIONS]
    rows += [{"id": a, "item": b, "owner": "BDOI IT" if "IT" in b else "BDOI", "due": "Before SIT",
              "status": "Open", "answer": ""} for a, b in C.DEPENDENCIES]
    rows += [{"id": f"P-{k + 1:02d}", "item": f"Confirm the value of: {p[0]} (default {p[1]})",
              "owner": C.OWNER, "due": "30 Nov 2026", "status": "Open", "answer": ""}
             for k, p in enumerate(C.PARAMETERS)]
    wb.sheet("Inputs", [Column("id", "No.", 10), Column("item", "What BDOI provides or decides", 64),
                        Column("owner", "Owner", 22), Column("due", "Needed by", 14),
                        Column("status", "Status", 12, values=["Open", "Received", "Confirmed"], status=True),
                        Column("answer", "Answer", 40)], rows,
             description=f"Open questions, dependencies and settings of the {FRS}.")
    return _save(wb, "INP"), len(rows)


def build_uat():
    wb = _wb("Business Unit Walkthrough Users", "User acceptance")
    users = [{"p": n, "acct": a, "does": d, "screens": s, "pw": "Issued separately by the iorta TechNXT Project Team"}
             for n, d, s, a in C.PERSONAS]
    wb.sheet("Users", [Column("p", "Persona", 24), Column("acct", "Walkthrough account", 26),
                       Column("does", "Does", 50), Column("screens", "Screens", 44), Column("pw", "Sign-in", 30)],
             users, description=f"One walkthrough account per persona of the {FRS}; the people are fictitious.")
    script = [{"sb": f"{sb['id']} {sb['title']}", "n": k, "who": who, "screen": screen, "does": does, "sees": sees,
               "result": "Not run"} for sb in P.STORYBOARDS
              for k, (who, screen, does, sees, _) in enumerate(sb["steps"], start=1)]
    wb.sheet("Script", [Column("sb", "Storyboard", 34), Column("n", "Step", 6, kind="number"),
                        Column("who", "Who", 22), Column("screen", "Screen", 24), Column("does", "Does", 42),
                        Column("sees", "Sees", 40), Column("result", "Result", 12, values=RESULT, status=True)],
             script, description=f"The storyboards of the Process and Storyboards v{C.VERSION}, step by step.")
    return _save(wb, "WTU"), len(script)


def main() -> int:
    for fn in (build_rtm, build_fitgap, build_cr, build_effort, build_inputs, build_uat):
        path, n = fn()
        print(f"wrote {Path(path).name} ({n} rows)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
