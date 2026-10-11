"""Plans the screen checks of the walkthrough script of the UAT users workbook: for every step, the screens the
step names (matched to the screens of the platform by their menu labels), the record pages to open from their
lists, the report to run and the tabs or actions the step names on them. Output: data/steps_plan.json, read by
run_steps.cjs (which signs in as the persona of the step and checks each screen).

Usage: python3 plan_steps.py
"""

from __future__ import annotations

import json
import re
from pathlib import Path

import openpyxl

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
WORKBOOK = ROOT / "docs/deliverables/out/Programme/UAT/BIBS_UAT_BRD-00_Walkthrough_Users_and_Sign-in_v1.0.xlsx"
FEATURES = ROOT / "frontend/src/features"
STOP = {"the", "a", "an", "of", "and", "or", "to", "on", "for", "in", "with", "page", "screen", "tab", "list",
        "record", "brd", "frs", "walkthrough", "storyboard"}
# The list a record page is opened from, when it is not the path before the record id.
RECORD_LISTS = {"/claims-handling/:id": "/claims-handling/worklist", "/submitted/policies/:id": "/submitted/masterlist"}
# Screen names of the script that are not menu labels.
ALIASES = {
    "my approvals": "/approvals",
    "my work": "/my-work",
    "reports": "/reports",
    "report centre": "/reports",
    "claim record": "/claims-handling/:id",
    "policy record": "/submitted/policies/:id",
    "account page": "/accounts/:id",
}


def screens() -> list[tuple[str, str]]:
    out = []
    for f in FEATURES.rglob("*.ts*"):
        if ".test." in f.name:
            continue
        text = f.read_text(encoding="utf-8")
        for m in re.finditer(r"path:\s*'([^']+)',\s*\n\s*label:\s*'([^']+)'", text):
            out.append((m.group(2), m.group(1)))
    return out


def tokens(text: str) -> set[str]:
    return {t for t in re.findall(r"[a-z0-9]+", text.lower()) if t not in STOP and len(t) > 1}


def match(name: str, catalogue: list[tuple[str, str]]) -> str | None:
    low = name.lower().strip()
    if low in ALIASES:
        return ALIASES[low]
    for label, path in catalogue:
        if label.lower() == low:
            return path
    best, score = None, 0.0
    want = tokens(name)
    if not want:
        return None
    for label, path in catalogue:
        have = tokens(label)
        if not have:
            continue
        s = len(want & have) / len(want | have)
        if s > score:
            best, score = path, s
    return best if score >= 0.5 else None


def parts(action: str) -> list[tuple[str, list[str]]]:
    """Screen names in the parentheses at the end of an action, each with the tabs or actions named inside it."""
    m = re.search(r"\(([^()]*(?:\([^()]*\)[^()]*)*)\)\s*$", action)
    if not m:
        return []
    out = []
    for item in re.split(r";\s*", m.group(1)):
        inner = re.search(r"^(.*?)\s*\((.*)\)\s*$", item)
        if inner:
            out.append((inner.group(1).strip(), [p.strip() for p in re.split(r",\s*", inner.group(2)) if p.strip()]))
        else:
            out.append((item.strip(), []))
    return out


def quoted_actions(action: str) -> list[str]:
    """Buttons the action names: 'clicks X', 'Clicks X and Y' (capitalised words)."""
    out = []
    for m in re.finditer(r"[Cc]licks? ((?:[A-Z][\w/-]*\s?)+)", action):
        out.append(m.group(1).strip())
    return out


def main() -> None:
    catalogue = screens()
    wb = openpyxl.load_workbook(WORKBOOK, read_only=True)
    rows = list(wb["Walkthrough Script"].iter_rows(values_only=True))[4:]
    plan = []
    for brd, walkthrough, step, persona, users, action, expected in rows:
        if not brd:
            continue
        user = re.split(r"[,;/ ]+", str(users).strip())[0] if users else ""
        checks = []
        for name, items in parts(str(action)):
            report = re.findall(r"\b[A-Z]{2,4}-[A-Z0-9-]{3,}\b", " ".join(items) + " " + name)
            path = match(name, catalogue)
            if report and (path in (None, "/reports") or name.lower().startswith("report")):
                checks += [{"name": f"Report {code}", "path": f"/reports/{code}", "items": []} for code in report]
                continue
            if path:
                record = ":" in path
                listing = RECORD_LISTS.get(path) or (path.split("/:")[0] if record else None)
                checks.append({"name": name, "path": path, "list": listing,
                               "items": [i for i in items if not re.match(r"[A-Z]{2,4}-|\d", i)]})
        plan.append({"brd": brd, "walkthrough": walkthrough, "step": step, "persona": persona, "user": user,
                     "users": users, "action": action, "expected": expected, "checks": checks,
                     "buttons": quoted_actions(str(action))})
    out = HERE / "data" / "steps_plan.json"
    out.parent.mkdir(exist_ok=True)
    out.write_text(json.dumps(plan, indent=1), encoding="utf-8")
    mapped = sum(1 for p in plan if p["checks"])
    print(f"{len(plan)} steps, {mapped} with a screen to check")


if __name__ == "__main__":
    main()
