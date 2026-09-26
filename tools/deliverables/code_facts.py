"""Facts read from the BIBS code for the business sign-off packs: menus, role grants and messages.

The sign-off pack of a BRD (docs/deliverables/src/signoff/) describes the system as built. This module
reads the facts that must never be typed by hand, so a pack can be regenerated after every change:

* ``frontend_menu()``   the sidebar: groups, sections and screens in display order, with the permission
                        (and the other permissions) that open each screen, read from
                        ``frontend/src/navigation/modules.ts`` and the ``module.ts`` files of the features;
* ``role_grants()``     the permissions of every role, replayed from the Flyway migrations and seeds
                        (``backend/src/main/resources/db``) in version order;
* ``menu_for(perms)``   what a user holding those permissions sees in the sidebar (same rule as
                        ``navigation/access.ts`` ``mayOpen`` and ``components/layout/navGroups.ts``);
* ``backend_messages(packages)``  every business-rule message of the given backend packages, with its code,
                        the text shown to the user (values filled in at run time appear as <name>), the class
                        and the method that raises it;
* ``bean_messages(packages)``     the explicit messages of the request validation annotations.

No build is needed: the sources are parsed as text. The parsers cover the patterns the code base uses;
``python tools/deliverables/code_facts.py --self-check`` reports anything they could not read.
"""

from __future__ import annotations

import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterable

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand  # noqa: E402

REPO = brand.REPO_ROOT
FEATURES = REPO / "frontend" / "src" / "features"
MODULES_TS = REPO / "frontend" / "src" / "navigation" / "modules.ts"
JAVA_ROOT = REPO / "backend" / "src" / "main" / "java" / "com" / "iortatechnxt" / "brokerverse"
DB_ROOT = REPO / "backend" / "src" / "main" / "resources" / "db"


# --------------------------------------------------------------------------------------------
# Small TypeScript / Java literal reader
# --------------------------------------------------------------------------------------------


def _skip_string(text: str, i: int) -> int:
    """Index after the string literal that starts at text[i] (', " or `)."""
    q = text[i]
    if q == '"' and text.startswith('"""', i):
        return text.index('"""', i + 3) + 3
    j = i + 1
    while j < len(text) and text[j] != q:
        if text[j] == "\\":
            j += 1
        j += 1
    return j + 1


def _skip_comment(text: str, i: int) -> int:
    if text.startswith("//", i):
        k = text.find("\n", i)
        return len(text) if k < 0 else k
    if text.startswith("/*", i):
        return text.index("*/", i + 2) + 2
    return i


def matching(text: str, i: int) -> int:
    """Index of the bracket that closes text[i] ((, [ or {), skipping strings and comments."""
    pairs = {"(": ")", "[": "]", "{": "}"}
    stack = [pairs[text[i]]]
    j = i + 1
    while j < len(text):
        c = text[j]
        if c in "'\"`":
            j = _skip_string(text, j)
            continue
        if text.startswith("//", j) or text.startswith("/*", j):
            j = _skip_comment(text, j)
            continue
        if c in pairs:
            stack.append(pairs[c])
        elif stack and c == stack[-1]:
            stack.pop()
            if not stack:
                return j
        j += 1
    raise ValueError("unbalanced bracket")


def split_top(text: str, sep: str = ",") -> list[str]:
    """Splits text on sep at bracket depth 0 (strings and comments skipped)."""
    parts, depth, start, j = [], 0, 0, 0
    while j < len(text):
        c = text[j]
        if c in "'\"`":
            j = _skip_string(text, j)
            continue
        if text.startswith("//", j) or text.startswith("/*", j):
            j = _skip_comment(text, j)
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == sep and depth == 0:
            parts.append(text[start:j].strip())
            start = j + 1
        j += 1
    tail = text[start:].strip()
    if tail:
        parts.append(tail)
    return parts


def _strip_comments(text: str) -> str:
    out, j = [], 0
    while j < len(text):
        c = text[j]
        if c in "'\"`":
            k = _skip_string(text, j)
            out.append(text[j:k])
            j = k
            continue
        if text.startswith("//", j) or text.startswith("/*", j):
            j = _skip_comment(text, j)
            continue
        out.append(c)
        j += 1
    return "".join(out)


def _ts_string(expr: str) -> str | None:
    expr = expr.strip()
    if len(expr) >= 2 and expr[0] in "'\"`" and expr[-1] == expr[0]:
        return expr[1:-1].replace("\\'", "'").replace('\\"', '"')
    return None


# --------------------------------------------------------------------------------------------
# Frontend menu
# --------------------------------------------------------------------------------------------


@dataclass
class Screen:
    path: str
    label: str
    permission: str | None
    also: list[str] = field(default_factory=list)
    hidden: bool = False
    source: str = ""

    def may_open(self, perms: set[str]) -> bool:
        """navigation/access.ts mayOpen: no permission, or the user holds one of them."""
        if self.permission is None:
            return True
        return self.permission in perms or any(p in perms for p in self.also)

    @property
    def opened_by(self) -> list[str]:
        return ([self.permission] if self.permission else []) + self.also


@dataclass
class Section:
    id: str
    title: str
    screens: list[Screen]


@dataclass
class Group:
    id: str
    title: str | None
    sections: list[Section]


