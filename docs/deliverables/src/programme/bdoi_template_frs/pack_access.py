"""Access Matrix of a BRD pack (ACX): group profile x screen x action, written from the code and the seed.

Read from the sources, never typed: the group profiles of the personas (K.PERSONA_ROLES) with their permissions after
every migration and seed script (code_facts.role_grants), the screens of the pack and the permission that opens each
one in the sidebar (navigation/modules.ts), the service calls each screen makes (as in the Developer Specification)
and the permissions each service operation asks for (@PreAuthorize of the controllers). Users and their group-profile
membership come from UIDM-ISC; the group profiles here are the ones UIDM-ISC assigns.
"""

from __future__ import annotations

import re
from pathlib import Path

import api_facts as af
import code_facts as cf
import pack_dsp
from pack_common import Column, save_workbook, workbook

YES = "Y"
VERBS = {"GET": "View", "POST": "Create or act", "PUT": "Change", "PATCH": "Change", "DELETE": "Remove"}


_LIT = re.compile(r"`[^`]*`|'[^']*'")


def _strip_query(path: str) -> str:
    path = re.sub(r"\$\{toQuery\(.*$", "", path)
    path = re.sub(r"\?.*$", "", path)
    return path


def _feature_calls(feature: str) -> dict[tuple[str, str], af.ClientCall]:
    """(object, function) -> call for the api files of a feature (frontend/src/features/<feature>/**/api.ts and
    *Api.ts): the path is resolved through the string constants and the small path helpers of the file."""
    out: dict[tuple[str, str], af.ClientCall] = {}
    feat = af.FRONT / "features" / feature
    for f in sorted(feat.rglob("api.ts")) + sorted(feat.rglob("*Api.ts")) + sorted((af.FRONT / "api").glob("*.ts")):
        t = f.read_text(encoding="utf-8")
        consts = {m.group(1): m.group(2) for m in re.finditer(r"^const (\w+)\s*=\s*['`]([^'`$]*)['`];", t, re.M)}
        helpers = {}
        for m in re.finditer(r"^const (\w+)\s*=\s*\(([^)]*)\)(?:\s*:\s*\w+)?\s*=>\s*\n?\s*`([^`]*)`", t, re.M):
            names = [a.split(":")[0].split("=")[0].strip() for a in m.group(2).split(",")]
            helpers[m.group(1)] = (names, m.group(3))

        def nested(text: str) -> str:
            # ${helper(args)} inside a template: the path of the helper
            return re.sub(r"\$\{(\w+)\(([^()]*(?:\([^()]*\))?[^()]*)\)\}",
                          lambda x: resolve(f"{x.group(1)}({x.group(2)})") if x.group(1) in helpers else x.group(0),
                          text)

        def resolve(expr: str) -> str:
            hm = re.match(r"(\w+)\((.*)\)$", expr.strip(), re.S)
            if hm and hm.group(1) in helpers:
                names, tpl = helpers[hm.group(1)]
                args = [a.strip() for a in cf.split_top(hm.group(2))]
                for name, arg in zip(names, args):
                    lit = _LIT.fullmatch(arg)
                    tpl = tpl.replace("${" + name + "}", lit.group(0)[1:-1] if lit else "{x}")
                for name in names[len(args):]:
                    tpl = tpl.replace("${" + name + "}", "")
                expr = "`" + nested(tpl) + "`"
            else:
                expr = nested(expr)
            parts = [x[1:-1] for x in _LIT.findall(expr)] or [consts.get(expr.strip(), "")]
            path = "".join(parts)
            path = re.sub(r"\$\{(\w+)\}", lambda x: consts.get(x.group(1), "{x}"), path)
            path = _strip_query(path)
            return re.sub(r"\$\{[^}]*\}", "{x}", path)

        for om in re.finditer(r"export const (\w+Api)\s*=\s*\{", t):
            start = om.end() - 1
            obj = t[start + 1:cf.matching(t, start)]
            for em in re.finditer(r"^\s{2}(\w+):\s*(?:async\s*)?\([^)]*\)[^=]*=>\s*\n?\s*(?:\w+\.)?api\.(get|post|put|"
                                  r"delete|patch|getFile|postFile|upload|download)\w*(?:<[^>]*(?:<[^>]*>)?[^>]*>)?\(",
                                  obj, re.M):
                open_at = em.end() - 1
                args = cf.split_top(obj[open_at + 1:cf.matching(obj, open_at)])
                if not args:
                    continue
                verb = {"getFile": "GET", "download": "GET", "postFile": "POST", "upload": "POST"}.get(
                    em.group(2), em.group(2).upper())
                out.setdefault((om.group(1), em.group(1)),
                               af.ClientCall(om.group(1), em.group(1), verb, resolve(args[0])))
    return out


