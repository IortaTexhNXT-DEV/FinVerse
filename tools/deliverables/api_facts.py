"""Facts read from the BIBS services and screens for the developer specification of a BRD (DSP).

    python tools/deliverables/api_facts.py nbadmin security identity      # prints the operations found

* ``operations(packages)``   every service operation of the controllers in the given backend packages: method, path,
                             permission expression, permissions, request body type, path and query parameters, the
                             returned type and the first sentence of its description;
* ``record_fields(name)``    the fields of a request record with their checks (@NotNull, @NotBlank, @Size, @Pattern,
                             @Min, @Max, @Email, @Past, @Future...) and the explicit messages of the checks;
* ``client_calls()``         every call of the screen clients (frontend/src/api/*.ts): object, function, method, path;
* ``screen_calls(feature, page)``  the client calls made by a screen and the local components it imports;
* ``client_fields(feature, files)`` the input fields of the screens: label, required, maximum length.

Paths are returned relative to the service base address, with the leading version prefix removed and every path
variable written {<resource>-<name>} (for example access-requests/{access-request-id}), as the client documents write
them. The sources are parsed as text; no build is needed.
"""

from __future__ import annotations

import os
import re
import sys
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import code_facts as cf  # noqa: E402

JAVA = cf.JAVA_ROOT
FRONT = cf.REPO / "frontend" / "src"
VERBS = {"GetMapping": "GET", "PostMapping": "POST", "PutMapping": "PUT", "DeleteMapping": "DELETE",
         "PatchMapping": "PATCH"}
CHECKS = ("NotNull", "NotBlank", "NotEmpty", "Size", "Pattern", "Min", "Max", "DecimalMin", "DecimalMax", "Email",
          "Past", "PastOrPresent", "Future", "FutureOrPresent", "Positive", "PositiveOrZero", "Digits", "Valid")


@dataclass
class Param:
    name: str
    type: str
    where: str            # path, query, body
    required: bool
    default: str = ""


@dataclass
class Operation:
    verb: str
    path: str
    raw_path: str
    permission: str
    permissions: list[str]
    params: list[Param]
    body: str | None
    returns: str
    summary: str
    cls: str
    method: str
    status: str = ""
    file: str = ""


@dataclass
class RecordField:
    name: str
    type: str
    checks: list[str] = field(default_factory=list)
    messages: list[str] = field(default_factory=list)
    mandatory: bool = False
    length: str = ""
    pattern: str = ""
    doc: str = ""


# ------------------------------------------------------------------------------------------------ helpers
def singular(seg: str) -> str:
    seg = seg.strip("{}")
    if seg.endswith("ies"):
        return seg[:-3] + "y"
    if seg.endswith("ses"):
        return seg[:-2]
    return seg[:-1] if seg.endswith("s") and not seg.endswith("ss") else seg


def kebab(name: str) -> str:
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"-\1", name).lower()


def client_path(path: str) -> str:
    """/api/v1/nbadmin/access-requests/{id} -> nbadmin/access-requests/{access-request-id}."""
    path = re.sub(r"^/?api/v\d+/?", "", path).strip("/")
    segs = path.split("/") if path else []
    out = []
    for i, s in enumerate(segs):
        m = re.fullmatch(r"\{(\w+)\}", s)
        if m:
            name = kebab(m.group(1))
            prev = singular(segs[i - 1]) if i else "item"
            out.append("{" + (f"{prev}-id" if name == "id" else name if "-" in name else f"{prev}-{name}") + "}")
        else:
            out.append(s)
    return "/".join(out)


def _string_consts(text: str) -> dict[str, str]:
    out = {}
    for m in re.finditer(r"static final String (\w+)\s*=\s*((?:\"(?:[^\"\\]|\\.)*\"\s*\+?\s*)+);", text):
        out[m.group(1)] = "".join(re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(2)))
    return out


def _resolve(expr: str, consts: dict[str, str]) -> str:
    expr = expr.strip()
    if expr.startswith("value"):
        expr = expr.split("=", 1)[1].strip()
    parts = [p.strip() for p in cf.split_top(expr, "+")]
    out = []
    for p in parts:
        if p.startswith('"'):
            out.append(p.strip('"'))
        elif p in consts:
            out.append(consts[p])
        elif "." in p and p.split(".")[-1] in _global_strings():
            out.append(_global_strings()[p.split(".")[-1]])
        else:
            out.append(p)
    return "".join(out)


