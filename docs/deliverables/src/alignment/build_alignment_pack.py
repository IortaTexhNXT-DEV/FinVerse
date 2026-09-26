"""Builds the programme alignment document and the integration inventory workbook (BRD-00).

Usage
  python docs/deliverables/src/alignment/build_alignment_pack.py --check      # checks only
  python docs/deliverables/src/alignment/build_alignment_pack.py              # build both files
  python docs/deliverables/src/alignment/build_alignment_pack.py --previews   # also page previews

Inputs (this folder)
  * alignment_data.yaml       drops, drop-to-document map, integrations, infrastructure, S3 changes,
                              Kubernetes sizing, timeline, concept-paper mapping, open questions (IQ##);
  * PROGRAMME_ALIGNMENT.md    Word source in the bdoi_docx format; lines <!-- al:<name> --> are replaced by
                              tables built from the YAML file (list in placeholders() below);
  * figures/*.dot             drop map, integration context, application and deployment diagrams.

Outputs (docs/deliverables/out/Alignment)
  * BIBS_Alignment_BRD-00_Drops_Integrations_Infrastructure_v<version>.docx
  * BIBS_Alignment_BRD-00_Integration_Inventory_v<version>.xlsx
  * IER/BIBS_IER_Application_Architecture.png and IER/BIBS_IER_Infrastructure_Deployment.png, for the
    "Architecture Diagram" and "Architecture Diagram (Infra)" sheets of the BDOI IER workbook.
"""

from __future__ import annotations

import argparse
import re
import shlex
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_docx import BdoiDocument, lint_source, load_source, meta_from, output_path, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

OUT = brand.OUT_DIR / "Alignment"
DOC = "PROGRAMME_ALIGNMENT.md"
PLACEHOLDER = re.compile(r"^<!--\s*al:(\w+)\s*(.*?)-->\s*$")
FITS = ["FIT", "CONFIGURE", "CHANGE", "NEW", "OUT"]
IER_FIGURES = {"al_application_architecture.dot": "BIBS_IER_Application_Architecture.png",
               "al_deployment.dot": "BIBS_IER_Infrastructure_Deployment.png"}
FILLER = ("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art", "best-in-class",
          "world-class")


def load() -> dict[str, Any]:
    return yaml.safe_load((HERE / "alignment_data.yaml").read_text(encoding="utf-8"))


# ------------------------------------------------------------------------------------------------ checks

def check(d: dict[str, Any]) -> list[str]:
    problems: list[str] = []
    qids = [q[0] for q in d["questions"]]
    if len(qids) != len(set(qids)):
        problems.append("duplicate IQ ids")
    for i, q in enumerate(d["questions"], start=1):
        if q[0] != f"IQ{i:02d}":
            problems.append(f"question {q[0]} out of sequence (expected IQ{i:02d})")
        if len(q) != 5:
            problems.append(f"{q[0]}: {len(q)} cells, expected 5")
    ids = [x["id"] for x in d["integrations"]]
    if len(ids) != len(set(ids)):
        problems.append("duplicate integration ids")
    for x in d["integrations"]:
        if x["fit"] not in FITS:
            problems.append(f"{x['id']}: fit {x['fit']}")
        for ref in re.findall(r"IQ\d+", str(x.get("questions", ""))):
            if ref not in qids:
                problems.append(f"{x['id']}: unknown question {ref}")
        for key in ("name", "drop", "bdoi", "understanding", "direction", "data", "module", "seam", "build"):
            if not x.get(key):
                problems.append(f"{x['id']}: missing {key}")
    for row in d["drop_items"]:
        for ref in re.findall(r"IQ\d+", row["refs"]):
            if ref not in qids:
                problems.append(f"drop item {row['num']}: unknown question {ref}")
    for table, width in (("s3_changes", 3), ("k8s", 7), ("timeline", 4), ("concept", 4), ("other_interfaces", 4)):
        for row in d[table]:
            if len(row) != width:
                problems.append(f"{table}: row with {len(row)} cells, expected {width}: {row[0]}")
    return problems


# ------------------------------------------------------------------------------------------------ Word placeholders

