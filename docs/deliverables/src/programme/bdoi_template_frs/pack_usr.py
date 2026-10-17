"""User Stories (USR) of a BRD pack: epics and stories with acceptance criteria in Given / When / Then, ready to
import into the backlog tool. The columns follow the programme user story backlog (out/Programme/Backlog): Issue Type,
Issue ID, Parent, Summary, Epic Name, Description, Priority, Labels, Component, Fix Version, Story Points, BRD IDs,
FR IDs, Acceptance Criteria; the pack adds Persona, Rules and Screen.
"""

from __future__ import annotations

from pathlib import Path

from pack_common import Column, save_workbook, workbook

PRIORITY = ["Highest", "High", "Medium", "Low"]


def stories(K) -> list[dict]:
    """The stories of the content module with their criteria (US-UA-025 takes K.REPORT_AC)."""
    out = []
    for sid, epic, persona, want, so, acs, frum, rules, screen, prio, pts in K.STORIES:
        acs = acs if acs is not None else K.REPORT_AC
        out.append({"id": sid, "epic": epic, "persona": persona, "want": want, "so": so, "acs": acs, "frum": frum,
                    "rules": rules, "screen": screen, "prio": prio, "pts": pts,
                    "story": f"As a {persona}, I want {want}, so that {so}." if persona[0] not in "AEIOU"
                    else f"As an {persona}, I want {want}, so that {so}.",
                    "ac": "\n".join(f"(AC{i}) Given {g}, when {w}, then {t}." for i, (g, w, t) in enumerate(acs, 1))})
    return out


def write(K, out: Path, fix_version: str) -> tuple[Path, dict, list[dict]]:
    rows = []
    epic_names = dict(K.EPICS)
    items = stories(K)
    base = 1000
    epic_id = {e: base + i for i, (e, _) in enumerate(K.EPICS, 1)}
    screens = K.screens_by_id()
    for e, name in K.EPICS:
        mine = [s for s in items if s["epic"] == e]
        frs = sorted({x.strip() for s in mine for x in s["frum"].split(",")})
        rows.append({"type": "Epic", "iid": epic_id[e], "parent": None, "key": e, "summary": f"{K.SHORT} - {name}",
                     "epic": f"{K.SHORT} - {name}", "persona": "-", "story": "-",
                     "desc": f"{name} of {K.NAME} ({K.BRD}). Stories: {len(mine)}; story points: "
                             f"{sum(s['pts'] for s in mine)}.",
                     "prio": "High", "labels": f"{K.BRD} {fix_version.replace(' ', '-')}", "comp": K.NAME,
                     "fix": fix_version, "pts": None, "brd": K.BRD, "fr": ", ".join(frs), "rules": "-",
                     "screen": "-", "ac": "-"})
    for i, s in enumerate(items, 1):
        scr = screens[s["screen"]]
        rows.append({"type": "Story", "iid": base + 100 + i, "parent": epic_id[s["epic"]], "key": s["id"],
                     "summary": s["want"][0].upper() + s["want"][1:], "epic": f"{K.SHORT} - {epic_names[s['epic']]}",
                     "persona": s["persona"], "story": s["story"],
                     "desc": f"{s['story']}\n\nScreen: {s['screen']} {scr['name']}. Requirements: {s['frum']}. "
                             f"Rules: {s['rules']}.",
                     "prio": s["prio"], "labels": f"{K.BRD} {fix_version.replace(' ', '-')}", "comp": K.NAME,
                     "fix": fix_version, "pts": s["pts"], "brd": K.BRD, "fr": s["frum"], "rules": s["rules"],
                     "screen": f"{s['screen']} {scr['name']}", "ac": s["ac"]})
    wb = workbook("User Stories", "User stories", K.BRD, K.version("USR"), K.DATE,
                  f"{K.NAME}: epics and user stories with acceptance criteria")
    cols = [Column("type", "Issue Type", 10, values=["Epic", "Story"]), Column("iid", "Issue ID", 9, kind="number"),
            Column("parent", "Parent", 9, kind="number"), Column("key", "Story ID", 11),
            Column("summary", "Summary", 40), Column("epic", "Epic Name", 30), Column("persona", "Persona", 22),
            Column("story", "User story", 60), Column("desc", "Description", 50),
            Column("prio", "Priority", 10, values=PRIORITY), Column("labels", "Labels", 16),
            Column("comp", "Component", 22), Column("fix", "Fix Version", 10),
            Column("pts", "Story Points", 9, kind="number"), Column("brd", "BRD IDs", 9),
            Column("fr", "FR IDs", 26), Column("rules", "Rule IDs", 18), Column("screen", "Screen", 30),
            Column("ac", "Acceptance Criteria", 80)]
    wb.sheet("Stories", cols, rows, description="One row per epic, then its stories. Issue ID and Parent link a "
             "story to its epic on import; the columns match the import file of the programme backlog.")
    wb.sheet("Epics", [Column("e", "Epic ID", 10), Column("n", "Epic", 36), Column("s", "Stories", 9, kind="number"),
                       Column("p", "Story points", 11, kind="number")],
             [{"e": e, "n": n, "s": sum(1 for s in items if s["epic"] == e),
               "p": sum(s["pts"] for s in items if s["epic"] == e)} for e, n in K.EPICS],
             description="Epics with their number of stories and points (first estimate).")
    target = save_workbook(wb, out / K.file_name("USR"))
    return target, {"Epics": len(K.EPICS), "Stories": len(items),
                    "Story points": sum(s["pts"] for s in items)}, items
