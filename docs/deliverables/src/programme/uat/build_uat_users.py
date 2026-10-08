"""Builds the UAT walkthrough users workbook of BIBS (programme level, BRD-00): BRD -> personas -> sign-in IDs.

    python docs/deliverables/src/programme/uat/build_uat_users.py                  # the workbook
    python docs/deliverables/src/programme/uat/build_uat_users.py --check          # checks only
    python docs/deliverables/src/programme/uat/build_uat_users.py --snapshot F     # refresh seed_users.yaml from F

Output: Programme/UAT/BIBS_UAT_BRD-00_Walkthrough_Users_and_Sign-in_v<version>.xlsx with the sheets Summary, one
sheet per BRD (BRD-00 to BRD-13), All Users, Walkthrough Script and Gaps fixed.

Sources:
  uat_users.yaml    the personas of each BRD in the order of the business flow, with their sign-in IDs (curated)
  seed_users.yaml   the SIT/UAT users, group profiles and branches of a fresh seed environment, as queried from it
                    (SNAPSHOT_QUERY below, run on a database migrated with the seed profile; the JSON it returns is
                    turned into seed_users.yaml with --snapshot). It never holds a password or a password hash.
  the FRS of each BRD (persona table: responsibilities; storyboard index: the process frames of the BRDs without a
  sign-off pack) and the BRD-13 handbook; the sign-off packs (pack/walkthroughs.yaml and the screen routes);
  the sidebar of the web client (code_facts.frontend_menu) for the menu path to start.

The SIT/UAT password is never written into the workbook: every row says "UAT seed password (provided separately)".
The insurer suite (its group profiles and SIT/UAT users) is removed from the platform and is not listed.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.append(str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
import code_facts  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

TITLE = "Walkthrough Users and Sign-in"
# The insurer suite: group profiles and SIT/UAT users that are removed from the platform and never listed.
INSURER_ROLES = re.compile(r"^(?:UNDERWRITER|CLAIMS_OFFICER|RI_OFFICER|SIT_INS_.*)$")
INSURER_USERS = {"uw", "claims", "reinsurer"}
# Sections of the sidebar of each BRD (route prefixes), for the menu path of a persona without a walkthrough step.
SECTIONS = {
    "BRD-00": ["/admin", "/setup", "/reports", "/approvals", "/"],
    "BRD-01": ["/crm", "/quotations", "/accounts", "/proposals", "/placement", "/issuance", "/booking", "/workspace"],
    "BRD-02": ["/operations", "/cashiering", "/remittance", "/prodrecon", "/adjustment", "/commission"],
    "BRD-03": ["/product-maintenance", "/catalog"],
    "BRD-04": ["/collections"],
    "BRD-05": ["/gl", "/payment-requests", "/disbursement", "/acsl", "/frbs", "/closing", "/payables"],
    "BRD-06": ["/renewal"],
    "BRD-07": ["/claims-handling"],
    "BRD-08": ["/eb"],
    "BRD-09": ["/csf"],
    "BRD-10": ["/screening", "/screening-setup"],
    "BRD-11": ["/user-access", "/admin"],
    "BRD-12": ["/submitted"],
    "BRD-13": ["/migration", "/legacy-inquiry"],
}
# The query that gives seed_users.yaml (psql, unaligned tuples): users, group profiles with their permissions,
# branches, companies and the sign-in parameters. No password or password hash is selected.
SNAPSHOT_QUERY = """
select json_build_object(
 'companies', (select json_agg(json_build_object('id', id, 'code', code, 'name', name)) from org_company),
 'branches', (select json_agg(json_build_object('id', id, 'code', code, 'name', name, 'company_id', company_id))
              from org_branch),
 'roles', (select json_agg(json_build_object('code', r.code, 'name', r.name, 'active', r.active,
            'permissions', (select coalesce(json_agg(p.permission order by p.permission), '[]'::json)
                            from sec_role_permission p where p.role_id = r.id))) from sec_role r),
 'users', (select json_agg(json_build_object('username', u.username, 'full_name', u.full_name,
            'enabled', u.enabled, 'locked', u.locked, 'limit', u.authorization_limit, 'branch_id', u.home_branch_id,
            'all_companies', u.all_companies,
            'roles', (select coalesce(json_agg(r.code order by r.code), '[]'::json)
                      from sec_user_role ur join sec_role r on r.id = ur.role_id where ur.user_id = u.id),
            'scope', (select coalesce(json_agg(json_build_object('company_id', s.company_id, 'branch_id', s.branch_id)),
                                      '[]'::json) from sec_user_data_scope s where s.user_id = u.id))
            order by u.username) from sec_user u),
 'params', (select json_agg(json_build_object('key', param_key, 'value', param_value)) from sys_parameter
            where param_key in ('LOGIN_MAX_FAILED_ATTEMPTS', 'PASSWORD_MAX_AGE_DAYS')));
