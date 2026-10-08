"""Builds the BIBS architecture document set (BRD-00, Programme/Architecture).

Usage
  python docs/deliverables/src/programme/architecture/build_architecture_pack.py --check     # checks only
  python docs/deliverables/src/programme/architecture/build_architecture_pack.py             # all outputs
  python docs/deliverables/src/programme/architecture/build_architecture_pack.py --only SOLUTION_ARCHITECTURE
  python docs/deliverables/src/programme/architecture/build_architecture_pack.py --previews  # page previews

Inputs (this folder)
  * architecture_data.yaml   bounded contexts, component baseline, NFR targets, environments, sizing, data services,
                             integration catalogue and Kafka topics, shared by the documents;
  * adr.yaml                 the architecture decision records;
  * api_catalogue.py         reads every operation of the backend (request mappings, access rules, descriptions);
  * *.md                     the Word sources in the bdoi_docx format; lines <!-- arch:<name> --> are replaced by
                             tables built from the inputs above and {{name}} by figures (placeholders() below);
  * figures/*.dot            the architecture figures.

Outputs (docs/deliverables/out/Programme/Architecture, from tools/deliverables/brand.py)
  * BIBS_Architecture_BRD-00_Solution_Architecture_v1.0.docx
  * BIBS_Architecture_BRD-00_Deployment_Architecture_v1.0.docx
  * BIBS_Architecture_BRD-00_Infrastructure_Architecture_v1.0.docx
  * BIBS_Architecture_BRD-00_Integration_Architecture_v1.0.docx
  * BIBS_Architecture_BRD-00_API_Specification_v1.0.docx and BIBS_Architecture_BRD-00_API_Catalogue_v1.0.xlsx
  * BIBS_Architecture_BRD-00_Architecture_Decision_Records_v1.0.docx and BIBS_Architecture_BRD-00_ADR_Log_v1.0.xlsx
"""

from __future__ import annotations

import argparse
import json
import re
import shlex
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
sys.path.insert(0, str(HERE))
import api_catalogue  # noqa: E402
import brand  # noqa: E402
import check_pack  # noqa: E402
from bdoi_docx import BdoiDocument, lint_source, load_source, meta_from, output_path, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

KIND = "Architecture"
VERSION = "1.0"
DATE = "08 October 2026"
SOURCES = ["SOLUTION_ARCHITECTURE.md", "DEPLOYMENT_ARCHITECTURE.md", "INFRASTRUCTURE_ARCHITECTURE.md",
           "INTEGRATION_ARCHITECTURE.md", "API_SPECIFICATION.md", "ARCHITECTURE_DECISION_RECORDS.md"]
PLACEHOLDER = re.compile(r"^<!--\s*arch:(\w+)\s*(.*?)-->\s*$")
TOKEN = re.compile(r"\{\{(\w+)\}\}")
FILLER = ("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art", "best-in-class",
          "world-class")
INTERFACES = {"api/v1/admin": "Administration", "integration": "System integration (Apigee X)"}


def load() -> dict[str, Any]:
    d = yaml.safe_load((HERE / "architecture_data.yaml").read_text(encoding="utf-8"))
    d["adrs"] = yaml.safe_load((HERE / "adr.yaml").read_text(encoding="utf-8"))["adrs"]
    return d


# ------------------------------------------------------------------------------------------------ API catalogue

def module_map(d: dict[str, Any]) -> dict[str, tuple[dict[str, Any], str]]:
    """Backend module -> (bounded context, module label)."""
    out = {}
    for ctx in d["contexts"]:
        for module, label in ctx["modules"].items():
            out[module] = (ctx, label)
    return out


def interface_of(path: str) -> str:
    for prefix, name in INTERFACES.items():
        if path == prefix or path.startswith(prefix + "/"):
            return name
    return "User interface"