class _TsIndex:
    """Exported constants of the feature files: arrays of screens, screens, modules, string constants."""

    def __init__(self) -> None:
        self.files: dict[Path, str] = {}
        for p in sorted(FEATURES.rglob("*.ts")):
            if p.name.endswith((".test.ts", ".d.ts")):
                continue
            self.files[p] = _strip_comments(p.read_text(encoding="utf-8"))

    def definition(self, name: str, near: Path | None = None) -> tuple[str, Path] | None:
        """The initializer text of `const name` (the file `near` first, then every feature file)."""
        order = ([near] if near in self.files else []) + [p for p in self.files if p != near]
        for p in order:
            m = re.search(rf"\bconst {re.escape(name)}\b[^=]*=\s*", self.files[p])
            if m:
                text = self.files[p]
                i = m.end()
                if text[i] in "[{(":
                    return text[i:matching(text, i) + 1], p
                end = text.find(";", i)
                return text[i:end].strip(), p
        return None

    def value(self, expr: str, near: Path) -> Any:
        """A string, a list of strings or None for a TS expression (identifiers resolved)."""
        expr = expr.strip()
        s = _ts_string(expr)
        if s is not None:
            return s
        if expr.startswith("["):
            items = []
            for part in split_top(expr[1:-1]):
                if part.startswith("..."):
                    v = self.value(part[3:], near)
                    items.extend(v if isinstance(v, list) else [])
                elif part:
                    items.append(self.value(part, near))
            return items
        if re.fullmatch(r"[A-Za-z_]\w*", expr):
            d = self.definition(expr, near)
            if d:
                return self.value(d[0], d[1])
        return None

    def screen(self, obj: str, near: Path) -> Screen | None:
        props: dict[str, str] = {}
        for part in split_top(obj.strip()[1:-1]):
            m = re.match(r"^(\w+)\s*:\s*(.*)$", part, re.S)
            if m:
                props[m.group(1)] = m.group(2).strip()
        if "path" not in props:
            return None
        also = self.value(props.get("alsoPermissions", "[]"), near) or []
        perm = self.value(props["permission"], near) if "permission" in props else None
        return Screen(path=self.value(props["path"], near) or props["path"],
                      label=self.value(props.get("label", "''"), near) or "",
                      permission=perm, also=[a for a in also if isinstance(a, str)],
                      hidden=props.get("hidden", "false") == "true",
                      source=str(near.relative_to(REPO)))

    def screens(self, array: str, near: Path) -> list[Screen]:
        """Screens of an array literal, spreads and named screen constants expanded in order."""
        out: list[Screen] = []
        for part in split_top(array.strip()[1:-1]):
            if not part:
                continue
            if part.startswith("..."):
                d = self.definition(part[3:].strip(), near)
                if d:
                    out.extend(self.screens(d[0], d[1]))
            elif part.startswith("{"):
                s = self.screen(part, near)
                if s:
                    out.append(s)
            elif re.fullmatch(r"[A-Za-z_]\w*", part):
                d = self.definition(part, near)
                if d and d[0].startswith("{"):
                    s = self.screen(d[0], d[1])
                    if s:
                        out.append(s)
                elif d and d[0].startswith("["):
                    out.extend(self.screens(d[0], d[1]))
        return out

    def module(self, name: str) -> Section:
        d = self.definition(name)
        if d is None:
            raise KeyError(f"module {name} not found under frontend/src/features")
        text, p = d
        mid = re.search(r"\bid:\s*'([^']+)'", text).group(1)
        title = re.search(r"\bsection:\s*'([^']+)'", text).group(1)
        m = re.search(r"\bscreens:\s*\[", text)
        arr = text[m.end() - 1:matching(text, m.end() - 1) + 1]
        return Section(mid, title, self.screens(arr, p))

    def wrapped(self, fn: str, section: Section) -> Section:
        """The screen-list wrappers of the feature modules (withOverviewScreens, withPackageRequests...)."""
        for p, text in self.files.items():
            m = re.search(rf"export function {fn}\b[^{{]*\{{", text)
            if not m:
                continue
            body = text[m.end() - 1:matching(text, m.end() - 1) + 1]
            s = re.search(r"screens:\s*\[(.*?)\]\s*\}", body, re.S)
            if s and "slice" not in s.group(1):
                screens: list[Screen] = []
                for part in split_top(s.group(1)):
                    name = part.lstrip(".").split(".")[0].strip()
                    if name in (section.id, "catalog", "overview", "gl") or part.endswith(".screens"):
                        screens.extend(section.screens)
                    else:
                        d = self.definition(name, p)
                        if d and d[0].startswith("{"):
                            screens.append(self.screen(d[0], d[1]))
                        elif d:
                            screens.extend(self.screens(d[0], d[1]))
                return Section(section.id, section.title, screens)
            # insertion after an anchor path (withJournalAutomation)
            anchor = re.search(r"s\.path === '([^']+)'", body)
            extra = re.search(r"\.\.\.(\w+),\s*\.\.\.\w+\.screens\.slice\(at\)", body)
            if anchor and extra:
                d = self.definition(extra.group(1), p)
                added = self.screens(d[0], d[1]) if d else []
                paths = [x.path for x in section.screens]
                at = paths.index(anchor.group(1)) + 1 if anchor.group(1) in paths else len(paths)
                return Section(section.id, section.title, section.screens[:at] + added + section.screens[at:])
        raise KeyError(f"wrapper {fn} not found")


