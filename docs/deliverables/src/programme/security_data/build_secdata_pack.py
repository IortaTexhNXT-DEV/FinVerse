"""Builds the Security and Data documents of the programme (BRD-00).

Usage
  python docs/deliverables/src/programme/security_data/build_secdata_pack.py --check        # checks only
  python docs/deliverables/src/programme/security_data/build_secdata_pack.py [--only NAME]  # build
  python docs/deliverables/src/programme/security_data/build_secdata_pack.py --previews     # also page previews

NAME is one of: dictionary (Data Dictionary workbook and the Entity Relationship Diagrams document), security
(Security Architecture and the ASVS control mapping workbook), data (Data Architecture).

Inputs (this folder)
  * data/catalog.json, data/descriptions.json  catalogue snapshot of the production schema (extract_catalog.py);
  * domains.yaml                                data domains, owners, excluded insurer tables;
  * asvs_mapping.yaml                           OWASP ASVS 4.0.3 Level 2 control mapping;
  * SECURITY_ARCHITECTURE.md, DATA_ARCHITECTURE.md, ERD_DATA_DICTIONARY.md   Word sources (bdoi_docx format);
    lines <!-- sd:<name> --> are replaced by tables or figures built from the data above;
  * figures/*.dot, figures/erd/*.dot (the ERD sources are written by this script).

Outputs (from tools/deliverables/brand.py)
  * Programme/Security/BIBS_Architecture_BRD-00_Security_Architecture_v1.0.docx
  * Programme/Security/BIBS_Security_BRD-00_ASVS_L2_Control_Mapping_v1.0.xlsx
  * Programme/Data/BIBS_Architecture_BRD-00_Data_Architecture_v1.0.docx
  * Programme/Data/BIBS_Data_BRD-00_Entity_Relationship_Diagrams_v1.0.docx
  * Programme/Data/BIBS_Data_BRD-00_Data_Dictionary_v1.0.xlsx

Identifiers of the schema are written in upper case (PostgreSQL folds unquoted names, so CLIENT_CODE and client_code
name the same column).
"""

from __future__ import annotations

import argparse
import csv
import html
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
import check_pack  # noqa: E402
from bdoi_docx import BdoiDocument, lint_source, load_source, meta_from, output_path, render_body  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

VERSION = "1.0"
DATE = "08 October 2026"
SECURITY_OUT = brand.out_dir("BRD-00", "Security")
DATA_OUT = brand.out_dir("BRD-00", "Data")
DICTIONARY = DATA_OUT / brand.output_name("Data", "BRD-00", "Data Dictionary", VERSION, "xlsx")
ASVS_BOOK = SECURITY_OUT / brand.output_name("Security", "BRD-00", "ASVS L2 Control Mapping", VERSION, "xlsx")
ERD_DIR = HERE / "figures" / "erd"
PLACEHOLDER = re.compile(r"^<!--\s*sd:(\w+)\s*(.*?)-->\s*$")
# Platform tables referenced from almost every table; their links are listed in the dictionary and drawn only in the
# figures of their own package.
COMMON_TARGETS = {"org_company", "org_branch"}


# ------------------------------------------------------------------------------------------------ wording checks

def violations(text: str) -> list[str]:
    """Restricted words and development-status wording that check_pack refuses in a document of out/."""
    found = [m.group(0) for m in check_pack.RESTRICTED.finditer(text)]
    for label, pattern in check_pack.BUILD_STATUS:
        found += [f"{label}: {m.group(0)}" for m in pattern.finditer(text)]
    return found


def up(name: str | None) -> str:
    return (name or "").upper()


SNAKE = re.compile(r"\b[a-z][a-z0-9]*(?:_[a-z0-9]+)+\b")


def upper_identifiers(text: str) -> str:
    """Upper-cases the snake_case identifiers of a text (outside quoted literals)."""
    parts = re.split(r"('(?:[^']|'')*')", text)
    return "".join(p if p.startswith("'") else SNAKE.sub(lambda m: m.group(0).upper(), p) for p in parts)


# ------------------------------------------------------------------------------------------------ catalogue model

def load_catalog() -> dict[str, Any]:
    snap = json.loads((HERE / "data" / "catalog.json").read_text(encoding="utf-8"))
    desc = json.loads((HERE / "data" / "descriptions.json").read_text(encoding="utf-8"))
    dom = yaml.safe_load((HERE / "domains.yaml").read_text(encoding="utf-8"))
    pkg_domain = {p: d for d in dom["domains"] for p in d["packages"]}
    excluded_prefixes = tuple(dom["excluded"]["prefixes"])
    tables: dict[str, dict] = {}
    excluded: list[str] = []
    unplaced: list[str] = []
    for t in snap["tables"]:
        name = t["name"]
        if name.startswith(excluded_prefixes):
            excluded.append(name)
            continue
        pkg = (desc.get(name) or {}).get("package")
        if not pkg:
            pkg = next((p for pre, p in dom["prefix_packages"].items() if name.startswith(pre)), None)
        if not pkg:
            # a child table (element collection or link table): the module of the longest table name it extends
            parents = [k for k in desc if name.startswith(k + "_")]
            if parents:
                pkg = desc[max(parents, key=len)]["package"]
            else:  # otherwise the module that owns most tables of its prefix
                owners = Counter(v["package"] for k, v in desc.items() if k.split("_")[0] == name.split("_")[0])
                pkg = owners.most_common(1)[0][0] if owners else None
        if pkg not in pkg_domain:
            unplaced.append(f"{name} ({pkg})")
            continue
        t = dict(t)
        t["package"] = pkg
        t["domain"] = pkg_domain[pkg]["id"]
        t["doc"] = (desc.get(name) or {}).get("doc") or ""
        tables[name] = t
    # foreign keys pointing into a table (from kept tables)
    incoming: dict[str, list] = defaultdict(list)
    for t in tables.values():
        for k in t["constraints"] or []:
            if k["type"] == "f":
                incoming[k["ref_table"]].append((t["name"], k))
    return {"tables": tables, "excluded": sorted(excluded), "unplaced": unplaced, "domains": dom["domains"],
            "excluded_meaning": dom["excluded"]["meaning"], "incoming": incoming, "views": snap.get("views", []),
            "migrations": snap.get("migrations", 0)}