def clean_purpose(op: api_catalogue.Operation) -> str:
    """The description of the operation, or its name in words when the description holds an internal reference."""
    text = op.purpose
    bad = check_pack.RESTRICTED.search(text) or any(p.search(text) for _, p in check_pack.BUILD_STATUS)
    if bad or not text:
        text = api_catalogue.humanise(op.method)
        for word, plain in (("rebuild", "regenerate"), ("build", "generate")):
            text = re.sub(rf"\b{word}", plain, text, flags=re.I)
        return text[:1].upper() + text[1:]
    return text


def api_rows(d: dict[str, Any]) -> list[dict[str, Any]]:
    mm = module_map(d)
    rows = []
    for op in api_catalogue.operations():
        if op.module not in mm:
            raise SystemExit(f"module {op.module} is not in architecture_data.yaml contexts")
        ctx, label = mm[op.module]
        rows.append({"context": f"{ctx['id']} {ctx['name']}", "module": label, "brd": ctx["brd"],
                     "drop": ctx["drop"], "method": op.verb, "path": op.path, "purpose": clean_purpose(op),
                     "permission": op.permission, "company": "Yes" if op.company_param else "-",
                     "interface": interface_of(op.path)})
    return rows


# ------------------------------------------------------------------------------------------------ Word placeholders

