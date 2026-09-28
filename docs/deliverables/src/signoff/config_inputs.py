"""Configuration inputs the business provides for a BRD release set: the FRS chapter and the input templates (06).

One builder for every Drop 0 set (BRD-03 keeps a thin entry point in its pack folder): the BRD, its name and the
version come from the pack.yaml next to the templates file.

Usage
  python docs/deliverables/src/signoff/config_inputs.py BRD-11            # template workbook
  python docs/deliverables/src/signoff/config_inputs.py BRD-11 --check    # checks only (and of the workbook written)

What it reads
  * <BRD folder>/pack/config_inputs.yaml: one entry per template, with its owner, source, how it is loaded, its
    columns (header | mandatory | format | what to enter | example), the check of each column (checks), the
    templates it depends on (depends) and the lists of allowed values (lists);
  * <BRD folder>/pack/pack.yaml and guide.yaml for the BRD, the module name, the version, the date and the owner;
  * Drop-0_Closure/drop0.yaml for the due date of each template (the register item that names it);
  * the platform lists of values and system parameters for the lists read from the platform (lov, lov_types,
    parameter_groups, parameters_from_column).

What it writes
  * 06_BIBS_Templates_BRD-nn_Configuration_Inputs_v<version>.xlsx in the BRD's release folder (brand.out_dir),
    a guided workbook (tools/deliverables/guided_xlsx.py): Start here with the templates in the order they are filled
    in, one self-contained sheet per template (header block, guide band above the column headers, example row, input
    rows with drop-downs), Reference lists, Questions and comments;
  * the FRS chapter through ```pack blocks: render(doc, render="summary" | "templates", source=<config_inputs.yaml>).
"""

from __future__ import annotations

import argparse
import codecs
import datetime as dt
import re
import sys
from functools import lru_cache
from pathlib import Path
from typing import Any, Callable

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402

COLS = ["header", "mandatory", "format", "what", "example"]
BANNED = re.compile(r"\b(" + codecs.decode("qrzb|qhzzl|snxr|fnzcyr qngn|cebgbglcr|cbp|fnaqobk|yberz vcfhz|gbqb|svkzr", "rot13")
                    + r"|json|sql|api)\b", re.I)
CHECK_KINDS = {"yn", "date", "number", "whole", "text", "list", "code", "ref", "many"}
PREFIX = {"BRD-03": "PM", "BRD-11": "UA"}  # template IDs of the sets in the Drop 0 workbook


def load(path: Path) -> dict[str, Any]:
    data = yaml.safe_load(path.read_text(encoding="utf-8"))
    for t in data["templates"]:
        rows = []
        for i, raw in enumerate(t["columns"], start=1):
            parts = [p.strip() for p in str(raw).split(" | ")]
            if len(parts) != len(COLS):
                raise ValueError(f"{t['id']} column {i}: {len(parts)} cells, expected {len(COLS)}: {raw}")
            rows.append(dict(zip(COLS, parts), no=i, check=str((t.get("checks") or {}).get(parts[0], ""))))
        t["rows"] = rows
    data.setdefault("lists", {})
    return data


def depends(t: dict[str, Any]) -> list[str]:
    """Templates whose codes or list values a template uses: its depends and the targets of its code and ref checks."""
    out = list(t.get("depends") or [])
    for r in t["rows"]:
        kind, _, arg = r["check"].partition(":")
        if kind in ("code", "ref"):
            tid = arg.split("/", 1)[0]
            if tid != t["id"] and tid not in out:
                out.append(tid)
    return out


