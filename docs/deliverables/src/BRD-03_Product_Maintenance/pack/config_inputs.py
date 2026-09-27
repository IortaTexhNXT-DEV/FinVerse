"""Configuration inputs the business provides for BRD-3 Product Maintenance: the FRS chapter and the input templates.

Usage
  python docs/deliverables/src/BRD-03_Product_Maintenance/pack/config_inputs.py            # template workbook
  python docs/deliverables/src/BRD-03_Product_Maintenance/pack/config_inputs.py --check    # checks only

What it reads
  * config_inputs.yaml (this folder): one entry per template, with its owner, source, how it is loaded and its
    columns (header | mandatory | format | what to enter | example);
  * pack.yaml (this folder) for the version and the date of the release set.

What it writes
  * BIBS_Templates_BRD-03_Configuration_Inputs_v<version>.xlsx in the BRD's release folder (brand.out_dir): a
    "How to fill in" sheet, a summary sheet and one sheet per template with the template columns and one fictitious
    example row, and a second sheet per template with the description of each column;
  * the FRS chapter through ```pack blocks: render(doc, render="summary" | "templates", source=<config_inputs.yaml>).
"""

from __future__ import annotations

import argparse
import codecs
import re
import sys
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402

COLS = ["header", "mandatory", "format", "what", "example"]
BANNED = re.compile(r"\b(" + codecs.decode("qrzb|qhzzl|snxr|fnzcyr qngn|cebgbglcr|cbp|fnaqobk|yberz vcfhz|gbqb|svkzr", "rot13")
                    + r")\b", re.I)


def load(path: Path | None = None) -> dict[str, Any]:
    path = path or HERE / "config_inputs.yaml"
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    for t in data["templates"]:
        rows = []
        for i, raw in enumerate(t["columns"], start=1):
            parts = [p.strip() for p in str(raw).split(" | ")]
            if len(parts) != len(COLS):
                raise ValueError(f"{t['id']} column {i}: {len(parts)} cells, expected {len(COLS)}: {raw}")
            rows.append(dict(zip(COLS, parts), no=i))
        t["rows"] = rows
    return data


def check(data: dict[str, Any]) -> list[str]:
    problems = []
    ids = [t["id"] for t in data["templates"]]
    if len(ids) != len(set(ids)):
        problems.append("duplicate template ids")
    for t in data["templates"]:
        texts = [t["name"], t["content"], t["owner"], t["source"], t["load"]]
        texts += [" ".join(str(v) for v in r.values()) for r in t["rows"]]
        for text in texts:
            if BANNED.search(text):
                problems.append(f"{t['id']}: banned word in '{text[:60]}'")
    return problems


# ============================================================================ Word rendering (```pack blocks)


def render(doc: Any, render: str, source: str, **_: Any) -> None:  # noqa: A002 - block key
    data = load((doc.base_dir / source).resolve())
    if render == "summary":
        rows = [[t["id"], t["name"], t["content"], t["owner"], t["load"]] for t in data["templates"]]
        doc.table(["ID", "Template", "Content", "Provided by", "How it is loaded"], rows,
                  widths=[1.2, 3.2, 5.6, 3.4, 5.2], caption="Configuration inputs of Product Maintenance", size=7.5,
                  keep_rows=False)
        return
    if render == "templates":
        for t in data["templates"]:
            doc.heading(f"{t['id']} {t['name']}", level=2)
            doc.key_values([("Content", t["content"]), ("Provided by", t["owner"]), ("Source", t["source"]),
                            ("How it is loaded", t["load"]), ("FRs", ", ".join(t.get("frs") or []))],
                           columns=1, label_width=3.2)
            rows = [[r["no"], r["header"], r["mandatory"], r["format"], r["what"], r["example"]] for r in t["rows"]]
            doc.table(["No.", "Column", "Mand.", "Format", "What to enter", "Example (fictitious)"], rows,
                      widths=[0.8, 3.4, 1.8, 3.6, 5.0, 3.0], caption=f"Columns of the template {t['name']}", size=7.5,
                      keep_rows=False)
        return
    raise ValueError(f"unknown render {render}")


