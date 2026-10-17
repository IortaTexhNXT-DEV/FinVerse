"""Reads the API catalogue of BIBS from the request mappings of the backend (BRD-00, Architecture).

    python docs/deliverables/src/programme/architecture/api_catalogue.py            # summary per module
    python docs/deliverables/src/programme/architecture/api_catalogue.py --unknown  # permission forms not read

Every operation of the user interface (prefix api/v1) and of the system integration interface (prefix integration)
is one row: module and bounded context (architecture_data.yaml), HTTP method, path, purpose (first sentence of the
operation's description in the backend), permission (the access rule of the operation, constants resolved),
whether the company or branch of the request is named in the path or the query (data scope check) and the class
that serves it. Paths are written relative to the BIBS host name, without the leading slash, and path variables as
:name, the notation of the API catalogue of the OOTB edition.

Modules of the insurer-company suite (removed from BIBS by the client decision of 8 October 2026) and the
operations that need the insurer tax permission are not read.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
from code_facts import matching, split_top  # noqa: E402

JAVA_ROOT = REPO / "backend" / "src" / "main" / "java" / "com" / "iortatechnxt" / "brokerverse"
# Insurer-company suite, removed from BIBS (client decision of 8 October 2026).
REMOVED_MODULES = {"underwriting", "claims", "reinsurance", "reserves", "insurance", "consolidation"}
REMOVED_CLASSES = {"IcController"}
REMOVED_PERMISSIONS = {"INSURER_TAX_VIEW", "INSURER_DASHBOARD_VIEW"}
# Insurer dashboard widgets (gross written premium, claims paid and outstanding), removed with the suite.
REMOVED_PATHS = {"api/v1/dashboard/claims", "api/v1/dashboard/premium"}
# Access rules written as a call of a Spring bean, in words.
BEAN_RULES = {
    "lovAccess.canRead": "LOV_MANAGE, or the owner permission of the list",
    "lovAccess.canMaintain": "LOV_MANAGE, or the owner permission of the list",
    "lovAccess.canMaintainValue": "LOV_MANAGE, or the owner permission of the list",
    "lovAccess.canAuthorizeValue": "MASTER_AUTHORIZE, or the owner permission of the list (never the maker)",
}
INTEGRATION_RULE = "API gateway token (Apigee X): issuer, audience and the scopes configured for the API"
MAPPING = re.compile(r"@(Get|Post|Put|Patch|Delete|Request)Mapping\b")
METHOD_DECL = re.compile(r"\b(?:public|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\], ?.]+?\s+(\w+)\s*\(")
CLASS_DECL = re.compile(r"\b(?:public\s+)?(?:final\s+)?class\s+(\w+)")
CONST_DECL = re.compile(r"\bstatic\s+final\s+String\s+(\w+)\s*=\s*")


@dataclass
class Operation:
    module: str
    cls: str
    method: str
    verb: str
    path: str
    purpose: str
    permission: str
    permission_codes: tuple[str, ...]
    company_param: bool


# ------------------------------------------------------------------------------------------------ Java reading

def _strip_line_comments(text: str) -> str:
    return re.sub(r"(?m)^\s*//.*$", "", text)


def _constants(text: str) -> dict[str, str]:
    """String constants of a class: NAME = expression up to the semicolon (literals and constants joined)."""
    out: dict[str, str] = {}
    for m in CONST_DECL.finditer(text):
        end = m.end()
        depth = 0
        i = end
        while i < len(text):
            c = text[i]
            if c == '"':
                j = i + 1
                while j < len(text) and text[j] != '"':
                    j += 2 if text[j] == "\\" else 1
                i = j + 1
                continue
            if c in "({[":
                depth += 1
            elif c in ")}]":
                depth -= 1
            elif c == ";" and depth == 0:
                break
            i += 1
        out[m.group(1)] = text[end:i].strip()
    return out


@lru_cache(maxsize=None)
def _global_constants() -> dict[str, dict[str, str]]:
    """Constants of every class of the backend: class simple name -> {NAME: expression}."""
    out: dict[str, dict[str, str]] = {}
    for p in JAVA_ROOT.rglob("*.java"):
        out.setdefault(p.stem, {}).update(_constants(p.read_text(encoding="utf-8")))
    return out


def evaluate(expr: str, local: dict[str, str], cls: str, depth: int = 0) -> str:
    """The string value of a Java constant expression: literals and constants joined with +."""
    expr = " ".join(expr.strip().split())
    if depth > 8 or not expr:
        return expr
    if expr.startswith("(") and matching(expr, 0) == len(expr) - 1:
        expr = expr[1:-1].strip()
    out = []
    for part in split_top(expr, "+"):
        part = part.strip()
        lit = re.fullmatch(r'"((?:[^"\\]|\\.)*)"', part)
        if lit:
            out.append(lit.group(1).replace('\\"', '"'))
        elif re.fullmatch(r"[A-Z_][A-Z0-9_]*", part) and part in local:
            out.append(evaluate(local[part], local, cls, depth + 1))
        elif re.fullmatch(r"[A-Z]\w*\.[A-Z_][A-Z0-9_]*", part):
            owner, name = part.split(".")
            consts = _global_constants().get(owner, {})
            out.append(evaluate(consts[name], consts, owner, depth + 1) if name in consts else part)
        else:
            out.append(part)
    return "".join(out)


def _annotation_args(text: str, start: int) -> tuple[str, int]:
    """The argument text of an annotation whose name ends at start ('' without parentheses) and the end index."""
    i = start
    while i < len(text) and text[i] in " \t":
        i += 1
    if i < len(text) and text[i] == "(":
        end = matching(text, i)
        return text[i + 1:end], end + 1
    return "", start


def _named(args: str) -> dict[str, str]:
    """Named arguments of an annotation; a single positional argument is 'value'."""
    parts = split_top(args, ",") if args.strip() else []
    named: dict[str, str] = {}
    positional: list[str] = []
    for part in parts:
        m = re.match(r"\s*(\w+)\s*=\s*(.*)$", part, re.S)
        if m and not part.strip().startswith('"'):
            named[m.group(1)] = m.group(2).strip()
        else:
            positional.append(part.strip())
    if positional and "value" not in named and "path" not in named:
        named["value"] = ", ".join(positional) if len(positional) > 1 else positional[0]
    return named


def _paths(args: str, local: dict[str, str], cls: str) -> list[str]:
    named = _named(args)
    expr = named.get("value") or named.get("path") or ""
    expr = expr.strip()
    if not expr:
        return [""]
    if expr.startswith("{") and expr.endswith("}"):
        return [evaluate(e, local, cls) for e in split_top(expr[1:-1], ",") if e.strip()]
    return [evaluate(expr, local, cls)]


def _verbs(kind: str, args: str) -> list[str]:
    if kind != "Request":
        return [kind.upper()]
    named = _named(args)
    found = re.findall(r"RequestMethod\.(\w+)", named.get("method", ""))
    return found or ["ANY"]


def _javadoc_before(text: str, pos: int) -> str:
    end = text.rfind("*/", 0, pos)
    start = text.rfind("/**", 0, pos)
    if start < 0 or end < start:
        return ""
    between = text[end + 2:pos]
    # only annotations between the comment and the mapping
    if re.search(r"[;{}]", re.sub(r'"(?:[^"\\]|\\.)*"', "", between)):
        return ""
    return text[start + 3:end]


def first_sentence(doc: str) -> str:
    lines = []
    for line in doc.splitlines():
        line = line.strip().lstrip("*").strip()
        if line.startswith("@"):
            break
        lines.append(line)
    text = " ".join(lines)
    text = re.sub(r"\{@(?:code|literal)\s+([^{}]*)\}", r"\1", text)
    text = re.sub(r"\{@link(?:plain)?\s+(?:[\w.]*#)?([^{}\s]+)(?:\s+([^{}]*))?\}",
                  lambda m: m.group(2) or m.group(1), text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = " ".join(text.split())
    m = re.match(r"(.+?\.)(?:\s|$)", text)
    return (m.group(1) if m else text).strip()


def humanise(name: str) -> str:
    words = re.sub(r"(?<!^)([A-Z])", r" \1", name).lower().split()
    return (" ".join(words)).capitalize() + "." if words else ""


def permission_text(expr: str) -> tuple[str, tuple[str, ...]]:
    """A readable access rule: the permission codes of hasAuthority / hasAnyAuthority, 'and' / 'or' kept."""
    if not expr:
        return "Any signed-in user (no permission named)", ()
    codes = tuple(dict.fromkeys(re.findall(r"'([A-Z0-9_]+)'", expr)))
    low = expr.replace(" ", "")
    if "permitAll" in low:
        return "Public (no sign-in)", ()
    if not codes and "isAuthenticated" in low:
        return "Any signed-in user", ()
    bean = re.match(r"@(\w+\.\w+)\(", expr.strip())
    if bean and bean.group(1) in BEAN_RULES:
        text = BEAN_RULES[bean.group(1)]
        return text, tuple(re.findall(r"\b[A-Z][A-Z0-9_]{3,}\b", text))
    if not codes:
        return "Rule: " + expr, ()
    if len(codes) == 1:
        return codes[0], codes
    joiner = " and " if " and " in expr and " or " not in expr and "hasAnyAuthority" not in expr else " or "
    if "hasAnyAuthority" in expr and " and " in expr:
        return "Combined rule: " + " ".join(expr.split()), codes
    return joiner.join(codes), codes


def _class_level(text: str, class_pos: int, local: dict[str, str], cls: str) -> tuple[list[str], str]:
    head = text[:class_pos]
    bases = [""]
    pre = ""
    for m in MAPPING.finditer(head):
        args, _ = _annotation_args(head, m.end())
        bases = _paths(args, local, cls)
    m = re.search(r"@PreAuthorize\s*\(", head)
    if m:
        end = matching(head, m.end() - 1)
        pre = evaluate(head[m.end():end], local, cls)
    return bases, pre


def _join(base: str, sub: str) -> str:
    path = "/".join(p.strip("/") for p in (base, sub) if p and p.strip("/"))
    return "/" + path if path else "/"


def relative(path: str) -> str:
    """The path relative to the host, path variables as :name (api/v1/clients/:id)."""
    path = re.sub(r"\{(\w+)(?::[^}]*)?\}", r":\1", path)
    path = path.replace("/**", "/*")
    return path.lstrip("/")


def read_controller(p: Path) -> list[Operation]:
    text = _strip_line_comments(p.read_text(encoding="utf-8"))
    if "@RestController" not in text:
        return []
    module = p.relative_to(JAVA_ROOT).parts[0]
    cls_match = CLASS_DECL.search(text)
    if not cls_match:
        return []
    cls = cls_match.group(1)
    local = {**_global_constants().get(cls, {})}
    bases, class_pre = _class_level(text, cls_match.start(), local, cls)
    ops: list[Operation] = []
    for m in MAPPING.finditer(text, cls_match.end()):
        args, after = _annotation_args(text, m.end())
        decl = METHOD_DECL.search(text, after)
        if not decl:
            continue
        region_start = text.rfind("*/", 0, m.start())
        region_start = max(region_start, text.rfind("}", 0, m.start()), text.rfind(";", 0, m.start()))
        region = text[region_start + 1:decl.start()]
        pre = class_pre
        pm = re.search(r"@PreAuthorize\s*\(", region)
        if pm:
            end = matching(region, pm.end() - 1)
            pre = evaluate(region[pm.end():end], local, cls)
        sig_end = matching(text, decl.end() - 1)
        signature = text[decl.end():sig_end]
        company = bool(re.search(r"\b(?:companyId|branchId)\b", signature))
        doc = _javadoc_before(text, m.start())
        purpose = first_sentence(doc) if doc else ""
        permission, codes = permission_text(pre)
        if module == "integration" or (bases and bases[0].startswith("/integration")):
            permission, codes = INTEGRATION_RULE, ()
        for base in bases:
            for sub in _paths(args, local, cls):
                for verb in _verbs(m.group(1), args):
                    ops.append(Operation(module, cls, decl.group(1), verb, relative(_join(base, sub)),
                                         purpose or humanise(decl.group(1)), permission, codes, company))
    return ops


def operations(include_removed: bool = False) -> list[Operation]:
    out: list[Operation] = []
    for p in sorted(JAVA_ROOT.rglob("*.java")):
        module = p.relative_to(JAVA_ROOT).parts[0]
        if not include_removed and (module in REMOVED_MODULES or p.stem in REMOVED_CLASSES):
            continue
        for op in read_controller(p):
            if not include_removed and (REMOVED_PERMISSIONS.intersection(op.permission_codes)
                                        or op.path in REMOVED_PATHS):
                continue
            out.append(op)
    out.sort(key=lambda o: (o.module, o.path, o.verb))
    return out


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--unknown", action="store_true", help="list the access rules that are not plain permissions")
    args = ap.parse_args(argv)
    ops = operations()
    counts = Counter(o.module for o in ops)
    for module, n in sorted(counts.items()):
        print(f"{module:14} {n}")
    print(f"total {len(ops)} operations in {len(counts)} modules; "
          f"{sum(1 for o in ops if o.company_param)} name a company or branch in the path or query")
    if args.unknown:
        for o in ops:
            if o.permission.startswith(("Rule:", "Combined", "Any signed-in user (no")):
                print(o.module, o.cls, o.method, o.verb, o.path, o.permission)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