def md_table(headers: list[str], rows: list[list[Any]], opts: str) -> list[str]:
    def cell(v: Any) -> str:
        if isinstance(v, (list, tuple)):
            v = "; ".join(str(x) for x in v)
        return str(v if v is not None else "").replace("|", "/").replace("\n", " ").strip() or "-"
    out = [f"<!-- table: {opts} -->", "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(cell(v) for v in r) + " |" for r in rows]
    return out + [""]


def tokens(d: dict[str, Any]) -> dict[str, str]:
    rows = d["_api"]
    verbs = Counter(r["method"] for r in rows)
    faces = Counter(r["interface"] for r in rows)
    perms = {code for r in rows for code in re.findall(r"\b[A-Z][A-Z0-9_]{3,}\b", r["permission"])}
    return {
        "api_total": f"{len(rows):,}",
        "api_modules": str(len({r['module'] for r in rows})),
        "api_contexts": str(len({r['context'] for r in rows})),
        "api_company": f"{sum(1 for r in rows if r['company'] == 'Yes'):,}",
        "api_get": f"{verbs['GET']:,}", "api_post": f"{verbs['POST']:,}", "api_put": f"{verbs['PUT']:,}",
        "api_delete": f"{verbs['DELETE']:,}", "api_patch": f"{verbs['PATCH']:,}",
        "api_user": f"{faces['User interface']:,}", "api_admin": f"{faces['Administration']:,}",
        "api_integration": f"{faces['System integration (Apigee X)']:,}",
        "api_permissions": f"{len(perms):,}",
        "adr_count": str(len(d["adrs"])),
        "adr_accepted": str(sum(1 for a in d["adrs"] if a["status"] == "Accepted")),
        "adr_proposed": str(sum(1 for a in d["adrs"] if a["status"] == "Proposed")),
        "int_count": str(len(d["integrations"])),
        "topic_count": str(len(d["topics"])),
    }


def adr_markdown(a: dict[str, Any]) -> list[str]:
    out = [f"## {a['id']} {a['title']}", ""]
    kv = {"Status": a["status"], "Date": a["date"], "Decided by": a["decided_by"], "Area": a["area"]}
    out += ["```keyvalues"] + [f"{k}: {json.dumps(v)}" for k, v in kv.items()] + ["```", ""]
    out += ["#### Context", ""] + [f"- {x}" for x in a["context"]] + [""]
    out += ["#### Decision", ""] + [f"- {x}" for x in a["decision"]] + [""]
    out += ["#### Alternatives considered", ""]
    out += md_table(["Alternative", "Why it was not chosen"], a["alternatives"],
                    f'widths=5,12 caption="{a["id"]} alternatives" size=8.5 bold=first')
    out += ["#### Consequences", ""]
    for c in a["consequences"]:
        sign, text = c[:1], c[2:]
        out.append(f"- **{'Benefit' if sign == '+' else 'Cost or risk'}:** {text}")
    out += ["", f"*Sources:* {a['sources']}.", ""]
    return out


def placeholders(d: dict[str, Any], name: str, opts: dict[str, str]) -> list[str]:  # noqa: C901
    cap = opts.get("caption", "")
    if name == "contexts":
        rows = [[c["id"], c["name"], ", ".join(c["modules"].values()), c["brd"], c["drop"], c["responsibility"]]
                for c in d["contexts"]]
        return md_table(["ID", "Bounded context", "Modules", "BRD", "Drop", "Responsibility"], rows,
                        f'widths=1,2.6,4.6,1.3,1.5,6.6 caption="{cap or "Bounded contexts and modules"}" '
                        'size=7.5 bold=first')
    if name == "context_api":
        per = defaultdict(Counter)
        for r in d["_api"]:
            per[r["context"]][r["method"]] += 1
        rows = []
        for c in d["contexts"]:
            k = per[f"{c['id']} {c['name']}"]
            rows.append([c["id"], c["name"], c["brd"], sum(k.values()), k["GET"], k["POST"], k["PUT"],
                         k["DELETE"] + k["PATCH"]])
        rows.append(["", "Total", "", len(d["_api"]), sum(r[4] for r in rows), sum(r[5] for r in rows),
                     sum(r[6] for r in rows), sum(r[7] for r in rows)])
        return md_table(["ID", "Bounded context", "BRD", "Operations", "GET", "POST", "PUT", "DELETE or PATCH"],
                        rows, f'widths=1,5,1.5,1.8,1.4,1.4,1.4,2 caption="{cap or "Operations per bounded context"}" '
                              'size=8 bold=first')
    if name == "baseline":
        return md_table(["Component", "Version baseline", "Licence", "Function", "Support horizon"], d["baseline"],
                        f'widths=3,2.8,2.4,4.6,4.8 caption="{cap or "Component baseline (8 October 2026)"}" '
                        'size=8 bold=first')
    if name == "components_more":
        return md_table(["Library or tool", "Version", "Licence", "Use"], d["components_more"],
                        f'widths=4,3,3.4,7.2 caption="{cap or "Further platform libraries and tools"}" '
                        'size=8 bold=first')
    if name == "nfr":
        return md_table(["Quality", "Target", "Source"], d["nfr"],
                        f'widths=2.4,9,6.2 caption="{cap or "Non-functional targets"}" size=8 bold=first')
    if name == "environments":
        return md_table(["Environment", "Purpose", "Data", "Hours", "Availability", "Owner"], d["environments"],
                        f'widths=2.3,4.8,3.3,2.8,2.3,2.3 caption="{cap or "Environments"}" size=8 bold=first')
    if name == "workloads":
        return md_table(["Workload", "Image", "Runtime role", "Serves", "Requests", "Limits", "Scaling"],
                        d["workloads"], f'widths=2.2,2.2,1.6,4.2,1.8,1.8,4 caption="{cap or "Workloads"}" '
                                        'size=7.5 bold=first')
    if name == "sizing":
        return md_table(["Environment", "bibs-web pods (min / max)", "bibs-jobs pods", "bibs-integration pods",
                         "bibs-frontend pods (min / max)", "Worker nodes"], d["sizing"],
                        f'widths=2.4,2.4,1.8,2.2,2.4,6.4 caption="{cap or "Proposed Kubernetes sizing"}" size=8 '
                        'bold=first')
    if name == "data_services":
        return md_table(["Environment", "PostgreSQL", "Valkey", "Kafka", "S3"], d["data_services"],
                        f'widths=2.3,4.8,3.9,4.2,2.4 caption="{cap or "Data services per environment"}" size=7.5 '
                        'bold=first')
    if name == "integrations":
        rows = [[x["id"], x["name"], x["system"], x["drop"], x["pattern"], x["direction"], x["questions"]]
                for x in d["integrations"]]
        return md_table(["ID", "Interface", "Counterpart", "Drop", "Pattern", "Direction", "Open"], rows,
                        f'widths=1.2,3.2,3.6,2.2,1.8,1.8,1.6 caption="{cap or "Interface catalogue"}" size=7.5 '
                        'bold=first')
    if name == "integration_transport":
        rows = [[x["id"], x["name"], x["transport"], x["data"]] for x in d["integrations"]]
        return md_table(["ID", "Interface", "Transport and format", "Data"], rows,
                        f'widths=1.2,3,8,5.4 caption="{cap or "Transport and data per interface"}" size=7.5 '
                        'bold=first')
    if name == "integration_security":
        rows = [[x["id"], x["name"], x["security"]] for x in d["integrations"]]
        return md_table(["ID", "Interface", "Security controls"], rows,
                        f'widths=1.2,3.4,13 caption="{cap or "Security per interface"}" size=7.5 bold=first')
    if name == "integration_errors":
        rows = [[x["id"], x["name"], x["errors"], x["recon"]] for x in d["integrations"]]
        return md_table(["ID", "Interface", "Error handling", "Reconciliation"], rows,
                        f'widths=1.2,3,7,6.4 caption="{cap or "Error handling and reconciliation"}" size=7.5 '
                        'bold=first')
    if name == "integration_owners":
        rows = [[x["id"], x["name"], x["owner"], x["bibs"]] for x in d["integrations"]]
        return md_table(["ID", "Interface", "BDOI owner (counterpart system)", "BIBS module and workload"], rows,
                        f'widths=1.2,3.4,5.6,7.4 caption="{cap or "Interface ownership"}" size=7.5 bold=first')
    if name == "topics":
        rows = [[t[0], t[1], t[2], t[3]] for t in d["topics"]]
        return md_table(["Topic", "Event", "Record key", "Published by"], rows,
                        f'widths=6.6,4.4,3,3.6 caption="{cap or "Kafka topics"}" size=8 bold=first')
    if name == "api_modules":
        per = Counter((r["context"].split()[0], r["module"]) for r in d["_api"])
        rows = [[ctx, mod, n] for (ctx, mod), n in sorted(per.items())]
        return md_table(["Context", "Module", "Operations"], rows,
                        f'widths=2,8,3 caption="{cap or "Operations per module"}" size=8 bold=first')
    if name == "adr_index":
        rows = [[a["id"], a["title"], a["area"], a["status"], a["date"], a["decided_by"]] for a in d["adrs"]]
        return md_table(["ID", "Decision", "Area", "Status", "Date", "Decided by"], rows,
                        f'widths=1.4,6,2.4,1.8,2,4 caption="{cap or "Architecture decision log"}" size=8 '
                        'status=Status bold=first')
    if name == "adrs":
        out: list[str] = []
        for a in d["adrs"]:
            out += adr_markdown(a)
        return out
    raise SystemExit(f"unknown placeholder arch:{name}")


def expand(d: dict[str, Any], lines: list[str]) -> list[str]:
    toks = tokens(d)
    out: list[str] = []
    for line in lines:
        m = PLACEHOLDER.match(line)
        if m:
            opts = dict(part.split("=", 1) for part in shlex.split(m.group(2).strip()))
            out += placeholders(d, m.group(1), opts)
            continue
        out.append(TOKEN.sub(lambda t: toks[t.group(1)], line))
    return out


def expanded_source(d: dict[str, Any], src: Path) -> Path:
    front_text, body = src.read_text(encoding="utf-8").split("---", 2)[1:]
    tmp = src.with_name(f".{src.stem}.expanded.md")
    tmp.write_text("---" + front_text + "---\n" + "\n".join(expand(d, body.splitlines())) + "\n", encoding="utf-8")
    return tmp


def wording_problems(name: str, text: str) -> list[str]:
    """Restricted words and development-status wording of the client pack rules (check_pack), before the build."""
    text = re.sub(r"<!--.*?-->", " ", text, flags=re.S)
    text = re.sub(r"\]\(figures/[\w.-]+\)", "]", text)
    front, body = text.split("---", 2)[1:] if text.startswith("---") else ("", text)
    front = re.sub(r"(?m)^\s*(?:#.*|output:.*)$", " ", front)
    front = re.sub(r"(?m)^(\s*-?\s*)\w+:", r"\1", front)
    text = front + "\n" + body
    problems = []
    for i, line in enumerate(text.splitlines(), start=1):
        for m in check_pack.RESTRICTED.finditer(line):
            problems.append(f"{name}:{i}: restricted word '{m.group(0)}'")
        for label, pattern in check_pack.BUILD_STATUS:
            for m in pattern.finditer(line):
                problems.append(f"{name}:{i}: {label} '{m.group(0)}'")
        for word in FILLER:
            if re.search(rf"\b{word}\b", line, re.I):
                problems.append(f"{name}:{i}: filler word {word}")
    return problems


def build_doc(d: dict[str, Any], src: Path, pdf: bool, keep_pdf: bool):
    front, lines = load_source(src)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(d, lines))
    return doc.publish(output_path(front, src), pdf=pdf, keep_pdf=keep_pdf)


