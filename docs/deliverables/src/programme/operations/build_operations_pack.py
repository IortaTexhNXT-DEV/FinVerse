"""Builds the operations and go-live documents of BIBS (programme level, BRD-00).

    python docs/deliverables/src/programme/operations/build_operations_pack.py            # all documents
    python docs/deliverables/src/programme/operations/build_operations_pack.py --check    # checks only
    python docs/deliverables/src/programme/operations/build_operations_pack.py --previews # with page previews
    python docs/deliverables/src/programme/operations/build_operations_pack.py --only SUPPORT_MODEL.md

Outputs (brand.out_path("BRD-00", kind, file)): Programme/Operations/ (support model, runbook and observability,
DR and BCP, production readiness checklist) and Programme/Go_Live/ (go-live, cutover and data migration plan;
hypercare, warranty and sign-off plan).

The Markdown sources carry <!-- op:NAME key=value --> markers that are expanded here:
  op:jobs          the scheduled jobs catalogue (ops_data.yaml, read from the job registry of the backend)
  op:exceptions    the application exception codes with their thresholds (the platform reference data) and
                   responders (ops_data.yaml)
  op:cut_roles, op:cut_hours, op:cut_rollback, op:cut_comms, op:cut_checkpoints, op:cut_freeze
                   the production cutover plan of the Data Migration Handbook (BRD-13 pack/cutover.yaml), so that
                   the go-live plan and the handbook stay one plan
  op:tech_cutover  the technical cutover tasks of this plan (golive_data.yaml)
  op:readiness     the summary of the production readiness checklist (readiness.yaml)
"""

from __future__ import annotations