def frontend_menu() -> list[Group]:
    """The sidebar groups in display order (navigation/modules.ts NAV_GROUPS)."""
    idx = _TsIndex()
    text = _strip_comments(MODULES_TS.read_text(encoding="utf-8"))
    m = re.search(r"NAV_GROUPS[^=]*=\s*\[", text)
    arr = text[m.end() - 1:matching(text, m.end() - 1) + 1]
    groups: list[Group] = []
    for g in split_top(arr[1:-1]):
        gid = re.search(r"\bid:\s*'([^']+)'", g).group(1)
        t = re.search(r"\btitle:\s*'([^']+)'", g)
        mm = re.search(r"\bmodules:\s*\[", g)
        mods = g[mm.end() - 1:matching(g, mm.end() - 1) + 1]
        sections = []
        for expr in split_top(mods[1:-1]):
            expr = expr.strip()
            call = re.fullmatch(r"(\w+)\((\w+)\)", expr)
            if call:
                sections.append(idx.wrapped(call.group(1), idx.module(call.group(2))))
            elif expr:
                sections.append(idx.module(expr))
        groups.append(Group(gid, t.group(1) if t else None, sections))
    return groups


def all_screens(groups: list[Group] | None = None) -> list[tuple[Group, Section, Screen]]:
    groups = groups or frontend_menu()
    return [(g, s, sc) for g in groups for s in g.sections for sc in s.screens]


def menu_for(perms: Iterable[str], groups: list[Group] | None = None) -> list[Group]:
    """The sidebar of a user holding perms: hidden screens and screens without permission dropped,
    then empty sections and groups (components/layout/navGroups.ts visibleGroups)."""
    held = set(perms)
    out = []
    for g in groups or frontend_menu():
        sections = [Section(s.id, s.title, [x for x in s.screens if not x.hidden and x.may_open(held)])
                    for s in g.sections]
        sections = [s for s in sections if s.screens]
        if sections:
            out.append(Group(g.id, g.title, sections))
    return out


def menu_path(group: Group, section: Section, screen: Screen, show_section: bool = True) -> str:
    """'Client & Policy › Clients › New Client' (a group without a title shows the screen only)."""
    parts = [group.title] if group.title else []
    if group.title and show_section:
        parts.append(section.title)
    parts.append(screen.label)
    return " › ".join(parts)


# --------------------------------------------------------------------------------------------
# Role grants (Flyway migrations and seeds)
# --------------------------------------------------------------------------------------------


def _version(p: Path) -> tuple:
    m = re.match(r"V(\d+)(?:_(\d+))?__", p.name)
    return (int(m.group(1)), int(m.group(2) or 0)) if m else (10**9, 0)


def _codes(text: str) -> list[str]:
    return re.findall(r"'([A-Z][A-Z0-9_]*)'", text)


def role_grants(include_seed: bool = True) -> dict[str, set[str]]:
    """Permissions of every role after all migrations (and the seed scripts) have run."""
    files = sorted(list((DB_ROOT / "migration").glob("V*.sql")) +
                   (list((DB_ROOT / "seed").glob("V*.sql")) if include_seed else []), key=_version)
    roles: dict[str, set[str]] = {}
    unread: list[str] = []
    for f in files:
        text = re.sub(r"--[^\n]*", "", f.read_text(encoding="utf-8"))
        for stmt in text.split(";"):
            s = " ".join(stmt.split())
            low = s.lower()
            for m in re.finditer(r"insert into sec_role \(code[^)]*\) values (.*)", s, re.I):
                for code in re.findall(r"\(\s*'([A-Z][A-Z0-9_]*)'", m.group(1)):
                    roles.setdefault(code, set())
            if "sec_role_permission" not in low or low.startswith("create table"):
                continue
            if low.startswith("delete from sec_role_permission"):
                perms = _codes(re.search(r"permission\s*(?:=|in)\s*(\([^)]*\)|'[^']*')", s, re.I).group(1))
                rm = re.search(r"code\s*(?:=|in)\s*(\([^)]*\)|'[^']*')", s, re.I)
                targets = _codes(rm.group(1)) if rm else list(roles)
                for r in targets:
                    roles.get(r, set()).difference_update(perms)
                continue
            if not low.startswith("insert into sec_role_permission"):
                unread.append(f"{f.name}: {s[:120]}")
                continue
            pairs = re.search(r"\(values (.*?)\) as g\(role_code, permission\)", s)
            if pairs:
                for r, p in re.findall(r"\('([A-Z][A-Z0-9_]*)',\s*'([A-Z][A-Z0-9_]*)'\)", pairs.group(1)):
                    roles.setdefault(r, set()).add(p)
                continue
            vals = re.search(r"cross join \(values (.*?)\) as p\(permission\)", s)
            perms = _codes(vals.group(1)) if vals else []
            un = re.search(r"unnest\(array\[(.*?)\]\)", s)
            if un:
                perms = _codes(un.group(1))
            lit = re.match(r"insert into sec_role_permission \(role_id, permission\) select (?:distinct )?"
                           r"(?:r\.id|id|holder\.role_id), '([A-Z0-9_]+)'", s, re.I)
            if lit:
                perms = [lit.group(1)]
            holder = re.search(r"where holder\.permission = '([A-Z0-9_]+)'", s)
            copy = re.search(r"join sec_role_permission p on p\.role_id = \(select id from sec_role where "
                             r"code = '([A-Z0-9_]+)'\)", s)
            where = re.search(r"where (?:r\.)?code\s*(=|in)\s*(\([^)]*\)|'[^']*')", s)
            if holder:
                targets = [r for r, ps in roles.items() if holder.group(1) in ps]
            elif where:
                targets = _codes(where.group(2))
            elif re.search(r"from sec_role\s*$", s, re.I):
                targets = list(roles)
            else:
                unread.append(f"{f.name}: {s[:120]}")
                continue
            if copy:
                perms = sorted(roles.get(copy.group(1), set()))
            if not perms:
                unread.append(f"{f.name}: {s[:120]}")
                continue
            for r in targets:
                roles.setdefault(r, set()).update(perms)
    role_grants.unread = unread  # type: ignore[attr-defined]
    return roles