def _words(fn: str) -> str:
    """'recordFollowUp' -> 'Record follow up'."""
    words = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", fn).lower().split()
    return " ".join(words).capitalize() if words else fn


def _segments(path: str) -> list[str]:
    path = re.sub(r"^/?api/v\d+/?", "", path).strip("/")
    return ["{}" if re.fullmatch(r"\{[^}]*\}", x) else x for x in path.split("/")]


def _loose_matches(call: af.ClientCall, ops: list) -> list:
    """The operations a call with a computed path segment (for example approve or reject) may reach."""
    want = _segments(call.path)
    out = []
    for op in ops:
        have = _segments(op.raw_path)
        if op.verb == call.verb and len(have) == len(want) and all(
                w == "{}" or w == h for w, h in zip(want, have)) and any(w == "{}" and h != "{}"
                                                                        for w, h in zip(want, have)):
            out.append(op)
    return out


_OPS: list = []


def _all_ops() -> list:
    if not _OPS:
        _OPS.extend(af.operations(packages=[""]))
    return _OPS


def _page_calls(s: dict) -> list[af.ClientCall]:
    feat, page = s["page"]
    calls = _feature_calls(feat)
    out: list[af.ClientCall] = []
    for f in af.screen_files(feat, page):
        for m in re.finditer(r"\b(\w+Api)\.(\w+)\b", f.read_text(encoding="utf-8")):
            c = calls.get((m.group(1), m.group(2)))
            if c and c not in out:
                out.append(c)
    return out


class _Facts(pack_dsp.Facts):
    """The facts of the Developer Specification, with the grants read from the migrations and the seed."""

    def _grants(self) -> dict[str, set[str]]:
        grants = cf.role_grants(include_seed=True)
        return {r: set(grants.get(r, set())) for _, r in self.K.PERSONA_ROLES}


def _menu_screen(route: str | None, menu: dict[str, cf.Screen]) -> cf.Screen | None:
    """The sidebar entry of a route, or of the closest parent route (a record page opens from its list)."""
    if not route:
        return None
    parts = route.strip("/").split("/")
    while parts:
        sc = menu.get("/".join(parts))
        if sc is not None:
            return sc
        parts.pop()
    return None


def _opens(s: dict, sc: cf.Screen | None, perms: set[str]) -> bool:
    if s.get("permission", "").startswith("None"):
        return True
    return sc is None or sc.may_open(perms)


def matrix(K) -> dict:
    """The rows of the workbook (also used by the tests of the pack)."""
    facts = _Facts(K)
    menu = {sc.path.lstrip("/"): sc for _, _, sc in cf.all_screens()}
    paths = {sc.path.lstrip("/"): cf.menu_path(g, sec, sc) for g, sec, sc in cf.all_screens()}
    personas = list(K.PERSONA_ROLES)
    labels = getattr(K, "ACTION_LABELS", {})
    rows, menu_rows, op_rows = [], [], {}
    for s in K.SCREENS:
        sc = _menu_screen(s.get("route"), menu)
        opens = {code: _opens(s, sc, facts.grants[code]) for _, code in personas}
        screen_perm = s.get("permission") or (", ".join(sc.opened_by) if sc and sc.opened_by else "Signed-in user")
        rows.append({"id": s["id"], "screen": s["name"], "action": "Open the screen", "kind": "Open",
                     "operation": "-", "perms": screen_perm,
                     **{code: YES if opens[code] else "" for _, code in personas}})
        for name, code in personas:
            entry = menu.get((s.get("route") or "").strip("/"))
            menu_rows.append({"persona": name, "profile": code, "id": s["id"], "screen": s["name"],
                              "path": paths.get((s.get("route") or "").strip("/"), "Opened from its list"),
                              "shown": YES if entry is not None and not entry.hidden and entry.may_open(
                                  facts.grants[code]) else "",
                              "opens": YES if opens[code] else ""})
        seen = set()
        calls = [x for x in facts.calls[s["id"]] if x[1] is not None]
        if s.get("page"):
            # every call of the page and its components, matched against every service operation of the platform
            for c in _page_calls(s):
                op = af.match_operation(c, facts.ops) or af.match_operation(c, _all_ops())
                calls += [(c.fn, op)] if op else [(c.fn, o) for o in _loose_matches(c, facts.ops)]
        for fn, op in calls:
            if op is None or (op.verb, op.path) in seen:
                continue
            seen.add((op.verb, op.path))
            need = list(op.permissions)
            allowed = {code: opens[code] and (not need or bool(set(need) & facts.grants[code]))
                       for _, code in personas}
            rows.append({"id": s["id"], "screen": s["name"], "action": labels.get(fn) or _words(fn),
                         "kind": VERBS.get(op.verb, op.verb), "operation": f"{op.verb} {op.path.lstrip('/')}",
                         "perms": ", ".join(need) or "Signed-in user",
                         **{code: YES if allowed[code] else "" for _, code in personas}})
            key = (op.verb, op.path)
            if key not in op_rows:
                op_rows[key] = {"operation": f"{op.verb} {op.path.lstrip('/')}",
                                "perms": ", ".join(need) or "Signed-in user",
                                "personas": ", ".join(n for n, c in personas
                                                      if not need or set(need) & facts.grants[c]) or "-",
                                "screens": s["id"]}
            elif s["id"] not in op_rows[key]["screens"]:
                op_rows[key]["screens"] += f", {s['id']}"
    prefixes = tuple(getattr(K, "PERMISSION_PREFIXES", ()) or ())
    used = {p.strip() for r in rows for p in r["perms"].split(",")}
    profiles = []
    for name, code in personas:
        mine = sorted(p for p in facts.grants[code] if p in used or (prefixes and p.startswith(prefixes)))
        profiles.append({"persona": name, "profile": code, "count": len(mine), "perms": ", ".join(mine) or "-",
                         "screens": sum(1 for r in rows if r["kind"] == "Open" and r[code] == YES),
                         "actions": sum(1 for r in rows if r["kind"] != "Open" and r[code] == YES)})
    return {"personas": personas, "rows": rows, "menu": menu_rows, "ops": list(op_rows.values()),
            "profiles": profiles}