TYPE_MAP = [
    (r"^character varying", "VARCHAR"), (r"^timestamp with time zone", "TIMESTAMPTZ"),
    (r"^timestamp without time zone", "TIMESTAMP"), (r"^time without time zone", "TIME"), (r"^character\b", "CHAR"),
    (r"^double precision", "DOUBLE PRECISION"),
]


def fmt_type(t: str) -> str:
    for pat, rep in TYPE_MAP:
        t = re.sub(pat, rep, t)
    return t.upper()


def fmt_default(col: dict) -> str:
    if col.get("identity") in ("a", "d"):
        return "IDENTITY (ALWAYS)" if col["identity"] == "a" else "IDENTITY (BY DEFAULT)"
    d = col.get("default")
    if d is None:
        return ""
    d = re.sub(r"::[a-z ]+(?:\[\])?(?:\(\d+(?:,\d+)?\))?", "", d)  # casts
    d = re.sub(r"nextval\('([^']+)'\)", lambda m: f"NEXTVAL({m.group(1).upper()})", d)
    parts = re.split(r"('(?:[^']|'')*')", d)
    return "".join(p if p.startswith("'") else p.upper() for p in parts)


def allowed_values(defn: str) -> list[str] | None:
    """The literal list of a CHECK (col IN (...)) / (col = ANY (ARRAY[...])) constraint."""
    m = re.match(r"^CHECK \(+\(?\w+\)?(?:::text)? = ANY \(+ARRAY\[(.*)\]\)?(?:::text\[\])?\)+$", defn)
    if not m:
        return None
    return re.findall(r"'((?:[^']|'')*)'", m.group(1))


def check_text(defn: str) -> str:
    body = re.sub(r"^CHECK \((.*)\)$", r"\1", defn)
    body = re.sub(r"::(?:text|character varying|numeric|integer|bigint|date|timestamp with time zone)(?:\[\])?", "", body)
    parts = re.split(r"('(?:[^']|'')*')", body)
    return "".join(p if p.startswith("'") else p.upper() for p in parts)


HUMAN = {"no": "number", "id": "identifier", "ref": "reference", "dt": "date", "amt": "amount", "pct": "percent",
         "qty": "quantity", "seq": "sequence", "desc": "description", "ccy": "currency", "arn": "ARN", "or": "OR",
         "ar": "AR", "tin": "TIN", "vat": "VAT", "ewt": "EWT", "cwt": "CWT", "gl": "GL", "sl": "SL", "kyc": "KYC",
         "str": "STR", "pdc": "PDC", "dv": "DV", "soa": "SOA", "bir": "BIR", "url": "URL", "sha256": "SHA-256",
         "ttl": "time to live", "uw": "underwriting", "eb": "employee benefits", "lov": "list of values",
         "mfa": "second factor", "sso": "single sign-on", "sod": "separation of duties", "ic": "Insurance Commission"}


def humanise(name: str) -> str:
    words = [HUMAN.get(w, w) for w in name.split("_")]
    text = " ".join(words)
    return text[:1].upper() + text[1:]


STANDARD = {
    "id": "Surrogate primary key",
    "version": "Optimistic-locking version, incremented on every update",
    "created_at": "Time the row was created (UTC)",
    "created_by": "User name that created the row",
    "updated_at": "Time of the last update (UTC)",
    "updated_by": "User name of the last update",
    "company_id": "Company the record belongs to (data scope)",
    "branch_id": "Branch the record belongs to (data scope)",
    "record_status": "Maker-checker status of a master record",
    "authorized_by": "User name of the checker who authorised the record (never the maker)",
    "authorized_at": "Time the record was authorised",
    "active": "Whether the record is in use",
    "remarks": "Free-text remarks",
    "currency": "ISO 4217 currency code",
}


def column_description(table: dict, col: dict, fk: dict | None, allowed: list[str] | None) -> str:
    if col.get("comment"):
        return upper_identifiers(col["comment"])
    name = col["name"]
    if name in STANDARD:
        return STANDARD[name]
    if fk:
        return f"Reference to {up(fk['ref_table'])}"
    if name.endswith("_at"):
        return humanise(name[:-3]) + " (time stamp)"
    if name.endswith("_by"):
        return humanise(name[:-3]) + " by (user name)"
    if name.endswith("_sha256"):
        return "SHA-256 checksum of " + humanise(name[:-7]).lower()
    text = humanise(name)
    if violations(text) and name.endswith("_code"):
        text = "Code of the " + humanise(name[:-5]).lower()
    return text