# --------------------------------------------------------------------------------------------
# Workflow definitions (wf_stage, wf_transition)
# --------------------------------------------------------------------------------------------


def _sql_tuples(values: str) -> list[list[str | None]]:
    """The rows of a VALUES list: strings unquoted, null as None, other literals as text."""
    rows = []
    for tup in re.finditer(r"\((.*?)\)\s*(?:,|$)", values, re.S):
        cells = []
        for cell in split_top(tup.group(1)):
            cell = cell.strip()
            if cell.lower() == "null":
                cells.append(None)
            elif cell.startswith("'"):
                cells.append(cell[1:-1].replace("''", "'"))
            else:
                cells.append(cell)
        rows.append(cells)
    return rows


def workflows(codes: Iterable[str] | None = None) -> dict[str, dict[str, list[dict[str, Any]]]]:
    """Stages and transitions per workflow code, from the migrations in version order."""
    wanted = set(codes) if codes else None
    out: dict[str, dict[str, list[dict[str, Any]]]] = {}
    files = sorted(list((DB_ROOT / "migration").glob("V*.sql")) + list((DB_ROOT / "seed").glob("V*.sql")),
                   key=_version)
    for f in files:
        text = re.sub(r"--[^\n]*", "", f.read_text(encoding="utf-8"))
        for stmt in text.split(";"):
            m = re.search(r"insert into (wf_stage|wf_transition)\s*\(([^)]*)\)\s*values(.*)", stmt, re.S | re.I)
            if not m:
                continue
            cols = [c.strip() for c in m.group(2).split(",")]
            for row in _sql_tuples(m.group(3).strip()):
                rec = dict(zip(cols, row))
                code = rec.get("workflow_code")
                if not code or (wanted and code not in wanted):
                    continue
                wf = out.setdefault(code, {"stages": [], "transitions": []})
                if m.group(1).lower() == "wf_stage":
                    wf["stages"].append(rec)
                else:
                    wf["transitions"].append(rec)
    for wf in out.values():
        wf["stages"].sort(key=lambda r: int(r.get("sort_order") or 0))
    return out


# --------------------------------------------------------------------------------------------
# Backend messages
# --------------------------------------------------------------------------------------------


@dataclass
class Message:
    code: str
    text: str
    kind: str  # Business rule | Field check | Not found | Duplicate | Request check
    file: str
    cls: str
    method: str
    line: int
    package: str

    @property
    def where(self) -> str:
        return f"{self.cls}.{self.method}"


def _java_constants(text: str) -> dict[str, str]:
    consts = {}
    for m in re.finditer(r"(?:static\s+)?final\s+String\s+(\w+)\s*=\s*(\"(?:[^\"\\]|\\.)*\")\s*;", text):
        consts[m.group(1)] = m.group(2)[1:-1]
    for m in re.finditer(r"(?:static\s+)?final\s+(?:int|long)\s+(\w+)\s*=\s*([\d_]+)L?\s*;", text):
        consts[m.group(1)] = m.group(2).replace("_", "")
    return consts


_GLOBAL_CONSTS: dict[str, str] = {}


def _global_constants() -> dict[str, str]:
    if not _GLOBAL_CONSTS:
        for p in JAVA_ROOT.rglob("*.java"):
            text = p.read_text(encoding="utf-8")
            for k, v in _java_constants(text).items():
                _GLOBAL_CONSTS.setdefault(f"{p.stem}.{k}", v)
    return _GLOBAL_CONSTS


def _placeholder(expr: str, consts: dict[str, str] | None = None) -> str:
    """A readable name for a value filled in at run time: client.getCode() -> <code>; a constant
    (MAX_FILES = 20) gives its value."""
    expr = expr.strip()
    if consts and expr in consts:
        return consts[expr]
    lit = re.fullmatch(r'"((?:[^"\\]|\\.)*)"', expr)
    if lit:
        return lit.group(1)
    if ".stream()" in expr or "Collectors.joining" in expr:
        return "<details>"
    join = re.match(r"(?:String\.join|Collectors\.joining)\(\s*\"[^\"]*\"\s*,\s*(\w+)", expr)
    if join:
        expr = join.group(1)
    m = re.search(r"(\w+)\(\)\s*$", expr)
    if m:
        name = m.group(1)
        name = re.sub(r"^(get|is)", "", name) or name
    else:
        name = re.split(r"[.\s(]", expr.split("?")[0].strip())[-1] or expr
        if name in ("join", "collect", "toString"):
            inner = re.search(r"\(([^,()]*)(?:,|\))", expr)
            name = inner.group(1).split(".")[-1] if inner else "values"
    name = name.strip("()")
    if re.fullmatch(r"[A-Z0-9_]+", name):
        name = name.replace("_", " ").lower()
    else:
        name = re.sub(r"(?<!^)([A-Z])", r" \1", name).lower().strip()
    return f"<{name}>"


