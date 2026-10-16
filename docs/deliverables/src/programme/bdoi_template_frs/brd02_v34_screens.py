"""Screens of the Operations Cashiering UX Design v3.4 (BRD-02): purpose, access, requirements, rules and states, fields
(type, required, default, rule, message), actions and the full-page screenshots of each screen. Read by
build_brd02_v34.py, brd02_pack.py and build_brd02_v34_workbooks.py.

The screen facts come from the screen specifications of the Operations sign-off pack
(docs/deliverables/src/BRD-02_Operations/pack/screens/*.yaml), kept with the system; this module renumbers the
Cashiering screens SCR-CSH-nn, gives them the FRS.CSH entries of the FRS v3.4, adds the screens the pack does not
specify (Unapplied to Income, Legacy PR 2307 Reversal, Check Pick-up) and names the screenshots of
figures/brd02_v34_screens (flows/brd02/shots_v34.sh).

    python brd02_v34_screens.py --manifest     # prints the screen states to capture (JSON), read by shots_v34.cjs
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
PACK = HERE.parents[1] / "BRD-02_Operations" / "pack" / "screens"

# Cashiering screen number: (screen of the Operations pack, FRS entries of the FRS v3.4)
ORDER = [
    ("SCR-OP-07", "FRS.CSH.01.03"),
    ("SCR-OP-65", "FRS.CSH.02.01, FRS.CSH.02.02"),
    ("SCR-OP-66", "FRS.CSH.02.05, FRS.CSH.03.01, FRS.CSH.04.01"),
    ("SCR-OP-67", "FRS.CSH.02.05, FRS.CSH.03.01, FRS.CSH.04.01"),
    ("SCR-OP-08", "FRS.CSH.02.05, FRS.CSH.05.03"),
    ("SCR-OP-09", "FRS.CSH.03.01, FRS.CSH.04.01, FRS.CSH.08.01"),
    ("SCR-OP-10", "FRS.CSH.02.04, FRS.CSH.02.06, FRS.CSH.03.01, FRS.CSH.04.01, FRS.CSH.08.03"),
    ("SCR-OP-11", "FRS.CSH.06.01, FRS.CSH.06.02"),
    ("SCR-OP-12", "FRS.CSH.06.01, FRS.CSH.06.02"),
    ("SCR-OP-72", "FRS.CSH.06.01"),
    ("SCR-OP-70", "FRS.CSH.02.01"),
    ("SCR-OP-71", "FRS.CSH.08.01"),
    ("SCR-OP-13", "FRS.CSH.06.03"),
    ("SCR-OP-14", "FRS.CSH.05.03, FRS.CSH.05.04"),
    ("SCR-OP-15", "FRS.CSH.02.02, FRS.CSH.05.01"),
    ("SCR-OP-68", "FRS.CSH.05.01, FRS.CSH.10.02, FRS.CSH.10.03"),
    ("SCR-OP-73", "FRS.CSH.07.01"),
    ("SCR-OP-16", "FRS.CSH.05.02"),
    ("SCR-OP-18", "FRS.CSH.02.04"),
    ("SCR-OP-19", "FRS.CSH.02.05"),
    ("SCR-OP-20", "FRS.CSH.02.02, FRS.CSH.07.01"),
    ("SCR-OP-69", "FRS.CSH.02.06"),
    ("SCR-OP-21", "FRS.CSH.02.03"),
    ("SCR-OP-22", "FRS.CSH.02.05, FRS.CSH.05.01"),
    ("SCR-X-INCOME", "FRS.CSH.06.01"),
    ("SCR-X-PR2307", "FRS.CSH.06.01"),
    ("SCR-X-PICKUP", "FRS.CSH.05.01"),
    ("SCR-OP-63", "FRS.CSH.09.01, FRS.CSH.09.02"),
    ("SCR-OP-64", "FRS.CSH.09.01, FRS.CSH.09.02"),
]

# Screens the Operations pack does not specify, in its format (fields: area | field | type | format | required |
# source | default | editable | check | message; actions: action | who | when | what | result | notice).
EXTRA = [
    {"id": "SCR-X-INCOME", "title": "Unapplied to Income", "route": "/cashiering/unapplied-income",
     "purpose": "Batches of old unapplied payments, new and legacy, reclassified to other income (unclaimed "
                "collections) after the approval of the Cashiering Team Leader and then of Top Management. "
                "[Data Migration scope]",
     "entry": ["Cashiering › Unapplied to Income"],
     "shots": [{"state": "list", "user": "cashier", "caption": "Unapplied to Income: the batches and their status"}],
     "fields": ["Batches | Batch / Lines / Total / Requested / Approved / Posted / Status | Table | One row per batch "
                "| - | Batches | - | Read only | - | No batch yet",
                "New Batch | Reason | Text | Up to 200 characters | Y | - | Blank | - | Not blank | Enter the reason",
                "New Batch | Currency | Drop-down | PHP, USD | Y | Currencies | PHP | - | - | -"],
     "actions": ["New Batch | Cashier | Always | Opens a batch in Draft for its lines | Draft | -",
                 "Add Unapplied Payments | Cashier | Draft | Adds unapplied payments received at least the days "
                 "chosen, with the reason of the lines | Lines added | -",
                 "Submit Batch | Cashier | Draft with a line | Sends the batch to the Team Leader | For approval | "
                 "Notice to the Team Leader",
                 "Approve Batch / Return Batch | Team Leader, then Top Management | For approval | Approves, or "
                 "returns with a reason; the lines post after the second approval | Posted | -"],
     "rules": ["The requester never approves the batch (BR-CSH-01).",
               "The lines post to other income after the second approval; the unapplied payments close."],
     "outcome": "Old unapplied payments are reclassified to other income with two approvals."},
    {"id": "SCR-X-PR2307", "title": "Legacy PR 2307 Reversal", "route": "/cashiering/legacy-pr2307",
     "purpose": "Batches that settle the PR 2307 balances of legacy invoices against the amount due to the insurer. "
                "[Data Migration scope]",
     "entry": ["Cashiering › Legacy PR 2307 Reversal"],
     "shots": [{"state": "list", "user": "cashier", "caption": "Legacy PR 2307 Reversal: the batches"}],
     "fields": ["Batches | Batch / Lines / Total / Requested / Approved / Posted / Status | Table | One row per batch "
                "| - | Batches | - | Read only | - | No batch yet",
                "Add a Legacy Invoice | Invoice No. | Text | Legacy invoice number | Y | Legacy invoices | Blank | - "
                "| A legacy invoice | <invoice> is not a legacy invoice",
                "Add a Legacy Invoice | Amount | Number | Two decimals | Y | - | Blank | - | Within the open PR 2307 "
                "and the due to insurer | <invoice>: the amount must be positive and within the open PR 2307 and "
                "due to insurer"],
     "actions": ["New Batch | Cashier | Always | Opens a batch in Draft | Draft | -",
                 "Submit Batch | Cashier | Draft with a line | Sends the batch to the Team Leader | For approval | -",
                 "Approve Batch / Return Batch | Team Leader | For approval | Posts the lines, or returns the batch "
                 "with a reason | Posted | -"],
     "rules": ["The requester never approves the batch (BR-CSH-01).",
               "Any premium receivable needed is first moved to PR 2307; each line posts on its own."],
     "outcome": "Legacy PR 2307 balances are settled with the amount due to the insurer."},
    {"id": "SCR-X-PICKUP", "title": "Check Pick-up", "route": "/cashiering/pickups",
     "purpose": "Checks tagged for pick-up by Collection, with the ARs of the checks picked up. CSHID.009 is removed "
                "from the BRD; the screen stays in the Cashier profile until BDOI decides (Q-CSH-07).",
     "entry": ["Cashiering › Check Pick-up"],
     "shots": [{"state": "list", "user": "cashier", "caption": "Check Pick-up: requests of Collection"}],
     "fields": ["Filters | Pick-up From / Pick-up To | Date | dd-MMM-yyyy | N | - | Blank | - | - | -",
                "Requests | Collection Ref. / Payor / Pick-up Date / Requested By / Check No. / Amount / AR / Status "
                "| Table | One row per request | - | Collection | - | Read only | - | -"],
     "actions": ["Import | Cashier | Always | Reads the pick-up requests of Collection | Requests listed | -",
                 "Print ARs | Cashier with the print permission | Requests due selected | Issues and prints the ARs "
                 "of the checks picked up | AR Printed | -"],
     "rules": ["A collection reference is queued once."],
     "outcome": "The ARs of the checks picked up are printed."},
]

NEW_ID = {pid: f"SCR-CSH-{n:02d}" for n, (pid, _) in enumerate(ORDER, 1)}
_REF = re.compile(r"\s*\((?:FR-OP-\d+|CLR-OP-\d+)(?:[,;] ?(?:FR-OP-\d+|CLR-OP-\d+|BRD [\d.]+))*\)")


def _clean(text: str) -> str:
    text = _REF.sub("", str(text))
    text = re.sub(r",? ?Appendix [A-Z](?:\.\d+)?", "", text)
    text = re.sub(r"\bSCR-OP-\d+\b", lambda m: NEW_ID.get(m.group(0), "-"), text)
    text = re.sub(r"\s*\((?:see )?Appendix [^)]*\)", "", text)
    text = re.sub(r"\(setting of Appendix [^)]*\)", "(setting)", text)
    text = re.sub(r"\b(FR-OP-\d+|CLR-OP-\d+)\b", "", text)
    text = text.replace("BRD-2 ", "BRD-02 ").replace("(SCR-OP-65)", "").replace("BRD-1)", "BRD-01)")
    text = text.replace("(setting of)", "(setting)").replace(" ()", "")
    return " ".join(text.split()).replace(" .", ".").replace(" ,", ",").replace("( ", "(").replace(" )", ")")


def _cells(line: str, n: int) -> list[str]:
    parts = [p.strip() for p in line.split(" | ")]
    return (parts + ["-"] * n)[:n]


def _field(line: str) -> list[str]:
    area, field, typ, fmt, req, src, default, when, check, msg = _cells(line, 10)
    name = field if area in ("-", "") else f"{area}: {field}"
    rule = "; ".join(x for x in (fmt, f"Source: {src}" if src not in ("-", "") else "",
                                 f"Editable: {when}" if when not in ("-", "") else "",
                                 check if check not in ("-", "") else "") if x and x != "-") or "-"
    return [_clean(name), typ, {"Y": "Y", "N": "N"}.get(req, req or "-"), default or "-", _clean(rule),
            _clean(msg) or "-"]


def _action(line: str) -> list[str]:
    act, who, when, what, result, notice = _cells(line, 6)
    if notice not in ("-", ""):
        result = f"{result}; {notice}" if result not in ("-", "") else notice
    return [_clean(act), _clean(who), _clean(when), _clean(what), _clean(result)]


# Shots of the Report Centre and Report taken as the Cashiering Team Leader on a Cashiering report.
SHOT_OVERRIDE = {
    "SCR-OP-08": [{"state": "page", "user": "cashier",
                   "caption": "Receive Payment with the posting step of ARs on: the payment is taken on Create AR/OR"}],
    "SCR-OP-16": [{"state": "list", "user": "cashier", "caption": "PDC Warehouse by maturity month"},
                  {"state": "add", "user": "cashier", "click": "^warehouse check$",
                   "caption": "Warehouse Post-dated Check dialog"}],
    "SCR-OP-68": [{"state": "files", "user": "cashier", "caption": "Payment Files with the Bills Payment file processed"},
                  {"state": "report", "user": "cashier", "click": "^run report$",
                   "caption": "Run report of a payment file per category and payment record"}],
    "SCR-OP-63": [{"state": "list", "user": "cashtl", "caption": "Report Centre: the Cashiering reports"}],
    "SCR-OP-64": [{"state": "record", "user": "cashtl", "open_report": "daily_cash", "click": "^run report$",
                   "caption": "Daily Cash Reconciliation Reports run on screen"}],
}


# A screen state that shows exactly the page of another screen keeps that screen's image.
SAME_SHOT = {"SCR-OP-08": "ux-02-01-empty"}


def _load() -> list[dict]:
    pack = {}
    for f in sorted(PACK.glob("*.yaml")):
        for s in yaml.safe_load(f.read_text(encoding="utf-8"))["screens"]:
            pack[s["id"]] = s
    for s in EXTRA:
        pack[s["id"]] = s
    out = []
    for n, (pid, frs) in enumerate(ORDER, 1):
        s = pack[pid]
        sid = f"SCR-CSH-{n:02d}"
        shots = []
        for k, sh in enumerate(SHOT_OVERRIDE.get(pid, s.get("shots", [])), 1):
            shot = {key: val for key, val in sh.items() if key != "caption"}
            shot["slug"] = SAME_SHOT.get(pid, f"ux-{n:02d}-{k:02d}-{sh['state']}")
            shot["pack_slug"] = f"{pid.lower()}-{k:02d}-{sh['state']}"
            shot["route"] = s["route"]
            shots.append((shot["slug"], _clean(sh["caption"]), shot))
        out.append({"id": sid, "pack_id": pid, "name": _clean(s["title"]), "route": s["route"],
                    "purpose": _clean(s["purpose"]), "access": "; ".join(_clean(e) for e in s.get("entry", []))
                    or "-", "frrn": frs, "rules": [_clean(r) for r in s.get("rules", [])],
                    "fields": [_field(x) for x in s.get("fields", [])],
                    "actions": [_action(x) for x in s.get("actions", [])],
                    "outcome": _clean(s.get("outcome", "")), "shots": shots})
    return out


SCREENS = _load()


def manifest() -> list[dict]:
    return [sh for s in SCREENS for _, _, sh in s["shots"]]


if __name__ == "__main__":
    if "--manifest" in sys.argv:
        json.dump(manifest(), sys.stdout, indent=1)
    else:
        for s in SCREENS:
            print(s["id"], s["pack_id"], s["name"], len(s["fields"]), len(s["actions"]), len(s["shots"]))
