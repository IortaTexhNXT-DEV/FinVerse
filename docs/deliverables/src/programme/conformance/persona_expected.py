"""Lists, for every persona of the FRS persona tables, its SIT/UAT sign-in and the sidebar the FRS gives it (the
"Menu by persona" of the sign-off packs: the menu screens the grants of the persona's role open; the personas
and their sign-ins are those of the UAT users workbook), and for the personas of the persona suites (BRD-7 to
BRD-13) the screens of the suite's sections. Output:
data/persona_expected.json (and the menu name of every screen, data/screen_labels.json), read by persona_access.cjs (which signs in as each user and compares the sidebar it
shows) and build_conformance.py.

The expected sidebar of a user holding several roles is the union of the menus of its roles (seed_users.yaml).

Usage: python3 persona_expected.py   (TOOLS_DIR overrides the folder of code_facts.py)
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
sys.path.insert(0, os.environ.get("TOOLS_DIR") or str(ROOT / "tools/deliverables"))
import code_facts  # noqa: E402

UAT_USERS = ROOT / "docs/deliverables/src/programme/uat/uat_users.yaml"
SUITES = ROOT / "frontend/src/navigation/personaMenus.json"
SEED_USERS = ROOT / "docs/deliverables/src/programme/uat/seed_users.yaml"


def main() -> None:
    groups = code_facts.frontend_menu()
    grants = code_facts.role_grants()
    roles_of = {u["id"]: u.get("roles") or [] for u in yaml.safe_load(SEED_USERS.read_text(encoding="utf-8"))["users"]}

    def menu(roles: list[str]) -> list[str]:
        perms: set[str] = set()
        for r in roles:
            perms |= grants.get(r, set())
        return sorted({sc.path for g in code_facts.menu_for(perms, groups) for s in g.sections for sc in s.screens})

    suites = {}
    for suite in json.loads(SUITES.read_text(encoding="utf-8"))["suites"]:
        for key, spec in suite["roles"].items():
            suites[(spec.get("role") or key.split(":")[-1], spec["seedUser"])] = (suite["sections"], spec["screens"])
    out = []
    for b in yaml.safe_load(UAT_USERS.read_text(encoding="utf-8"))["brds"]:
        for p in b.get("personas") or []:
            role, users = p.get("role"), p.get("users") or []
            if not role or not users:
                continue
            user = users[0] if isinstance(users[0], str) else users[0]["id"]
            if user not in roles_of:
                continue
            sections, screens = suites.get((role, user), (None, None))
            out.append({"brd": b["brd"], "persona": p["persona"], "role": role, "user": user,
                        "frs_menu": menu([role]), "expected": menu(roles_of[user]), "sections": sections,
                        "section_screens": screens})
    labels = {sc.path: f"{g.title or 'Home'} > {sc.label}" for g in groups for s in g.sections for sc in s.screens}
    target = HERE / "data" / "persona_expected.json"
    target.write_text(json.dumps(out, indent=1), encoding="utf-8")
    (HERE / "data" / "screen_labels.json").write_text(json.dumps(labels, indent=1, sort_keys=True), encoding="utf-8")
    print(f"{len(out)} personas")


if __name__ == "__main__":
    main()
