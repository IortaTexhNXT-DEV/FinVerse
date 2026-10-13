"""Builds the BIBS Database Design (Word) and the BIBS Database Inventory (Excel) from the schema snapshot.

Usage
  python3 docs/deliverables/src/programme/database_design/extract_schema.py --as-postgres   # refresh the snapshot
  python3 docs/deliverables/src/programme/database_design/build_database_design.py [--check] [--no-pdf] [--keep-pdf]

Inputs
  * data/catalog.json, data/descriptions.json   snapshot of the schema (extract_schema.py);
  * ../security_data/domains.yaml               data domains and module (package) of each table;
  * DATABASE_DESIGN.md                          Word source (bdoi_docx format); lines <!-- dd:<name> --> (and the
                                                <!-- sd:<name> --> lines of build_secdata_pack.py) are expanded here;
  * ../security_data/figures/erd/*.dot          the entity relationship diagrams (rewritten by this script).

Outputs (Programme/Database_Design/)
  * BIBS_DB_BIBS_Database_Design_v1.0.docx (and the PDF with --keep-pdf; PDFs are not committed)
  * BIBS_DB_BIBS_Database_Inventory_v1.0.xlsx

The table model (domains, modules, descriptions, dictionary rows, ERD plan) is that of build_secdata_pack.py, which
this script imports and points at this folder.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
SEC = HERE.parent / "security_data"
sys.path.insert(0, str(SEC))
sys.path.insert(0, str(HERE))
import build_secdata_pack as bsp  # noqa: E402

from bdoi_docx import BdoiDocument, load_source, meta_from, output_path, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

import brand  # noqa: E402

VERSION = "1.0"
DATE = "13 October 2026"
OUT = brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "Database_Design"
INVENTORY = OUT / f"BIBS_DB_BIBS_Database_Inventory_v{VERSION}.xlsx"
SOURCE = HERE / "DATABASE_DESIGN.md"
PLACEHOLDER = re.compile(r"^<!--\s*(dd|sd):(\w+)\s*(.*?)-->\s*$")

# build_secdata_pack works on this folder: its figures, its snapshot
# the diagrams are the shared figures of security_data/figures/erd (one set of ERD sources for the programme);
# figure paths in the Word source are relative to the programme folder
bsp.HERE = HERE.parent
bsp.ERD_DIR = SEC / "figures" / "erd"
bsp._PLAN.clear()
# iNXT BrokerVerse design documents carry the iorta TechNXT logo only (cover and header), no client logo
brand.BDO_LOGO, brand.IORTA_LOGO = brand.IORTA_LOGO, brand.IORTA_LOGO.with_name("_none.png")
up = bsp.up


# ------------------------------------------------------------------------------------------------ catalogue

def load_catalog() -> dict[str, Any]:
    """The catalogue with each table placed in its domain and module (rules of build_secdata_pack.load_catalog)."""
    snap = json.loads((HERE / "data" / "catalog.json").read_text(encoding="utf-8"))
    desc = json.loads((HERE / "data" / "descriptions.json").read_text(encoding="utf-8"))
    dom = yaml.safe_load((SEC / "domains.yaml").read_text(encoding="utf-8"))
    pkg_domain = {p: d for d in dom["domains"] for p in d["packages"]}
    excluded_prefixes = tuple(dom["excluded"]["prefixes"])
    tables: dict[str, dict] = {}
    excluded, unplaced = [], []
    for t in snap["tables"]:
        name = t["name"]
        if name.startswith(excluded_prefixes):
            excluded.append(name)
            continue
        pkg = (desc.get(name) or {}).get("package")
        if not pkg:
            pkg = next((p for pre, p in dom["prefix_packages"].items() if name.startswith(pre)), None)
        if not pkg:
            parents = [k for k in desc if name.startswith(k + "_")]
            if parents:
                pkg = desc[max(parents, key=len)]["package"]
            else:
                owners = Counter(v["package"] for k, v in desc.items() if k.split("_")[0] == name.split("_")[0])
                pkg = owners.most_common(1)[0][0] if owners else None
        if pkg not in pkg_domain:
            unplaced.append(f"{name} ({pkg})")
            continue
        t = dict(t)
        t.update(package=pkg, domain=pkg_domain[pkg]["id"], doc=(desc.get(name) or {}).get("doc") or "")
        tables[name] = t
    incoming: dict[str, list] = defaultdict(list)
    for t in tables.values():
        for k in t["constraints"] or []:
            if k["type"] == "f":
                incoming[k["ref_table"]].append((t["name"], k))
    cat = {"tables": tables, "excluded": sorted(excluded), "unplaced": unplaced, "domains": dom["domains"],
           "excluded_meaning": dom["excluded"]["meaning"], "incoming": incoming, "views": snap.get("views", []),
           "migrations": snap.get("migrations", 0), "sequences": snap.get("sequences", [])}
    cat["levels"], cat["cycles"] = load_levels(cat)
    return cat


# ------------------------------------------------------------------------------------------------ migration facts

MASTER_PACKAGES = {"organization", "currency", "period", "dimension", "crm", "party", "catalog", "productmaint", "coa", "security", "nbadmin"}
REFERENCE_TABLES = {"lov_type", "lov_value", "sys_parameter"}
LOG = re.compile(r"(?:^audit_|_log$|_history$|_hist$|_outbox$|_inbox$|_attempt$|^evt_|_archive$)")
LEGACY_COLS = ("origin", "source_system", "legacy_ref", "migration_batch")


def table_class(t: dict) -> str:
    """Class of a table for data migration: Reference, Master, Transactional, Audit and log, Migration control,
    System (technical state)."""
    n, cols = t["name"], {c["name"] for c in t["columns"]}
    if n in REFERENCE_TABLES or t["package"] == "lov":
        return "Reference"
    if n.startswith("mig_") or t["package"] == "migration":
        return "Migration control"
    if LOG.search(n) or bsp.table_facts(t)["immutable"]:
        return "Audit and log"
    if n.startswith(("sys_", "document_sequence", "stored_", "sto_", "bulk_", "report_", "msg_", "alt_", "wf_",
                     "idn_", "sec_session", "sec_login")):
        return "System"
    if "record_status" in cols or t["package"] in MASTER_PACKAGES:
        return "Master"
    return "Transactional"


def migration_load(cls: str) -> str:
    return {"Reference": "Configured or loaded first (code lists)",
            "Master": "Loaded from legacy masters after reference data",
            "Transactional": "Loaded from legacy transactions (open items and history in scope)",
            "Audit and log": "Not loaded; written by the system",
            "Migration control": "Written by the migration console",
            "System": "Not loaded; technical state of the system"}[cls]


def natural_keys(t: dict) -> list[str]:
    """Unique keys other than the surrogate identifier: constraints and unique indexes (partial ones marked)."""
    out = []
    seen = set()
    for k in t["constraints"] or []:
        if k["type"] == "u" and k["cols"] != ["id"]:
            key = ", ".join(up(c) for c in k["cols"])
            seen.add(key)
            out.append(key)
    for x in t["indexes"] or []:
        if x["unique"] and not x["primary"]:
            m = re.search(r"USING \w+ \((.*?)\)(?: WHERE (.*))?$", x["def"])
            if not m:
                continue
            key = re.sub(r"::[a-z ]+", "", m.group(1)).replace("lower((", "lower(").replace("))", ")")
            key = "".join(x if x.startswith("'") else x.upper() for x in re.split(r"('(?:[^']|'')*')", key))
            if key in seen or key == "ID":
                continue
            seen.add(key)
            out.append(key + (" (when " + bsp.check_text("CHECK (" + m.group(2) + ")") + ")" if m.group(2) else ""))
    return out


CODE_COL = re.compile(r"(?:^code$|_code$|_no$|_number$|^arn$|_ref$|^reference$|_key$)")


def code_columns(t: dict) -> list[str]:
    return [up(c["name"]) for c in t["columns"] if CODE_COL.search(c["name"])]


def mandatory_columns(t: dict) -> list[str]:
    """NOT NULL columns without a default or identity: the load must provide them."""
    return [up(c["name"]) for c in t["columns"]
            if not c["nullable"] and c.get("default") is None and not c.get("identity")]


def load_levels(cat: dict) -> tuple[dict[str, int], list[list[str]]]:
    """Load level of each table: 1 + the highest level of the tables it references (self references ignored);
    tables that reference each other (a cycle) share one level and are listed in the cycles."""
    names = set(cat["tables"])
    deps = {n: {k["ref_table"] for k in bsp.table_facts(t)["fks"] if k["ref_table"] in names and k["ref_table"] != n}
            for n, t in cat["tables"].items()}
    # strongly connected components (iterative Tarjan)
    index: dict[str, int] = {}
    low: dict[str, int] = {}
    on, stack, comps = set(), [], []
    counter = [0]
    for root in sorted(names):
        if root in index:
            continue
        work = [(root, iter(sorted(deps[root])))]
        index[root] = low[root] = counter[0]
        counter[0] += 1
        stack.append(root)
        on.add(root)
        while work:
            v, it = work[-1]
            nxt = next(it, None)
            if nxt is not None:
                if nxt not in index:
                    index[nxt] = low[nxt] = counter[0]
                    counter[0] += 1
                    stack.append(nxt)
                    on.add(nxt)
                    work.append((nxt, iter(sorted(deps[nxt]))))
                elif nxt in on:
                    low[v] = min(low[v], index[nxt])
                continue
            work.pop()
            if work:
                low[work[-1][0]] = min(low[work[-1][0]], low[v])
            if low[v] == index[v]:
                comp = []
                while True:
                    w = stack.pop()
                    on.discard(w)
                    comp.append(w)
                    if w == v:
                        break
                comps.append(sorted(comp))
    comp_of = {n: i for i, c in enumerate(comps) for n in c}
    level: dict[int, int] = {}
    for i, comp in enumerate(comps):  # Tarjan emits components in reverse topological order: parents first
        parents = {comp_of[d] for n in comp for d in deps[n]} - {i}
        level[i] = 1 + max((level[p] for p in parents), default=0)
    return {n: level[comp_of[n]] for n in names}, [c for c in comps if len(c) > 1]


def enumerations(cat: dict) -> list[dict]:
    rows = []
    for t in sorted(cat["tables"].values(), key=lambda t: t["name"]):
        for k in bsp.table_facts(t)["checks"]:
            vals = bsp.allowed_values(k["def"])
            if vals is not None and len(k["cols"] or []) == 1:
                if any(bsp.SNAKE.fullmatch(v) for v in vals):  # codes stored in lower case
                    vals = [v.upper() for v in vals]
                rows.append({"table": up(t["name"]), "module": t["package"].upper(), "column": up(k["cols"][0]),
                             "values": ", ".join(vals), "count": len(vals), "kind": "Allowed values"})
            else:
                rows.append({"table": up(t["name"]), "module": t["package"].upper(),
                             "column": ", ".join(up(c) for c in k["cols"] or []), "values": bsp.check_text(k["def"]),
                             "count": None, "kind": "Rule"})
    return rows


def number_series(cat: dict) -> list[dict]:
    rows = []
    for n, what in [("document_sequence", "Counter per document number series (series key and next value)"),
                    ("sys_shared_counter", "Shared counters (key, value, expiry) used across application instances"),
                    ("csh_receipt_series", "Official and acknowledgement receipt series per branch, with the BIR "
                                           "authority to print, range and next number"),
                    ("coa_numbering", "Numbering of the chart of accounts below a parent account (separator, width)")]:
        if n in cat["tables"]:
            rows.append({"object": up(n), "kind": "Number series table", "use": what})
    ident = [s for s in cat["sequences"] if s["owned_by"]]
    rows.append({"object": f"{len(ident)} identity sequences", "kind": "Identity",
                 "use": "One per surrogate key ID (identity BY DEFAULT): a load may supply the identifier; the "
                        "sequence is then advanced past the highest loaded value"})
    for s in cat["sequences"]:
        if not s["owned_by"]:
            rows.append({"object": up(s["name"]), "kind": "Free sequence", "use": "Fencing token of the job lock"})
    return rows


# ------------------------------------------------------------------------------------------------ rows

def table_rows(cat: dict) -> list[dict]:
    order = {d["id"]: i for i, d in enumerate(cat["domains"])}
    names = {d["id"]: d["name"] for d in cat["domains"]}
    rows = []
    for t in sorted(cat["tables"].values(), key=lambda t: (order[t["domain"]], t["package"], t["name"])):
        f = bsp.table_facts(t)
        cls = table_class(t)
        cols = {c["name"] for c in t["columns"]}
        rows.append({
            "domain": f"{t['domain']} {names[t['domain']]}", "module": t["package"].upper(), "table": up(t["name"]),
            "description": bsp.table_description(t), "class": cls, "columns": len(t["columns"]),
            "pk": ", ".join(up(c) for c in f["pk"]), "nk": "; ".join(natural_keys(t)),
            "codes": ", ".join(code_columns(t)), "mandatory": ", ".join(mandatory_columns(t)),
            "fk_out": ", ".join(sorted({up(k["ref_table"]) for k in f["fks"]})),
            "fk_in": len(cat["incoming"].get(t["name"], [])),
            "indexes": len([x for x in f["indexes"] if not x["primary"]]), "checks": len(f["checks"]),
            "immutable": "Yes" if f["immutable"] else "",
            "legacy": ", ".join(up(c) for c in LEGACY_COLS if c in cols),
            "level": cat["levels"][t["name"]], "load": migration_load(cls),
            "figure": bsp.erd_figure_title(t["domain"], t["package"], cat, t["name"]),
        })
    return rows


FIXED_DESCRIPTIONS = {"BUILT_IN": "Standard row supplied with the system (not created by a user)"}


def dictionary_rows(cat: dict, domain_id: str) -> list[dict]:
    """build_secdata_pack.dictionary_rows with the descriptions of FIXED_DESCRIPTIONS."""
    rows = bsp.dictionary_rows(cat, domain_id)
    for r in rows:
        if r["column"] in FIXED_DESCRIPTIONS and bsp.violations(r["description"]):
            r["description"] = FIXED_DESCRIPTIONS[r["column"]]
    return rows


def column_rows(cat: dict, domain_id: str | None = None) -> list[dict]:
    rows = []
    doms = [d["id"] for d in cat["domains"]] if domain_id is None else [domain_id]
    mand = {(up(t["name"]), m) for t in cat["tables"].values() for m in mandatory_columns(t)}
    mod = {up(n): t["package"].upper() for n, t in cat["tables"].items()}
    for d in doms:
        for r in dictionary_rows(cat, d):
            r = dict(r)
            r["module"] = mod[r["table"]]
            r["mandatory"] = "Yes" if (r["table"], r["column"]) in mand else ""
            rows.append(r)
    return rows


def fk_rows(cat: dict) -> list[dict]:
    rule = {"a": "No action", "r": "Restrict", "c": "Cascade", "n": "Set null", "d": "Set default"}
    rows = []
    for t in sorted(cat["tables"].values(), key=lambda t: t["name"]):
        for k in bsp.table_facts(t)["fks"]:
            nullable = any(c["nullable"] for c in t["columns"] if c["name"] in k["cols"])
            rows.append({"table": up(t["name"]), "module": t["package"].upper(), "name": up(k["name"]),
                         "cols": ", ".join(up(c) for c in k["cols"]), "ref_table": up(k["ref_table"]),
                         "ref_cols": ", ".join(up(c) for c in k["ref_cols"] or []),
                         "optional": "Optional" if nullable else "Mandatory",
                         "on_delete": rule.get(k["on_delete"], k["on_delete"]),
                         "ref_module": cat["tables"][k["ref_table"]]["package"].upper()
                         if k["ref_table"] in cat["tables"] else "-"})
    return rows


# ------------------------------------------------------------------------------------------------ workbook

def build_inventory(cat: dict) -> Path:
    tables = cat["tables"]
    n_cols = sum(len(t["columns"]) for t in tables.values())
    fks = fk_rows(cat)
    wb = BdoiWorkbook("BIBS Database Inventory", doc_type="Database inventory", brd="BRD-00", version=VERSION,
                      date=DATE, subtitle="Every table, column, key, index, enumeration and the load order of the "
                                          "BIBS database, for data migration")
    wb.cover_notes = [
        f"BIBS database on PostgreSQL 16: {cat['migrations']} schema versions applied in order to an empty database, "
        f"then read from the system catalogue on {DATE}. Reference rows and test data are not part of the inventory.",
        f"{len(tables)} tables, {n_cols:,} columns and {len(fks)} foreign keys in {len(cat['domains'])} data domains "
        "(one sheet per domain, with the module of each table).",
        "Class of a table: Reference (code lists and parameters), Master, Transactional, Audit and log, Migration "
        "control, System. Load level: the order in which the tables are loaded (a table references only tables of "
        "lower levels; tables that reference each other share a level and are listed in the Load order sheet).",
        "Companion document: BIBS Database Design v1.0 (principles, conventions, diagrams and the data migration "
        "notes). It supersedes the BIBS Data Dictionary v1.0 of 08 October 2026.",
        "Identifiers are written in upper case; PostgreSQL folds unquoted names, so they match the catalogue.",
    ]
    wb.legend = [("PK", "Primary key"), ("FK", "Foreign key"), ("UQ", "Unique (constraint or unique index)"),
                 ("Mandatory", "NOT NULL without a default: every loaded row provides a value"),
                 ("Immutable", "Insert-only table: database triggers refuse UPDATE, DELETE and TRUNCATE")]
    classes = ["Reference", "Master", "Transactional", "Audit and log", "Migration control", "System"]
    wb.sheet("All tables", [
        Column("domain", "Domain", 22, "Data domain"),
        Column("module", "Module", 14, "Module that owns the table"),
        Column("table", "Table", 30, "Table name"),
        Column("description", "Purpose", 56, "What one row of the table records"),
        Column("class", "Class", 15, "Reference, Master, Transactional, Audit and log, Migration control, System",
               values=classes),
        Column("level", "Load level", 9, "Order of loading (Load order sheet)", kind="number"),
        Column("columns", "Columns", 9, "Number of columns", kind="number"),
        Column("pk", "Primary key", 12, "Primary key columns"),
        Column("nk", "Natural / unique keys", 34, "Unique keys other than the identifier: usable to map legacy "
                                                  "records"),
        Column("codes", "Code and number columns", 30, "Columns holding codes, numbers and references"),
        Column("mandatory", "Mandatory columns", 40, "NOT NULL columns without a default"),
        Column("fk_out", "References", 34, "Tables this table references"),
        Column("fk_in", "Referenced by", 10, "Foreign keys of other tables to this table", kind="number"),
        Column("indexes", "Indexes", 8, "Secondary indexes", kind="number"),
        Column("checks", "Checks", 8, "Check constraints", kind="number"),
        Column("immutable", "Immutable", 10, "Yes: insert-only"),
        Column("legacy", "Legacy columns", 24, "Columns that record the origin of a migrated row"),
        Column("load", "Data migration", 30, "How the table is filled at go-live"),
        Column("figure", "Figure", 18, "Figure of the Database Design that shows the table"),
    ], table_rows(cat), description=f"{len(tables)} tables by domain and module")
    col_cols = [
        Column("table", "Table", 28, "Table name"),
        Column("module", "Module", 13, "Module that owns the table"),
        Column("pos", "#", 5, "Position of the column", kind="number"),
        Column("column", "Column", 28, "Column name"),
        Column("type", "Data type", 18, "PostgreSQL data type"),
        Column("null", "Null", 7, "Yes: the column accepts NULL", values=["Yes", "No"]),
        Column("mandatory", "Mandatory", 10, "Yes: NOT NULL without a default", values=["Yes"]),
        Column("default", "Default", 20, "Default value or identity generation"),
        Column("key", "Key", 8, "PK, FK, UQ"),
        Column("ref", "References", 26, "Referenced table and column"),
        Column("rule", "Allowed values and checks", 36, "Check constraints of the column"),
        Column("index", "Indexes", 30, "Secondary indexes led by the column"),
        Column("description", "Description", 44, "Meaning of the column"),
    ]
    for d in cat["domains"]:
        rows = column_rows(cat, d["id"])
        n = len({r["table"] for r in rows})
        n_fk = sum(1 for r in fks if cat["tables"][r["table"].lower()]["domain"] == d["id"])
        wb.sheet(d["sheet"], col_cols, rows, description=f"{d['id']} {d['name']}: {n} tables, {len(rows):,} columns, "
                                                         f"{n_fk} foreign keys. Modules: "
                                                         + ", ".join(p.upper() for p in d["packages"]))
    wb.sheet("Columns", [Column("domain", "Domain", 8, "Data domain")] + col_cols,
             [dict(r, domain=cat["tables"][r["table"].lower()]["domain"]) for r in column_rows(cat)],
             description=f"All {n_cols:,} columns")
    wb.sheet("Foreign keys", [
        Column("table", "Table", 28, "Child table"), Column("module", "Module", 13, "Module of the child table"),
        Column("cols", "Columns", 24, "Foreign key columns"), Column("ref_table", "References", 28, "Parent table"),
        Column("ref_cols", "Parent columns", 16, "Referenced columns"),
        Column("ref_module", "Parent module", 13, "Module of the parent table"),
        Column("optional", "Link", 11, "Mandatory: the columns are NOT NULL", values=["Mandatory", "Optional"]),
        Column("on_delete", "On delete", 11, "Rule when the parent row is deleted"),
        Column("name", "Constraint", 40, "Constraint name"),
    ], fks, description=f"{len(fks)} foreign keys")
    lo = sorted(table_rows(cat), key=lambda r: (r["level"], r["class"] != "Reference", r["table"]))
    cyc = {up(n): i for i, c in enumerate(cat["cycles"], 1) for n in c}
    for r in lo:
        r["cycle"] = f"Group {cyc[r['table']]}" if r["table"] in cyc else ""
    wb.sheet("Load order", [
        Column("level", "Load level", 9, "Tables of a level reference only tables of lower levels", kind="number"),
        Column("table", "Table", 30, "Table name"), Column("module", "Module", 13, "Module"),
        Column("class", "Class", 15, "Class of the table", values=classes),
        Column("fk_out", "Depends on", 40, "Tables referenced (loaded before)"),
        Column("cycle", "Mutual reference", 14, "Tables that reference each other: load with the optional link "
                                                "empty, then update it"),
        Column("load", "Data migration", 30, "How the table is filled at go-live"),
    ], lo, description=f"{len(lo)} tables in {max(cat['levels'].values())} load levels")
    enums = enumerations(cat)
    wb.sheet("Enumerations", [
        Column("table", "Table", 28, "Table"), Column("module", "Module", 13, "Module"),
        Column("column", "Column", 24, "Column(s)"),
        Column("kind", "Kind", 13, "Allowed values: list of codes; Rule: other check", values=["Allowed values", "Rule"]),
        Column("count", "Values", 8, "Number of allowed values", kind="number"),
        Column("values", "Allowed values or rule", 70, "The codes a loaded row may hold, or the condition"),
    ], enums, description=f"{len(enums)} check constraints, "
                          f"{sum(1 for e in enums if e['kind'] == 'Allowed values')} of them lists of allowed values")
    wb.sheet("Number series", [
        Column("object", "Object", 30, "Table or sequence"), Column("kind", "Kind", 18, "Kind of series"),
        Column("use", "Use and migration note", 80, "What it numbers"),
    ], number_series(cat), description="Number series, counters and sequences")
    return wb.save(INVENTORY)


# ------------------------------------------------------------------------------------------------ Word placeholders

md = bsp.md_table


def placeholders(cat: dict, name: str, opts: str) -> list[str]:
    tables = cat["tables"]
    rows_all = table_rows(cat)
    if name == "stats":
        fks = fk_rows(cat)
        ix = sum(len([x for x in bsp.table_facts(t)["indexes"] if not x["primary"]]) for t in tables.values())
        ck = sum(len(bsp.table_facts(t)["checks"]) for t in tables.values())
        uq = sum(1 for t in tables.values() for x in t["indexes"] or [] if x["unique"] and not x["primary"])
        trg = sum(len(t["triggers"] or []) for t in tables.values())
        rows = [["Schema versions applied", cat["migrations"]], ["Tables", len(tables)],
                ["Columns", f"{sum(len(t['columns']) for t in tables.values()):,}"], ["Foreign keys", len(fks)],
                ["Secondary indexes (of which unique)", f"{ix} ({uq})"], ["Check constraints", ck],
                ["Triggers", trg], ["Insert-only tables", sum(1 for r in rows_all if r["immutable"])],
                ["Views", len(cat["views"])], ["Sequences", len(cat["sequences"])],
                ["Load levels", max(cat["levels"].values())], ["Entity relationship figures", len(bsp.erd_figures(cat))]]
        return md(["Measure", "Value"], rows, "7,3", "The BIBS database in figures", " bold=first")
    if name == "types":
        cnt = Counter(bsp.fmt_type(re.sub(r"\(.*", "", c["type"])) for t in tables.values() for c in t["columns"])
        use = {"VARCHAR": "Codes, names, numbers, statuses (length set per column)",
               "BIGINT": "Surrogate keys, foreign keys, counters", "TIMESTAMPTZ": "Time stamps (stored in UTC)",
               "NUMERIC": "Amounts (NUMERIC(18,2)), rates and percentages (higher scale)",
               "INTEGER": "Counts, sequence numbers, years", "DATE": "Business dates (effective, due, posting)",
               "BOOLEAN": "Flags", "TEXT": "Long free text", "BYTEA": "Binary content (encrypted values, files)",
               "TIMESTAMP": "Local time stamps of external files", "UUID": "Correlation identifiers",
               "JSONB": "Structured documents (events)", "SMALLINT": "Small codes"}
        return md(["Data type", "Columns", "Used for"], [[k, v, use.get(k, "")] for k, v in cnt.most_common()],
                  "3,2,10", "Column data types", " bold=first")
    if name == "classes":
        cnt = Counter(r["class"] for r in rows_all)
        meaning = {"Reference": "Code lists and parameters (LOV types and values, system parameters)",
                   "Master": "Masters with maker-checker approval or owned by a master module (organisation, "
                             "currency, period, product catalogue, chart of accounts, users and roles)",
                   "Transactional": "Business documents and their lines, balances and work items",
                   "Audit and log": "Audit trail, histories, event and message logs; insert-only where marked",
                   "Migration control": "Batches, staging rows, mappings and logs of the migration console",
                   "System": "Technical state: counters, sessions, jobs, workflow, messaging, report and file "
                             "management"}
        rows = [[c, cnt.get(c, 0), sum(r["columns"] for r in rows_all if r["class"] == c), meaning[c],
                 migration_load(c)] for c in meaning]
        rows.append(["Total", len(rows_all), sum(r["columns"] for r in rows_all), "", ""])
        return md(["Class", "Tables", "Columns", "Content", "Data migration"], rows, "2.6,1.3,1.5,6.5,4",
                  "Tables by class", " bold=first")
    if name == "domain_classes":
        classes = ["Reference", "Master", "Transactional", "Audit and log", "Migration control", "System"]
        rows = []
        for d in cat["domains"]:
            rs = [r for r in rows_all if r["domain"].startswith(d["id"] + " ")]
            n_fk = sum(len(bsp.table_facts(tables[r["table"].lower()])["fks"]) for r in rs)
            rows.append([d["id"], d["name"], len(rs), sum(r["columns"] for r in rs), n_fk] +
                        [sum(1 for r in rs if r["class"] == c) or "-" for c in classes])
        rows.append(["", "Total", len(rows_all), sum(r["columns"] for r in rows_all),
                     sum(len(bsp.table_facts(t)["fks"]) for t in tables.values())] +
                    [sum(1 for r in rows_all if r["class"] == c) for c in classes])
        return md(["ID", "Domain", "Tables", "Cols", "FKs", "Ref", "Master", "Trans", "Log", "Mig", "Sys"], rows,
                  "1,4.2,1.2,1.2,1.1,1,1.2,1.2,1,1,1", "Tables, columns and foreign keys per domain and class",
                  " bold=first size=8.5")
    if name == "audit":
        rows = [[r["table"], r["module"], r["description"], r["immutable"] or "-"]
                for r in rows_all if r["class"] == "Audit and log"]
        return md(["Table", "Module", "Purpose", "Insert-only"], rows, "4,2,9,1.6", "Audit, history and log tables",
                  " size=8.5")
    if name == "numbering":
        return md(["Object", "Kind", "Use"], [[r["object"], r["kind"], r["use"]] for r in number_series(cat)],
                  "4,3,9", "Number series, counters and sequences", " bold=first")
    if name == "reference":
        rows = [[r["table"], r["module"], r["description"], r["nk"] or "-"] for r in rows_all
                if r["class"] == "Reference"]
        return md(["Table", "Module", "Purpose", "Natural key"], rows, "3.5,2,8,3", "Reference tables", "")
    if name == "enum_summary":
        en = [e for e in enumerations(cat) if e["kind"] == "Allowed values"]
        common = Counter(e["column"] for e in en)
        rows = []
        for col, n in common.most_common(25):
            vals = Counter(e["values"] for e in en if e["column"] == col).most_common(1)[0][0]
            rows.append([col, n, vals if len(vals) < 160 else vals[:157] + "..."])
        return md(["Column", "Tables", "Most common list of allowed values"], rows, "4,1.5,11",
                  f"The 25 columns most often restricted to a list ({len(en)} lists in all)", " size=8.5")
    if name == "catalogue":
        out: list[str] = []
        for d in cat["domains"]:
            rs = [r for r in rows_all if r["domain"].startswith(d["id"] + " ")]
            out += [f"## {d['id']} {d['name']}", "",
                    f"{d['summary']} Modules: {', '.join(p.upper() for p in d['packages'] if any(r['module'] == p.upper() for r in rs))}. "
                    f"{len(rs)} tables, {sum(r['columns'] for r in rs):,} columns.", ""]
            out += md(["Table", "Purpose", "Class", "PK", "Natural / unique key", "References", "Lvl"],
                      [[r["table"], r["description"], r["class"], r["pk"] or "-", r["nk"] or "-", r["fk_out"] or "-",
                        r["level"]] for r in rs],
                      "3.1,5.9,2.3,1.9,3.3,3.3,0.8", f"Table catalogue of {d['id']} {d['name']}", " size=7.5")
        return out
    if name == "load_levels":
        by = defaultdict(list)
        for r in rows_all:
            by[r["level"]].append(r)
        rows = []
        for lv in sorted(by):
            rs = by[lv]
            cnt = Counter(r["class"] for r in rs)
            ex = ", ".join(r["table"] for r in sorted(rs, key=lambda r: (-r["fk_in"], r["table"]))[:6])
            rows.append([lv, len(rs), "; ".join(f"{k} {v}" for k, v in cnt.most_common()), ex])
        return md(["Level", "Tables", "Classes", "Examples (most referenced first)"], rows, "1.2,1.3,5.5,9",
                  "Load levels", " bold=first size=8.5")
    if name == "cycles":
        if not cat["cycles"]:
            return ["No two tables reference each other: every table can be loaded after its parents.", ""]
        rows = []
        for i, c in enumerate(cat["cycles"], 1):
            links = []
            for n in c:
                for k in bsp.table_facts(tables[n])["fks"]:
                    if k["ref_table"] in c and k["ref_table"] != n:
                        opt = any(x["nullable"] for x in tables[n]["columns"] if x["name"] in k["cols"])
                        links.append(f"{up(n)}.{', '.join(up(x) for x in k['cols'])} -> {up(k['ref_table'])}"
                                     f" ({'optional' if opt else 'mandatory'})")
            rows.append([f"Group {i}", ", ".join(up(n) for n in c), "; ".join(links)])
        return md(["Group", "Tables", "Links (optional links are loaded empty, then updated)"], rows, "1.5,5,10",
                  "Tables that reference each other", " size=8.5")
    if name == "legacy":
        rows = [[r["table"], r["module"], r["legacy"]] for r in rows_all if r["legacy"]]
        return md(["Table", "Module", "Origin columns"], rows, "5,3,8", "Tables that record the origin of a row",
                  " size=8.5")
    if name == "self_refs":
        rows = []
        for t in sorted(tables.values(), key=lambda t: t["name"]):
            for k in bsp.table_facts(t)["fks"]:
                if k["ref_table"] == t["name"]:
                    rows.append([up(t["name"]), ", ".join(up(c) for c in k["cols"])])
        return md(["Table", "Column referencing the same table"], rows, "6,8",
                  "Hierarchies (a row references a row of the same table)", " size=8.5")
    if name == "erd_count":
        return [f"{len(bsp.erd_figures(cat))} figures follow, grouped by data domain and module.", ""]
    raise KeyError(f"unknown placeholder dd:{name}")


def expand(cat: dict, lines: list[str]) -> list[str]:
    out: list[str] = []
    for line in lines:
        m = PLACEHOLDER.match(line)
        if not m:
            out.append(line)
        elif m.group(1) == "sd":
            out += bsp.placeholders(cat, m.group(2), m.group(3))
        else:
            out += placeholders(cat, m.group(2), m.group(3))
    return out


def build_doc(cat: dict, pdf: bool, keep_pdf: bool):
    front, lines = load_source(SOURCE)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE.parent)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(cat, lines))
    return doc.publish(output_path(front, SOURCE), pdf=pdf, keep_pdf=keep_pdf)


# ------------------------------------------------------------------------------------------------ checks

def check(cat: dict) -> list[str]:
    problems = []
    if cat["unplaced"]:
        problems.append("tables without a domain: " + ", ".join(cat["unplaced"]))
    _, lines = load_source(SOURCE)
    for i, line in enumerate(expand(cat, lines), 1):
        if line.lstrip().startswith("<!--"):
            continue
        line = re.sub(r"\]\([^)]*\)(\{[^}]*\})?", "]", line)
        v = bsp.violations(line)
        if v:
            problems.append(f"design line {i}: {v} in {line[:120]}")
    for r in table_rows(cat) + fk_rows(cat) + enumerations(cat) + number_series(cat):
        for k, v in r.items():
            if isinstance(v, str) and bsp.violations(v):
                problems.append(f"inventory {r.get('table') or r.get('object')} {k}: {bsp.violations(v)}")
    for d in cat["domains"]:
        for r in dictionary_rows(cat, d["id"]):
            for k, v in r.items():
                if isinstance(v, str) and bsp.violations(v):
                    problems.append(f"{r['table']}.{r['column']} {k}: {bsp.violations(v)}")
    return problems


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="BIBS Database Design and Database Inventory")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--no-pdf", action="store_true")
    ap.add_argument("--keep-pdf", action="store_true", help="keep the PDF next to the Word file")
    ap.add_argument("--only", choices=["inventory", "design"])
    args = ap.parse_args(argv)
    cat = load_catalog()
    problems = check(cat)
    for p in problems[:60]:
        print("problem:", p)
    if args.check or problems:
        print(f"{len(cat['tables'])} tables, {len(problems)} problem(s)")
        return 1 if problems else 0
    OUT.mkdir(parents=True, exist_ok=True)
    built = []
    if args.only in (None, "inventory"):
        built.append(build_inventory(cat))
    if args.only in (None, "design"):
        built.append(build_doc(cat, not args.no_pdf, args.keep_pdf)[0])
    import office_meta  # noqa: PLC0415
    for p in built:
        office_meta.fix(p)
        print(f"built: {p.relative_to(brand.REPO_ROOT)} ({p.stat().st_size / 1e6:.2f} MB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