# ------------------------------------------------------------------------------------------------ workbooks

def build_api_workbook(d: dict[str, Any]) -> Path:
    rows = d["_api"]
    wb = BdoiWorkbook("API Catalogue", doc_type="API catalogue", brd="BRD-00", version=VERSION, date=DATE,
                      subtitle="Every operation of the BIBS backend: user interface, administration and system "
                               "integration")
    wb.legend = [("GET", "Reads data; no change"), ("POST", "Creates a record or runs an action"),
                 ("PUT", "Replaces or updates a record"), ("DELETE", "Removes a record")]
    wb.cover_notes = [
        "Generated from the request mappings of the BIBS backend (method, path, access rule and description of every "
        "operation); the same figures are in the API Specification v1.0.",
        "Paths are relative to the BIBS host name, without the leading slash; path variables are written :name.",
        "The OpenAPI 3 description of the operations is served on developer environments only and exported with "
        "each release (API Specification, chapter 4); SIT, UAT and production do not serve it.",
        "Modules of the insurer-company suite are not part of BIBS (client decision of 8 October 2026).",
    ]
    wb.sheet("API catalogue", [
        Column("context", "Bounded context", 26, "Bounded context of the module (Solution Architecture, chapter 4)"),
        Column("module", "Module", 24, "Backend module that serves the operation"),
        Column("brd", "BRD", 8, "BRD of the bounded context (BRD-00 for platform services)"),
        Column("drop", "Drop", 10, "BDOI drop of the BRD"),
        Column("interface", "Interface", 18, "User interface (prefix api/v1), Administration (api/v1/admin) or "
                                             "System integration (prefix integration, through Apigee X)",
               values=["User interface", "Administration", "System integration (Apigee X)"]),
        Column("method", "Method", 9, "HTTP method", values=["GET", "POST", "PUT", "PATCH", "DELETE"]),
        Column("path", "Path", 46, "Path relative to the host name; :name marks a path variable"),
        Column("purpose", "Purpose", 60, "What the operation does (first sentence of its description)"),
        Column("permission", "Permission", 34, "Permission code(s) the user must hold; 'or' means any of them. "
                                               "System integration operations need the Apigee X token and scopes"),
        Column("company", "Company in request", 11, "Yes when the company or branch is named in the path or the "
                                                     "query; the data scope guard checks it (request bodies are "
                                                     "checked as well and are not marked here)", values=["Yes", "-"]),
    ], rows=rows, description=f"{len(rows):,} operations of the BIBS backend, by bounded context and module")
    per = defaultdict(Counter)
    meta = {}
    for r in rows:
        key = (r["context"], r["module"])
        per[key][r["method"]] += 1
        per[key]["company"] += r["company"] == "Yes"
        meta[key] = r
    summary = []
    for (ctx, mod), k in sorted(per.items()):
        summary.append({"context": ctx, "module": mod, "brd": meta[(ctx, mod)]["brd"],
                        "drop": meta[(ctx, mod)]["drop"],
                        "total": sum(v for m, v in k.items() if m != "company"), "get": k["GET"], "post": k["POST"],
                        "put": k["PUT"], "delete": k["DELETE"] + k["PATCH"], "company": k["company"]})
    wb.sheet("Summary by module", [
        Column("context", "Bounded context", 30, "Bounded context"),
        Column("module", "Module", 30, "Backend module"),
        Column("brd", "BRD", 8, "BRD"),
        Column("drop", "Drop", 10, "BDOI drop"),
        Column("total", "Operations", 11, "Number of operations", kind="number"),
        Column("get", "GET", 8, "Read operations", kind="number"),
        Column("post", "POST", 8, "Create and action operations", kind="number"),
        Column("put", "PUT", 8, "Update operations", kind="number"),
        Column("delete", "DELETE or PATCH", 10, "Delete and partial update operations", kind="number"),
        Column("company", "Company in request", 11, "Operations naming a company or branch in the path or query",
               kind="number"),
    ], rows=summary, description="Operations per module")
    standards = [
        ["Versioning", "Major version in the path (api/v1, integration/v1); additive changes keep the version; a "
                       "breaking change is a new version served next to the old one until every caller has moved"],
        ["Errors", "RFC 7807 problem details with the fields code, detail, correlationId; status 400 validation, "
                   "401 sign-in, 403 permission or data scope, 404 not found, 409 duplicate or concurrent change, "
                   "413 too large, 422 business rule, 429 rate limit, 500 with a reference"],
        ["Pagination", "page (zero-based) and size query parameters, sort=field,asc|desc; the answer carries "
                       "content, page, size, totalElements, totalPages"],
        ["Idempotency", "Reads are safe; creations carry a business reference refused when repeated (409 "
                        "DUPLICATE); updates carry the record version (409 CONCURRENT_MODIFICATION); system "
                        "integration creations carry an Idempotency-Key header"],
        ["Security", "User interface: BIBS access token (15 minutes) and the permission of the operation; "
                     "company and branch checked against the data scope. System integration: Apigee X OAuth 2.0 "
                     "token, issuer, audience and scopes per API"],
        ["Tracing", "X-Correlation-Id on every request and answer; the same identifier is in the log line and in "
                    "the events the request published"],
    ]
    wb.sheet("Standards", [Column("topic", "Standard", 16, "API standard"),
                           Column("rule", "Rule", 110, "The rule every operation follows (API Specification, "
                                                       "chapter 5)")],
             rows=[{"topic": a, "rule": b} for a, b in standards], description="API standards of BIBS")
    path = brand.out_path("BRD-00", KIND, brand.output_name(KIND, "BRD-00", "API Catalogue", VERSION, "xlsx"))
    return wb.save(path)