@lru_cache(maxsize=1)
def _global_strings() -> dict[str, str]:
    out: dict[str, str] = {}
    for p in JAVA.rglob("*.java"):
        t = p.read_text(encoding="utf-8")
        if "static final String" in t:
            for k, v in _string_consts(t).items():
                out.setdefault(k, v)
    return out


def permissions_of(expr: str) -> list[str]:
    return re.findall(r"'([A-Z][A-Z0-9_]+)'", expr)


def _javadoc_summary(doc: str) -> str:
    lines = [re.sub(r"^\s*\*\s?", "", ln) for ln in doc.splitlines()]
    text = " ".join(ln for ln in lines if ln and not ln.startswith("@")).replace("/**", "").replace("*/", "")
    text = re.sub(r"\{@(?:link|code)\s+([^}]+)\}", r"\1", text)
    text = " ".join(text.split())
    m = re.match(r"(.+?\.)(\s|$)", text)
    return (m.group(1) if m else text).strip()


# ------------------------------------------------------------------------------------------------ backend
def controller_files(packages) -> list[Path]:
    out = []
    for pkg in packages:
        for p in sorted((JAVA / pkg).rglob("*Controller.java")):
            if "@RestController" in p.read_text(encoding="utf-8"):
                out.append(p)
    return out


def operations(packages=None, files=None) -> list[Operation]:
    ops: list[Operation] = []
    for p in files or controller_files(packages):
        text = p.read_text(encoding="utf-8")
        consts = _string_consts(text)
        cls = p.stem
        head = text[:text.index("class " + cls)] if "class " + cls in text else ""
        base = ""
        m = re.search(r"@RequestMapping\(([^)]*)\)", head)
        if m:
            base = _resolve(m.group(1), consts)
        cls_perm = ""
        m = re.search(r"@PreAuthorize\(\s*((?:[^()]|\([^()]*\))*)\)", head)
        if m:
            cls_perm = _resolve(m.group(1), consts)
        body = text[text.index("class " + cls):] if "class " + cls in text else text
        for m in re.finditer(r"((?:/\*\*(?:[^*]|\*(?!/))*\*/\s*)?)((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*(?://[^\n]*\n\s*)?)+)"
                             r"public\s+([\w<>\[\], ?.]+?)\s+(\w+)\s*\(", body):
            annots = m.group(2)
            vm = re.search(r"@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\(([^)]*)\))?", annots)
            if not vm:
                continue
            sub = _resolve(vm.group(2), consts) if vm.group(2) and vm.group(2).strip() else ""
            sub = re.sub(r"^(?:path|value)\s*=\s*", "", (cf.split_top(sub) or [""])[0].strip()).strip('"')
            pm = re.search(r"@PreAuthorize\(\s*((?:[^()]|\([^()]*\))*)\)", annots)
            perm = _resolve(pm.group(1), consts) if pm else cls_perm
            sm = re.search(r"@ResponseStatus\((?:code\s*=\s*)?HttpStatus\.(\w+)", annots)
            start = m.end() - 1
            args = body[start + 1:cf.matching(body, start)]
            params, req_body = [], None
            for a in cf.split_top(args):
                a = " ".join(a.split())
                if not a:
                    continue
                name = a.split()[-1]
                typ = re.sub(r"@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*", "", a).rsplit(" ", 1)[0].strip()
                if "@RequestBody" in a:
                    req_body = typ
                    continue
                if "@PathVariable" in a:
                    params.append(Param(name, typ, "path", True))
                elif "@RequestParam" in a:
                    dm = re.search(r'defaultValue\s*=\s*"([^"]*)"', a)
                    req = not re.search(r"required\s*=\s*false", a) and not dm
                    params.append(Param(name, typ, "query", req, dm.group(1) if dm else ""))
                elif "@Valid" in a or "@ModelAttribute" in a or (typ[:1].isupper() and typ not in (
                        "Authentication", "HttpServletRequest", "HttpServletResponse", "Principal", "Pageable")):
                    if typ.endswith(("Query", "Filter", "Filters", "Criteria")):
                        params.append(Param(name, typ, "query", False))
            raw = (base.rstrip("/") + "/" + sub.lstrip("/")).rstrip("/") if sub else base
            ops.append(Operation(VERBS[vm.group(1)], client_path(raw), raw, perm, permissions_of(perm), params,
                                 req_body, m.group(3).strip(), _javadoc_summary(m.group(1)), cls, m.group(4),
                                 sm.group(1) if sm else "", str(p)))
    return ops