def md_table(headers: list[str], rows: list[list[Any]], opts: str) -> list[str]:
    def cell(v: Any) -> str:
        return str(v if v is not None else "").replace("|", "/").replace("\n", " ").strip() or "-"
    out = [f"<!-- table: {opts} -->", "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(cell(v) for v in r) + " |" for r in rows]
    return out + [""]


def placeholders(d: dict[str, Any], name: str, opts: dict[str, str]) -> list[str]:
    if name == "drop_items":
        drop = opts.get("drop", "").replace("_", " ")
        rows = [[r["num"], r["item"], r["bdoi_brd"], r["brd"], r["frs"], r["bibs"], r["status"], r["gap"], r["refs"]]
                for r in d["drop_items"] if not drop or r["drop"] == drop]
        return md_table(["#", "Drop item", "BRD named by BDOI", "BIBS BRD", "FRS", "BIBS modules and screens",
                         "Status", "Gap or mismatch", "Refs"], rows,
                        f'widths=0.9,2.2,2.2,2.2,1.9,3.2,1.3,4.2,1.5 caption="{opts.get("caption", "Drop items")}" '
                        'status=Status size=7.5 bold=first')
    if name == "documents":
        rows = [[r["doc"], r["kind"], r["drop"], r["shared"], r["note"]] for r in d["documents"]]
        return md_table(["Document", "Kind", "Drop (folder)", "Shared with", "Note"], rows,
                        'widths=4.2,3.6,2.2,4,3.6 caption="Drop-to-document map" size=8 bold=first')
    if name == "integrations":
        rows = [[x["id"], x["name"], x["drop"], x["bdoi"], x["direction"], x["module"], x["fit"], x["questions"]]
                for x in d["integrations"]]
        return md_table(["ID", "Integration", "Drop", "BDOI meaning", "Direction", "BIBS module", "Fit", "Questions"],
                        rows, 'widths=1.2,2.6,2,4.2,2.6,3.4,2,1.4 caption="Integration inventory (summary)" '
                              'status=Fit size=7.5 bold=first')
    if name == "integration_detail":
        rows = [[x["id"], x["name"], x["understanding"], x["data"], x["seam"], x["build"], x["open"]]
                for x in d["integrations"]]
        return md_table(["ID", "Integration", "Our understanding (to confirm)", "Data", "Existing seam in BIBS",
                         "Build needed", "Open"], rows,
                        'widths=1.1,2,4.6,3,4.4,4,2.6 caption="Integration inventory (detail)" size=7 bold=first')
    if name == "other_interfaces":
        return md_table(["Interface", "Named in", "BIBS seam", "To confirm"], d["other_interfaces"],
                        'widths=4,4,5,4.6 caption="Interfaces named in the BRDs but not on the slide" size=8 '
                        'bold=first')
    if name == "infra":
        rows = [[r["topic"], r["ier"], r["bibs"], r["assessment"], r["change"], r["ref"]] for r in d["infra"]]
        return md_table(["Topic", "IER (BDOI IT)", "BIBS as built", "Assessment", "Change needed", "Ref"], rows,
                        'widths=2.2,6,5.6,1.8,5.4,1.8 caption="IER against BIBS" status=Assessment size=7 '
                        'bold=first')
    if name == "s3":
        return md_table(["Area", "Today", "Change"], d["s3_changes"],
                        'widths=4.4,5.2,8 caption="Documents in S3 only: changes in BIBS" size=7.5 bold=first')
    if name == "k8s":
        return md_table(["Environment", "Deployment", "Min", "Max", "Requests CPU / memory", "Limits CPU / memory",
                         "Note"], d["k8s"],
                        'widths=2,3.2,1,1,2.6,2.6,5.2 caption="Proposed Kubernetes sizing of the BIBS workloads" '
                        'size=8 bold=first')
    if name == "timeline":
        return md_table(["Stream", "BDOI window", "Status today", "What we deliver, by when"], d["timeline"],
                        'widths=3.4,3.4,5.2,7.6 caption="BDOI timeline against the BIBS plan" size=7.5 bold=first')
    if name == "concept":
        return md_table(["Concept paper capability", "BIBS design", "Status", "Note"], d["concept"],
                        'widths=4.4,6.6,2.4,4.2 caption="Concept paper capabilities against the BIBS designs" '
                        'size=8 bold=first')
    if name == "questions":
        rows = sorted(d["questions"], key=lambda q: (q[1], q[0]))
        return md_table(["ID", "Prio.", "Topic", "Question", "Source"],
                        [[q[0], q[1], q[2], q[3], q[4]] for q in rows],
                        'widths=1.1,1.2,2.6,10,3 caption="Open questions for BDOI, by priority" size=7.5 '
                        'bold=first')
    raise SystemExit(f"unknown placeholder al:{name}")


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
    lines = expand(d, body.splitlines())
    tmp = src.with_name(f".{src.stem}.expanded.md")
    tmp.write_text("---" + front_text + "---\n" + "\n".join(lines) + "\n", encoding="utf-8")
    return tmp


def build_doc(d: dict[str, Any], src: Path, pdf: bool, keep_pdf: bool):
    front, lines = load_source(src)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(d, lines))
    return doc.publish(output_path(front, src), pdf=pdf, keep_pdf=keep_pdf)


# ------------------------------------------------------------------------------------------------ workbook