def check(data: dict[str, Any]) -> list[str]:
    problems = []
    ids = [t["id"] for t in data["templates"]]
    by_id = {t["id"]: t for t in data["templates"]}
    if len(ids) != len(set(ids)):
        problems.append("duplicate template ids")
    for t in data["templates"]:
        texts = [t["name"], t["content"], t["owner"], t["source"], t["load"]]
        texts += [" ".join(str(v) for v in r.values()) for r in t["rows"]]
        for text in texts:
            if BANNED.search(text):
                problems.append(f"{t['id']}: banned word '{BANNED.search(text).group(0)}' in '{text[:60]}'")
        headers = [r["header"] for r in t["rows"]]
        if len(headers) != len(set(headers)):
            problems.append(f"{t['id']}: two columns with the same header")
        for h in t.get("checks") or {}:
            if h not in headers:
                problems.append(f"{t['id']}: check of an unknown column {h}")
        for d in t.get("depends") or []:
            if d not in by_id:
                problems.append(f"{t['id']}: depends on the unknown template {d}")
        for r in t["rows"]:
            kind, _, arg = r["check"].partition(":")
            if not r["check"]:
                continue
            if kind not in CHECK_KINDS:
                problems.append(f"{t['id']} {r['header']}: unknown check {r['check']}")
            elif kind in ("list", "many") and arg not in data["lists"]:
                problems.append(f"{t['id']} {r['header']}: unknown list {arg}")
            elif kind in ("code", "ref"):
                tid, _, col = arg.partition("/")
                if tid not in by_id or col not in [x["header"] for x in by_id[tid]["rows"]]:
                    problems.append(f"{t['id']} {r['header']}: unknown code column {arg}")
            if r["mandatory"] in ("Y", "Yes") and r["example"] in ("-", ""):
                problems.append(f"{t['id']} {r['header']}: mandatory column without an example")
    try:
        import guided_xlsx  # noqa: PLC0415

        guided_xlsx.fill_in_order([guided_xlsx.Template(t["id"], t["name"], "", "", "", "", "", [], depends=depends(t))
                                   for t in data["templates"]])
    except ValueError as e:
        problems.append(str(e))
    return problems


# ============================================================================ lists, dues and templates


@lru_cache(maxsize=1)
def _drop0() -> dict[str, Any]:
    return yaml.safe_load((brand.SRC_DIR / "Drop-0_Closure" / "drop0.yaml").read_text(encoding="utf-8"))


def fmt_date(d: dt.date) -> str:
    return d.strftime("%d-%b-%Y").lstrip("0")


def register_due(template_id: str) -> tuple[dt.date, str] | None:
    """(date, text) of the earliest due of the Drop 0 register items that name the template (PM-nn, UA-nn, D0-nn)."""
    d = _drop0()
    dues = {x["code"]: x for x in d["due"]}
    found = [dues[i["due"]] for i in d["items"] if i.get("template") == template_id]
    if not found:
        return None
    x = min(found, key=lambda x: str(x["date"]))
    date = x["date"] if isinstance(x["date"], dt.date) else dt.date.fromisoformat(str(x["date"]))
    return date, f"{fmt_date(date)} – {x['label']}"


def template_dues(ids: list[str], deps: dict[str, list[str]], lookup: Callable[[str], tuple[dt.date, str] | None],
                  default: str) -> dict[str, str]:
    """Due text per template: its register due; else the earliest due of the templates that use its codes (it is
    needed before them); else the due of the set."""
    own = {tid: lookup(tid) for tid in ids}
    out = {}
    for tid in ids:
        if own[tid]:
            out[tid] = own[tid][1]
            continue
        users = [own[u] for u in ids if tid in deps[u] and own[u]]
        out[tid] = (f"{min(users)[1].split(' – ')[0]} – before the templates that use its codes" if users else default)
    return out


def _param_label(key: str, description: str) -> str:
    import importlib.util  # noqa: PLC0415

    name = "drop_closure"
    if name not in sys.modules:
        spec = importlib.util.spec_from_file_location(name, REPO / "tools" / "deliverables" / "drop_closure.py")
        module = importlib.util.module_from_spec(spec)
        sys.modules[name] = module
        spec.loader.exec_module(module)  # type: ignore[union-attr]
    dc = sys.modules[name]
    return (dc.data().get("parameter_text") or {}).get(key) or dc._clean(description)