def java_texts(expr: str, consts: dict[str, str], cls: str) -> list[str]:
    """The texts of a Java message expression: literals joined, values as <name>. A top-level
    conditional (a ? "x" : "y") gives one text per alternative."""
    expr = " ".join(expr.strip().split())
    tern = split_top(expr, "?")
    if len(tern) == 2 and ":" in tern[1]:
        branches = split_top(tern[1], ":")
        if len(branches) == 2:
            return java_texts(branches[0], consts, cls) + java_texts(branches[1], consts, cls)
    return [java_text(expr, consts, cls)]


def java_text(expr: str, consts: dict[str, str], cls: str) -> str:
    """The text of a Java message expression: literals joined, values as <name>."""
    expr = " ".join(expr.split())
    if expr.startswith("(") and expr.endswith(")") and matching(expr, 0) == len(expr) - 1:
        expr = expr[1:-1].strip()
    fm = re.fullmatch(r'"((?:[^"\\]|\\.)*)"\s*\.formatted\((.*)\)', expr)
    if fm:
        args = split_top(fm.group(2))
        text = fm.group(1)
        for a in args:
            text = re.sub(r"%[sd]", _placeholder(a, consts).replace("\\", "\\\\"), text, count=1)
        return text.replace('\\"', '"')
    fm = re.fullmatch(r'String\.format\(\s*"((?:[^"\\]|\\.)*)"\s*,(.*)\)', expr)
    if fm:
        text = fm.group(1)
        for a in split_top(fm.group(2)):
            text = re.sub(r"%[sd,.0-9]*[sdf]", _placeholder(a, consts).replace("\\", "\\\\"), text, count=1)
        return text.replace('\\"', '"')
    parts = split_top(expr, "+")
    out = []
    for part in parts:
        part = part.strip()
        if part.startswith('"') and part.endswith('"'):
            out.append(part[1:-1].replace('\\"', '"').replace("\\n", " "))
        elif part in consts:
            out.append(consts[part])
        elif re.fullmatch(r"[A-Z]\w*\.[A-Z_][A-Z0-9_]*", part) and part in _global_constants():
            out.append(_global_constants()[part])
        elif " ? " in part and part.startswith("("):
            branches = re.findall(r'"((?:[^"\\]|\\.)*)"', part)
            out.append(" / ".join(branches) if branches else _placeholder(part, consts))
        else:
            out.append(_placeholder(part, consts))
    return "".join(out)


def _method_at(text: str, pos: int) -> str:
    """Name of the method (or constructor) that contains position pos."""
    best = "?"
    for m in re.finditer(r"\n\s+(?:public |protected |private |static |final |synchronized |<[^>]+> )*"
                         r"[\w<>\[\], ?]+\s+(\w+)\s*\([^;{]*\)\s*(?:throws [\w., ]+)?\{", text[:pos]):
        if m.group(1) not in ("if", "for", "while", "switch", "catch", "return", "new"):
            best = m.group(1)
    return best


def backend_messages(packages: Iterable[str]) -> list[Message]:
    """BusinessRuleException, FieldValidationException, ResourceNotFoundException and
    DuplicateResourceException messages raised in the given packages (e.g. 'crm', 'quotation')."""
    out: list[Message] = []
    for pkg in packages:
        for p in sorted((JAVA_ROOT / pkg).rglob("*.java")):
            raw = p.read_text(encoding="utf-8")
            text = _strip_comments(raw)
            consts = _java_constants(text)
            for m in re.finditer(r"new (BusinessRuleException|FieldValidationException|ResourceNotFoundException|"
                                 r"DuplicateResourceException|Violation|Warning|Unmet)\(", text):
                start = m.end() - 1
                args = split_top(text[start + 1:matching(text, start)])
                kind = m.group(1)
                pairs: list[tuple[str, str]] = []
                if kind in ("Violation", "Warning", "Unmet"):
                    if len(args) < 2 or not (args[0].startswith('"') or re.fullmatch(r"[A-Z_]+", args[0])):
                        continue
                if kind in ("BusinessRuleException", "FieldValidationException", "Violation", "Warning", "Unmet") \
                        and len(args) >= 2:
                    codes = []
                    for code_expr in java_texts(args[0], consts, p.stem) if "?" in args[0] else [args[0]]:
                        code_expr = code_expr.strip()
                        codes.append(code_expr[1:-1] if code_expr.startswith('"') else
                                     consts.get(code_expr) or _global_constants().get(code_expr, code_expr))
                    texts = java_texts(args[1], consts, p.stem)
                    if len(codes) == len(texts):
                        pairs = list(zip(codes, texts))
                    else:
                        pairs = [(c, t) for c in codes for t in texts]
                    label = {"FieldValidationException": "Field check", "Warning": "Warning"}.get(kind, "Business rule")
                elif kind == "ResourceNotFoundException" and args:
                    what = java_text(args[0], consts, p.stem)
                    key = _placeholder(args[1], consts)[1:-1] if len(args) > 1 else "key"
                    pairs = [("NOT_FOUND", f"{what} not found: <{key}>")]
                    label = "Not found"
                elif kind == "DuplicateResourceException" and args:
                    pairs = [("DUPLICATE", f"{java_text(args[0], consts, p.stem)} already exists: <key>")]
                    label = "Duplicate"
                else:
                    continue
                probe = text[m.start():m.start() + 60]
                line = raw[:raw.find(probe)].count("\n") + 1 if probe in raw else 0
                for code, msg in pairs:
                    out.append(Message(code=code, text=msg, kind=label, file=str(p.relative_to(REPO)), cls=p.stem,
                                       method=_method_at(text, m.start()), line=line, package=pkg))
    return out