# ============================================================================ workbook


def build_workbook(data: dict[str, Any], version: str, date: str) -> Path:
    from bdoi_xlsx import BdoiWorkbook, Column

    wb = BdoiWorkbook(f"{data['meta']['title']} BRD-3 Product Maintenance", doc_type="Configuration input templates",
                      brd="BRD-03", version=version, date=date,
                      subtitle="What the business provides to set up Product Maintenance and to load it at go-live")
    wb.cover_notes = [
        "One sheet per template: fill in one row per record from row 6, keep the column headers unchanged.",
        f"Due: {data['meta']['due']}. The example row is fictitious and is deleted before the file is returned.",
    ]
    wb.sheet("How to fill in", [Column("no", "No.", 6, "Step"), Column("text", "How to fill in", 110, "Instruction")], [
        {"no": 1, "text": "Read the chapter Configuration inputs the business provides of FRS BRD-3 v2.0 for the purpose of "
                          "each template and who provides it."},
        {"no": 2, "text": "On each template sheet, replace the example row with the real records, one per row. The "
                          "Columns sheet of each template says what to enter in each column and whether it is mandatory."},
        {"no": 3, "text": "Codes must be the codes of the other templates (for example the risk codes of Product "
                          "catalogue in Packages in force and in Package map)."},
        {"no": 4, "text": "Return the workbook to the iorta TechNXT project team. Templates loaded by the data "
                          "migration follow its mocks and the map freeze; the others are entered on the screens and "
                          "authorised by a second user."},
    ], description="How the business fills in the configuration input templates", freeze_first_column=False)
    wb.sheet("Summary", [
        Column("id", "ID", 8, "Template identifier"),
        Column("name", "Template", 30, "Template name"),
        Column("content", "Content", 60, "What the template holds"),
        Column("owner", "Provided by", 34, "BDOI owner"),
        Column("source", "Source", 34, "Where the content comes from"),
        Column("load", "How it is loaded", 60, "Data migration object or screen"),
    ], data["templates"], description="The configuration input templates of Product Maintenance")
    for t in data["templates"]:
        cols = [Column(f"c{r['no']}", r["header"], max(14, min(40, len(r["header"]) + 6)), r["what"]) for r in t["rows"]]
        example = {f"c{r['no']}": ("" if r["example"] == "-" else r["example"]) for r in t["rows"]}
        wb.sheet(f"{t['id']} {t.get('sheet', t['name'])}"[:31], cols, [example], description=f"{t['name']}: {t['content']}"[:250])
        wb.sheet(f"{t['id']} Columns", [
            Column("no", "No.", 6, "Column order"),
            Column("header", "Column", 32, "Header of the template"),
            Column("mandatory", "Mandatory", 18, "Y, N or the condition"),
            Column("format", "Format", 40, "Length, format or allowed values"),
            Column("what", "What to enter", 60, "Content of the column"),
            Column("example", "Example (fictitious)", 30, "Example value"),
        ], t["rows"], description=f"Columns of the template {t['name']}")
    name = brand.output_name("Templates", "BRD-03", data["meta"]["workbook_name"], version, "xlsx")
    return wb.save(brand.out_dir("BRD-03", "Templates") / name)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Build the configuration input templates of BRD-3 Product Maintenance")
    ap.add_argument("--check", action="store_true", help="check the templates only")
    args = ap.parse_args(argv)
    data = load()
    problems = check(data)
    for p in problems:
        print(p)
    print(f"{len(data['templates'])} templates, {sum(len(t['rows']) for t in data['templates'])} columns")
    if args.check:
        return 1 if problems else 0
    pack = yaml.safe_load((HERE / "pack.yaml").read_text(encoding="utf-8"))["meta"]
    print(f"xlsx: {build_workbook(data, str(pack['version']), str(pack['date']))}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