def write(K, out: Path) -> tuple[Path, dict]:
    m = matrix(K)
    wb = workbook("Access Matrix", "Access matrix", K.BRD, K.version("ACX"), K.DATE,
                  f"{K.NAME}: group profile x screen x action, from the permissions of the release")
    persona_cols = [Column(code, name, 12, f"Group profile {code}: Y when the persona may do the action",
                           values=[YES, ""]) for name, code in m["personas"]]
    wb.sheet("Matrix", [Column("id", "Screen ID", 12, "Screen of the UX Design"),
                        Column("screen", "Screen", 26, "Screen name"),
                        Column("action", "Action", 30, "Opening the screen or an action of the screen"),
                        Column("kind", "Kind", 12, "Open, view, create or act, change, remove"),
                        Column("operation", "Service operation", 40, "Service operation the action calls"),
                        Column("perms", "Permission", 34, "Permission asked for (any one of them)"),
                        *persona_cols], m["rows"],
             description="One row per screen and action; one column per group profile: Y when the group profile "
                         "opens the screen and holds a permission the action asks for, as the release grants it.")
    wb.sheet("Group profiles", [Column("persona", "Persona", 28, "Persona of the FRS"),
                                Column("profile", "Group profile", 16, "Group profile assigned in UIDM-ISC"),
                                Column("count", "Permissions", 10, "Permissions of the pack held", kind="number"),
                                Column("screens", "Screens opened", 10, "Screens of the pack it opens",
                                       kind="number"),
                                Column("actions", "Actions allowed", 10, "Actions of the pack it may do",
                                       kind="number"),
                                Column("perms", "Permissions held", 80, "Permissions of the pack held")],
             m["profiles"], description="Each group profile with the permissions of the pack it holds after every "
                                        "migration and seed script")
    wb.sheet("Menu by persona", [Column("persona", "Persona", 28, "Persona of the FRS"),
                                 Column("profile", "Group profile", 16, "Group profile assigned in UIDM-ISC"),
                                 Column("id", "Screen ID", 12, "Screen of the UX Design"),
                                 Column("screen", "Screen", 26, "Screen name"),
                                 Column("path", "Menu path", 44, "Where the screen is in the sidebar"),
                                 Column("shown", "In the menu", 10, "Y when the sidebar of the persona shows it",
                                        values=[YES, ""]),
                                 Column("opens", "Opens", 10, "Y when the persona may open it", values=[YES, ""])],
             m["menu"], description="The sidebar of each persona for the screens of the pack")
    wb.sheet("Service operations", [Column("operation", "Service operation", 44, "Verb and path"),
                                    Column("perms", "Permission", 34, "Checked by the server: any one of them"),
                                    Column("personas", "Personas allowed", 50, "Personas whose group profile holds "
                                                                               "one of them"),
                                    Column("screens", "Screens", 20, "Screens that call it")],
             m["ops"], description="The service operations the screens of the pack call, with the permission each "
                                   "one checks on the server")
    target = save_workbook(wb, out / K.file_name("ACX"))
    return target, {"rows": len(m["rows"]), "profiles": len(m["personas"]), "operations": len(m["ops"])}