def platform_messages() -> list[Message]:
    """Messages of the platform error handler (common/api/GlobalExceptionHandler): the same on every screen.

    Handlers that pass the exception's own text (business rules, not found, duplicates) are covered by the
    messages of the services; only the fixed texts are returned here."""
    path = JAVA_ROOT / "common" / "api" / "GlobalExceptionHandler.java"
    text = path.read_text(encoding="utf-8")
    out: list[Message] = []
    for m in re.finditer(r'problem\(\s*HttpStatus\.\w+,\s*"([A-Z_]+)",\s*(.*?)\);', text, re.S):
        code, expr = m.group(1), m.group(2).strip()
        line = text.count("\n", 0, m.start()) + 1
        texts: list[str] = []
        if expr.startswith("detail"):
            texts = [_placeholder_join(t) for t in re.findall(r'->\s*(.*?);', text[text.rfind("switch", 0, m.start()):m.start()], re.S)]
        elif "getMessage()" in expr:
            continue
        else:
            texts = [_placeholder_join(expr)]
        for t in texts:
            out.append(Message(code, t, "Request check", str(path.relative_to(REPO)), "GlobalExceptionHandler",
                               "handler", line, "common.api"))
    return out


def _placeholder_join(expr: str) -> str:
    """Joins a Java string concatenation: literals kept, other terms shown as <name>."""
    parts = []
    for term in split_top(expr.strip(), "+"):
        term = term.strip()
        lit = re.fullmatch(r'"((?:[^"\\]|\\.)*)"', term, re.S)
        if lit:
            parts.append(lit.group(1).replace('\\"', '"'))
        else:
            name = re.sub(r"^.*\.(get)?", "", term).rstrip("()")
            name = re.sub(r"(?<!^)(?=[A-Z])", " ", name).lower() or "value"
            parts.append(f"<{name}>")
    return "".join(parts)


def bean_messages(packages: Iterable[str]) -> list[Message]:
    """Explicit messages of request validation annotations (@NotBlank(message = "..."))."""
    out = []
    for pkg in packages:
        for p in sorted((JAVA_ROOT / pkg).rglob("*.java")):
            text = p.read_text(encoding="utf-8")
            for m in re.finditer(r"@(\w+)\(([^)]*?)message\s*=\s*\"((?:[^\"\\]|\\.)*)\"", text):
                field_m = re.search(r"\s(\w+)\s*[,)]", text[m.end():m.end() + 200])
                out.append(Message(code="VALIDATION_FAILED", text=f"{field_m.group(1) if field_m else ''}: {m.group(3)}",
                                   kind="Request check", file=str(p.relative_to(REPO)), cls=p.stem,
                                   method=m.group(1), line=text[:m.start()].count("\n") + 1, package=pkg))
    return out


# --------------------------------------------------------------------------------------------
# Bulk upload templates (BulkImportHandler implementations)
# --------------------------------------------------------------------------------------------


@dataclass
class BulkTemplate:
    code: str
    title: str
    permission: str
    instructions: str
    columns: list[dict[str, Any]]
    row_messages: list[str]
    file: str


def _package_constants(folder: Path) -> dict[str, str]:
    """String constants of every class of a package, as NAME and Class.NAME (nested classes too)."""
    out: dict[str, str] = {}
    for p in folder.glob("*.java"):
        text = p.read_text(encoding="utf-8")
        for m in re.finditer(r"(?:static\s+)?final\s+String\s+(\w+)\s*=\s*\"((?:[^\"\\]|\\.)*)\"\s*;", text):
            out.setdefault(m.group(1), m.group(2))
            out.setdefault(f"{p.stem}.{m.group(1)}", m.group(2))
            # nested class Headers { static final String X = ...; }
            cls = [c for c in re.finditer(r"class (\w+)\s*\{", text[:m.start()])]
            if cls:
                out.setdefault(f"{cls[-1].group(1)}.{m.group(1)}", m.group(2))
    return out


def _method_body(text: str, name: str) -> str | None:
    m = re.search(rf"\b{name}\s*\([^)]*\)\s*\{{", text)
    if not m:
        return None
    start = m.end() - 1
    return text[start:matching(text, start) + 1]