def build_adr_workbook(d: dict[str, Any]) -> Path:
    wb = BdoiWorkbook("Architecture Decision Log", doc_type="ADR log", brd="BRD-00", version=VERSION, date=DATE,
                      subtitle="One row per architecture decision of BIBS; the full record is in the "
                               "Architecture Decision Records v1.0")
    wb.legend = [("Accepted", "Decided; in force"), ("Proposed", "Recommended by the project; BDOI to decide"),
                 ("Superseded", "Replaced by a later decision")]
    rows = []
    for a in d["adrs"]:
        rows.append({"id": a["id"], "title": a["title"], "area": a["area"], "status": a["status"],
                     "date": a["date"], "by": a["decided_by"], "decision": a["decision"],
                     "alternatives": [f"{x[0]} - {x[1]}" for x in a["alternatives"]],
                     "consequences": [c[2:] for c in a["consequences"]], "sources": a["sources"]})
    wb.sheet("ADR log", [
        Column("id", "ID", 9, "Decision identifier"),
        Column("title", "Decision", 34, "Title of the decision"),
        Column("area", "Area", 16, "Architecture area"),
        Column("status", "Status", 12, "Accepted, Proposed or Superseded", values=["Accepted", "Proposed",
                                                                                    "Superseded"]),
        Column("date", "Date", 12, "Date of the decision (or of this record for a proposal)"),
        Column("by", "Decided by", 26, "Who took the decision, and the open item when BDOI still decides"),
        Column("decision", "What was decided", 70, "The decision"),
        Column("alternatives", "Alternatives considered", 50, "Options considered and why they were not chosen"),
        Column("consequences", "Consequences", 50, "Benefits, costs and risks"),
        Column("sources", "Sources", 30, "Documents the decision is based on"),
    ], rows=rows, description=f"{len(rows)} architecture decisions of BIBS")
    path = brand.out_path("BRD-00", KIND, brand.output_name(KIND, "BRD-00", "ADR Log", VERSION, "xlsx"))
    return wb.save(path)


