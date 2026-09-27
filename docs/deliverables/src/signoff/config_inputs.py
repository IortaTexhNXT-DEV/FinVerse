"""Configuration inputs the business provides for a BRD release set: the FRS chapter and the input templates (06).

The generic form of the BRD-03 builder (BRD-03_Product_Maintenance/pack/config_inputs.py): the BRD, its name and
the version come from the pack.yaml next to the templates file, so every Drop 0 set uses the same builder.

Usage
  python docs/deliverables/src/signoff/config_inputs.py BRD-11            # template workbook
  python docs/deliverables/src/signoff/config_inputs.py BRD-11 --check    # checks only

What it reads
  * <BRD folder>/pack/config_inputs.yaml: one entry per template, with its owner, source, how it is loaded and its
    columns (header | mandatory | format | what to enter | example);
  * <BRD folder>/pack/pack.yaml for the BRD, the module name, the version and the date of the release set.

What it writes
  * 06_BIBS_Templates_BRD-nn_Configuration_Inputs_v<version>.xlsx in the BRD's release folder (brand.out_dir): a
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
REPO = HERE.parents[3]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402

COLS = ["header", "mandatory", "format", "what", "example"]
BANNED = re.compile(r"\b(" + codecs.decode("qrzb|qhzzl|snxr|fnzcyr qngn|cebgbglcr|cbp|fnaqobk|yberz vcfhz|gbqb|svkzr", "rot13")
                    + r")\b", re.I)


def load(path: Path) -> dict[str, Any]:
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


def pack_meta(folder: Path) -> dict[str, Any]:
    """meta of the pack.yaml next to the templates file (brd, brd_label, name, version, date)."""
    return yaml.safe_load((folder / "pack.yaml").read_text(encoding="utf-8"))["meta"]


def render(doc: Any, render: str, source: str, **_: Any) -> None:  # noqa: A002 - block key
    path = (doc.base_dir / source).resolve()
    data = load(path)
    if render == "summary":
        rows = [[t["id"], t["name"], t["content"], t["owner"], t["load"]] for t in data["templates"]]
        doc.table(["ID", "Template", "Content", "Provided by", "How it is loaded"], rows,
                  widths=[1.2, 3.2, 5.6, 3.4, 5.2], caption=f"Configuration inputs of {pack_meta(path.parent)['name']}",
                  size=7.5, keep_rows=False)
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


def build_workbook(data: dict[str, Any], meta: dict[str, Any]) -> Path:
    from bdoi_xlsx import BdoiWorkbook, Column

    brd, label, name = meta["brd"], meta["brd_label"], meta["name"]
    version, date = str(meta["version"]), str(meta["date"])
    wb = BdoiWorkbook(f"{data['meta']['title']} {label} {name}", doc_type="Configuration input templates",
                      brd=brd, version=version, date=date,
                      subtitle=data["meta"].get("subtitle") or f"What the business provides to set up {name} and to "
                                                                 "load it at go-live")
    wb.cover_notes = [
        "One sheet per template: fill in one row per record from row 6, keep the column headers unchanged.",
        f"Due: {data['meta']['due']}. The example row is fictitious and is deleted before the file is returned.",
    ]
    wb.sheet("How to fill in", [Column("no", "No.", 6, "Step"), Column("text", "How to fill in", 110, "Instruction")], [
        {"no": 1, "text": f"Read the chapter Configuration inputs the business provides of FRS {label} v{version} for the "
                          "purpose of each template and who provides it."},
        {"no": 2, "text": "On each template sheet, replace the example row with the real records, one per row. The "
                          "Columns sheet of each template says what to enter in each column and whether it is mandatory."},
        {"no": 3, "text": data["meta"].get("codes_note") or "Codes must be the codes of the other templates."},
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
    ], data["templates"], description=f"The configuration input templates of {name}")
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
    file = brand.output_name("Templates", brd, data["meta"]["workbook_name"], version, "xlsx")
    return wb.save(brand.out_dir(brd, "Templates") / file)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Build the configuration input templates of a BRD release set")
    ap.add_argument("brd", help="BRD of the set (e.g. BRD-11)")
    ap.add_argument("--check", action="store_true", help="check the templates only")
    args = ap.parse_args(argv)
    folder = brand.src_dir(brand.brd_of_code(args.brd)) / "pack"
    data = load(folder / "config_inputs.yaml")
    problems = check(data)
    for p in problems:
        print(p)
    print(f"{len(data['templates'])} templates, {sum(len(t['rows']) for t in data['templates'])} columns")
    if args.check:
        return 1 if problems else 0
    print(f"xlsx: {build_workbook(data, pack_meta(folder))}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