def _columns_of(body: str, consts: dict[str, str], folder: Path, depth: int = 0) -> list[dict[str, Any]]:
    cols: list[dict[str, Any]] = []
    pattern = re.compile(r"(BulkColumn\.required|BulkColumn\.optional|new BulkColumn|\b(?:\w+\.)?number|\b(?:\w+\.)?date|"
                         r"\b(\w+)\.(\w+)Columns)\(")
    for m in pattern.finditer(body):
        if m.group(2):  # AccountBulkSupport.itemColumns()
            if depth > 2:
                continue
            for p in folder.glob("*.java"):
                if p.stem == m.group(2):
                    inner = _method_body(_strip_comments(p.read_text(encoding="utf-8")), m.group(3) + "Columns")
                    if inner:
                        cols.extend(_columns_of(inner, consts, folder, depth + 1))
            continue
        start = m.end() - 1
        args = split_top(body[start + 1:matching(body, start)])
        kind = m.group(1)

        def val(expr: str) -> str:
            expr = expr.strip()
            if expr.startswith('"'):
                return expr[1:-1]
            return consts.get(expr, consts.get(expr.split(".")[-1], expr))
        if kind.endswith("required") and len(args) >= 3:
            cols.append({"header": val(args[0]), "description": java_text(args[1], consts, ""), "required": True,
                         "type": "TEXT", "example": val(args[2])})
        elif kind.endswith("optional") and len(args) >= 3:
            cols.append({"header": val(args[0]), "description": java_text(args[1], consts, ""), "required": False,
                         "type": "TEXT", "example": val(args[2])})
        elif kind == "new BulkColumn" and len(args) >= 5:
            cols.append({"header": val(args[0]), "description": java_text(args[1], consts, ""),
                         "required": args[2].strip() == "true", "type": args[3].split(".")[-1],
                         "example": val(args[4])})
        elif kind.endswith("number") and len(args) >= 4:
            cols.append({"header": val(args[0]), "description": java_text(args[1], consts, ""),
                         "required": args[2].strip() == "true", "type": "NUMBER", "example": val(args[3])})
        elif kind.endswith("date") and len(args) >= 3:
            cols.append({"header": val(args[0]), "description": java_text(args[1], consts, ""),
                         "required": args[2].strip() == "true", "type": "DATE", "example": "2026-10-01"})
    return cols


def bulk_templates(class_names: Iterable[str]) -> list[BulkTemplate]:
    """Code, title, permission, instructions and template columns of the named upload handlers, with the
    row-level messages their validate() adds."""
    wanted = set(class_names)
    out = []
    for p in sorted(JAVA_ROOT.rglob("*.java")):
        if p.stem not in wanted:
            continue
        text = _strip_comments(p.read_text(encoding="utf-8"))
        consts = {**_global_constants_simple(), **_package_constants(p.parent), **_java_constants(text)}

        def ret(method: str) -> str:
            body = _method_body(text, method) or ""
            m = re.search(r"\breturn\s+", body)
            if not m:
                return ""
            parts = split_top(body[m.end():], ";")
            expr = parts[0].strip() if parts else ""
            return consts.get(expr, java_text(expr, consts, p.stem)) if expr else ""
        cols = _columns_of(_method_body(text, "columns") or "", consts, p.parent)
        rows = []
        for m in re.finditer(r"(?:errors|problems)\.add\(", text):
            arg = text[m.end():matching(text, m.end() - 1)]
            if '"' in arg:
                rows.extend(java_texts(arg, consts, p.stem))
        out.append(BulkTemplate(code=ret("code"), title=ret("title"), permission=ret("permission"),
                                instructions=ret("instructions"), columns=cols, row_messages=rows,
                                file=str(p.relative_to(REPO))))
    return out


def _global_constants_simple() -> dict[str, str]:
    return {k: v for k, v in _global_constants().items()}


# --------------------------------------------------------------------------------------------
# Frontend messages
# --------------------------------------------------------------------------------------------


@dataclass
class UiMessage:
    text: str
    kind: str  # Confirmation | Validation | Warning | Information | Error
    file: str
    context: str
    line: int


def _ts_text(expr: str) -> str | None:
    """A TS string or template literal as text, ${...} as <name>."""
    expr = expr.strip()
    if expr.startswith("`") and expr.endswith("`"):
        body = expr[1:-1]

        def name(m: re.Match) -> str:
            return _expr_name(m.group(1))
        return re.sub(r"\$\{((?:[^{}]|\{[^}]*\})*)\}", name, body)
    return _ts_string(expr)


def _expr_name(expr: str) -> str:
    """<name> for a JSX or template expression: the last member name, in words; <list> for a .map()."""
    if ".map(" in expr:
        return "<list>"
    inner = expr.strip()
    for _ in range(4):
        inner = re.sub(r"\.(toLowerCase|toUpperCase|trim|toString|toFixed|toLocaleString|replace|replaceAll)\([^()]*\)$", "", inner)
        inner = re.sub(r"^\w+\((.*)\)$", r"\1", inner, flags=re.S).strip()
    inner = re.split(r"\?\?|\.join\(|\?|&&|\|\||,", inner)[0].strip()
    last = re.split(r"[.\s(]", inner.rstrip(")"))[-1] or "value"
    return "<" + re.sub(r"(?<!^)([A-Z])", r" \1", last).lower() + ">"


def jsx_text(chunk: str) -> str:
    """The visible text of a JSX fragment: attributes dropped, {expressions} as <name>, tags removed."""
    out = []
    i = 0
    while i < len(chunk):
        ch = chunk[i]
        if ch == "{":
            try:
                j = matching(chunk, i)
            except ValueError:
                j = len(chunk) - 1
            expr = chunk[i + 1:j]
            # attribute value (name={...}) or text expression
            if re.search(r"=\s*$", chunk[max(0, i - 2):i]):
                out.append('""')
            else:
                lit = _ts_string(expr.strip())
                tern = re.fullmatch(r"[^?]+\?\s*('[^']*'|\"[^\"]*\")\s*:\s*('[^']*'|\"[^\"]*\")\s*", expr, re.S)
                if lit is None and tern:
                    lit = _ts_string(tern.group(1))
                out.append(lit if lit is not None else "\u27e8" + _expr_name(expr)[1:-1] + "\u27e9")
            i = j + 1
            continue
        out.append(ch)
        i += 1
    text = re.sub(r"<[^<>]*>", " ", "".join(out))
    text = text.replace("\u27e8", "<").replace("\u27e9", ">").replace("&nbsp;", " ")
    return " ".join(text.split())