def workbook_problems(paths: list[Path]) -> list[str]:
    problems = []
    for p in paths:
        for i, line in enumerate(check_pack.office_lines(p)):
            for m in check_pack.RESTRICTED.finditer(line):
                problems.append(f"{p.name}: restricted word '{m.group(0)}' in '{line[:80]}'")
            for label, pattern in check_pack.BUILD_STATUS:
                for m in pattern.finditer(line):
                    problems.append(f"{p.name}: {label} '{m.group(0)}' in '{line[:80]}'")
    return problems


# ------------------------------------------------------------------------------------------------ main

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="checks only")
    ap.add_argument("--previews", action="store_true", help="render page previews of every output")
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF pass of the Word documents")
    ap.add_argument("--only", action="append", help="build only these sources (stem of the .md file)")
    args = ap.parse_args(argv)
    d = load()
    d["_api"] = api_rows(d)
    problems: list[str] = []
    sources = [s for s in SOURCES if not args.only or Path(s).stem in args.only]
    for name in sources:
        tmp = expanded_source(d, HERE / name)
        try:
            problems += [f"{name}: {p}" for p in lint_source(tmp)]
            problems += wording_problems(name, tmp.read_text(encoding="utf-8"))
        finally:
            tmp.unlink(missing_ok=True)
    for p in problems:
        print(p)
    if problems:
        return 1
    print(f"check: {len(d['_api']):,} operations, {len(d['adrs'])} ADRs, {len(d['integrations'])} interfaces, "
          f"{len(sources)} documents - OK")
    if args.check:
        return 0
    import render

    outputs = []
    if not args.only:
        outputs = [build_api_workbook(d), build_adr_workbook(d)]
        bad = workbook_problems(outputs)
        for p in bad:
            print(p)
        if bad:
            return 1
        for x in outputs:
            print(f"xlsx: {x}")
            if args.previews:
                pdf = render.to_pdf(x)
                render.previews(pdf)
                pdf.unlink(missing_ok=True)
    for name in sources:
        docx_path, pdf_path = build_doc(d, HERE / name, pdf=not args.no_pdf, keep_pdf=args.previews)
        print(f"docx: {docx_path}")
        if pdf_path and args.previews:
            print(f"pages: {render.page_count(pdf_path)}")
            render.previews(pdf_path)
            pdf_path.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