def table_description(t: dict) -> str:
    """First sentence of the entity documentation, cleaned for a client document; a generic text without one."""
    doc = t.get("comment") or t.get("doc") or ""
    doc = re.sub(r"\{@(?:code|literal)\s+([^}]*)\}", r"\1", doc)
    doc = re.sub(r"\{@link(?:plain)?\s+([\w.#]+)(?:\s+([^}]*))?\}", lambda m: m.group(2) or m.group(1).split(".")[-1].split("#")[-1], doc)
    doc = re.sub(r"<[^>]+>", "", doc)
    doc = re.sub(r"\s+", " ", doc).strip()
    m = re.match(r"(.+?[.!?])(?:\s+[A-Z(]|$)", doc)
    sentence = m.group(1) if m else doc
    if len(sentence) > 420:
        sentence = sentence[:417].rsplit(" ", 1)[0] + " ..."

    def drop_refs(s: str) -> str:
        return re.sub(r"\s*\((?:[^()]*?(?:\bV\d{3,4}\b|DESIGN|design|section|\.java|seed|Seed|build|built)[^()]*)\)", "", s)

    for candidate in (sentence, drop_refs(sentence), re.sub(r"\s*\([^()]*\)", "", sentence)):
        candidate = upper_identifiers(candidate).strip()
        if candidate and not violations(candidate) and not re.search(r"\b[A-Z][a-z]+(?:[A-Z][a-z0-9]+)+\b", candidate):
            return candidate
    return f"{humanise(t['name'].split('_', 1)[1] if '_' in t['name'] else t['name'])} records."


def table_facts(t: dict) -> dict[str, Any]:
    cons = t["constraints"] or []
    pk = next((k for k in cons if k["type"] == "p"), None)
    fks = [k for k in cons if k["type"] == "f"]
    uqs = [k for k in cons if k["type"] == "u"]
    checks = [k for k in cons if k["type"] == "c"]
    trig = t["triggers"] or []
    immutable = any(re.search(r"immutable|insert_only|append_only", g["name"]) for g in trig)
    return {"pk": pk["cols"] if pk else [], "fks": fks, "uqs": uqs, "checks": checks, "indexes": t["indexes"] or [],
            "triggers": trig, "immutable": immutable}


# ------------------------------------------------------------------------------------------------ dictionary rows

def dictionary_rows(cat: dict, domain_id: str) -> list[dict]:
    rows: list[dict] = []
    tables = sorted((t for t in cat["tables"].values() if t["domain"] == domain_id), key=lambda t: (t["package"], t["name"]))
    for t in tables:
        f = table_facts(t)
        fk_by_col = {}
        for k in f["fks"]:
            for i, c in enumerate(k["cols"]):
                fk_by_col[c] = {"ref_table": k["ref_table"], "ref_col": k["ref_cols"][i], "on_delete": k["on_delete"]}
        uq_cols = {c for k in f["uqs"] for c in k["cols"]}
        for x in f["indexes"]:
            if x["unique"] and not x["primary"]:
                m = re.search(r"\(([^()]*)\)", x["def"])
                if m and "," not in m.group(1) and re.fullmatch(r"\w+", m.group(1).strip()):
                    uq_cols.add(m.group(1).strip())
        col_checks: dict[str, list[str]] = defaultdict(list)
        col_allowed: dict[str, list[str]] = {}
        for k in f["checks"]:
            vals = allowed_values(k["def"])
            if vals is not None and len(k["cols"] or []) == 1:
                col_allowed[k["cols"][0]] = vals
            else:
                for c in (k["cols"] or [])[:1]:
                    col_checks[c].append(check_text(k["def"]))
        idx_by_col: dict[str, list[str]] = defaultdict(list)
        for x in f["indexes"]:
            if x["primary"]:
                continue
            m = re.search(r"USING \w+ \((.*)\)", x["def"])
            if not m:
                continue
            first = re.match(r"\(?\s*(?:lower\()?(\w+)", m.group(1))
            if first:
                idx_by_col[first.group(1)].append(up(x["name"]) + (" (unique)" if x["unique"] else ""))
        for col in t["columns"]:
            name = col["name"]
            keys = []
            if name in f["pk"]:
                keys.append("PK")
            if name in fk_by_col:
                keys.append("FK")
            if name in uq_cols:
                keys.append("UQ")
            fk = fk_by_col.get(name)
            ref = ""
            if fk:
                ref = f"{up(fk['ref_table'])}.{up(fk['ref_col'])}"
                if fk["on_delete"] == "c":
                    ref += " (delete cascades)"
            allowed = col_allowed.get(name)
            rule = ""
            if allowed is not None:
                if any(SNAKE.fullmatch(v) for v in allowed):
                    rule = ("Allowed (stored in lower case): " + ", ".join(v.upper() for v in allowed))
                else:
                    rule = "Allowed: " + ", ".join(allowed)
            if col_checks.get(name):
                rule = "; ".join(filter(None, [rule] + col_checks[name]))
            rows.append({
                "table": up(t["name"]), "pos": col["pos"], "column": up(name), "type": fmt_type(col["type"]),
                "null": "Yes" if col["nullable"] else "No", "default": fmt_default(col), "key": " ".join(keys),
                "ref": ref, "rule": rule, "index": "; ".join(idx_by_col.get(name, [])),
                "description": column_description(t, col, fk, allowed),
            })
    return rows


def index_rows(cat: dict) -> list[dict]:
    order = {d["id"]: i for i, d in enumerate(cat["domains"])}
    names = {d["id"]: d["name"] for d in cat["domains"]}
    rows = []
    for t in sorted(cat["tables"].values(), key=lambda t: (order[t["domain"]], t["package"], t["name"])):
        f = table_facts(t)
        rows.append({
            "domain": f"{t['domain']} {names[t['domain']]}", "module": t["package"].upper(), "table": up(t["name"]),
            "description": table_description(t), "columns": len(t["columns"]), "pk": ", ".join(up(c) for c in f["pk"]),
            "fk_out": ", ".join(sorted({up(k["ref_table"]) for k in f["fks"]})),
            "fk_in": len(cat["incoming"].get(t["name"], [])), "indexes": len([x for x in f["indexes"] if not x["primary"]]),
            "immutable": "Yes" if f["immutable"] else "",
            "figure": erd_figure_title(t["domain"], t["package"], cat, t["name"]),
        })
    return rows