import argparse
import re
import shlex
import sys
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
# Appended, so that a toolkit given on PYTHONPATH takes precedence.
sys.path.append(str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_docx import BdoiDocument, lint_source, load_source, meta_from, output_path, render_body  # noqa: E402

CUTOVER = brand.SRC_DIR / "BRD-13_Data_Migration" / "pack" / "cutover.yaml"
DOCS = ["SUPPORT_MODEL.md", "OPERATIONS_RUNBOOK.md", "DR_BCP_PLAN.md", "GO_LIVE_CUTOVER_PLAN.md",
        "HYPERCARE_WARRANTY_PLAN.md"]
PLACEHOLDER = re.compile(r"^<!--\s*op:(\w+)\s*(.*?)-->\s*$")
FILLER = ("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art", "best-in-class",
          "world-class", "TBD", "lorem")
# A requirement reference at the end of a reference-data description, e.g. "(CSHID.015)" or "(BRCLXN.001-015)".
REQ_REF = re.compile(r"\s*\((?=[^)]*(?:\d|_|\b[A-Z]{2,}[A-Z0-9]*\b))[A-Z][^()]*\)\s*\.?\s*$")


def load_yaml(name: str) -> Any:
    return yaml.safe_load((HERE / name).read_text(encoding="utf-8"))


def data() -> dict[str, Any]:
    d = {"ops": load_yaml("ops_data.yaml"), "golive": load_yaml("golive_data.yaml"),
         "readiness": load_yaml("readiness.yaml"),
         "cutover": yaml.safe_load(CUTOVER.read_text(encoding="utf-8"))}
    return d


# ------------------------------------------------------------------------------------------------ tables

def cell(v: Any) -> str:
    return str("" if v is None else v).replace("|", "/").replace("\n", " ").strip()


def md_table(headers: list[str], rows: list[list[Any]], opts: str) -> list[str]:
    out = [f"<!-- table: {opts} -->", "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(cell(c) for c in r) + " |" for r in rows]
    return out + [""]


def exception_rows(d: dict[str, Any]) -> list[list[Any]]:
    import sql_facts  # noqa: PLC0415  (reference rows of the platform, read without a database)
    owners = d["ops"]["exception_owners"]
    excluded = set(d["ops"]["exception_excluded_modules"])
    rows = []
    for r in sql_facts.rows("alt_exception_code"):
        module = r.get("module") or ""
        if module in excluded or str(r.get("code", "")).startswith("RI_"):
            continue
        threshold = []
        if r.get("threshold_amount") not in (None, ""):
            threshold.append(f"PHP {float(r['threshold_amount']):,.2f}")
        if r.get("threshold_days") not in (None, ""):
            threshold.append(f"{r['threshold_days']} days")
        desc = REQ_REF.sub("", str(r.get("description") or "")).strip()
        l2, owner = owners.get(module, ["L2 Platform", "BIBS Service Owner"])
        rows.append([r["code"], r.get("name"), desc, (r.get("severity") or "").title(),
                     " / ".join(threshold) or "-", f"{l2}; {owner}"])
    return rows


def placeholders(d: dict[str, Any], name: str, opts: dict[str, str]) -> list[str]:
    cut = d["cutover"]
    if name == "jobs":
        rows = [[j, area, sched, f"bibs-{wl}", what, cls, fail]
                for j, area, sched, wl, what, cls, fail in d["ops"]["jobs"]]
        if opts.get("class"):
            rows = [r for r in rows if r[5] in opts["class"].split(",")]
        return md_table(["Job", "Area", "Schedule (PHT)", "Runs on", "What it does", "Class", "When it fails"],
                        rows, 'widths=4.2,2.8,3,2.3,6.6,1.1,5.4 caption="' + opts.get("caption", "Scheduled jobs")
                        + '" size=7.5')
    if name == "exceptions":
        return md_table(["Code", "Alert", "Raised when", "Severity", "Threshold", "Responder; business owner"],
                        exception_rows(d), 'widths=4.4,3.6,7.6,1.8,2.2,5.2 caption="Application exception codes '
                        'and their responders" size=7.5')
    if name == "cut_roles":
        return md_table(["Code", "Role", "Organisation"], [list(r) for r in cut["roles"]],
                        'widths=1.6,11,4 caption="Cutover roles (Data Migration Handbook, Part D)" bold=first '
                        'size=8.5')
    if name == "cut_hours":
        phases = opts.get("phases", "C,D,E,F").split(",")
        rows = []
        for t in cut["tasks"]:
            tid, phase, day, time, task, owner, hours, after, verify, chk = t
            if phase not in phases:
                continue
            when = f"{day} {time}".strip()
            rows.append([when, tid, task, owner, hours, after or "-", verify,
                         chk or "-"] + (["", ""] if opts.get("template") else []))
        headers = ["When", "Task", "What", "Owner", "Hours", "After", "Evidence", "Gate"]
        widths = "1.9,1.5,8,1.4,1.3,2,5,1.8"
        if opts.get("template"):
            headers += ["Actual start / end", "Done by"]
            widths += ",2.2,1.8"
        return md_table(headers, rows, f'widths={widths} caption="' + opts.get("caption", "Cutover tasks")
                        + '" size=7.5')
    if name == "cut_rollback":
        # RB-4 of the BRD-13 source names CT-037 (the last client delta) as the rollback point; the rollback point
        # is the snapshot of CT-040 (Part D, Rollback). Shown as CT-040 here; the source is to be corrected.
        rows = [[i, step, what.replace("rollback-point backup (CT-037)", "rollback-point backup (CT-040)"), who]
                for i, step, what, who in cut["rollback"]]
        return md_table(["Step", "Action", "What is done", "Owner"], rows,
                        'widths=1.3,2.4,11.4,1.5 caption="Rollback steps (until T 18:00)" bold=first size=8.5')
    if name == "cut_comms":
        rows = [[n, what, who, how, when, own] for n, what, who, how, when, own in cut["communication"]]
        return md_table(["#", "Message", "Audience", "Channel", "When", "Owner"], rows,
                        'widths=0.7,6,3.8,2.8,1.8,1.4 caption="Cutover communication plan" size=8')
    if name == "cut_checkpoints":
        out: list[str] = []
        for c in cut["checkpoints"]:
            out += [f"#### {c['id']} - {c['when']}: {c['decision']}", ""]
            out += [f"- {x}" for x in c["criteria"]] + [""]
        return out
    if name == "cut_freeze":
        return md_table(["Object", "Freeze from", "Until", "What is frozen", "Exception route"],
                        [list(r) for r in cut["freeze_windows"]],
                        'widths=2.6,2.6,2.2,5.2,4 caption="Freeze windows" bold=first size=8')
    if name == "hc_roster":
        return md_table(["Role", "Who", "Hours", "Reports to"], [list(r) for r in cut["hypercare_roster"]],
                        'widths=3.4,5.4,5,2.8 caption="Hypercare roster (Data Migration Handbook, Part D)" bold=first '
                        'size=8.5')
    if name == "hc_exit":
        rows = [[f"HX-{i:02d}", x, "Data Migration Handbook, Part D"] for i, x in enumerate(cut["hypercare_exit"], 1)]
        return md_table(["Ref", "Criterion", "Source"], rows,
                        'widths=1.4,11.6,3.6 caption="Exit criteria of the cutover plan" bold=first size=8.5')
    if name == "tech_cutover":
        rows = [list(r) + (["", ""] if opts.get("template") else []) for r in d["golive"]["tech_cutover"]]
        headers = ["When", "Task", "What", "Owner", "Hours", "Evidence"]
        widths = "1.9,1.5,7.6,1.4,1.3,5"
        if opts.get("template"):
            headers += ["Actual start / end", "Done by"]
            widths += ",2.2,1.8"
        return md_table(headers, rows, f'widths={widths} caption="Technical cutover tasks" size=7.5')
    if name == "readiness":
        items = d["readiness"]["items"]
        areas: dict[str, int] = {}
        gates: dict[str, int] = {}
        for it in items:
            areas[it["area"]] = areas.get(it["area"], 0) + 1
            gates[it["gate"]] = gates.get(it["gate"], 0) + 1
        rows = [[a, n, ", ".join(sorted({it["owner"] for it in items if it["area"] == a}))]
                for a, n in areas.items()]
        return md_table(["Area", "Criteria", "Owners"], rows + [["Total", len(items), ""]],
                        'widths=3.4,1.6,11.6 caption="Production readiness criteria by area" bold=first size=8.5')
    raise SystemExit(f"unknown placeholder op:{name}")


def expand(d: dict[str, Any], lines: list[str]) -> list[str]:
    out: list[str] = []
    for line in lines:
        m = PLACEHOLDER.match(line)
        if not m:
            out.append(line)
            continue
        opts = dict(part.split("=", 1) for part in shlex.split(m.group(2).strip()))
        out += placeholders(d, m.group(1), opts)
    return out


def expanded_source(d: dict[str, Any], src: Path) -> Path:
    front_text, body = src.read_text(encoding="utf-8").split("---", 2)[1:]
    tmp = src.with_name(f".{src.stem}.expanded.md")
    tmp.write_text("---" + front_text + "---\n" + "\n".join(expand(d, body.splitlines())) + "\n", encoding="utf-8")
    return tmp


def build_doc(d: dict[str, Any], src: Path, pdf: bool, keep_pdf: bool):
    front, lines = load_source(src)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(d, lines))
    return doc.publish(output_path(front, src), pdf=pdf, keep_pdf=keep_pdf)


# ------------------------------------------------------------------------------------------------ checks

def check(d: dict[str, Any], docs: list[str]) -> list[str]:
    problems: list[str] = []
    for job in d["ops"]["jobs"]:
        if len(job) != 7 or job[5] not in ("A", "B", "C"):
            problems.append(f"ops_data.yaml: job row {job[0]} malformed")
    ids = [it["id"] for it in d["readiness"]["items"]]
    if len(ids) != len(set(ids)):
        problems.append("readiness.yaml: duplicate ids")
    for it in d["readiness"]["items"]:
        for key in ("id", "area", "criterion", "measure", "evidence", "owner", "due", "gate", "status"):
            if not it.get(key):
                problems.append(f"readiness.yaml: {it.get('id')} has no {key}")
    readiness_text = "---\n---\n" + "\n".join(str(v) for it in d["readiness"]["items"] for v in it.values())
    problems += [f"readiness.yaml: {p}" for p in client_words(readiness_text)]
    for name in docs:
        tmp = expanded_source(d, HERE / name)
        try:
            problems += [f"{name}: {p}" for p in lint_source(tmp)]
            text = tmp.read_text(encoding="utf-8")
            problems += [f"{name}: {p}" for p in client_words(text)]
            for word in FILLER:
                if re.search(rf"\b{re.escape(word)}\b", text, re.I if word.islower() else 0):
                    problems.append(f"{name}: filler or placeholder word {word}")
        finally:
            tmp.unlink(missing_ok=True)
    return problems


def client_words(text: str) -> list[str]:
    """The restricted words and the development-status wording of check_pack in the client text of an expanded
    source (front matter, comments, figure and directive lines left out), so a build never needs a second pass."""
    import check_pack  # noqa: PLC0415
    body = re.sub(r"<!--.*?-->", " ", text.split("---", 2)[2], flags=re.S)
    out: list[str] = []
    for line in body.splitlines():
        if line.lstrip().startswith(("![", "```")):
            continue
        for m in check_pack.RESTRICTED.finditer(line):
            out.append(f"restricted word {m.group(0)}")
        for label, pattern in check_pack.BUILD_STATUS:
            for m in pattern.finditer(line):
                out.append(f"{label}: {m.group(0).strip()} in: {line.strip()[:80]}")
    return out


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="checks only")
    ap.add_argument("--previews", action="store_true", help="render page previews of every output")
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF pass of the Word documents")
    ap.add_argument("--only", action="append", help="build only this source (repeatable); 'xlsx' for the checklist")
    args = ap.parse_args(argv)
    d = data()
    docs = [x for x in DOCS if not args.only or x in args.only]
    problems = check(d, docs)
    for p in problems:
        print(p)
    if problems:
        return 1
    print(f"check: {len(d['ops']['jobs'])} jobs, {len(d['readiness']['items'])} readiness criteria, "
          f"{len(d['cutover']['tasks'])} cutover tasks - OK")
    if args.check:
        return 0
    import render  # noqa: PLC0415
    from readiness_workbook import build_workbook  # noqa: PLC0415

    if not args.only or "xlsx" in args.only:
        xlsx = build_workbook(d["readiness"])
        print(f"xlsx: {xlsx}")
        if args.previews:
            pdf = render.to_pdf(xlsx)
            render.previews(pdf)
            pdf.unlink(missing_ok=True)
    for name in docs:
        docx_path, pdf_path = build_doc(d, HERE / name, pdf=not args.no_pdf, keep_pdf=args.previews)
        print(f"docx: {docx_path}")
        if pdf_path and args.previews:
            print(f"pages: {render.page_count(pdf_path)}")
            render.previews(pdf_path)
            pdf_path.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    sys.path.insert(0, str(HERE))
    raise SystemExit(main())