def frontend_messages(dirs: Iterable[str]) -> list[UiMessage]:
    """Toasts, field validation messages, alert texts and empty-list texts of the given feature folders
    (relative to frontend/src, e.g. 'features/crm', 'components/broking')."""
    out: list[UiMessage] = []
    root = REPO / "frontend" / "src"
    for d in dirs:
        for p in sorted((root / d).rglob("*.ts*")):
            if re.search(r"\.(test|spec)\.tsx?$", p.name) or p.name == "help.ts":
                continue
            raw = p.read_text(encoding="utf-8")
            text = _strip_comments(raw)
            rel = str(p.relative_to(REPO))

            def add(msg: str | None, kind: str, pos: int, ctx: str) -> None:
                if msg and len(msg.strip()) > 2 and re.search(r"[a-z]", msg):
                    out.append(UiMessage(" ".join(msg.split()), kind, rel, ctx, text[:pos].count("\n") + 1))
            for m in re.finditer(r"toast\.(success|error|info|warning)\(", text):
                arg = split_top(text[m.end():matching(text, m.end() - 1)])
                if arg:
                    kind = "Confirmation" if m.group(1) == "success" else ("Error" if m.group(1) == "error" else "Information")
                    add(_ts_text(arg[0]), kind, m.start(), "toast")
            for m in re.finditer(r"(?:errors|e)\.(\w+)\s*=\s*(`[^`]*`|'[^']*'|\"[^\"]*\")", text):
                add(_ts_text(m.group(2)), "Validation", m.start(), m.group(1))
            for m in re.finditer(r"\b(\w+)\s*:\s*(`[^`]*`|'[^']*')\s*[,}]", text):
                # messages in error maps { amount: 'Enter the amount' }
                val = _ts_text(m.group(2)) or ""
                if re.match(r"^(Enter|Select|Choose|Give|Upload|Attach|Add|Use|The |A |An |Only |Check|Complete|"
                            r"Correct|Record|Pick|Type|At least|Must|Cannot)", val) and m.group(1) not in (
                        "label", "title", "description", "placeholder", "hint", "header", "section", "path", "key",
                        "emptyMessage", "confirmLabel", "noun"):
                    add(val, "Validation", m.start(), m.group(1))
            for m in re.finditer(r"return\s+(`[^`]*`|'[^']*')\s*;", text):
                val = _ts_text(m.group(1)) or ""
                if re.match(r"^(Enter|Select|Choose|Give|Upload|Attach|Add|Use|The |A |An |Only |Check|Complete|"
                            r"Correct|Record|Pick|At least|Must|Cannot)", val):
                    add(val, "Validation", m.start(), "check")
            for m in re.finditer(r'className="alert (warning|danger|info|success)"[^>]*>', text):
                end = text.find("</div>", m.end())
                chunk = jsx_text(text[m.end():end])
                kind = {"warning": "Warning", "danger": "Error", "info": "Information", "success": "Confirmation"}[m.group(1)]
                add(chunk.strip(), kind, m.start(), "alert")
            for m in re.finditer(r"\bdone:\s*\([^)]*\)\s*=>\s*(`[^`]*`|'[^']*')", text):
                add(_ts_text(m.group(1)), "Confirmation", m.start(), "done")
            for m in re.finditer(r'emptyMessage="([^"]+)"', text):
                add(m.group(1), "Information", m.start(), "empty list")
    seen: set[tuple[str, str]] = set()
    unique = []
    for u in out:
        if (u.text, u.file) not in seen:
            seen.add((u.text, u.file))
            unique.append(u)
    return unique


# --------------------------------------------------------------------------------------------
# Self-check
# --------------------------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    import argparse

    ap = argparse.ArgumentParser(description="Read menus, grants and messages from the BIBS code")
    ap.add_argument("--self-check", action="store_true", help="report statements and screens not read")
    ap.add_argument("--menu", metavar="ROLE", help="print the sidebar of a role")
    ap.add_argument("--messages", metavar="PKGS", help="print the messages of comma-separated packages")
    ap.add_argument("--ui", metavar="DIRS", help="print the screen messages of comma-separated frontend folders")
    args = ap.parse_args(argv)
    groups = frontend_menu()
    grants = role_grants()
    if args.self_check:
        n = sum(len(s.screens) for g in groups for s in g.sections)
        print(f"menu: {len(groups)} groups, {sum(len(g.sections) for g in groups)} sections, {n} screens")
        print(f"roles: {len(grants)}")
        for u in getattr(role_grants, "unread", []):
            print("grant statement not read:", u)
        for g, s, sc in all_screens(groups):
            if not sc.label or (sc.permission is None and not sc.hidden and sc.path not in ("/approvals", "/help", "/profile")):
                print("screen without label or permission:", sc)
    if args.menu:
        for g in menu_for(grants.get(args.menu, set()), groups):
            print(g.title or "(top)")
            for s in g.sections:
                print("  ", s.title)
                for sc in s.screens:
                    print("      ", sc.label, sc.path)
    if args.ui:
        for u in frontend_messages(args.ui.split(",")):
            print(f"{u.kind:13} {Path(u.file).name:32} {u.text}")
    if args.messages:
        for msg in backend_messages(args.messages.split(",")):
            print(f"{msg.code:32} {msg.where:50} {msg.text}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