@lru_cache(maxsize=1)
def _records() -> dict[str, list[tuple[Path, str, str]]]:
    """Simple name -> [(file, record components, javadoc)] of every record of the backend."""
    out: dict[str, list[tuple[Path, str, str]]] = {}
    for p in JAVA.rglob("*.java"):
        t = p.read_text(encoding="utf-8")
        for m in re.finditer(r"((?:/\*\*(?:[^*]|\*(?!/))*\*/\s*)?)(?:@\w+(?:\([^)]*\))?\s*)*"
                             r"(?:public\s+|static\s+|private\s+)*record\s+(\w+)\s*(?:<[^>]*>)?\s*\(", t):
            start = m.end() - 1
            comps = t[start + 1:cf.matching(t, start)]
            out.setdefault(m.group(2), []).append((p, comps, m.group(1)))
    return out


def record_fields(name: str, near: str | None = None) -> list[RecordField]:
    """Fields of the record; near: the class whose own record of that name is preferred (nested records)."""
    name = re.sub(r"<.*", "", name).split(".")[-1]
    if name not in _records():
        return []
    found = _records()[name]
    if near and len(found) > 1:
        def shared(r):  # the record of the same class first, then the nearest package
            if r[0].stem == Path(near).stem:
                return 10_000
            return len(os.path.commonpath([str(r[0]), near])) if near.startswith("/") else 0
        found = sorted(found, key=shared, reverse=True)
    _, comps, doc = found[0]
    doc = doc.replace("*/", "")
    docs = {m.group(1): " ".join(m.group(2).split()) for m in
            re.finditer(r"@param (\w+)\s+((?:(?!@param|\*/).)*)", doc.replace("*", " "), re.S)}
    out = []
    for c in cf.split_top(cf._strip_comments(comps)):
        c = " ".join(c.split())
        if not c:
            continue
        fname = c.split()[-1]
        typ = re.sub(r"@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*", "", c).rsplit(" ", 1)[0].strip()
        f = RecordField(fname, typ, doc=docs.get(fname, ""))
        for am in re.finditer(r"@(\w+)(?:\(((?:[^()]|\([^()]*\))*)\))?", c):
            a, args = am.group(1), am.group(2) or ""
            if a not in CHECKS:
                continue
            msg = re.search(r'message\s*=\s*"((?:[^"\\]|\\.)*)"', args)
            if msg:
                f.messages.append(msg.group(1))
            if a in ("NotNull", "NotBlank", "NotEmpty"):
                inner = c.index("@" + a) > c.find("<") > -1 and c.find("<") < c.index("@" + a) < c.rfind(">")
                if not inner:
                    f.mandatory = True
                f.checks.append(("each value: " if inner else "") + {"NotNull": "required", "NotBlank":
                                                                     "required, not blank", "NotEmpty":
                                                                     "at least one"}[a])
            elif a == "Size":
                lo = re.search(r"min\s*=\s*(\d+)", args)
                hi = re.search(r"max\s*=\s*(\d+)", args)
                f.length = (f"{lo.group(1)}-" if lo else "") + (hi.group(1) if hi else "")
                many = typ.startswith(("List", "Set", "Collection"))
                f.checks.append((f"at most {hi.group(1)} items" if many and hi else f"length {f.length}")
                                if f.length else "size")
                if many:
                    f.length = ""
            elif a == "Pattern":
                rx = re.search(r'regexp\s*=\s*"((?:[^"\\]|\\.)*)"', args)
                if rx:
                    f.pattern = rx.group(1).replace("\\\\", "\\")
                    f.checks.append(f"format {f.pattern}")
            elif a in ("Min", "Max", "DecimalMin", "DecimalMax"):
                v = re.search(r'(?:value\s*=\s*)?"?(-?[\d.]+)"?', args)
                f.checks.append(f"{'minimum' if 'Min' in a else 'maximum'} {v.group(1) if v else args}")
            elif a == "Valid":
                continue
            else:
                f.checks.append(re.sub(r"(?<!^)([A-Z])", r" \1", a).lower())
        out.append(f)
    return out


@lru_cache(maxsize=None)
def enum_values(name: str) -> list[str]:
    name = re.sub(r"<.*", "", name).split(".")[-1]
    for p in JAVA.rglob("*.java"):
        t = p.read_text(encoding="utf-8")
        m = re.search(r"\benum\s+" + re.escape(name) + r"\b[^{]*\{", t)
        if m:
            start = m.end() - 1
            body = cf._strip_comments(t[start + 1:cf.matching(t, start)]).split(";")[0]
            return [v.split("(")[0].strip() for v in cf.split_top(body) if re.match(r"\s*[A-Z][A-Z0-9_]*", v)
                    and v.split("(")[0].strip()]
    return []