"""


# --------------------------------------------------------------------------------------------- sources


def load_yaml(path: Path) -> Any:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def snapshot_to_yaml(src: Path, dst: Path) -> None:
    """seed_users.yaml from the JSON of SNAPSHOT_QUERY (insurer suite left out)."""
    text = src.read_text(encoding="utf-8")
    raw = json.loads(text[text.index("{"):])  # psql may print its settings first
    branches = {b["id"]: b for b in raw["branches"]}
    companies = {c["id"]: c for c in raw["companies"]}
    users = []
    for u in raw["users"]:
        if u["username"] in INSURER_USERS:
            continue
        roles = [r for r in u["roles"] if not INSURER_ROLES.match(r)]
        b = branches.get(u["branch_id"]) or {}
        users.append({
            "id": u["username"], "name": u["full_name"], "roles": roles,
            "company": companies.get(b.get("company_id"), {}).get("name"), "branch": b.get("name"),
            "status": "Locked" if u["locked"] else ("Active" if u["enabled"] else "Disabled"),
            "limit": float(u["limit"]) if u["limit"] is not None else None,
            "scope": "All companies and branches" if u["all_companies"] else "; ".join(
                f"{companies[s['company_id']]['name']}" + (f" - {branches[s['branch_id']]['name']}"
                                                           if s["branch_id"] else " (all branches)")
                for s in u["scope"]),
        })
    roles = sorted(({"code": r["code"], "name": r["name"], "active": r["active"], "permissions": r["permissions"]}
                    for r in raw["roles"] if not INSURER_ROLES.match(r["code"])), key=lambda r: r["code"])
    params = {p["key"]: p["value"] for p in raw.get("params") or []}
    head = ("# SIT/UAT users, group profiles and branches of a fresh seed environment (seed profile), queried from it\n"
            "# with build_uat_users.SNAPSHOT_QUERY and converted with --snapshot. No password or password hash.\n"
            "# The insurer suite (removed from the platform) is left out.\n")
    body = yaml.safe_dump({"params": params, "users": users, "roles": roles}, sort_keys=False, allow_unicode=True,
                          width=120)
    dst.write_text(head + body, encoding="utf-8")


def _md_table_rows(text: str) -> list[list[str]]:
    rows = []
    for line in text.splitlines():
        if line.startswith("|") and not line.startswith("|---"):
            rows.append([c.strip() for c in line.strip().strip("|").split("|")])
    return rows


def frs_file(brd: str) -> Path:
    folder = brand.src_dir(brd)
    if brd == "BRD-13":
        return folder / "HANDBOOK_BRD13_DATA_MIGRATION.md"
    return next(folder.glob("FRS_*.md"))


def frs_personas(brd: str) -> dict[str, str]:
    """Persona -> responsibilities of the persona table of the FRS (BRD-13: of the handbook)."""
    text = frs_file(brd).read_text(encoding="utf-8")
    m = re.search(r"^## Personas[^\n]*\n(.*?)(?=^#{1,2} )", text, re.S | re.M)
    rows = _md_table_rows(m.group(1)) if m else []
    out = {}
    for r in rows[1:]:
        resp = r[1] if brd in ("BRD-00", "BRD-13") else (r[2] if len(r) > 2 else "")
        out[r[0]] = resp
    return out


def storyboard(brd: str) -> list[dict[str, str]]:
    """The frames of the storyboard index of the FRS: frame, group title, persona, action, screen, outcome."""
    text = frs_file(brd).read_text(encoding="utf-8")
    m = re.search(r"^# Appendix: Storyboard index\n(.*?)(?=^# |\Z)", text, re.S | re.M)
    rows = _md_table_rows(m.group(1)) if m else []
    if not rows:
        return []
    head = [h.lower() for h in rows[0]]
    frames, group = [], ""
    for r in rows[1:]:
        cell = dict(zip(head, r))
        if cell.get("frame", "").startswith("**"):
            group = (r[2] if len(r) > 2 else "").strip("* ")
            continue
        if brd == "BRD-13":
            action, screen, outcome = cell["action"], cell["screen"], cell["what the user sees / outcome"]
            group = flow_titles(brd).get(cell["frame"].split(".")[0], "")
        else:
            action, screen, outcome = cell["step"], cell["screen or document"], cell["outcome"]
        frames.append({"id": cell["frame"], "group": group, "persona": cell["persona"], "action": action,
                       "screen": screen, "outcome": outcome})
    return frames


def flow_titles(brd: str) -> dict[str, str]:
    """Flow ID -> title of the end-to-end flows of the UX Screen Deck (pack/ux.yaml), the storyboard of BRD-13."""
    path = brand.src_dir(brd) / "pack" / "ux.yaml"
    return {f["id"]: f["title"] for f in (load_yaml(path) or {}).get("flows") or []} if path.exists() else {}


def pack_walkthroughs(brd: str) -> list[dict[str, Any]]:
    path = brand.src_dir(brd) / "pack" / "walkthroughs.yaml"
    return load_yaml(path)["walkthroughs"] if path.exists() else []


def screen_routes() -> dict[str, str]:
    """Screen ID -> route, from the screen specifications of every sign-off pack."""
    routes = {}
    for f in sorted(brand.SRC_DIR.glob("BRD-*/pack/screens/*.yaml")):
        for s in (load_yaml(f) or {}).get("screens") or []:
            if s.get("route"):
                routes[s["id"]] = s["route"]
    return routes


# --------------------------------------------------------------------------------------------- model


class Model:
    def __init__(self) -> None:
        self.spec = load_yaml(HERE / "uat_users.yaml")
        seed = load_yaml(HERE / "seed_users.yaml")
        self.users: dict[str, dict] = {u["id"]: u for u in seed["users"]}
        self.roles: dict[str, dict] = {r["code"]: r for r in seed["roles"]}
        self.params: dict[str, str] = seed.get("params") or {}
        self.routes = screen_routes()
        self.menu = code_facts.frontend_menu()
        self.errors: list[str] = []
        self.brds = self.spec["brds"]
        for b in self.brds:
            b["frs"] = frs_personas(b["brd"])
            b["walkthroughs"] = pack_walkthroughs(b["brd"])
            b["frames"] = [] if b["walkthroughs"] else storyboard(b["brd"])
            for p in b["personas"]:
                p["users"] = [u if isinstance(u, dict) else {"id": u} for u in p.get("users") or []]
            b["script"] = self._script(b)
        self._check()

    # ---- helpers

    def role_name(self, code: str) -> str:
        return self.roles[code]["name"] if code in self.roles else code

    def persona_of(self, b: dict, key: str) -> dict | None:
        """The persona of a walkthrough step (role code) or a storyboard frame (persona name or alias)."""
        alias = (b.get("aliases") or {}).get(key)
        if isinstance(alias, str):
            key = alias
        for p in b["personas"]:
            if p["persona"] == key or p.get("role") == key:
                return p
        return None

    def step_signins(self, b: dict, step_id: str, key: str) -> list[str]:
        override = (b.get("step_users") or {}).get(step_id)
        if override:
            return list(override)
        alias = (b.get("aliases") or {}).get(key)
        if isinstance(alias, list):
            return alias
        p = self.persona_of(b, key)
        if p is None or not p["users"]:
            return []
        return [p["users"][0]["id"]]

    def _script(self, b: dict) -> list[dict[str, Any]]:
        rows = []
        excl = set(b.get("exclude_steps") or []) | set(b.get("exclude_frames") or [])
        titles = b.get("titles") or {}
        for w in b["walkthroughs"]:
            for i, s in enumerate(w["steps"], start=1):
                sid = f"{w['id']}.{i}"
                if sid in excl:
                    continue
                p = self.persona_of(b, s[0])
                rows.append({"brd": b["brd"], "flow": f"{w['id']} {titles.get(w['id'], w['title'])}", "step": sid,
                             "key": s[0], "persona": p["persona"] if p else s[0], "screen": s[1],
                             "users": self.step_signins(b, sid, s[0]), "action": s[2], "result": s[3]})
        for f in b["frames"]:
            if f["id"] in excl:
                continue
            p = self.persona_of(b, f["persona"])
            sid = re.match(r"SCR-[A-Z]+-\d+", f["screen"] or "")
            rows.append({"brd": b["brd"], "flow": f"{f['id'].split('.')[0]} {f['group']}".strip(), "step": f["id"],
                         "key": f["persona"], "persona": p["persona"] if p else f["persona"],
                         "screen": sid.group(0) if sid else None, "screen_text": f["screen"],
                         "users": self.step_signins(b, f["id"], f["persona"]),
                         "action": f"{f['action']} ({f['screen']})" if f["screen"] else f["action"],
                         "result": f["outcome"]})
        for e in b.get("extra_steps") or []:
            p = self.persona_of(b, e["persona"])
            rows.append({"brd": b["brd"], "flow": e["flow"], "step": e["step"], "key": e["persona"],
                         "persona": p["persona"] if p else e["persona"], "screen": e.get("screen"),
                         "users": self.step_signins(b, e["step"], e["persona"]), "action": e["action"],
                         "result": e["result"]})
        return rows

    def steps_of(self, b: dict, p: dict, user: str) -> list[str]:
        ids = []
        for r in b["script"]:
            if user in r["users"] or (r["persona"] == p["persona"] and not r["users"]):
                ids.append(r["step"])
        return ids

    def menu_path(self, b: dict, p: dict, user: str) -> str:
        perms = set(self.roles[p["role"]]["permissions"]) if p.get("role") in self.roles else set()
        visible = [(g, s, sc) for g in code_facts.menu_for(perms, self.menu) for s in g.sections for sc in s.screens]
        chosen = (b.get("menus") or {}).get(user)
        if chosen:
            if chosen not in {code_facts.menu_path(*v) for v in visible}:
                self.errors.append(f"{b['brd']} {user}: {chosen} is not in the menu of {p['role']}")
            return chosen
        routes = [self.routes.get(r["screen"]) for r in b["script"] if user in r["users"] and r["screen"]]
        for route in [r for r in routes if r]:
            best = None
            for g, s, sc in visible:
                path = sc.path
                if route == path or (path != "/" and route.startswith(path.rstrip("/") + "/")):
                    if best is None or len(path) > len(best[2].path):
                        best = (g, s, sc)
            if best:
                return code_facts.menu_path(*best)
        texts = [r.get("screen_text") or "" for r in b["script"] if user in r["users"]]
        for text in texts:
            hits = [(g, s, sc) for g, s, sc in visible if sc.label and sc.label.lower() in text.lower()]
            if hits:
                return code_facts.menu_path(*max(hits, key=lambda h: len(h[2].label)))
        for prefix in SECTIONS.get(b["brd"], []):
            for g, s, sc in visible:
                if sc.path == prefix or (prefix != "/" and sc.path.startswith(prefix + "/")) or sc.path == prefix:
                    return code_facts.menu_path(g, s, sc)
        return code_facts.menu_path(*visible[0]) if visible else "Home"

    # ---- checks

    def _check(self) -> None:
        err = self.errors.append
        for b in self.brds:
            for p in b["personas"]:
                if p.get("no_user"):
                    if p["no_user"] not in self.spec["no_user"]:
                        err(f"{b['brd']} {p['persona']}: unknown no_user reason {p['no_user']}")
                    continue
                role = p.get("role")
                if role not in self.roles:
                    err(f"{b['brd']} {p['persona']}: group profile {role} is not in the seed environment")
                elif not self.roles[role]["active"]:
                    err(f"{b['brd']} {p['persona']}: group profile {role} is inactive")
                if INSURER_ROLES.match(role or ""):
                    err(f"{b['brd']} {p['persona']}: insurer group profile {role}")
                if not p["users"]:
                    err(f"{b['brd']} {p['persona']}: no sign-in ID")
                for u in p["users"]:
                    seed = self.users.get(u["id"])
                    if seed is None:
                        err(f"{b['brd']} {p['persona']}: {u['id']} is not a SIT/UAT user")
                    elif seed["status"] != "Active" and not u.get("note"):
                        err(f"{b['brd']} {p['persona']}: {u['id']} is {seed['status']}")
                    elif role not in seed["roles"]:
                        err(f"{b['brd']} {p['persona']}: {u['id']} does not hold {role}")
            created = set(b.get("created_in_walkthrough") or [])
            for r in b["script"]:
                if self.persona_of(b, r["key"]) is None and not (b.get("aliases") or {}).get(r["key"]):
                    err(f"{b['brd']} {r['step']}: persona {r['key']} is not on the sheet")
                for u in r["users"]:
                    if u not in self.users and u not in created:
                        err(f"{b['brd']} {r['step']}: {u} is not a SIT/UAT user")
            for p in b["personas"]:
                for u in p["users"]:
                    if p.get("role") in self.roles:
                        self.menu_path(b, p, u["id"])
            for uid in b.get("menus") or {}:
                if not any(u["id"] == uid for p in b["personas"] for u in p["users"]):
                    err(f"{b['brd']}: menu of {uid}, who is not on the sheet")
        for g in self.spec["gaps"]:
            for u in g.get("added") or []:
                if u not in self.users or g["role"] not in self.users[u]["roles"]:
                    err(f"Gap {g['persona']}: {u} missing or without {g['role']}")


# --------------------------------------------------------------------------------------------- workbook


def amount(v: float | None) -> str | None:
    return None if v is None else f"PHP {v:,.2f}"


def place(u: dict, unit: str | None) -> str:
    parts = [u.get("company"), u.get("branch"), unit]
    return " / ".join(x for x in parts if x)


def sheet_name(b: dict) -> str:
    name = f"{b['brd']} {b['module']}"
    name = name.replace("Accounting, Disbursement and ACSL", "Accounting and ACSL")
    name = name.replace("Core Replacement (cross-cutting)", "Cross-cutting")
    name = name.replace("Customer Servicing Facility", "Customer Servicing")
    return name[:31]


def brd_rows(m: Model, b: dict) -> list[dict[str, Any]]:
    rows = []
    units = m.spec["units"]
    pw = m.spec["password_text"]
    for p in b["personas"]:
        if p.get("no_user"):
            rows.append({"persona": p["persona"], "profile": None, "code": None, "id": "None in UAT",
                         "name": None, "place": None, "does": p.get("does") or b["frs"].get(p["persona"]),
                         "menu": None, "steps": ", ".join(r["step"] for r in b["script"]
                                                          if r["persona"] == p["persona"]) or None,
                         "scope": None, "password": None, "remarks": m.spec["no_user"][p["no_user"]]})
            continue
        for u in p["users"]:
            seed = m.users[u["id"]]
            unit = p.get("unit") or units.get(p["role"])
            scope = [x for x in (p.get("scope"), ("Authorisation limit " + amount(seed["limit"]))
                                 if seed["limit"] else None, seed["scope"]) if x]
            others = [m.role_name(r) for r in seed["roles"] if r != p["role"]]
            remarks = [x for x in (u.get("note"), p.get("remarks"),
                                   ("Also holds: " + ", ".join(others)) if others else None,
                                   None if seed["status"] == "Active" else f"Status {seed['status']}") if x]
            rows.append({"persona": p["persona"], "profile": m.role_name(p["role"]), "code": p["role"],
                         "id": u["id"], "name": seed["name"], "place": place(seed, unit),
                         "does": p.get("does") or b["frs"].get(p["persona"]) or None,
                         "menu": m.menu_path(b, p, u["id"]),
                         "steps": ", ".join(m.steps_of(b, p, u["id"])) or "Not in a walkthrough step",
                         "scope": "; ".join(scope), "password": pw, "remarks": "; ".join(remarks) or None})
    return rows


def usage(m: Model) -> dict[str, list[str]]:
    used: dict[str, list[str]] = {}
    for b in m.brds:
        ids = {u["id"] for p in b["personas"] for u in p["users"]} | {u for r in b["script"] for u in r["users"]}
        for i in ids:
            used.setdefault(i, []).append(b["brd"])
    return used


def build(m: Model) -> Path:
    spec = m.spec
    version = str(spec["version"])
    wb = BdoiWorkbook("UAT Walkthrough Users and Sign-in", doc_type="UAT reference", brd="BRD-00", version=version,
                      date=spec["date"], subtitle="Each BRD, the personas of its walkthrough and the UAT sign-in ID "
                                                  "of each persona")
    lock = m.params.get("LOGIN_MAX_FAILED_ATTEMPTS", "3")
    wb.cover_notes = [
        "Sign-in IDs: every persona of each BRD has a sign-in ID in the UAT environment, listed on the sheet of the "
        "BRD in the order of the business flow (maker before checker). The Walkthrough Script sheet gives the steps "
        "with the sign-in ID of each step; All Users lists every SIT/UAT user of the environment.",
        "Password: all SIT/UAT users share the seed password of the UAT environment. The system administrator sets "
        "it in the hosting variables of the UAT back end (BROKERVERSE_SEED_PASSWORD) and gives it to the testers "
        "separately; it is never written in this workbook or any other document.",
        "First sign-in: as delivered (BROKERVERSE_SEED_PASSWORD_MUST_CHANGE = false) testers sign in with the seed "
        "password and go straight to their home page, so several testers can share a persona. When the "
        "administrator sets it to true, each user sees Change Your Password at the first sign-in (\"Your password "
        "was set by an administrator. Choose your own password to continue.\") and chooses a personal password of at "
        "least 10 characters with an upper-case letter, a lower-case letter, a digit and a symbol; from then on only "
        "that personal password opens the user.",
        f"Locked user: the account locks after {lock} wrong passwords in a row; a locked or disabled user sees "
        "\"Invalid user name or password\". The System Administrator (sign-in ID admin) unlocks it on Setup & "
        "Administration › Administration › Users: Status Locked, then Unlock on the row of the user. Where testers "
        "chose personal passwords, a forgotten one is reset through Forgot password? on the Login page or by the "
        "System Administrator; a reload of the UAT environment gives every SIT/UAT user the seed password again.",
        "Group profiles are shown by name; the Group profile code column is for the system administrator.",
    ]
    persona_cols = [
        Column("persona", "Persona", 26, "Business role as named in the persona table of the FRS of the BRD"),
        Column("profile", "Group profile", 26, "Name of the group profile the user holds for this persona"),
        Column("id", "Sign-in ID", 13, "User ID to type on the Login page"),
        Column("name", "User name", 24, "Name of the SIT/UAT user, as shown on the screens and in the history"),
        Column("place", "Company / branch / unit", 30, "Company and home branch of the user, and the unit"),
        Column("does", "What this user does in the walkthrough", 46, "Responsibilities of the persona in the BRD"),
        Column("menu", "Menu path to start", 34, "Where the user starts the walkthrough in the sidebar"),
        Column("steps", "Walkthrough steps", 24, "Steps of the Walkthrough Script sheet done by this user"),
        Column("scope", "Approval limits or data scope", 28, "Authorisation limit and the companies and branches "
                                                              "the user works for"),
        Column("password", "Password", 22, "Never written here: the UAT seed password is provided separately"),
        Column("remarks", "Remarks", 36, "Second users, other group profiles held, decisions"),
        Column("code", "Group profile code", 22, "Code of the group profile, for the system administrator"),
    ]
    summary = []
    sheets = []
    for b in m.brds:
        rows = brd_rows(m, b)
        flows = sorted({r["flow"] for r in b["script"]}, key=lambda f: f.split(" ")[0])
        ids = {r["id"] for r in rows if r["code"]}
        summary.append({"brd": b["brd"], "drop": b["drop"], "module": b["module"],
                        "personas": len(b["personas"]), "users": len(ids), "walkthroughs": len(flows),
                        "steps": len(b["script"]),
                        "source": "Sign-off pack walkthroughs" if b["walkthroughs"] else (
                            "FRS journey index" if b["brd"] == "BRD-00" else "FRS storyboard of the process flows"),
                        "missing": sum(1 for p in b["personas"] if p.get("no_user")) or None})
        sheets.append((b, rows))
    wb.sheet("Summary", [
        Column("brd", "BRD", 9, "BRD number (BRD-00: cross-cutting)"),
        Column("drop", "Drop", 11, "BDOI drop of the BRD"),
        Column("module", "Module", 32, "Module of the BRD"),
        Column("personas", "Personas", 10, "Personas listed on the sheet of the BRD", kind="number"),
        Column("users", "Users", 9, "Distinct sign-in IDs on the sheet of the BRD", kind="number"),
        Column("walkthroughs", "Walkthroughs", 13, "End-to-end walkthroughs (or process storyboards)", kind="number"),
        Column("steps", "Steps", 8, "Steps on the Walkthrough Script sheet", kind="number"),
        Column("source", "Source of the steps", 30, "Where the walkthrough steps come from"),
        Column("missing", "Personas without sign-in", 14, "Personas without a UAT sign-in (see Gaps fixed)",
               kind="number"),
    ], summary, description="Per BRD: personas, users and walkthroughs of the UAT walkthrough")
    for b, rows in sheets:
        wb.sheet(sheet_name(b), persona_cols, rows,
                 description=f"{b['brd']} {b['module']}: personas in the order of the business flow, with the UAT "
                             f"sign-in ID of each")
    used = usage(m)
    all_rows = []
    for uid, u in sorted(m.users.items()):
        all_rows.append({"id": uid, "name": u["name"], "profiles": ", ".join(m.role_name(r) for r in u["roles"]),
                         "codes": ", ".join(u["roles"]), "place": place(u, None), "status": u["status"],
                         "limit": amount(u["limit"]), "scope": u["scope"],
                         "brds": ", ".join(used.get(uid, [])) or None})
    wb.sheet("All Users", [
        Column("id", "Sign-in ID", 13, "User ID to type on the Login page"),
        Column("name", "User name", 28, "Name of the SIT/UAT user"),
        Column("profiles", "Group profiles", 40, "Names of the group profiles the user holds"),
        Column("place", "Company / branch", 34, "Company and home branch"),
        Column("status", "Status", 10, "Active, Disabled or Locked in a fresh UAT environment",
               values=["Active", "Disabled", "Locked"]),
        Column("limit", "Authorisation limit", 18, "Approval limit of the user, where set"),
        Column("scope", "Data scope", 24, "Companies and branches the user works for"),
        Column("brds", "Used in BRDs", 22, "BRD sheets and walkthrough steps that use the user"),
        Column("password", "Password", 22, "Never written here: the UAT seed password is provided separately"),
        Column("codes", "Group profile code", 26, "Codes of the group profiles, for the system administrator"),
    ], [{**r, "password": spec["password_text"]} for r in all_rows],
        description="Every SIT/UAT user of the UAT environment, as loaded by the seed data")
    script_rows = []
    for b in m.brds:
        for r in b["script"]:
            script_rows.append({"brd": b["brd"], "flow": r["flow"], "step": r["step"], "persona": r["persona"],
                                "users": ", ".join(r["users"]) or "No UAT sign-in", "action": r["action"],
                                "result": r["result"]})
    wb.sheet("Walkthrough Script", [
        Column("brd", "BRD", 9, "BRD number"),
        Column("flow", "Walkthrough", 34, "End-to-end walkthrough, or process of the storyboard"),
        Column("step", "Step", 9, "Step ID (walkthrough letter and step number, or storyboard frame)"),
        Column("persona", "Persona", 26, "Persona who does the step"),
        Column("users", "Sign-in ID", 16, "Sign-in ID to use for the step"),
        Column("action", "Action", 60, "What the user does"),
        Column("result", "Expected result", 46, "What the user sees, or the outcome of the step"),
    ], script_rows, description="Per BRD, the end-to-end scenario step by step with the sign-in ID of each step")
    gap_rows = []
    for g in spec["gaps"]:
        added = g.get("added") or []
        gap_rows.append({"brd": g["brd"], "persona": g["persona"],
                         "profile": m.role_name(g["role"]) if g.get("role") else None, "before": g["before"],
                         "added": "; ".join(f"{u} ({m.users[u]['name']})" for u in added) or None,
                         "status": "Added" if added else "BDOI decision",
                         "remarks": spec["no_user"][g["decision"]] if g.get("decision") else
                         "Added to the SIT/UAT users of every seed environment; signs in with the UAT seed password"})
    wb.sheet("Gaps fixed", [
        Column("brd", "BRD", 9, "BRD number"),
        Column("persona", "Persona", 30, "Persona of the BRD or FRS"),
        Column("profile", "Group profile", 30, "Group profile of the user added"),
        Column("before", "Before 08-Oct-2026", 44, "What was missing"),
        Column("added", "Sign-in ID added", 30, "SIT/UAT user added, with the name"),
        Column("status", "Status", 14, "Added, or a decision of BDOI is needed", values=["Added", "BDOI decision"],
               status=True),
        Column("remarks", "Remarks", 50, "How the user signs in, or the decision BDOI is asked for"),
    ], gap_rows, description="Personas named in a BRD or FRS that had no UAT user, and what was added")
    wb.legend = [("Added", "SIT/UAT user added for the persona"),
                 ("BDOI decision", "No UAT sign-in until BDOI decides")]
    out = brand.out_path("BRD-00", "UAT", brand.output_name("UAT", "BRD-00", TITLE, version, "xlsx"))
    return wb.save(out)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="check the sources only")
    ap.add_argument("--snapshot", type=Path, help="JSON of SNAPSHOT_QUERY to turn into seed_users.yaml")
    args = ap.parse_args(argv)
    if args.snapshot:
        snapshot_to_yaml(args.snapshot, HERE / "seed_users.yaml")
        print(f"seed_users.yaml refreshed from {args.snapshot}")
    m = Model()
    for e in m.errors:
        print("ERROR", e)
    if m.errors:
        return 1
    if args.check:
        print(f"OK: {sum(len(b['personas']) for b in m.brds)} personas, {len(m.users)} users, "
              f"{sum(len(b['script']) for b in m.brds)} steps")
        return 0
    path = build(m)
    print(f"{path.relative_to(REPO)} ({path.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
