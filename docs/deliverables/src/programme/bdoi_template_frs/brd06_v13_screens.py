"""Screens of the Renewal UX Design v1.3 (BRD-06): purpose, access, requirements, rules and states, fields (type,
required, default, rule, message), actions and the full-page screenshots of each screen. Read by build_brd06_v13.py,
brd06_pack.py and build_brd06_v13_workbooks.py.

The screen facts come from the screen specifications of the Renewal sign-off pack
(docs/deliverables/src/BRD-06_Renewal/pack/screens/*.yaml), kept with the system; this module gives them the FRRN
items of the FRS v1.3 and the screenshots of figures/brd06_v13_screens (flows/brd06/shots_v13.sh).

    python brd06_v13_screens.py --manifest     # prints the screen states to capture (JSON), read by shots_v13.cjs
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
PACK = HERE.parents[1] / "BRD-06_Renewal" / "pack" / "screens"

# FRRN entries of each screen (FRS v1.3).
FRRN = {
    "SCR-RN-01": "FRRN.002, FRRN.003, FRRN.044",
    "SCR-RN-02": "FRRN.004, FRRN.006, FRRN.010, FRRN.011, FRRN.045",
    "SCR-RN-03": "FRRN.001, FRRN.009, FRRN.014, FRRN.039",
    "SCR-RN-04": "FRRN.004, FRRN.006, FRRN.007, FRRN.008, FRRN.009, FRRN.014, FRRN.018",
    "SCR-RN-05": "FRRN.013, FRRN.014, FRRN.022, FRRN.023, FRRN.025, FRRN.043, FRRN.047",
    "SCR-RN-06": "FRRN.002",
    "SCR-RN-07": "FRRN.014, FRRN.016, FRRN.018",
    "SCR-RN-08": "FRRN.016, FRRN.020",
    "SCR-RN-09": "FRRN.011",
    "SCR-RN-10": "FRRN.003, FRRN.005, FRRN.010, FRRN.015, FRRN.031",
    "SCR-RN-11": "FRRN.013, FRRN.021",
    "SCR-RN-12": "FRRN.013",
    "SCR-RN-13": "FRRN.022, FRRN.023, FRRN.024",
    "SCR-RN-14": "FRRN.025",
    "SCR-RN-15": "FRRN.023, FRRN.047",
    "SCR-RN-16": "FRRN.012",
    "SCR-RN-17": "FRRN.006, FRRN.007, FRRN.038, FRRN.040",
    "SCR-RN-18": "FRRN.008, FRRN.040",
    "SCR-RN-19": "FRRN.046",
    "SCR-RN-20": "FRRN.045",
    "SCR-RN-21": "FRRN.002, FRRN.042",
    "SCR-RN-22": "FRRN.002, FRRN.008",
    "SCR-RN-23": "FRRN.003, FRRN.010",
    "SCR-RN-24": "FRRN.011",
    "SCR-RN-25": "FRRN.016, FRRN.026",
    "SCR-RN-26": "FRRN.029, FRRN.030, FRRN.031, FRRN.032",
    "SCR-RN-27": "FRRN.017, FRRN.018, FRRN.019",
    "SCR-RN-28": "FRRN.039",
    "SCR-RN-29": "FRRN.038",
    "SCR-RN-30": "FRRN.027, FRRN.028",
    "SCR-RN-31": "FRRN.033",
    "SCR-RN-32": "FRRN.041",
    "SCR-RN-33": "FRRN.017, FRRN.022, FRRN.023, FRRN.024, FRRN.027",
    "SCR-RN-34": "FRRN.043",
    "SCR-RN-35": "FRRN.042",
    "SCR-RN-36": "FRRN.014, FRRN.016, FRRN.017, FRRN.025, FRRN.029, FRRN.032, FRRN.034, FRRN.036, FRRN.037",
}
# Who opens each screen (menu personas; record screens and dialogs follow the actions of the stage).
ACCESS = {
    "SCR-RN-03": "Every Renewal user, from a list or a notice; the actions shown follow the stage and the permissions.",
    "SCR-RN-04": "Every Renewal user, on the renewal.", "SCR-RN-05": "Every Renewal user, on the renewal.",
    "SCR-RN-07": "Account Officer and Marketing Team Lead (dispose permission); Processing Team Lead to re-open.",
    "SCR-RN-12": "Processing Team Lead and Officer (insurer permission).",
    "SCR-RN-14": "Account Officer, Marketing Team Lead, Processing (accept permission).",
    "SCR-RN-17": "Business Administrator; read only for the Team Leads.",
    "SCR-RN-18": "Business Administrator.", "SCR-RN-19": "Processing Team Lead and Officer (package choice).",
    "SCR-RN-20": "Business Administrator; Processing Team Lead.",
    "SCR-RN-36": "Every Renewal user, on the renewal; the actions follow the stage and the permissions.",
}
_REF = re.compile(r"\s*\((?:FR-RN-\d+|FRRN\.\d+(?:\.\d+)*)(?:[,;] ?(?:FR-RN-\d+|FRRN\.\d+(?:\.\d+)*|BRRN\.\d+|"
                  r"BRD [\d.]+))*\)")


def _clean(text: str) -> str:
    text = _REF.sub("", str(text))
    text = re.sub(r"\(setting, Annex [^)]*\)", "(setting)", text)
    text = re.sub(r"\s*\((?:see )?Annex [^)]*\)", "", text)
    text = re.sub(r"\b(FR-RN-\d+)\b", "", text)
    text = text.replace("BRD-6", "BRD-06").replace("(NT-", "(notice NT-")
    return " ".join(text.split()).replace(" .", ".").replace(" ,", ",")


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


def _slug(sid: str, k: int, state: str) -> str:
    return f"ux-{sid[-2:]}-{k:02d}-{state}"


def _load() -> list[dict]:
    out = []
    for f in sorted(PACK.glob("*.yaml")):
        for s in yaml.safe_load(f.read_text(encoding="utf-8"))["screens"]:
            shots = []
            for k, sh in enumerate(s.get("shots", []), 1):
                shot = {key: val for key, val in sh.items() if key != "caption"}
                shot["slug"] = _slug(s["id"], k, sh["state"])
                shot["pack_slug"] = f"{s['id'].lower()}-{k:02d}-{sh['state']}"
                shot["route"] = s["route"]
                shots.append((shot["slug"], _clean(sh["caption"]), shot))
            out.append({"id": s["id"], "name": _clean(s["title"]), "route": s["route"],
                        "purpose": _clean(s["purpose"]),
                        "access": ACCESS.get(s["id"], "; ".join(_clean(e) for e in s.get("entry", [])) or "-"),
                        "frrn": FRRN[s["id"]], "rules": [_clean(r) for r in s.get("rules", [])],
                        "fields": [_field(x) for x in s.get("fields", [])],
                        "actions": [_action(x) for x in s.get("actions", [])],
                        "outcome": _clean(s.get("outcome", "")), "shots": shots,
                        "entry": [_clean(e) for e in s.get("entry", [])]})
    return out


SCREENS = _load()


def manifest() -> list[dict]:
    return [sh for s in SCREENS for _, _, sh in s["shots"]]


if __name__ == "__main__":
    if "--manifest" in sys.argv:
        json.dump(manifest(), sys.stdout, indent=1)
    else:
        for s in SCREENS:
            print(s["id"], s["name"], len(s["fields"]), len(s["actions"]), len(s["shots"]))