# ------------------------------------------------------------------------------------------------ frontend
@dataclass
class ClientCall:
    obj: str
    fn: str
    verb: str
    path: str


@lru_cache(maxsize=1)
def client_calls() -> dict[tuple[str, str], ClientCall]:
    out: dict[tuple[str, str], ClientCall] = {}
    for p in sorted((FRONT / "api").glob("*.ts")):
        t = p.read_text(encoding="utf-8")
        consts = {m.group(1): m.group(2) for m in re.finditer(r"^const (\w+)\s*=\s*['`]([^'`$]*)['`];", t, re.M)}
        funcs = {m.group(1): m.group(2) for m in
                 re.finditer(r"^const (\w+)\s*=\s*\([^)]*\)(?:\s*:\s*\w+)?\s*=>\s*`([^`]*)`;", t, re.M)}
        for om in re.finditer(r"export const (\w+Api)\s*=\s*\{", t):
            start = om.end() - 1
            obj = t[start + 1:cf.matching(t, start)]
            for em in re.finditer(r"^\s{2}(\w+):\s*(?:async\s*)?\([^)]*\)[^=]*=>\s*\n?\s*(?:\w+\.)?api\.(get|post|put|"
                                  r"delete|patch|getFile|postFile|upload|download)\w*(?:<[^>]*(?:<[^>]*>)?[^>]*>)?\(\s*"
                                  r"(`[^`]*`|'[^']*'|\w+(?:\([^)]*\))?)", obj, re.M):
                expr = em.group(3)
                fm = re.fullmatch(r"(\w+)\(([^)]*)\)", expr)
                if fm and fm.group(1) in funcs:
                    expr = "`" + funcs[fm.group(1)] + "`"
                elif expr in consts:
                    expr = "'" + consts[expr] + "'"
                path = expr.strip("`'")
                path = re.sub(r"\$\{(\w+)\}", lambda x: consts.get(x.group(1), x.group(0)), path)
                path = re.sub(r"\$\{(\w+)\}", lambda x: consts.get(x.group(1), x.group(0)), path)
                path = re.sub(r"\$\{(\w+)\([^)]*\)\}", lambda x: re.sub(r"\$\{(\w+)\}", lambda y: consts.get(
                    y.group(1), y.group(0)), funcs[x.group(1)]) if x.group(1) in funcs else x.group(0), path)
                path = re.sub(r"\$\{toQuery\([^}]*\}\)?\}?", "", path)
                path = re.sub(r"\$\{[^}]*\}", "{x}", path)
                verb = {"getFile": "GET", "download": "POST", "postFile": "POST", "upload": "POST"}.get(
                    em.group(2), em.group(2).upper())
                out[(om.group(1), em.group(1))] = ClientCall(om.group(1), em.group(1), verb, path.split("?")[0])
    return out


def _local_imports(path: Path, feature: Path, seen: set[Path]) -> set[Path]:
    if path in seen or not path.exists():
        return seen
    seen.add(path)
    for m in re.finditer(r"from '(\./[^']+|\.\./[^']+|@/features/[^']+)'", path.read_text(encoding="utf-8")):
        spec = m.group(1)
        base = (FRONT / spec[2:]) if spec.startswith("@/") else (path.parent / spec)
        for cand in (base.with_suffix(".tsx"), base.with_suffix(".ts"), base / "index.ts"):
            cand = cand.resolve()
            if cand.exists() and feature.resolve() in cand.parents:
                _local_imports(cand, feature, seen)
                break
    return seen


def screen_files(feature: str, page: str) -> list[Path]:
    feat = FRONT / "features" / feature
    return sorted(_local_imports((feat / page).resolve(), feat, set()))


def screen_calls(feature: str, page: str) -> list[ClientCall]:
    calls = client_calls()
    out: list[ClientCall] = []
    for f in screen_files(feature, page):
        for m in re.finditer(r"\b(\w+Api)\.(\w+)\b", f.read_text(encoding="utf-8")):
            c = calls.get((m.group(1), m.group(2)))
            if c and c not in out:
                out.append(c)
    return out