def build_workbook(d: dict[str, Any]) -> Path:
    m = d["meta"]
    wb = BdoiWorkbook("BIBS Integration Inventory", doc_type="Integration inventory", brd="BRD-00",
                      version=m["version"], date=m["date"],
                      subtitle="Drops, integrations and infrastructure: BDOI plan against BIBS as built")
    wb.legend = [("FIT", "Works today"), ("CONFIGURE", "Set-up only (layout, parameters)"),
                 ("CHANGE", "Extends an existing seam or module"), ("NEW", "New build"),
                 ("OUT", "Out of scope"), ("BUILT", "BRD built in BIBS"), ("GAP", "Missing in the IER or the plan")]
    wb.cover_notes = [
        "Integration meanings are those given by BDOI on 26-Sep-2026 (answer A2); 'Our understanding' is the "
        "project team's reading and is confirmed through the IQ questions.",
        "Documents and attachments live in Amazon S3 only (answer A4); the sheet 'S3 changes' lists the build "
        "item.",
        "Sources: BDOI drop plan and timeline slides, IER workbook v20, concept paper (superseded), "
        "docs/architecture/PROGRAMME_ALIGNMENT.md.",
    ]
    wb.sheet("Integrations", [
        Column("id", "ID", 8, "Integration identifier INT-nn"),
        Column("name", "Integration", 18, "Name used by BDOI"),
        Column("drop", "Drop", 13, "Drop of the BDOI plan"),
        Column("bdoi", "BDOI meaning", 28, "Meaning given by BDOI (answer A2)"),
        Column("understanding", "Our understanding (to confirm)", 42, "What the interface does, as the project "
                                                                          "team reads it"),
        Column("direction", "Direction", 18, "Inbound to BIBS, outbound, both, manual"),
        Column("data", "Data", 30, "Main data exchanged"),
        Column("module", "BIBS module", 24, "Module(s) that own the interface"),
        Column("seam", "Existing seam in BIBS", 42, "Port, handler or mechanism in the code today"),
        Column("fit", "Fit", 11, "FIT / CONFIGURE / CHANGE / NEW / OUT", values=FITS, status=True),
        Column("build", "Build needed", 40, "What must be built or configured"),
        Column("open", "Open points", 30, "What BDOI IT still has to specify"),
        Column("questions", "Questions", 10, "IQ questions of this document"),
    ], d["integrations"], description="One row per integration named on the drop plan or by BDOI")
    wb.sheet("Other interfaces", [
        Column("a", "Interface", 34, "Interface named in a BRD"), Column("b", "Named in", 30, "BRD and requirement"),
        Column("c", "BIBS seam", 38, "Mechanism in BIBS"), Column("d", "To confirm", 34, "What BDOI confirms"),
    ], [dict(zip("abcd", r)) for r in d["other_interfaces"]],
        description="Interfaces named in the BRDs that the drop plan does not list")
    wb.sheet("Drop map", [
        Column("drop", "Drop", 14, "Drop of the BDOI plan"), Column("num", "#", 6, "Line item"),
        Column("item", "Drop item", 26, "Module / functionality on the slide"),
        Column("bdoi_brd", "BRD named by BDOI", 26, "BRD file column of the slide"),
        Column("brd", "BIBS BRD", 28, "BIBS BRD number(s)"), Column("frs", "FRS", 24, "FRS and requirement prefix"),
        Column("bibs", "BIBS modules and screens", 40, "Where it is in BIBS"),
        Column("status", "Status", 12, "Built / Being built / Designed / Mixed", status=True),
        Column("gap", "Gap or mismatch", 50, "Observation"), Column("refs", "Refs", 18, "Questions and register"),
    ], d["drop_items"], description="Every line item of the drop plan mapped to BRDs, FRS and BIBS")
    wb.sheet("Documents by drop", [
        Column("doc", "Document", 38, "BRD or collateral"), Column("kind", "Kind", 30, "FRS, test plan, ..."),
        Column("file", "File", 40, "Output file(s)"), Column("drop", "Drop (folder)", 16, "Folder of the restructure"),
        Column("shared", "Shared with", 34, "Other drops that use it"), Column("note", "Note", 34, "Note"),
    ], d["documents"], description="Drop-to-document map for the folder restructure (answer A5)")
    wb.sheet("Infrastructure", [
        Column("topic", "Topic", 20, "Area"), Column("ier", "IER (BDOI IT)", 50, "What the IER workbook states"),
        Column("bibs", "BIBS as built", 46, "Code, deployment files, architecture documents"),
        Column("assessment", "Assessment", 14, "Aligned / Mismatch / Gap", status=True),
        Column("change", "Change needed", 46, "Change in the IER or in BIBS"), Column("ref", "Ref", 14, "Register, IQ"),
    ], d["infra"], description="IER workbook v20 against the BIBS architecture and deployment")
    wb.sheet("S3 changes", [
        Column("a", "Area", 40, "What is stored"), Column("b", "Today", 44, "Tables and classes"),
        Column("c", "Change", 70, "Change for S3-only storage (answer A4)"),
    ], [dict(zip("abc", r)) for r in d["s3_changes"]], description="Build item: documents and files in Amazon S3")
    wb.sheet("K8s sizing", [
        Column("a", "Environment", 12, "Environment"), Column("b", "Deployment", 22, "Kubernetes deployment"),
        Column("c", "Min replicas", 10, "HPA minimum"), Column("d", "Max replicas", 10, "HPA maximum"),
        Column("e", "Requests CPU / memory", 18, "Per pod"), Column("f", "Limits CPU / memory", 18, "Per pod"),
        Column("g", "Note", 50, "Note"),
    ], [dict(zip("abcdefg", r)) for r in d["k8s"]],
        description="Replaces the bv-* rows of the IER K8s sheets (to be proved by the performance test)")
    wb.sheet("Timeline", [
        Column("a", "Stream", 30, "Timeline bar"), Column("b", "BDOI window", 30, "From the slide"),
        Column("c", "Status today", 44, "BIBS status"), Column("d", "What we deliver, by when", 60, "Plan"),
    ], [dict(zip("abcd", r)) for r in d["timeline"]], description="BDOI timeline against the BIBS plan")
    wb.sheet("Questions", [
        Column("a", "ID", 7, "IQ question"), Column("b", "Priority", 8, "1 = answer by 16 Oct 2026"),
        Column("c", "Topic", 24, "Topic"), Column("d", "Question", 90, "Question to BDOI"),
        Column("e", "Source", 30, "Where it comes from"),
        Column("f", "Status", 12, "Open / Answered", values=["OPEN", "ANSWERED", "CLOSED"], status=True),
        Column("g", "BDOI answer", 40, "Filled by BDOI"),
    ], [dict(zip("abcdefg", [*q, "OPEN", ""])) for q in d["questions"]],
        description="Open questions IQ01-IQ35 (programme, integrations, infrastructure)")
    OUT.mkdir(parents=True, exist_ok=True)
    return wb.save(OUT / brand.output_name("Alignment", "BRD-00", "Integration Inventory", m["version"], "xlsx"))