def erd_figure_title(domain_id: str, package: str, cat: dict, table: str | None = None) -> str:
    """Name of the figure that shows a table ("listed only" for a table without a link inside its module)."""
    plan = erd_plan(cat)
    for fig in plan.get((domain_id, package), []):
        if table is None or table in fig["tables"]:
            return fig["title"]
    return "Listed only (no link)"


_PLAN: dict = {}
MAX_PER_FIGURE = 10


def _module_links(cat: dict, t: dict) -> list[tuple[dict, str]]:
    """Foreign keys of a table that a figure draws, with the referenced table (company and branch links only in the
    Organisation module)."""
    out = []
    for k in table_facts(t)["fks"]:
        if k["ref_table"] in COMMON_TARGETS and t["package"] != "organization":
            continue
        out.append((k, k["ref_table"]))
    return out


def erd_plan(cat: dict) -> dict[tuple[str, str], list[dict]]:
    """Figures per module: the connected groups of tables (by foreign key inside the module), packed into figures of
    at most MAX_PER_FIGURE tables; a larger group keeps its own figure. Tables with no drawn link are listed only."""
    if _PLAN:
        return _PLAN
    by_mod: dict[tuple[str, str], list[dict]] = defaultdict(list)
    for t in cat["tables"].values():
        by_mod[(t["domain"], t["package"])].append(t)
    for key, ts in by_mod.items():
        names = {t["name"] for t in ts}
        links = [(t["name"], ref) for t in ts for _, ref in _module_links(cat, t)]
        has_link = {a for a, _ in links} | {r for _, r in links if r in names}
        hubs: set[str] = set()
        while True:
            parent = {n: n for n in names}

            def find(x: str) -> str:
                while parent[x] != x:
                    parent[x] = parent[parent[x]]
                    x = parent[x]
                return x
            for a, r in links:
                if r in names and r not in hubs and a not in hubs:
                    parent[find(a)] = find(r)
            groups: dict[str, list[str]] = defaultdict(list)
            for n in sorted(names):
                if n in has_link and n not in hubs:
                    groups[find(n)].append(n)
            big = [g for g in groups.values() if len(g) > MAX_PER_FIGURE * 3 // 2]
            if not big:
                break
            # a hub referenced by many tables of a large group is drawn once per figure instead of joining them all
            indeg = Counter(r for a, r in links if r in big[0] and a in big[0] and r != a)
            hubs.add(indeg.most_common(1)[0][0])
        for h in sorted(hubs):  # the hub joins the group that references it most
            refs = Counter(find(a) for a, r in links if r == h and a not in hubs)
            target = refs.most_common(1)[0][0] if refs else h
            groups[target].append(h)
        comps = sorted(groups.values(), key=lambda g: (-len(g), g[0]))
        figs: list[list[str]] = []
        for g in comps:
            for f in figs:
                if len(f) + len(g) <= MAX_PER_FIGURE:
                    f.extend(g)
                    break
            else:
                figs.append(list(g))
        domain_id, pkg = key
        out = []
        for i, f in enumerate(figs, 1):
            suffix = f" ({i} of {len(figs)})" if len(figs) > 1 else ""
            out.append({"title": f"{domain_id} {pkg.upper()}{suffix}", "tables": set(f),
                        "path": ERD_DIR / f"{domain_id.lower()}_{pkg}{'_' + str(i) if len(figs) > 1 else ''}.dot"})
        _PLAN[key] = out
    return _PLAN


def build_dictionary_workbook(cat: dict) -> Path:
    wb = BdoiWorkbook("BIBS Data Dictionary", doc_type="Data dictionary", brd="BRD-00", version=VERSION, date=DATE,
                      subtitle="Tables, columns, keys and indexes of the production schema, by data domain")
    n_tables = len(cat["tables"])
    n_cols = sum(len(t["columns"]) for t in cat["tables"].values())
    wb.cover_notes = [
        f"Production schema of BIBS on PostgreSQL 16: {cat['migrations']} schema migrations applied to an empty "
        f"database, then read from the system catalogue on {DATE}. Seed data for SIT, UAT and training is not part "
        "of the schema and is not applied.",
        f"{n_tables} tables and {n_cols:,} columns in {len(cat['domains'])} data domains; one sheet per domain and "
        "the Index sheet with one row per table.",
        f"Not included: the {len(cat['excluded'])} tables of the insurer-company modules being removed from BIBS "
        "(prefixes " + ", ".join(p.upper() for p in cat["excluded_meaning"]) + "): insurer underwriting, insurer "
        "claims, reinsurance treaty and cession accounting, actuarial reserves and group consolidation. BIBS is an "
        "insurance broking system and does not use them.",
        "Identifiers are written in upper case; PostgreSQL folds unquoted names, so they match the catalogue.",
        "Diagrams: BIBS Entity Relationship Diagrams v1.0 (one figure per module, named in the Figure column of the "
        "Index sheet).",
    ]
    wb.legend = [("PK", "Primary key"), ("FK", "Foreign key"), ("UQ", "Unique (constraint or unique index)"),
                 ("Immutable", "Insert-only table: database triggers refuse UPDATE, DELETE and TRUNCATE")]
    wb.sheet("Index", [
        Column("domain", "Domain", 22, "Data domain (Data Architecture, chapter 3)"),
        Column("module", "Module", 14, "Module (bounded context) that owns the table"),
        Column("table", "Table", 30, "Table name"),
        Column("description", "Description", 60, "What one row of the table records"),
        Column("columns", "Columns", 9, "Number of columns", kind="number"),
        Column("pk", "Primary key", 14, "Primary key columns"),
        Column("fk_out", "References", 34, "Tables this table references by foreign key"),
        Column("fk_in", "Referenced by", 11, "Number of foreign keys of other tables that reference this table",
               kind="number"),
        Column("indexes", "Indexes", 9, "Secondary indexes (the primary key index not counted)", kind="number"),
        Column("immutable", "Immutable", 11, "Yes: insert-only, database triggers refuse UPDATE, DELETE and TRUNCATE"),
        Column("figure", "Figure", 18, "Figure of the Entity Relationship Diagrams document that shows the table"),
    ], index_rows(cat), description=f"{n_tables} tables of the production schema by domain and module")
    cols = [
        Column("table", "Table", 28, "Table name"),
        Column("pos", "#", 5, "Position of the column in the table", kind="number"),
        Column("column", "Column", 28, "Column name"),
        Column("type", "Data type", 18, "PostgreSQL data type (VARCHAR(n) = character varying, TIMESTAMPTZ = timestamp "
                                         "with time zone)"),
        Column("null", "Null", 7, "Yes: the column accepts NULL", values=["Yes", "No"]),
        Column("default", "Default", 20, "Default value or identity generation"),
        Column("key", "Key", 8, "PK primary key, FK foreign key, UQ unique"),
        Column("ref", "References", 26, "Referenced table and column of a foreign key"),
        Column("rule", "Allowed values and checks", 36, "Check constraints of the column"),
        Column("index", "Indexes", 30, "Secondary indexes whose first column is this column"),
        Column("description", "Description", 46, "Meaning of the column"),
    ]
    for d in cat["domains"]:
        rows = dictionary_rows(cat, d["id"])
        n = len({r["table"] for r in rows})
        wb.sheet(d["sheet"], cols, rows,
                 description=f"{d['id']} {d['name']} ({d['brd']}): {n} tables, {len(rows):,} columns. Owner: {d['owner']}")
    path = wb.save(DICTIONARY)
    return path


# ------------------------------------------------------------------------------------------------ ERD figures

def _node_label(t: dict, f: dict, drawn_fk_cols: set[str]) -> str:
    rows = [f'<tr><td bgcolor="@HEADER_BLUE" align="left" colspan="2"><font color="@WHITE" point-size="12"><b>'
            f'{html.escape(up(t["name"]))}</b></font></td></tr>']
    shown = 0
    for col in t["columns"]:
        name = col["name"]
        tag = "PK" if name in f["pk"] else ("FK" if name in drawn_fk_cols else "")
        if not tag:
            continue
        shown += 1
        rows.append(f'<tr><td align="left"><font point-size="10" color="@MUTED">{tag}</font></td>'
                    f'<td align="left" port="{name}"><font point-size="11">{html.escape(up(name))}</font></td></tr>')
    rest = len(t["columns"]) - shown
    if rest > 0:
        rows.append(f'<tr><td colspan="2" align="left"><font point-size="10" color="@MUTED">+ {rest} columns</font>'
                    "</td></tr>")
    return ('<<table border="0" cellborder="1" cellspacing="0" cellpadding="3" color="@BORDER">' + "".join(rows) +
            "</table>>")


def _scale(text: str) -> float:
    """Print scale of a figure on a landscape page (24 x 12.5 cm), from the Graphviz layout size."""
    w_cm, h_cm = _natural_cm(text)
    return min(24.0 / w_cm, 12.5 / h_cm) if w_cm else 0.0


def _natural_cm(text: str) -> tuple[float, float]:
    """Width and height of a Graphviz figure at 100 % (its fonts at their point size)."""
    import shutil  # noqa: PLC0415
    import subprocess  # noqa: PLC0415

    dot = shutil.which("dot")
    if not dot:
        return 0.0, 0.0
    for token in sorted(brand.DOT_TOKENS, key=len, reverse=True):
        text = text.replace(token, brand.DOT_TOKENS[token])
    res = subprocess.run([dot, "-Tplain"], input=text, capture_output=True, text=True, check=True)
    _, _, w, h = res.stdout.splitlines()[0].split()[:4]
    return float(w) * 2.54, float(h) * 2.54


def erd_dot(cat: dict, fig: dict) -> str:
    """The figure in the direction (left to right or top to bottom) that prints larger."""
    texts = [_erd_dot(cat, fig, d) for d in ("LR", "TB", "neato")]
    return max(texts, key=_scale)


def _erd_dot(cat: dict, fig: dict, rankdir: str) -> str:
    tables = [cat["tables"][n] for n in sorted(fig["tables"])]
    layout = ('layout=neato, overlap=prism, overlap_scaling=-4, sep="+12", splines=true, '
              if rankdir == "neato" else f"rankdir={rankdir}, ")
    lines = ['digraph erd {', f'  graph [{layout}fontname="Arial", nodesep=0.3, ranksep=0.8, pad=0.2, '
             'bgcolor="white"];',
             '  node [shape=plain, fontname="Arial"];',
             '  edge [color="@CTA_BLUE", arrowsize=0.8, penwidth=1.2, dir=both];']
    externals: dict[str, str] = {}
    edges = []
    for t in tables:
        f = table_facts(t)
        links = _module_links(cat, t)
        drawn = {c for k, _ in links for c in k["cols"]}
        lines.append(f'  "{t["name"]}" [label={_node_label(t, f, drawn)}];')
        for k, ref in links:
            if ref not in fig["tables"]:
                ext = cat["tables"].get(ref)
                externals[ref] = ext["package"].upper() if ext else "REMOVED MODULE"
            nullable = any(c["nullable"] for c in t["columns"] if c["name"] in k["cols"])
            head = "teeodot" if nullable else "teetee"
            edges.append(f'  "{t["name"]}":"{k["cols"][0]}" -> "{ref}" [arrowtail=crow, arrowhead={head}];')
    for ref, pkg in sorted(externals.items()):
        lines.append(f'  "{ref}" [shape=box, style="rounded,filled", fillcolor="@DIRTY_WHITE", color="@MUTED", '
                     f'fontsize=11, label=<<font color="@MUTED">{html.escape(up(ref))}<br/>'
                     f'<font point-size="9">{html.escape(pkg)}</font></font>>];')
    lines += edges
    lines.append("}")
    return "\n".join(lines) + "\n"


def erd_figures(cat: dict) -> list[tuple[str, str, dict]]:
    """Writes the figure sources of figures/erd; returns (domain, package, figure) in document order."""
    ERD_DIR.mkdir(parents=True, exist_ok=True)
    plan = erd_plan(cat)
    out = []
    keep = set()
    for d in cat["domains"]:
        for p in d["packages"]:
            for fig in plan.get((d["id"], p), []):
                text = erd_dot(cat, fig)
                path = fig["path"]
                keep.add(path.name)
                if not path.exists() or path.read_text(encoding="utf-8") != text:
                    path.write_text(text, encoding="utf-8")
                out.append((d["id"], p, fig))
    for old in ERD_DIR.glob("*.dot"):
        if old.name not in keep:
            old.unlink()
            old.with_suffix(".png").unlink(missing_ok=True)
    return out


# ------------------------------------------------------------------------------------------------ Word placeholders

def md_table(headers: list[str], rows: list[list[Any]], widths: str, caption: str, extra: str = "") -> list[str]:
    def cell(v: Any) -> str:
        return str(v).replace("|", "/").replace("\n", " ")
    out = [f'<!-- table: widths={widths} caption="{caption}"{extra} -->',
           "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    out += ["| " + " | ".join(cell(v) for v in r) + " |" for r in rows]
    return out + [""]


def placeholders(cat: dict, name: str, opts: str) -> list[str]:
    doms = {d["id"]: d for d in cat["domains"]}
    tables = cat["tables"]
    if name == "domain_summary":
        rows = []
        for d in cat["domains"]:
            ts = [t for t in tables.values() if t["domain"] == d["id"]]
            rows.append([d["id"], d["name"], d["brd"], ", ".join(p.upper() for p in d["packages"]
                                                            if any(t["package"] == p for t in ts)),
                         len(ts), sum(len(t["columns"]) for t in ts)])
        rows.append(["", "Total", "", "", len(tables), sum(len(t["columns"]) for t in tables.values())])
        return md_table(["ID", "Domain", "BRD", "Modules", "Tables", "Columns"], rows, "1,3.6,1.8,6.4,1.2,1.6",
                        "Data domains of the production schema", " bold=first")
    if name == "domain_owners":
        rows = [[d["id"], d["name"], d["brd"], d["owner"]] for d in cat["domains"]]
        return md_table(["ID", "Domain", "BRD", "Data owner (BDOI)"], rows, "1,3.6,1.8,8", "Data domains and their owners",
                        " bold=first")
    if name == "excluded":
        groups: dict[str, list[str]] = defaultdict(list)
        for n in cat["excluded"]:
            pre = next(p for p in cat["excluded_meaning"] if n.startswith(p))
            groups[pre].append(up(n))
        rows = [[pre.upper() + "*", cat["excluded_meaning"][pre], len(v), ", ".join(v)] for pre, v in groups.items()]
        return md_table(["Prefix", "Insurer-company module", "Tables", "Tables not included"], rows, "1.4,4,1,9",
                        "Tables of the insurer-company modules, not included", " bold=first")
    if name == "erd_domains":
        out: list[str] = []
        figs = erd_figures(cat)
        for d in cat["domains"]:
            ts = [t for t in tables.values() if t["domain"] == d["id"]]
            out += [f"## {d['id']} {d['name']}", "",
                    f"{d['summary']} Owner: {d['owner']}. BRD: {d['brd']}. "
                    f"{len(ts)} tables, {sum(len(t['columns']) for t in ts):,} columns.", ""]
            for pkg in d["packages"]:
                pts = sorted((t for t in ts if t["package"] == pkg), key=lambda t: t["name"])
                if not pts:
                    continue
                out += [f"### Module {pkg.upper()}", ""]
                out += md_table(["Table", "Description", "Cols", "Links", "Figure"],
                                [[up(t["name"]), table_description(t), len(t["columns"]),
                                  ", ".join(sorted({up(r) for _, r in _module_links(cat, t)})) or "-",
                                  erd_figure_title(d["id"], pkg, cat, t["name"])] for t in pts],
                                "3.2,8.5,0.9,3.6,2.4", f"Tables of module {pkg.upper()} ({d['id']})", " size=8")
                for dom_id, p2, fig in figs:
                    if dom_id == d["id"] and p2 == pkg:
                        rel = fig["path"].relative_to(HERE).as_posix()
                        # never print a table name larger than about 8 points
                        width = min(24.0, round(_natural_cm(fig["path"].read_text(encoding="utf-8"))[0] * 0.7, 1))
                        out += [f"![{fig['title']}: tables, primary keys and the foreign keys inside the module]"
                                f"({rel}){{width={width}}}", ""]
        return out
    if name == "erd_stats":
        n_fk = sum(len(table_facts(t)["fks"]) for t in tables.values())
        n_ix = sum(len([x for x in table_facts(t)["indexes"] if not x["primary"]]) for t in tables.values())
        n_ck = sum(len(table_facts(t)["checks"]) for t in tables.values())
        n_imm = sum(1 for t in tables.values() if table_facts(t)["immutable"])
        rows = [["Schema migrations applied", cat["migrations"]], ["Tables (in scope)", len(tables)],
                ["Columns", f"{sum(len(t['columns']) for t in tables.values()):,}"], ["Foreign keys", n_fk],
                ["Secondary indexes", n_ix], ["Check constraints", n_ck], ["Insert-only tables (triggers)", n_imm],
                ["Views", len(cat["views"])], ["Insurer tables not included", len(cat["excluded"])]]
        return md_table(["Measure", "Value"], rows, "6,2", "The production schema in figures", " bold=first")
    if name == "asvs_summary":
        rows = asvs_summary_rows()
        n = len(rows[0])
        return md_table(rows[0], rows[1:], ",".join(["5"] + ["1.6"] * (n - 1)),
                        "OWASP ASVS 4.0.3 Level 2: requirements per chapter and status", " bold=first")
    if name == "owasp_top10":
        data = yaml.safe_load((HERE / "asvs_mapping.yaml").read_text(encoding="utf-8"))
        return md_table(["ID", "Category", "BIBS controls", "Verified by"],
                        [[r["id"], r["name"], r["controls"], r["verify"]] for r in data["owasp_top10"]],
                        "1,2.6,8,4", "OWASP Top 10 (2021) and the BIBS controls", " bold=first size=8.5")
    if name == "asvs_gaps":
        data = yaml.safe_load((HERE / "asvs_mapping.yaml").read_text(encoding="utf-8"))
        rows = [[r["id"], r["status"], r["gap"], r["owner"], r["due"]] for r in data["requirements"]
                if r["status"] in ("Partly met", "Planned", "BDOI") and r.get("gap")]
        return md_table(["ASVS", "Status", "Gap and action", "Owner", "Due"], rows, "1.2,1.4,9,2.4,1.8",
                        "ASVS Level 2 rows with an action", " size=8 status=Status")
    raise KeyError(f"unknown placeholder sd:{name}")


def expand(cat: dict | None, lines: list[str]) -> list[str]:
    out: list[str] = []
    for line in lines:
        m = PLACEHOLDER.match(line)
        if not m:
            out.append(line)
            continue
        if cat is None:
            raise KeyError(f"placeholder sd:{m.group(1)} needs the catalogue")
        out += placeholders(cat, m.group(1), m.group(2))
    return out


def build_doc(src: Path, cat: dict | None, pdf: bool, keep_pdf: bool):
    front, lines = load_source(src)
    doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
    doc.cover()
    doc.front_matter()
    render_body(doc, expand(cat, lines))
    return doc.publish(output_path(front, src), pdf=pdf, keep_pdf=keep_pdf)


# ------------------------------------------------------------------------------------------------ ASVS workbook

def build_asvs_workbook() -> Path:
    data = yaml.safe_load((HERE / "asvs_mapping.yaml").read_text(encoding="utf-8"))
    reqs = {r["id"]: r for r in data["requirements"]}
    rows = []
    for rid, r in reqs.items():
        rows.append({"id": rid, "chapter": r["chapter"], "section": r["section"], "requirement": r["text"],
                     "status": r["status"], "how": r["how"], "evidence": r["evidence"], "gap": r.get("gap") or "-",
                     "owner": r.get("owner", "iorta TechNXT"), "due": r.get("due", "")})
    wb = BdoiWorkbook("BIBS ASVS Level 2 Control Mapping", doc_type="Security control mapping", brd="BRD-00",
                      version=VERSION, date=DATE,
                      subtitle="OWASP ASVS 4.0.3 Level 2 requirements against the BIBS controls, with evidence and gaps")
    wb.legend = [(s["status"], s["meaning"]) for s in data["statuses"]]
    wb.cover_notes = data["cover_notes"]
    statuses = [s["status"] for s in data["statuses"]]
    wb.sheet("ASVS L2 Mapping", [
        Column("id", "ASVS ID", 9, "Requirement identifier of OWASP ASVS 4.0.3"),
        Column("chapter", "Chapter", 16, "ASVS chapter"),
        Column("section", "Section", 18, "ASVS section"),
        Column("requirement", "Requirement (ASVS 4.0.3, wording adapted)", 48,
               "The Level 2 requirement, in the wording of ASVS 4.0.3 adapted to this document's terms"),
        Column("status", "Status", 12, "Met, Partly met, Planned, BDOI (met by a BDOI or BDO platform service), "
               "Not applicable", values=statuses, status=True),
        Column("how", "How BIBS meets it", 52, "The control in BIBS"),
        Column("evidence", "Evidence (code, configuration, pipeline)", 44, "Where an assessor verifies it"),
        Column("gap", "Gap and action", 40, "What is missing and the action, owner and date"),
        Column("owner", "Owner", 16, "Owner of the action or of the control"),
        Column("due", "Due", 12, "Date of the action"),
    ], rows, description=f"{len(rows)} Level 2 requirements of OWASP ASVS 4.0.3")
    counts = Counter((r["chapter"], r["status"]) for r in rows)
    chapters = list(dict.fromkeys(r["chapter"] for r in rows))
    summary = []
    for ch in chapters:
        row = {"chapter": ch}
        for s in statuses:
            row[s] = counts.get((ch, s), 0)
        row["total"] = sum(counts.get((ch, s), 0) for s in statuses)
        summary.append(row)
    total = {"chapter": "Total", **{s: sum(r[s] for r in summary) for s in statuses}}
    total["total"] = sum(total[s] for s in statuses)
    summary.append(total)
    wb.sheet("Summary", [Column("chapter", "Chapter", 40, "ASVS chapter")] +
             [Column(s, s, 12, f"Requirements with status {s}", kind="number") for s in statuses] +
             [Column("total", "Total", 10, "Level 2 requirements of the chapter", kind="number")],
             summary, description="Level 2 requirements per chapter and status")
    top10 = data["owasp_top10"]
    wb.sheet("OWASP Top 10 2021", [
        Column("id", "ID", 8, "OWASP Top 10 2021 category"), Column("name", "Category", 26, "Category name"),
        Column("controls", "BIBS controls", 70, "The controls of BIBS that address the category"),
        Column("asvs", "ASVS chapters", 18, "Related ASVS chapters (sheet ASVS L2 Mapping)"),
        Column("verify", "Verified by", 40, "How the control is verified"),
    ], top10, description="OWASP Top 10 (2021) against the BIBS controls")
    return wb.save(ASVS_BOOK)


def asvs_summary_rows() -> list[list[Any]]:
    data = yaml.safe_load((HERE / "asvs_mapping.yaml").read_text(encoding="utf-8"))
    statuses = [s["status"] for s in data["statuses"]]
    counts = Counter((r["chapter"], r["status"]) for r in data["requirements"])
    chapters = list(dict.fromkeys(r["chapter"] for r in data["requirements"]))
    rows = [[ch] + [counts.get((ch, s), 0) for s in statuses] + [sum(counts.get((ch, s), 0) for s in statuses)]
            for ch in chapters]
    rows.append(["Total"] + [sum(r[i + 1] for r in rows) for i in range(len(statuses))] + [sum(r[-1] for r in rows)])
    return [["Chapter"] + statuses + ["Total"]] + rows


# ------------------------------------------------------------------------------------------------ checks

def check(cat: dict) -> list[str]:
    problems = []
    if cat["unplaced"]:
        problems.append("tables without a domain: " + ", ".join(cat["unplaced"]))
    for d in cat["domains"]:
        if not any(t["domain"] == d["id"] for t in cat["tables"].values()):
            problems.append(f"domain {d['id']} has no table")
    for t in cat["tables"].values():
        text = table_description(t)
        if violations(text):
            problems.append(f"{t['name']}: description wording {violations(text)}")
    for d in cat["domains"]:
        for r in dictionary_rows(cat, d["id"]):
            for k, v in r.items():
                if isinstance(v, str) and violations(v):
                    problems.append(f"{d['id']} {r['table']}.{r['column']} {k}: {violations(v)}")
    return problems


def check_sources() -> list[str]:
    problems = []
    for src in ("SECURITY_ARCHITECTURE.md", "DATA_ARCHITECTURE.md", "ERD_DATA_DICTIONARY.md"):
        p = HERE / src
        if not p.exists():
            continue
        problems += [f"{src}: {x}" for x in lint_source(p)]
        _, lines = load_source(p)
        for i, line in enumerate(lines, 1):
            if line.lstrip().startswith("<!--"):
                continue
            line = re.sub(r"\]\([^)]*\)(\{[^}]*\})?", "]", line)  # figure paths are not printed
            v = violations(line)
            if v:
                problems.append(f"{src}:{i}: {v}")
    p = HERE / "asvs_mapping.yaml"
    if p.exists():
        data = yaml.safe_load(p.read_text(encoding="utf-8"))
        for r in data["requirements"]:
            for k in ("text", "how", "evidence", "gap"):
                v = violations(str(r.get(k) or ""))
                if v:
                    problems.append(f"ASVS {r['id']} {k}: {v}")
        for r in data["owasp_top10"]:
            for k, val in r.items():
                v = violations(str(val))
                if v:
                    problems.append(f"Top 10 {r['id']} {k}: {v}")
    return problems


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Security and Data documents (BRD-00)")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--only", choices=["dictionary", "security", "data"])
    ap.add_argument("--previews", action="store_true")
    ap.add_argument("--no-pdf", action="store_true")
    args = ap.parse_args(argv)
    cat = load_catalog()
    problems = check(cat) + check_sources()
    for p in problems:
        print("problem:", p)
    if args.check:
        print(f"{len(cat['tables'])} tables, {len(cat['excluded'])} excluded, {len(problems)} problem(s)")
        return 1 if problems else 0
    if problems:
        return 1
    built: list[Path] = []
    pdf = not args.no_pdf
    if args.only in (None, "dictionary"):
        built.append(build_dictionary_workbook(cat))
        built.append(build_doc(HERE / "ERD_DATA_DICTIONARY.md", cat, pdf, args.previews)[0])
    if args.only in (None, "security"):
        built.append(build_asvs_workbook())
        built.append(build_doc(HERE / "SECURITY_ARCHITECTURE.md", cat, pdf, args.previews)[0])
    if args.only in (None, "data"):
        built.append(build_doc(HERE / "DATA_ARCHITECTURE.md", cat, pdf, args.previews)[0])
    for p in built:
        print(f"built: {p.relative_to(REPO)} ({p.stat().st_size / 1e6:.2f} MB)")
    if args.previews:
        import render  # noqa: PLC0415

        for p in built:
            pdf_path = p.with_suffix(".pdf")
            if p.suffix == ".docx" and pdf_path.exists():
                sheets = render.previews(pdf_path)
                print(f"previews: {sheets[0].parent}")
                pdf_path.unlink()
    return 0


if __name__ == "__main__":
    sys.exit(main())