def resolve_lists(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    """code -> {name, values [(code, label)], strict, note} of the lists of a templates file."""
    import sql_facts  # noqa: PLC0415

    out = {}
    lov_values = sql_facts.rows("lov_value", ("type_code", "code"))
    lov_types = {r["code"]: r["name"] for r in sql_facts.rows("lov_type")}
    params = sql_facts.rows("sys_parameter", "param_key")
    by_id = {t["id"]: t for t in data["templates"]}
    for code, spec in data["lists"].items():
        values: list[tuple[str, str]] = [(str(v[0]), str(v[1])) for v in spec.get("values") or []]
        strict, note = True, spec.get("note", "")
        if spec.get("lov"):
            values += [(v["code"], v["label"]) for v in lov_values if v["type_code"] == spec["lov"]]
            strict = False
            note = note or (f"The values of the list {spec['lov']} as delivered with BIBS; a new value is added with "
                            "its template and may then be used here.")
        for t in spec.get("lov_types") or []:
            values.append((t, f"List: {lov_types.get(t, t)}"))
        for g in spec.get("parameter_groups") or []:
            values += [(p["param_key"], "Parameter: " + _param_label(p["param_key"], p["description"]))
                       for p in params if p["category"] == g]
        if spec.get("accounting_events"):
            values += [(r["code"], r["name"]) for r in sorted(sql_facts.rows("acc_event_type"),
                                                               key=lambda x: (x["category"], x["code"]))]
        if spec.get("parameters_from_column"):
            tid, n = spec["parameters_from_column"]
            keys = [k.strip() for k in by_id[tid]["rows"][n - 1]["format"].split(",")]
            known = {p["param_key"]: p for p in params}
            values += [(k, _param_label(k, known[k]["description"]) if k in known else "") for k in keys]
        out[code] = {"name": spec["name"], "values": values, "strict": strict, "note": note}
    return out


def guided_templates(data: dict[str, Any], dues: dict[str, str], id_of: Callable[[str], str] = lambda x: x,
                     sheet_of: Callable[[dict[str, Any]], str] | None = None) -> list[Any]:
    """The guided_xlsx templates of a templates file; id_of maps the IDs (CI-01 -> PM-01 in the Drop 0 workbook) and
    list_of the list codes."""
    import guided_xlsx as g  # noqa: PLC0415

    out = []
    for t in data["templates"]:
        tid = id_of(t["id"])
        cols = []
        for r in t["rows"]:
            check = r["check"]
            kind, _, arg = check.partition(":")
            if kind in ("code", "ref"):
                ref_id, _, col = arg.partition("/")
                check = f"{kind}:{id_of(ref_id)}/{col}"
            cols.append(g.GuideColumn(r["header"], r["mandatory"], r["format"], r["what"], r["example"], check))
        sheet = sheet_of(t) if sheet_of else f"{tid} {t.get('sheet', t['name'])}"[:31]
        out.append(g.Template(tid, t["name"], sheet, t["content"], t["owner"], dues[t["id"]], t["load"], cols,
                              source=t["source"], depends=[id_of(d) for d in depends(t)],
                              capacity=int(t.get("capacity", 500))))
    return out


def add_lists(book: Any, data: dict[str, Any]) -> None:
    for code, x in resolve_lists(data).items():
        book.add_list(code, x["name"], x["values"], x["note"], x["strict"])


def finish_checks(book: Any, templates: list[Any]) -> None:
    """ref and many checks are guide texts and links only (several codes in one cell)."""
    import guided_xlsx as g  # noqa: PLC0415

    for t in templates:
        for c in t.columns:
            kind, _, arg = c.check.partition(":")
            if kind == "ref":
                tid, _, col = arg.partition("/")
                p = book.plans[tid]
                c.allowed = f"Codes of {p.template.id} {p.template.name}, column {col} (one or more)"
                c.allowed_link = f"{p.sheet}!{p.letters[col]}{p.header_row}"
                c.check = ""
            elif kind == "many":
                ref = book.lists[arg]
                c.allowed = f"One or more codes of the list {ref.name} ({g.LISTS}), separated by commas"
                c.allowed_link = f"{g.LISTS}!A{ref.row}"
                ref.used_in.append(f"{t.sheet}: {c.header}")
                c.check = ""


# ============================================================================ Word rendering (```pack blocks)


def pack_meta(folder: Path) -> dict[str, Any]:
    """meta of the pack.yaml next to the templates file (brd, brd_label, name, version, date)."""
    return yaml.safe_load((folder / "pack.yaml").read_text(encoding="utf-8"))["meta"]


def ordered(data: dict[str, Any]) -> list[dict[str, Any]]:
    import guided_xlsx as g  # noqa: PLC0415

    by_id = {t["id"]: t for t in data["templates"]}
    order = g.fill_in_order([g.Template(t["id"], t["name"], "", "", "", "", "", [], depends=depends(t))
                             for t in data["templates"]])
    return [by_id[t.id] for t in order]


def allowed_text(data: dict[str, Any], r: dict[str, Any]) -> str:
    kind, _, arg = r["check"].partition(":")
    names = {t["id"]: t["name"] for t in data["templates"]}
    if kind == "yn":
        return "Y or N"
    if kind in ("list", "many"):
        return f"List {data['lists'][arg]['name']}" + (" (one or more)" if kind == "many" else "")
    if kind in ("code", "ref"):
        tid, _, col = arg.partition("/")
        return f"Codes of {tid} {names[tid]} ({col})"
    if kind == "date":
        return "Date"
    if kind in ("number", "whole"):
        lo, _, hi = arg.partition("-")
        return ("Whole number" if kind == "whole" else "Number") + (f" {lo} to {hi}" if arg else "")
    return "-"


def render(doc: Any, render: str, source: str, **_: Any) -> None:  # noqa: A002 - block key
    path = (doc.base_dir / source).resolve()
    data = load(path)
    if render == "summary":
        rows = [[i, t["id"], t["name"], t["content"], t["owner"], ", ".join(depends(t)) or "-"]
                for i, t in enumerate(ordered(data), start=1)]
        doc.table(["Step", "ID", "Template", "Content", "Provided by", "Uses the codes of"], rows,
                  widths=[1.0, 1.2, 3.0, 6.0, 3.6, 2.8],
                  caption=f"Configuration inputs of {pack_meta(path.parent)['name']}, in the order they are filled in",
                  size=7.5, keep_rows=False)
        return
    if render == "templates":
        for t in ordered(data):
            doc.heading(f"{t['id']} {t['name']}", level=2)
            doc.key_values([("Content", t["content"]), ("Provided by", t["owner"]), ("Source", t["source"]),
                            ("How it is loaded", t["load"]), ("Uses the codes of", ", ".join(depends(t)) or "-"),
                            ("FRs", ", ".join(t.get("frs") or []))],
                           columns=1, label_width=3.2)
            rows = [[r["no"], r["header"], r["mandatory"], r["format"], allowed_text(data, r), r["what"], r["example"]]
                    for r in t["rows"]]
            doc.table(["No.", "Column", "Mand.", "Format", "Allowed values", "What to enter", "Example (fictitious)"],
                      rows, widths=[0.7, 3.0, 1.6, 3.0, 2.6, 4.2, 2.5], caption=f"Columns of the template {t['name']}",
                      size=7.5, keep_rows=False)
        return
    raise ValueError(f"unknown render {render}")


# ============================================================================ workbook


def build_workbook(data: dict[str, Any], meta: dict[str, Any], owner: str = "") -> Path:
    import guided_xlsx as g  # noqa: PLC0415

    brd, label, name = meta["brd"], meta["brd_label"], meta["name"]
    version, date = str(meta["version"]), str(meta["date"])
    title = f"{data['meta']['title']} {label} {name}"
    prefix = PREFIX.get(brd, "")
    deps = {t["id"]: depends(t) for t in data["templates"]}
    dues = template_dues([t["id"] for t in data["templates"]], deps,
                         lambda tid: register_due(f"{prefix}-{tid.split('-')[-1]}") if prefix else None,
                         data["meta"]["due"])
    book = g.GuidedBook(title, version)
    add_lists(book, data)
    templates = guided_templates(data, dues)
    book.plan_all(templates)
    finish_checks(book, templates)
    order = g.fill_in_order(templates)
    for i, t in enumerate(order, start=1):
        book.template_sheet(t, i, len(order))
    idx = []
    for i, t in enumerate(order, start=1):
        p = book.plans[t.id]
        idx.append({"Step": i, "ID": t.id, "Template": t.name, "What it holds": t.purpose, "Provided by": t.owner,
                    "Due": t.due, "How it is loaded": t.load,
                    "Depends on": ", ".join(t.depends) or "-", "Rows entered": p.rows_formula(),
                    "Mandatory cells missing": p.missing_formula(), "Status": "Not started",
                    "Open": ("Open →", p.sheet)})
    steps = [
        "Follow the steps of the index below in their order: a template whose codes other templates use comes first, "
        "so that the drop-downs of the later templates offer the codes already entered.",
        "On each template sheet, read the header block (purpose, who provides it, due date, how it is loaded, what it "
        "depends on) and the guide above each column: Mandatory, Format, Allowed values and What to enter. Columns "
        "marked * are mandatory; point at a column header for its full description.",
        "Overwrite or delete the grey example row, then enter one row per record under it. Drop-downs offer the "
        "allowed values (sheet Reference lists) and the codes of the templates filled in before; a mandatory cell "
        "left empty in a row turns red.",
        "Follow Rows entered and Mandatory cells missing in the index, set the Status of each template and record on "
        "the template sheet who confirmed it and when. Raise questions on the sheet Questions and comments.",
        f"Return the workbook to the {brand.VENDOR} project team by the due dates. "
        + (data["meta"].get("codes_note") or "")
        + " Templates loaded by the data migration follow its mocks and the map freeze; the others are entered on "
          "the screens and authorised by a second user.",
    ]
    identity = [("Client", brand.CLIENT), ("Release set", f"{label} {name} (Drop 0, Setup and Data Migration)"),
                ("Version and date", f"Version {version}, {date}"), ("Owner", owner or "-"),
                ("Due", data["meta"]["due"]), ("Prepared by", brand.VENDOR),
                ("Classification", brand.CLASSIFICATION)]
    columns = [("Step", 6), ("ID", 8), ("Template", 24), ("What it holds", 40), ("Provided by", 24), ("Due", 22),
               ("How it is loaded", 38), ("Depends on", 12), ("Rows entered", 9), ("Mandatory cells missing", 10),
               ("Status", 14), ("Open", 8)]

    def more(ws, row):
        return book.small_table(ws, row, "Other sheets", ["Sheet", "What it holds"], [
            ((g.LISTS, g.LISTS), "Every list of allowed values of the drop-downs, with code and label"),
            ((g.QUESTIONS, g.QUESTIONS), "Questions and comments per template and column, with the answer"),
        ], spans=[3, 6])

    book.questions_sheet(order)
    book.start_sheet(title, data["meta"].get("subtitle") or f"What the business provides to set up {name} and to "
                                                             "load it at go-live",
                     identity, steps, "Templates in the order they are filled in", columns, idx, after=more)
    book.lists_sheet()
    file = brand.output_name("Templates", brd, data["meta"]["workbook_name"], version, "xlsx")
    path = brand.out_dir(brd, "Templates") / file
    return book.save(path, [g.START] + [t.sheet for t in order] + [g.LISTS, g.QUESTIONS],
                     {"title": title, "keywords": f"Configuration input templates, {brd}, BIBS, BDOI"})


def folder_of(brd: str) -> Path:
    return brand.src_dir(brand.brd_of_code(brd)) / "pack"


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Build the configuration input templates of a BRD release set")
    ap.add_argument("brd", help="BRD of the set (e.g. BRD-11)")
    ap.add_argument("--check", action="store_true", help="check the templates (and the workbook written) only")
    args = ap.parse_args(argv)
    folder = folder_of(args.brd)
    data = load(folder / "config_inputs.yaml")
    problems = check(data)
    meta = pack_meta(folder)
    print(f"{len(data['templates'])} templates, {sum(len(t['rows']) for t in data['templates'])} columns, "
          f"fill-in order {', '.join(t['id'] for t in ordered(data))}")
    if args.check:
        import guided_xlsx as g  # noqa: PLC0415

        path = brand.out_dir(meta["brd"], "Templates") / brand.output_name(
            "Templates", meta["brd"], data["meta"]["workbook_name"], str(meta["version"]), "xlsx")
        if path.exists():
            problems += g.verify(path, [f"{t['id']} {t.get('sheet', t['name'])}"[:31] for t in data["templates"]])
    for p in problems:
        print(p)
    if args.check:
        return 1 if problems else 0
    guide = yaml.safe_load((folder / "guide.yaml").read_text(encoding="utf-8")) if (folder / "guide.yaml").exists() else {}
    print(f"xlsx: {build_workbook(data, meta, (guide.get('meta') or {}).get('owner', ''))}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