# ------------------------------------------------------------------------------------------------ IER diagrams

def ier_pngs() -> list[Path]:
    """Renders the two IER diagrams at 300 dpi into out/Alignment/IER (for pasting into the IER workbook)."""
    dot = shutil.which("dot")
    target = OUT / "IER"
    target.mkdir(parents=True, exist_ok=True)
    out: list[Path] = []
    for src, name in IER_FIGURES.items():
        text = (HERE / "figures" / src).read_text(encoding="utf-8")
        for token in sorted(brand.DOT_TOKENS, key=len, reverse=True):
            text = text.replace(token, brand.DOT_TOKENS[token])
        path = target / name
        if dot:
            subprocess.run([dot, "-Tpng", "-Gdpi=300", "-o", str(path)], input=text.encode("utf-8"), check=True)
        out.append(path)
    return out


# ------------------------------------------------------------------------------------------------ main

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="checks only")
    ap.add_argument("--previews", action="store_true", help="render page previews of every output")
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF pass of the Word document")
    args = ap.parse_args(argv)
    d = load()
    problems = check(d)
    tmp = expanded_source(d, HERE / DOC)
    try:
        problems += [f"{DOC}: {p}" for p in lint_source(tmp)]
        text = tmp.read_text(encoding="utf-8")
        for word in FILLER:
            if re.search(rf"\b{word}\b", text, re.I):
                problems.append(f"{DOC}: filler word {word}")
    finally:
        tmp.unlink(missing_ok=True)
    for p in problems:
        print(p)
    if problems:
        return 1
    print(f"check: {len(d['drop_items'])} drop items, {len(d['documents'])} documents, {len(d['integrations'])} "
          f"integrations, {len(d['infra'])} infrastructure rows, {len(d['questions'])} questions - OK")
    if args.check:
        return 0
    import render

    xlsx = build_workbook(d)
    print(f"xlsx: {xlsx}")
    for p in ier_pngs():
        print(f"png: {p}")
    if args.previews:
        pdf = render.to_pdf(xlsx)
        render.previews(pdf)
        pdf.unlink(missing_ok=True)
    docx_path, pdf_path = build_doc(d, HERE / DOC, pdf=not args.no_pdf, keep_pdf=args.previews)
    print(f"docx: {docx_path}")
    if pdf_path and args.previews:
        print(f"pages: {render.page_count(pdf_path)}")
        render.previews(pdf_path)
        pdf_path.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
