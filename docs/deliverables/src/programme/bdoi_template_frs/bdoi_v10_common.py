"""Helpers shared by the v1.0 pack modules of Operations Remittance (brd02r_v10_pack.py) and Claims
(brd07_v10_pack.py): the service calls of a screen read from the API client of its feature folder, the user stories
made from the FRS entries, the file names of the pack and the extra sheets of the data dictionary.
"""

from __future__ import annotations

import dataclasses
import re
from pathlib import Path

import api_facts as af
import code_facts as cf

EXT = {"README": "docx", "FRS": "docx", "BPS": "docx", "UXD": "docx", "RNO": "docx", "ICD": "docx", "DSP": "docx",
       "DDI": "xlsx", "USR": "xlsx", "RTM": "xlsx", "FIT": "xlsx", "CRR": "xlsx", "CRE": "xlsx", "INP": "xlsx",
       "WTU": "xlsx"}


def make_set(C, short: str, reviewers: dict[str, str]) -> list[tuple]:
    """The documents of the pack: key, kind, title in the file name, version, purpose, reviewer. The review workbooks
    carry the module name so that they stay apart from the workbooks of another FRS of the same BRD."""
    n = C.NAME.replace(" ", "_")
    return [
        ("README", "Inputs", f"{short}_How_to_Read_this_Pack", "1.0", "This note: the documents of the pack and who "
         "reviews them.", "All reviewers"),
        ("FRS", "FRS-BDOI", n, C.VERSION, "Requirements, business rules, validations, personas and permissions, "
         "settings and lists of values, statuses, review of BDOI's FRS.", reviewers["FRS"]),
        ("BPS", "BPS-BDOI", f"{n}_Process_and_Storyboards", C.VERSION, "Journey, to-be processes and storyboards.",
         "BDOI Business Unit owners"),
        ("UXD", "UXD-BDOI", f"{n}_UX_Design", C.VERSION, "Every screen: purpose, users, fields, actions, states, "
         "messages.", "BDOI Business Unit owners; UAT lead"),
        ("RNO", "RNO-BDOI", f"{n}_Reports_Notifications_Outputs", C.VERSION, "Reports, documents and files, exports, "
         "notices, alerts and jobs.", "BDOI Business Unit owners; Internal Audit"),
        ("ICD", "ICD-BDOI", f"{n}_Interface_Specification", C.ICD_VERSION, reviewers["ICD"], "BDOI IT (integration)"),
        ("DSP", "DSP-BDOI", f"{n}_Developer_Specification", "1.0", "Screens, service operations, field validations, "
         "rules, states, jobs, audit events and permissions, as the platform works today.", "BDOI IT; iorta TechNXT "
         "developers and testers"),
        ("DDI", "DDI-BDOI", f"{n}_Data_Dictionary", "1.0", "Tables, columns, lists of values, parameters, status "
         "models and indexes" + reviewers.get("DDI", "."), "BDOI IT (database administration); Data Migration"),
        ("USR", "USR-BDOI", f"{n}_User_Stories", "1.0", "User stories with acceptance criteria, ready to import into "
         "the backlog tool.", "BDOI Product Owner; UAT lead"),
        ("RTM", "RTM", f"{short}_Test_Cases_and_Traceability", C.WB_VERSION, "Test cases (every acceptance "
         "criterion) and the trace BRD, requirement, user story, screen, test case.", "UAT lead; Internal Audit"),
        ("FIT", "FitGap", f"{short}_BRD_vs_OOTB_vs_Best_Practice", C.WB_VERSION, "Each requirement against the BRD "
         "and the BIBS platform, with the fit and the delivery work.", "BDOI Product Owner"),
        ("CRR", "CR", f"{short}_Change_Request_Register", C.WB_VERSION, "Change requests and their status.",
         "BDOI Product Owner"),
        ("CRE", "CR", f"{short}_Effort_Estimate", C.WB_VERSION, "Effort of the open change requests and of the "
         "delivery work.", "BDOI Product Owner"),
        ("INP", "Inputs", f"{short}_Business_Unit_Requirements_Collection", C.WB_VERSION, "Values and answers the "
         "Business Unit is asked to give.", "BDOI Business Unit owners"),
        ("WTU", "UAT", f"{short}_Business_Unit_Walkthrough_Users", C.WB_VERSION, "Accounts and script of the "
         "walkthroughs.", "UAT lead"),
    ]