def match_operation(call: ClientCall, ops: list[Operation]) -> Operation | None:
    """The service operation a client call reaches (same method, same path with any variable)."""
    want = re.sub(r"\{[^}]*\}", "{}", call.path.strip("/"))
    for op in ops:
        have = re.sub(r"\{[^}]*\}", "{}", re.sub(r"^/?api/v\d+/?", "", op.raw_path).strip("/"))
        if op.verb == call.verb and have == want:
            return op
    return None


@dataclass
class ClientField:
    label: str
    required: bool
    max_length: str
    file: str


def client_fields(feature: str, files: list[str]) -> list[ClientField]:
    """Input fields of the screen files: <Field label="..." required ...> with the maxLength of the input inside."""
    out = []
    for name in files:
        p = FRONT / "features" / feature / name
        t = p.read_text(encoding="utf-8")
        for m in re.finditer(r"<Field\s+label=(?:\"([^\"]+)\"|\{'([^']+)'\}|\{([^}]+)\})([^>]*)>", t):
            label = m.group(1) or m.group(2) or "<" + (m.group(3) or "").strip() + ">"
            rest = m.group(4)
            close = t.find("</Field>", m.end())
            inner = t[m.end():close if close > 0 else m.end() + 600]
            ml = re.search(r"maxLength=\{(\d+)\}", inner)
            out.append(ClientField(label, bool(re.search(r"\brequired\b(?!=\{false)", rest)),
                                   ml.group(1) if ml else "", name))
    return out


# ------------------------------------------------------------------------------------------------ jobs and audit
@dataclass
class Job:
    name: str
    cron_utc: str
    time_pht: str
    summary: str
    cls: str


def jobs(packages) -> list[Job]:
    """Scheduled jobs of the packages: name, schedule (UTC cron and the time in Manila, UTC+8), description."""
    out = []
    for pkg in packages:
        for p in sorted((JAVA / pkg).rglob("*.java")):
            t = p.read_text(encoding="utf-8")
            n = re.search(r'JOB_NAME\s*=\s*"([A-Z0-9_]+)"', t)
            c = re.search(r'@Value\("\$\{brokerverse\.jobs\.[\w-]+:([^}]+)\}"\)', t)
            if not n:
                continue
            cron = c.group(1).strip() if c else ""
            when = ""
            parts = cron.split()
            if len(parts) == 6 and parts[1].isdigit() and parts[2].isdigit():
                h = (int(parts[2]) + 8) % 24
                when = f"Daily {h:02d}:{int(parts[1]):02d}" if parts[3:] == ["*", "*", "*"] else cron
            elif len(parts) == 6 and parts[2].startswith("*/"):
                when = f"Every {parts[2][2:]} hours"
            elif len(parts) == 6 and parts[1].startswith("*/"):
                when = f"Every {parts[1][2:]} minutes"
            doc = re.search(r"(/\*\*(?:[^*]|\*(?!/))*\*/)\s*(?:@\w+(?:\([^)]*\))?\s*)*public\s+(?:final\s+)?class", t)
            out.append(Job(n.group(1), cron, when or cron, _javadoc_summary(doc.group(1)) if doc else "", p.stem))
    return out


@dataclass
class AuditEvent:
    entity: str
    action: str
    cls: str
    method: str


def audit_events(packages) -> list[AuditEvent]:
    """The audit records written by the packages: entity type and action (AuditAction), with the class and method."""
    out: list[AuditEvent] = []
    for pkg in packages:
        for p in sorted((JAVA / pkg).rglob("*.java")):
            t = cf._strip_comments(p.read_text(encoding="utf-8"))
            consts = _string_consts(t)
            for m in re.finditer(r"\baudit\w*\.(record\w*)\(", t):
                start = m.end() - 1
                args = [a.strip() for a in cf.split_top(t[start + 1:cf.matching(t, start)])]
                act = next((a for a in args if a.startswith("AuditAction.")), None)
                if not act or not args:
                    continue
                ent = args[0]
                ent = ent.strip('"') if ent.startswith('"') else consts.get(ent, _global_strings().get(
                    ent.split(".")[-1], ent))
                ev = AuditEvent(ent, act.split(".", 1)[1], p.stem, cf._method_at(t, m.start()))
                if not any(e.entity == ev.entity and e.action == ev.action for e in out):
                    out.append(ev)
    return out


def main(argv: list[str]) -> int:
    for op in operations(argv or ["nbadmin"]):
        print(f"{op.verb:6} {op.path:60} {','.join(op.permissions) or op.permission:40} {op.body or '-'}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