def file_name(SET, brd: str, key: str) -> str:
    kind, title, ver = next((k2, t, v) for k, k2, t, v, *_ in SET if k == key)
    return f"BIBS_{kind}_{brd}_{title}_v{ver}.{EXT[key]}"


def stories(C, personas: dict[str, str], epic_of: dict[str, int]) -> list[tuple]:
    """One user story per FRS entry, covering every requirement row of the entry."""
    out = []
    for f in C.FRUM:
        f["epic"] = epic_of[f["id"]]
        p, want, so, screen, prio, pts, gwt = f["story"]
        rules = sorted({x.strip() for r in f["reqs"] for x in r["rule"].replace(";", ",").split(",")
                        if x.strip().startswith("BR-")})
        out.append((f"US-{C.CODE}-{f['id'].rsplit('.', 1)[1]}", f"EP-{C.CODE}-{f['epic']:02d}", personas[p], want,
                    so, gwt, ", ".join(r["id"] for r in f["reqs"]), ", ".join(rules) or "-", screen, prio, pts))
    return out


def _client_calls(files: list[Path]) -> dict[tuple[str, str], tuple[str, str]]:
    """(object, function) -> (verb, path) of the API clients of a feature folder (same reading as
    api_facts.client_calls, which reads the shared client folder only)."""
    out: dict[tuple[str, str], tuple[str, str]] = {}
    for p in files:
        t = p.read_text(encoding="utf-8")
        consts = {m.group(1): m.group(2) for m in re.finditer(r"^const (\w+)\s*=\s*['`]([^'`$]*)['`];", t, re.M)}
        for om in re.finditer(r"export const (\w+Api)\s*=\s*\{", t):
            start = om.end() - 1
            obj = t[start + 1:cf.matching(t, start)]
            for em in re.finditer(r"^\s{2}(\w+):\s*(?:async\s*)?\([^)]*\)[^=]*=>\s*\n?\s*(?:\w+\.)?api\.(get|post|put|"
                                  r"delete|patch|getFile|postFile|upload|download)\w*(?:<[^>]*(?:<[^>]*>)?[^>]*>)?\(\s*"
                                  r"(`[^`]*`|'[^']*')", obj, re.M):
                path = em.group(3).strip("`'")
                path = re.sub(r"\$\{(\w+)\}", lambda x: consts.get(x.group(1), "{x}"), path)
                path = re.sub(r"\$\{[^}]*\}", "{x}", path)
                verb = {"getFile": "GET", "download": "POST", "postFile": "POST", "upload": "POST"}.get(
                    em.group(2), em.group(2).upper())
                out[(om.group(1), em.group(1))] = (verb, path.split("?")[0])
    return out


def screen_calls(feature: str, page: str, ops) -> list[tuple[str, str, str]]:
    """(verb, path, function) of the calls a screen makes that reach an operation of the module."""
    feat = af.FRONT / "features" / feature
    calls = _client_calls(sorted(feat.rglob("api.ts")) + sorted(feat.rglob("*Api.ts")))
    out: list[tuple[str, str, str]] = []
    for f in af.screen_files(feature, page):
        for m in re.finditer(r"\b(\w+Api)\.(\w+)\b", f.read_text(encoding="utf-8")):
            c = calls.get((m.group(1), m.group(2)))
            if not c:
                continue
            verb, path = c
            call = af.ClientCall("x", m.group(2), verb, path)
            if af.match_operation(call, ops) and (verb, path, m.group(2)) not in out:
                out.append((verb, path, m.group(2)))
    return out


def clean_operations() -> None:
    """Paths of upload operations carry the media type of their mapping ("x/upload\\", consumes = ..."); the reader
    of the operations is wrapped so that the developer specification shows the path only."""
    if getattr(af.operations, "_clean", False):
        return
    orig = af.operations

    def operations(*args, **kwargs):
        return [dataclasses.replace(o, path=o.path.split('"')[0], raw_path=o.raw_path.split('"')[0])
                for o in orig(*args, **kwargs)]
    operations._clean = True
    af.operations = operations


def layout_sheet(wb, Column, name: str, title: str, rows: list[tuple], heads: tuple[str, ...], description: str):
    """An extra sheet of the data dictionary with the fields of a file layout."""
    keys = ["a", "b", "c", "d"][:len(heads)]
    widths = [10, 34, 20, 60][:len(heads)]
    wb.sheet(name, [Column(k, h, w) for k, h, w in zip(keys, heads, widths)],
             [dict(zip(keys, r)) for r in rows], description=description)
