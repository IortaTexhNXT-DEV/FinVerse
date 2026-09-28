"""BRD-13 Data Migration business sign-off set (release set v2.0, Drop 0): the migration catalogue, its checks, the
tables of the Handbook and of the Start Here guide (```pack blocks) and the 03 Migration Workbook.

Usage
  python docs/deliverables/src/BRD-13_Data_Migration/build_dm_pack.py --check            # checks only
  python docs/deliverables/src/BRD-13_Data_Migration/build_dm_pack.py                    # 03 Migration Workbook
  python docs/deliverables/src/BRD-13_Data_Migration/build_dm_pack.py --out DIR          # workbook into DIR

The other documents of the set use the shared builders (docs/deliverables/src/signoff/README.md):
  python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-13_Data_Migration/START_HERE_BRD13.md      # 00
  python docs/deliverables/src/signoff/build_guide_deck.py BRD-13                                              # 01
  python tools/deliverables/bdoi_docx.py docs/deliverables/src/BRD-13_Data_Migration/HANDBOOK_BRD13_DATA_MIGRATION.md  # 02
  python docs/deliverables/src/testplans/build_test_plan.py docs/deliverables/src/BRD-13_Data_Migration/brd13_cases.yaml  # 04, 05

What it reads
  * the layout definitions that the Migration Console holds and exports as load templates (its seed catalogue:
    the layouts in force and their columns, the code map sets, the data-quality rules and the masking rules), so
    the data requirements of the workbook are the console templates column for column;
  * the column list of the control-file template and the "How to fill" rules of the console workbook;
  * pack/catalogue.yaml  objects with their proposed decisions, owners and extract planning; milestones; open
                         decisions; proposed business rules for confirmation; reconciliation and verification plan;
  * pack/cutover.yaml    the production cut-over plan: roles, phases, calendar, tasks, checkpoints, freeze windows,
                         rollback, communication, hypercare and decommissioning;
  * pack/pack.yaml and the rest of the sign-off pack (screens, messages, notifications, contract, guide), through
    docs/deliverables/src/signoff/signoff_pack.py.

What it writes
  * 03_BIBS_Workbook_BRD-13_Data_Migration_v<version>.xlsx in the BRD-13 release-set folder (brand.out_dir): the
    migration sheets of this module, then the screen sheets, comments log, meeting minutes, version history and
    sign-off certificate of signoff_pack.build_workbook;
  * the Handbook and Start Here tables through render(doc, render=<name>, source=<pack.yaml>).
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from functools import cached_property
from pathlib import Path
from typing import Any

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
PACK_DIR = HERE / "pack"
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
sys.path.insert(0, str(REPO / "docs" / "deliverables" / "src" / "signoff"))
import brand  # noqa: E402

# The catalogue of the Migration Console (its seed data) and the template export of the console.
SEED_SQL = REPO / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V1081__migration_objects_maps.sql"
# Later scripts that update the texts of the V1081 catalogue (applied in version order after the seed).
SEED_UPDATES = [SEED_SQL.parent / "V1090__migration_catalogue_wording.sql",
                SEED_SQL.parent / "V1091__migration_rule_wording.sql"]
JAVA = REPO / "backend" / "src" / "main" / "java" / "com" / "iortatechnxt" / "brokerverse"
TEMPLATE_EXPORT = JAVA / "migration" / "mapping" / "service" / "TemplateExport.java"
CODE_MAP_EXCEL = JAVA / "migration" / "mapping" / "service" / "CodeMapExcel.java"
REPORT_ROOTS = [JAVA / "migration" / "report", JAVA / "prodrecon" / "report", JAVA / "cashiering" / "report"]

CLASS_LABEL = {"MIGRATE": "Migrate", "CARRY_FORWARD": "Carry forward", "CONDITIONAL": "Conditional",
               "ARCHIVE": "Archive", "EXCLUDED": "Excluded"}
CATEGORY_LABEL = {"REFERENCE": "Reference", "CLIENT": "Client", "POLICY": "Policy", "OPEN_ITEM": "Open item",
                  "GL": "GL", "HISTORY": "History"}
TYPE_LABEL = {"TEXT": "Text", "CODE": "Code", "DATE": "Date", "TIMESTAMP": "Timestamp", "AMOUNT": "Amount",
              "INTEGER": "Integer", "DECIMAL": "Decimal", "FLAG": "Flag"}
HASH_LABEL = {"DISTINCT_COUNT": "Count of distinct keys", "ROW_COUNT": "Row count", "NUMERIC_SUM": "Sum of the "
              "numeric part of the key"}
MASK_LABEL = {"PERSON_NAME": "Substitute name", "LAST_NAME": "Substitute last name", "FIRST_NAME": "Substitute first "
              "name", "CORPORATE_NAME": "Substitute company name", "ADDRESS": "Substitute address", "DIGITS":
              "Digits replaced, length kept", "EMAIL": "Substitute e-mail address", "BIRTH_DATE": "Date shifted by "
              "up to 30 days", "FREE_TEXT": "Text replaced"}
KIND_LABEL = {"MANDATORY": "Mandatory", "FORMAT": "Format", "LOOKUP": "Code map", "UNIQUE": "Unique",
              "REFERENTIAL": "Reference", "CROSS_FIELD": "Cross-field", "BALANCE": "Balance",
              "PLAUSIBILITY": "Plausibility", "DUPLICATE_CLIENT": "Duplicate client"}
SEVERITY_LABEL = {"ERROR": "Error - the row is not loaded", "WARNING": "Warning - loaded and reported"}
SOURCES = ["EBIX", "QPS", "ISYS", "CMS", "EXCEL"]
FILLER = ("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art", "best-in-class",
          "world-class")
DECISION_VALUES = ["Agree", "Agree with change", "Disagree", "Need more information"]

# Client wording for the few references of the seed texts that name a record store of BIBS instead of what the user
# sees. Applied to every text taken from the seed catalogue; the check lists any reference left.
CLIENT_WORDING = [
    (r"\(cat_product\.line_code\)", "(line of the product in the product master)"),
    (r"Code map GL_ACCOUNT to coa_account", "GL account code map to the chart of accounts"),
    (r"\bcoa_account codes\b", "Account codes of the BIBS chart of accounts"),
    (r"The payee migration handler exists \(DISB_PAYEE_MIGRATION\); in scope if BDOI confirms\.",
     "Loaded through the payee registration of Disbursement if BDOI confirms (DMQ30)."),
    (r"Loaded through the Submitted Policies migration handler \(SBM_MIGRATION\) if BDOI confirms; layout issued with "
     r"that module\.", "Loaded through the policy registration of Submitted Policies if BDOI confirms (DMQ30); the layout "
     "is issued with the Submitted Policies set."),
    (r"Loaded through EB_PROGRAMME_LOAD if BDOI confirms; layout issued with the EB module\.",
     "Loaded through the programme set-up of Employee Benefits if BDOI confirms (DMQ30); the layout is issued with the "
     "Employee Benefits set."),
    (r"Loaded through the Claims migration \(BCL_CLAIM_MIGRATION, V1025 held\) if BDOI decides; closed claims are "
     r"archived\.", "Loaded through the claim registration of Claims if BDOI decides (DMQ30); closed claims are archived."),
    (r"\bupp_ref of F02\b", "legacy_upp_ref of F02"),
    (r"whose balance F01 or F02 rebuilds in detail", "whose balance F01 or F02 carries in detail"),
    (r"\bthe crm formats\b", "the client formats of BIBS"),
    (r"\bMIG_GOLIVE_RENEWAL_TO\b", "go-live renewal window"),
    (r"\biorta \(load order\)", "iorta TechNXT (load order)"),
    # Layout H01 (archive): the legacy details column; the platform wording fix of these texts is requested.
    (r"^All other legacy columns as a JSON object of label and value$",
     "All other legacy columns as label and value pairs, as in the example"),
    (r"^JSON object$", "Label and value pairs in braces, as in the example"),
    (r"^Valid JSON$", "Label and value pairs as in the example"),
]


def client_text(text: Any) -> str:
    """A seed text in client wording (see CLIENT_WORDING)."""
    out = "" if text is None else str(text)
    for pattern, repl in CLIENT_WORDING:
        out = re.sub(pattern, repl, out)
    return out


# ============================================================================ seed catalogue of the console


def sql_literals(text: str) -> list[Any]:
    """The top-level comma-separated SQL literals of a VALUES or SELECT list: strings unquoted, null as None."""
    out: list[str] = []
    cur, depth, quoted, i = "", 0, False, 0
    while i < len(text):
        ch = text[i]
        if quoted:
            if ch == "'" and text[i + 1:i + 2] == "'":
                cur += "''"
                i += 2
                continue
            quoted = ch != "'"
            cur += ch
        elif ch == "'":
            quoted = True
            cur += ch
        elif ch in "()":
            depth += 1 if ch == "(" else -1
            cur += ch
        elif ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
        i += 1
    if cur.strip():
        out.append(cur.strip())

    def value(c: str) -> Any:
        if c.lower() == "null":
            return None
        if c.startswith("'") and c.endswith("'"):
            return c[1:-1].replace("''", "'")
        return c

    return [value(c) for c in out]


def seed_rows(table: str, text: str) -> list[dict[str, Any]]:
    """The rows one seed script inserts into a table (single-row VALUES, or SELECT ... FROM mig_layout WHERE ...)."""
    rows = []
    pattern = (rf"^insert into {table} \(([^)]*)\) (?:values \((.*)\)|select (.*) from mig_layout where code = "
               rf"'([^']+)' and version_no = (\d+));$")
    for m in re.finditer(pattern, text, re.M):
        cols = [c.strip() for c in m.group(1).split(",")]
        if m.group(2) is not None:
            vals = sql_literals(m.group(2))
        else:
            vals = sql_literals(m.group(3))
            vals[0] = (m.group(4), int(m.group(5)))
        if len(cols) != len(vals):
            raise SystemExit(f"{table}: {len(cols)} columns and {len(vals)} values in {m.group(0)[:100]}")
        rows.append(dict(zip(cols, vals)))
    return rows


def apply_updates(tables: dict[str, list[dict[str, Any]]], text: str) -> None:
    """Applies the "update <table> set col = '...'[, ...] where ..." statements of a later script to the seed rows.
    The where clause is a list of col = 'value' / number conditions; a column row is found by its layout code, version
    and seq ("layout_id = (select id from mig_layout where code = 'X' and version_no = n)")."""
    for m in re.finditer(r"^update (\w+) set (.*?) where (.*?);$", text, re.M):
        table, assigns, where = m.group(1), m.group(2), m.group(3)
        values = {}
        for part in re.finditer(r"(\w+) = ('(?:[^']|'')*'|[\w.]+)", assigns):
            values[part.group(1)] = sql_literals(part.group(2))[0]
        sub = re.search(r"layout_id = \(select id from mig_layout where code = '([^']+)' and version_no = (\d+)\)", where)
        conds = {k: sql_literals(v)[0] for k, v in re.findall(r"(\w+) = ('(?:[^']|'')*'|\d+)", re.sub(r"\(.*\)", "", where))}
        if sub:
            conds["layout_id"] = (sub.group(1), int(sub.group(2)))
        hit = 0
        for row in tables.get(table, []):
            if all(str(row.get(k)) == str(v) for k, v in conds.items()):
                row.update(values)
                hit += 1
        if hit == 0:
            raise SystemExit(f"{table}: no seed row for the update where {where}")


@dataclass
class Layout:
    code: str
    version: int
    object: str
    title: str
    key: list[str]
    hash_rule: str
    hash_columns: list[str]
    amounts: list[str]
    columns: list[dict[str, Any]] = field(default_factory=list)

    @property
    def sheet_name(self) -> str:
        """The sheet of the layout in the console workbook (TemplateExport.sheetName): code and title, 31 characters."""
        name = f"{self.code} " + re.sub(r"[\\/?*\[\]:]", " ", self.title)
        return name[:31].strip() if len(name) > 31 else name

    @property
    def template(self) -> str:
        return f"{self.code}_template.csv"


def java_strings(expr: str) -> str:
    """The text of a Java string expression made of literals joined with +."""
    return "".join(bytes(s, "utf-8").decode("unicode_escape") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', expr))


# ============================================================================ catalogue


# Columns of a code map version in Excel, as the Code Maps screen exports and imports it.
CODE_MAP_COLUMNS = ["source_system", "legacy_code", "legacy_description", "qualifier", "qualifier_value", "action",
                    "target_code", "remarks"]

# Archive layouts shared by the history objects that are archived (closed records and documents).
ARCHIVE_LAYOUTS = ("H01", "H02")

class Catalogue:
    """Everything the migration part of the set is built from."""

    def __init__(self) -> None:
        sql = SEED_SQL.read_text(encoding="utf-8")
        tables = {name: seed_rows(name, sql) for name in ("mig_data_object", "mig_layout", "mig_layout_column",
                                                          "mig_rule", "mig_code_map_set", "mig_masking_rule")}
        for script in SEED_UPDATES:
            if script.exists():
                apply_updates(tables, script.read_text(encoding="utf-8"))
        self.seed_objects = {r["code"]: r for r in tables["mig_data_object"]}
        self.layouts: dict[str, Layout] = {}
        for r in tables["mig_layout"]:
            if r["status"] != "FROZEN":
                continue
            self.layouts[r["code"]] = Layout(
                code=r["code"], version=int(r["version_no"]), object=r["object_code"], title=r["title"],
                key=[k.strip() for k in (r["key_columns"] or "").split(",") if k.strip()], hash_rule=r["hash_rule"],
                hash_columns=[k.strip() for k in (r["hash_columns"] or "").split(",") if k.strip()],
                amounts=[k.strip() for k in (r["amount_columns"] or "").split(",") if k.strip()])
        for r in tables["mig_layout_column"]:
            code, version = r["layout_id"]
            layout = self.layouts.get(code)
            if layout and layout.version == version:
                layout.columns.append(r)
        for layout in self.layouts.values():
            layout.columns.sort(key=lambda c: int(c["seq"]))
        self.rules = tables["mig_rule"]
        self.map_sets = tables["mig_code_map_set"]
        self.masking = tables["mig_masking_rule"]
        java = TEMPLATE_EXPORT.read_text(encoding="utf-8")
        block = re.search(r"CONTROL_COLUMNS\s*=\s*List\.of\((.*?)\);", java, re.S)
        self.control_columns = re.findall(r'"([a-z_]+)"', block.group(1)) if block else []
        how = re.search(r"HOW_TO_FILL\s*=\s*List\.of\((.*?)\);\n", java, re.S)
        self.how_to_fill: list[tuple[str, str]] = []
        for item in re.findall(r"List\.of\(\s*(\"[^\"]*\")\s*,\s*((?:\"(?:[^\"\\]|\\.)*\"\s*\+?\s*)+)\)",
                               how.group(1) if how else ""):
            self.how_to_fill.append((java_strings(item[0]), java_strings(item[1])))
        data = yaml.safe_load((PACK_DIR / "catalogue.yaml").read_text(encoding="utf-8"))
        self.data = data
        self.objects: list[dict[str, Any]] = data["objects"]
        self.by_code = {o["code"]: o for o in self.objects}
        self.milestones: list[list[str]] = data["milestones"]
        self.decisions: list[list[str]] = data["decisions"]
        self.register_refs: dict[str, str] = data.get("register_refs") or {}
        self.proposals: list[list[str]] = data["proposals"]
        self.reconciliation: list[dict[str, Any]] = data["reconciliation"]
        self.cutover: dict[str, Any] = yaml.safe_load((PACK_DIR / "cutover.yaml").read_text(encoding="utf-8"))

    # ------------------------------------------------------------------ helpers

    def milestone_text(self, ref: Any) -> str:
        """'M4; final at T-2' -> 'M4 (9 Apr 2027); final at T-2'."""
        dates = {m[0]: m[2] for m in self.milestones}
        return re.sub(r"\bM(\d)\b", lambda m: f"M{m.group(1)} ({dates.get('M' + m.group(1), '?')})", str(ref))

    def map_label(self, code: str) -> str:
        labels = self.data.get("map_labels") or {}
        if code in labels:
            return labels[code]
        prefix = code.split(":", 1)[0] + ":*"
        return labels.get(prefix, code)

    def map_due(self, code: str) -> str:
        due = self.data.get("map_due") or {}
        return self.milestone_text(due.get(code, due.get("default", "")))

    def layouts_of(self, obj: str) -> list[Layout]:
        return [layout for layout in self.layouts.values() if layout.object == obj]

    @cached_property
    def reports(self) -> dict[str, tuple[str, str]]:
        """The reports of the migration and of the legacy items: code -> (name, what it shows)."""
        out: dict[str, tuple[str, str]] = {}
        for root in REPORT_ROOTS:
            for p in sorted(root.rglob("*.java")):
                text = p.read_text(encoding="utf-8")
                for m in re.finditer(r'"((?:MIG|PRC-LEGACY|CSH-UPP)-[A-Z0-9-]+)",\s*"([^"]+)",\s*((?:"(?:[^"\\]|\\.)*"'
                                     r'\s*\+?\s*)+)', text):
                    out.setdefault(m.group(1), (m.group(2), java_strings(m.group(3))))
        return out

    def recon_group(self, code: str) -> dict[str, Any] | None:
        return next((g for g in self.reconciliation if code in g["objects"]), None)

    @property
    def field_names(self) -> set[str]:
        """Every column name of the extract layouts and of the control file: the agreed interface of the files BDOI
        extracts (the header row of each template), business names although written in lower case."""
        names = {c["name"] for layout in self.layouts.values() for c in layout.columns}
        return names | set(self.control_columns) | set(CODE_MAP_COLUMNS)

    # ------------------------------------------------------------------ checks

    def check(self) -> list[str]:
        problems: list[str] = []
        classes = set(CLASS_LABEL)
        for o in self.objects:
            code = o["code"]
            seed = self.seed_objects.get(code)
            if seed is None:
                problems.append(f"object {code}: not in the console catalogue")
                continue
            if o["decision"] not in classes:
                problems.append(f"object {code}: decision {o['decision']}")
            if o["decision"] != seed["proposed_class"]:
                problems.append(f"object {code}: class {o['decision']} but the console proposes {seed['proposed_class']}")
            if CATEGORY_LABEL.get(seed["category"], "").lower() != str(o["category"]).lower():
                problems.append(f"object {code}: category {o['category']} but the console has {seed['category']}")
            if int(o["order"] or 0) != int(seed["load_order"] or 0):
                problems.append(f"object {code}: load order {o['order']} but the console has {seed['load_order']}")
            seed_src = {s.strip().upper() for s in seed["source_systems"].split(",")}
            yaml_src = {s.strip().upper() for s in re.split(r"[,/]", str(o["source"])) if s.strip().upper() in SOURCES}
            if not yaml_src <= seed_src:
                problems.append(f"object {code}: sources {sorted(yaml_src)} not all in the console ({sorted(seed_src)})")
            own = sorted(x.code for x in self.layouts_of(code))
            shared = o["decision"] == "ARCHIVE" and set(o["layouts"]) <= set(ARCHIVE_LAYOUTS) and not own
            if not shared and sorted(o["layouts"]) != own:
                problems.append(f"object {code}: layouts {o['layouts']} but the console has "
                                f"{[x.code for x in self.layouts_of(code)]}")
            if o["decision"] in ("MIGRATE", "CARRY_FORWARD") and not o["layouts"] and code != "F05":
                problems.append(f"object {code}: {o['decision']} without a layout")
        for code in self.seed_objects:
            if code not in self.by_code:
                problems.append(f"object {code} of the console is not in catalogue.yaml")
        for layout in self.layouts.values():
            names = [c["name"] for c in layout.columns]
            if not names:
                problems.append(f"layout {layout.code}: no columns")
            for k in layout.key + layout.amounts:
                if k not in names:
                    problems.append(f"layout {layout.code}: {k} is not a column")
        if [f[0] for f in self.data.get("control_fields") or []] != self.control_columns:
            problems.append(f"control_fields differ from the control-file template of the console {self.control_columns}")
        java_cols = re.findall(r'"([a-z_]+)"', re.search(r"COLUMNS\s*=\s*List\.of\((.*?)\);",
                                                           CODE_MAP_EXCEL.read_text(encoding="utf-8"), re.S).group(1))
        if java_cols != CODE_MAP_COLUMNS:
            problems.append(f"code map columns differ from the Excel of the Code Maps screen {java_cols}")
        if not self.how_to_fill:
            problems.append("the How to fill rules of the console workbook were not read")
        decision_ids = {d[0] for d in self.decisions}
        for o in self.objects:
            for q in re.findall(r"(DMQ\d\d|DCR-\d+)", str(o["questions"])):
                if q not in decision_ids:
                    problems.append(f"object {o['code']}: question {q} is not in decisions")
        for p in self.proposals:
            if len(p) != 7:
                problems.append(f"proposal {p[0]}: {len(p)} items, expected 7")
                continue
            for q in re.findall(r"(DMQ\d\d|DCR-\d+)", p[5]):
                if q not in decision_ids:
                    problems.append(f"proposal {p[0]}: {q} is not in decisions")
        covered = {c for g in self.reconciliation for c in g["objects"]}
        for o in self.objects:
            archived = o["decision"] == "ARCHIVE" and set(o["layouts"]) <= set(ARCHIVE_LAYOUTS)
            if o["layouts"] and o["code"] not in covered and not (archived and covered & set(ARCHIVE_LAYOUTS)):
                problems.append(f"object {o['code']}: no reconciliation group")
        cut = self.cutover
        ids = [t[0] for t in cut["tasks"]]
        roles = {r[0] for r in cut["roles"]}
        if len(ids) != len(set(ids)):
            problems.append("cutover tasks: duplicate ids")
        for t in cut["tasks"]:
            if len(t) != 10:
                problems.append(f"task {t[0]}: {len(t)} items, expected 10")
                continue
            for pred in [p.strip() for p in str(t[7]).split(",") if p.strip()]:
                if pred not in ids:
                    problems.append(f"task {t[0]}: predecessor {pred} unknown")
                elif ids.index(pred) > ids.index(t[0]):
                    problems.append(f"task {t[0]}: predecessor {pred} comes later in the list")
            if t[5] not in roles:
                problems.append(f"task {t[0]}: owner {t[5]} not in roles")
        for r in cut["rollback"]:
            if r[3] not in roles:
                problems.append(f"rollback {r[0]}: owner {r[3]} not in roles")
        texts = [yaml.safe_dump(self.data), yaml.safe_dump(cut)] + [self._seed_texts()]
        for text in texts:
            for word in FILLER:
                if re.search(rf"\b{word}\b", text, re.I):
                    problems.append(f"filler word: {word}")
        problems += self.wording_problems()
        return problems

    def _seed_texts(self) -> str:
        parts = []
        for layout in self.layouts.values():
            parts.append(layout.title)
            for c in layout.columns:
                parts += [client_text(c.get(k)) for k in ("description", "allowed_values", "format", "example",
                                                          "validation")]
        for r in self.rules:
            parts += [client_text(r.get(k)) for k in ("columns", "description", "message", "fixed_by")]
        for s in self.map_sets:
            parts += [client_text(s.get(k)) for k in ("name", "used_by", "owner_title", "steward_title")]
        return "\n".join(parts)

    def wording_problems(self) -> list[str]:
        """Development-status wording and internal references in the texts of the set, with the patterns of
        tools/deliverables/check_pack.py when it has them (the extract column names are allowed)."""
        try:
            import check_pack  # noqa: PLC0415
        except ImportError:
            return []
        patterns = getattr(check_pack, "BUILD_STATUS", None)
        if not patterns:
            return []
        allowed = self.field_names
        hits: set[str] = set()
        def values(node: Any) -> list[str]:
            if isinstance(node, dict):
                return [v for x in node.values() for v in values(x)]
            if isinstance(node, list):
                return [v for x in node for v in values(x)]
            return [str(node)] if isinstance(node, str) else []

        texts = [self._seed_texts(), "\n".join(values(self.data)), "\n".join(values(self.cutover))]
        for text in texts:
            for line in text.splitlines():
                for label, pattern in patterns:
                    for m in pattern.finditer(line):
                        if label == "internal code" and m.group(0) in allowed:
                            continue
                        hits.add(f"{label}: {m.group(0).strip()}")
        return [f"wording: {h}" for h in sorted(hits)]


_CAT: Catalogue | None = None


def catalogue() -> Catalogue:
    global _CAT
    if _CAT is None:
        _CAT = Catalogue()
    return _CAT


def deck_table(source: str) -> tuple[list[str], list[list[str]], list[float]]:
    """A table of the 01 Guide deck (guide.yaml module_slides with plugin build_dm_pack.py)."""
    cat = catalogue()
    if source == "objects":
        rows: dict[str, list[str]] = {}
        for o in cat.objects:
            rows.setdefault(CLASS_LABEL[o["decision"]], []).append(f"{o['code']} {str(o['name']).split(' (')[0]}")
        return (["Decision (proposed)", "Objects"],
                [[k, "; ".join(v)] for k, v in rows.items()], [2.6, 10.0])
    raise ValueError(f"deck table {source}: unknown")


def extract_field_names() -> set[str]:
    """The column names of the extract layouts and of the control file (for tools/deliverables/check_pack.py)."""
    return catalogue().field_names


# ============================================================================ Word tables (```pack blocks)


def _table(doc: Any, headers: list[str], rows: list[list[Any]], widths: list[float], caption: str,
           size: float = 8.5, bold_first: bool = True, status: list[int] | None = None) -> None:
    doc.table(headers, [["-" if v in (None, "") else str(v) for v in r] for r in rows], widths=widths,
              caption=caption, size=size, first_col_bold=bold_first, keep_rows=False, status_cols=status or [])


def r_objects(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[o["code"], o["name"], CLASS_LABEL[o["decision"]], o["rationale"]] for o in cat.objects]
    _table(doc, ["Object", "Data object", "Decision", "Rationale"], rows, [1.3, 4.6, 2.1, 8.6],
           "Data objects and proposed decisions (confirmed by the business owner at gate G1)", size=8)


def r_criteria(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = []
    for o in cat.objects:
        s = cat.seed_objects[o["code"]]
        yes = lambda flag: "Yes" if str(flag).lower() == "true" else "No"  # noqa: E731
        rows.append([o["code"], yes(s["day1_need"]), yes(s["compliance_need"]), yes(s["archival_option"]),
                     str(s["data_trust"]).title(), CLASS_LABEL[o["decision"]], client_text(s.get("condition_text"))])
    _table(doc, ["Object", "Day-1 need", "Compliance need", "Archive option", "Data trust", "Proposed class",
                 "Condition"], rows, [1.3, 1.6, 1.9, 1.8, 1.6, 2.2, 6.2],
           "The four criteria of the BRD per object, as proposed in the Migration Console", size=8)


def r_planning(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[o["code"], o["history"], o["delta"], o["format"], cat.milestone_text(o["due"])] for o in cat.objects
            if o["decision"] != "EXCLUDED"]
    _table(doc, ["Object", "History depth", "Delta after the first full extract", "Format", "Due"], rows,
           [1.3, 5.2, 4.6, 2.2, 3.3], "Depth, delta frequency, format and due dates per object", size=8)


def r_owners(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[o["code"], o["name"], o["owner"], o["steward"]] for o in cat.objects if o["decision"] != "EXCLUDED"]
    _table(doc, ["Object", "Data object", "Business owner (signs G1, G2 and G6)", "Data steward"], rows,
           [1.3, 5.2, 5.4, 4.7], "Business owner and data steward per object (proposal; BDOI names the people, "
           "DMQ01)", size=8)


def r_milestones(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Milestone", "What is due", "When"], [list(m) for m in cat.milestones], [1.8, 11.4, 3.4],
           "Milestones of the BDOI inputs", size=9)


def r_sources(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = []
    for o in cat.objects:
        src = str(o["source"])
        marks = ["Y" if re.search(rf"\b{s}\b", src, re.I) else "" for s in ["EBIX", "QPS", "ISYS", "CMS", "Excel"]]
        marks.append("Y" if re.search(r"file shares|shares", src, re.I) else "")
        rows.append([o["code"], o["name"], *marks])
    _table(doc, ["Object", "Data object", "EBIX", "QPS", "ISYS", "CMS", "Excel", "File shares"], rows,
           [1.3, 7.0, 1.3, 1.3, 1.3, 1.3, 1.3, 1.8], "Source systems per data object (system of record per DMQ03)",
           size=8)


def r_targets(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[o["code"], o["target"], o["brd"], str(o["order"] or "-"),
             cat.seed_objects[o["code"]].get("depends_on") or "-"] for o in cat.objects if o["decision"] != "EXCLUDED"]
    _table(doc, ["Object", "Where it lands in BIBS", "BRD", "Load order", "Loads after"], rows,
           [1.3, 8.8, 2.6, 1.5, 2.4], "Target in BIBS and load order", size=8)


def r_layouts(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = []
    for layout in cat.layouts.values():
        cols = layout.columns
        rows.append([layout.code, layout.title, layout.object, len(cols), sum(1 for c in cols if c["mandatory"] == "Y"),
                     ", ".join(layout.key), ", ".join(layout.amounts) or "-", layout.template])
    total = sum(len(x.columns) for x in cat.layouts.values())
    _table(doc, ["Layout", "Content", "Object", "Columns", "Mandatory", "Key", "Amount totals", "Template"], rows,
           [1.2, 4.2, 1.2, 1.3, 1.6, 3.0, 2.4, 2.7],
           f"The {len(cat.layouts)} extract layouts in force ({total} columns): the load templates of the Migration "
           "Console", size=7.5)


def r_file_rules(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[a, b] for a, b in cat.how_to_fill] + [list(x) for x in cat.data.get("file_rules_extra") or []]
    _table(doc, ["Topic", "Rule"], rows, [3.2, 13.4], "Rules of every extract file", size=8.5)


def r_control_file(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[i, *f] for i, f in enumerate(cat.data["control_fields"], start=1)]
    _table(doc, ["No.", "Column", "What it holds", "Mand.", "Format", "Example"], rows,
           [0.9, 2.6, 4.8, 1.2, 4.5, 2.6], "Columns of the control file (one row per measure)", size=8,
           bold_first=False)


def r_controls(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = []
    for layout in cat.layouts.values():
        key = ", ".join(layout.hash_columns or layout.key)
        rows.append([layout.code, layout.title, f"{HASH_LABEL.get(layout.hash_rule, layout.hash_rule)} of {key}",
                     ", ".join(layout.amounts) or "-"])
    _table(doc, ["Layout", "Content", "Hash total (L3)", "Amount totals per currency (L2)"], rows,
           [1.2, 5.0, 5.2, 5.2], "Control totals per layout (every file also carries its row count and SHA-256)",
           size=8)


def r_map_sets(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[s["code"], client_text(s["name"]), cat.map_label(s["code"]), s["owner_title"], s["steward_title"],
             s["used_by"]] for s in cat.map_sets]
    _table(doc, ["Code map", "What is mapped", "BIBS values", "Approves (G2)", "Prepares", "Used by"], rows,
           [2.6, 4.6, 2.6, 2.4, 2.2, 2.2], f"The {len(rows)} code map sets", size=7.5)


def r_rules_summary(doc: Any, cat: Catalogue, **_: Any) -> None:
    kinds: dict[str, list[str]] = {}
    for r in cat.rules:
        kinds.setdefault(r["kind"], []).append(r["code"])
    rows = [[KIND_LABEL.get(k, k), len(v), ", ".join(v)] for k, v in kinds.items()]
    _table(doc, ["Kind", "Rules", "Rule IDs"], rows, [3.2, 1.4, 12.0],
           f"The {len(cat.rules)} data-quality rules by kind (full list in the Migration Workbook)", size=8.5)


def r_masking(doc: Any, cat: Catalogue, **_: Any) -> None:
    by_layout: dict[str, list[str]] = {}
    for m in cat.masking:
        by_layout.setdefault(m["layout_code"], []).append(f"{m['column_name']} ({MASK_LABEL.get(m['rule'], m['rule'])})")
    rows = [[k, "; ".join(v)] for k, v in by_layout.items()]
    _table(doc, ["Layout", "Columns masked outside production"], rows, [1.6, 15.0], "Masking of personal data "
           "outside production", size=8.5)


def r_decisions(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[d[0], cat.register_refs.get(d[0], "-"), d[1], d[3], cat.milestone_text(d[4]), d[5], d[6]]
            for d in cat.decisions]
    _table(doc, ["ID", "Register", "Decision", "What waits for it", "Needed by", "BDOI owner", "Status"], rows,
           [1.5, 1.6, 3.0, 3.4, 2.7, 2.6, 2.4], "Open decisions, register items and the date each is needed by",
           size=7.5, status=[6])


def r_proposals(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[p[0], p[1], p[2], p[3], f"{p[4].rstrip('.')}. Needed by {p[6]}" + (f" ({p[5]})." if p[5] not in ("-", "") else ".")]
            for p in cat.proposals]
    _table(doc, ["Ref", "Topic", "Proposed rule", "Reason", "Decision requested"], rows,
           [1.6, 2.6, 5.8, 3.4, 3.2], "Proposed business rules and clarifications for confirmation", size=7.5)


def r_reports(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[name, what] for code, (name, what) in cat.reports.items()]
    _table(doc, ["Report", "What it shows"], rows, [5.0, 11.6], "Reports of the migration and of the legacy items "
           "(Excel and PDF; Report Centre, category Data Migration, Operations and Cashiering)", size=8.5)


def r_recon_objects(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[", ".join(g["objects"]), g["l1"], g["l2"], g["l3"], g["l4"], g["business"]] for g in cat.reconciliation]
    _table(doc, ["Objects", "L1 counts", "L2 amounts", "L3 hash", "L4 fields", "L5 or business check"], rows,
           [1.8, 2.8, 2.8, 2.2, 2.8, 4.2], "What is reconciled per object", size=7.5)


def r_samples(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [[", ".join(g["objects"]), g["sample"]] for g in cat.reconciliation]
    _table(doc, ["Objects", "Sample and what is checked on the BIBS screens"], rows, [2.4, 14.2],
           "Business verification samples per object (gate G6)", size=8)


def r_cut_roles(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Code", "Role", "Organisation"], [list(r) for r in cat.cutover["roles"]], [1.6, 11.0, 4.0],
           "Roles of the cut-over plan", size=8.5)


def r_cut_phases(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = []
    for p in cat.cutover["phases"]:
        tasks = [t for t in cat.cutover["tasks"] if t[1] == p[0]]
        rows.append([p[0], p[1], p[2], len(tasks), f"{tasks[0][0]} to {tasks[-1][0]}" if tasks else "-"])
    _table(doc, ["Phase", "Name", "When", "Tasks", "IDs"], rows, [1.2, 6.0, 4.6, 1.4, 3.4], "Cut-over phases", size=9)


def r_cut_calendar(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Day", "Date (T = 3-Jan-2028)", "Note"], [list(c) for c in cat.cutover["calendar"]],
           [1.6, 3.6, 11.4], "Relative days on the calendar of the proposed go-live", size=8.5)


def r_cut_freeze(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Freeze", "From", "Until", "Scope", "Exception route"],
           [list(f) for f in cat.cutover["freeze_windows"]], [2.6, 2.8, 1.8, 5.4, 4.0], "Freeze windows", size=8.5)


def r_cut_checkpoints(doc: Any, cat: Catalogue, **_: Any) -> None:
    for c in cat.cutover["checkpoints"]:
        _table(doc, ["No.", "Criterion"], [[i, x] for i, x in enumerate(c["criteria"], start=1)], [1.0, 15.6],
               f"{c['id']} at {c['when']}: {c['decision']}", size=8.5, bold_first=False)


def r_cut_tasks(doc: Any, cat: Catalogue, phase: str = "", **_: Any) -> None:
    rows = [[t[0], t[2] + (f" {t[3]}" if t[3] else ""), t[4], t[5], t[6], t[7] or "-", t[8]]
            for t in cat.cutover["tasks"] if t[1] == phase]
    title = next(p[1] for p in cat.cutover["phases"] if p[0] == phase)
    _table(doc, ["ID", "When", "Task", "Owner", "Hours", "After", "Verification"], rows,
           [1.4, 1.7, 8.6, 1.2, 1.1, 2.4, 6.4], f"Phase {phase}: {title}", size=8)


def r_cut_rollback(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Step", "Name", "Action", "Owner"], [list(r) for r in cat.cutover["rollback"]],
           [1.2, 2.2, 11.6, 1.6], "Rollback procedure", size=8.5)


def r_cut_communication(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["No.", "Message", "Audience", "Channel", "When", "Owner"],
           [list(c) for c in cat.cutover["communication"]], [0.8, 5.4, 3.8, 3.0, 2.0, 1.4], "Communication plan",
           size=8, bold_first=False)


def r_cut_roster(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["Function", "Who", "Coverage", "Escalates to"], [list(r) for r in cat.cutover["hypercare_roster"]],
           [3.2, 5.4, 4.8, 3.2], "Hypercare roster (names in the Migration Workbook)", size=8.5)


def r_cut_exit(doc: Any, cat: Catalogue, **_: Any) -> None:
    _table(doc, ["No.", "Exit criterion"], [[i, c] for i, c in enumerate(cat.cutover["hypercare_exit"], start=1)],
           [1.0, 15.6], "Hypercare exit criteria", size=9, bold_first=False)


def r_cut_decommissioning(doc: Any, cat: Catalogue, **_: Any) -> None:
    for system, items in cat.cutover["decommissioning"].items():
        _table(doc, ["No.", "Criterion"], [[i, c] for i, c in enumerate(items, start=1)], [1.0, 15.6],
               f"Decommissioning checklist: {system}", size=8.5, bold_first=False)


def r_counts(doc: Any, cat: Catalogue, **_: Any) -> None:
    rows = [["Data objects in the register", len(cat.objects)],
            ["Objects with a load template", sum(1 for o in cat.objects if o["layouts"])],
            ["Extract layouts (load templates)", len(cat.layouts)],
            ["Template columns", sum(len(x.columns) for x in cat.layouts.values())],
            ["Code map sets", len(cat.map_sets)], ["Data-quality rules", len(cat.rules)],
            ["Columns masked outside production", len(cat.masking)],
            ["Cut-over tasks", len(cat.cutover["tasks"])],
            ["Go / no-go checkpoints", len(cat.cutover["checkpoints"])],
            ["Open decisions", len(cat.decisions)], ["Proposed rules for confirmation", len(cat.proposals)]]
    _table(doc, ["Content", "Count"], rows, [10.0, 3.0], "The migration catalogue in numbers", size=9,
           bold_first=False)


RENDERERS = {
    "dm-objects": r_objects, "dm-criteria": r_criteria, "dm-planning": r_planning, "dm-owners": r_owners,
    "dm-milestones": r_milestones, "dm-sources": r_sources, "dm-targets": r_targets, "dm-layouts": r_layouts,
    "dm-file-rules": r_file_rules, "dm-control-file": r_control_file, "dm-controls": r_controls,
    "dm-map-sets": r_map_sets, "dm-rules": r_rules_summary, "dm-masking": r_masking, "dm-decisions": r_decisions,
    "dm-proposals": r_proposals, "dm-reports": r_reports, "dm-recon-objects": r_recon_objects,
    "dm-samples": r_samples, "cut-roles": r_cut_roles, "cut-phases": r_cut_phases, "cut-calendar": r_cut_calendar,
    "cut-freeze": r_cut_freeze, "cut-checkpoints": r_cut_checkpoints, "cut-tasks": r_cut_tasks,
    "cut-rollback": r_cut_rollback, "cut-communication": r_cut_communication, "cut-roster": r_cut_roster,
    "cut-exit": r_cut_exit, "cut-decommissioning": r_cut_decommissioning, "dm-counts": r_counts,
}


def render(doc: Any, render: str, source: str = "pack/pack.yaml", **opts: Any) -> None:  # noqa: A002 - block key
    """Entry point of the ```pack blocks of the Handbook and Start Here: the migration tables of this module; every
    other part (screens, messages, guide tables) is rendered by the shared sign-off pack builder."""
    if render in RENDERERS:
        RENDERERS[render](doc, catalogue(), **opts)
        return
    import signoff_pack  # noqa: PLC0415

    signoff_pack.render(doc, render=render, source=source, **opts)


# ============================================================================ Migration Workbook


def workbook_sheets(wb: Any, pack: Any, date_sheets: list[Any]) -> None:
    """The migration sheets of the 03 Migration Workbook (signoff_pack.build_workbook calls this after How to
    review). Sheets with rows the business confirms carry the BU review columns."""
    from bdoi_xlsx import Column  # noqa: PLC0415
    import signoff_pack  # noqa: PLC0415

    cat = catalogue()
    review = signoff_pack.review_columns

    def reviewed(ws: Any) -> None:
        date_sheets.append(ws)

    # Object catalogue and decisions
    rows = []
    for o in cat.objects:
        s = cat.seed_objects[o["code"]]
        yes = lambda flag: "Yes" if str(flag).lower() == "true" else "No"  # noqa: E731
        rows.append({"code": o["code"], "name": o["name"], "category": o["category"],
                     "decision": CLASS_LABEL[o["decision"]], "rationale": o["rationale"],
                     "day1": yes(s["day1_need"]), "compliance": yes(s["compliance_need"]),
                     "archive": yes(s["archival_option"]), "trust": str(s["data_trust"]).title(),
                     "condition": client_text(s.get("condition_text")), "source": o["source"],
                     "layouts": ", ".join(o["layouts"]) or "-", "target": o["target"], "order": o["order"] or "-",
                     "depends": s.get("depends_on") or "-", "brd": o["brd"], "questions": o["questions"]})
    reviewed(wb.sheet("Object catalogue", [
        Column("code", "Object", 8, "Data object code"),
        Column("name", "Data object", 34, "What the object holds"),
        Column("category", "Category", 11, "Reference, Client, Policy, Open item, GL, History"),
        Column("decision", "Decision (proposed)", 15, "Migrate, Carry forward, Conditional, Archive or Excluded; "
               "the business owner confirms at gate G1", values=list(CLASS_LABEL.values())),
        Column("rationale", "Rationale", 50, "Why this decision"),
        Column("day1", "Day-1 need", 9, "Needed in BIBS on the first business day"),
        Column("compliance", "Compliance need", 11, "Must be kept for compliance or audit"),
        Column("archive", "Archive option", 10, "Read-only legacy or the BIBS archive is enough"),
        Column("trust", "Data trust", 9, "High, Medium or Low, before cleansing"),
        Column("condition", "Condition", 36, "Condition of a conditional object"),
        Column("source", "Source system", 14, "Legacy system(s) that hold the object"),
        Column("layouts", "Templates", 12, "Load templates of the object (sheets of this workbook)"),
        Column("target", "Where it lands in BIBS", 34, "The BIBS records the object becomes"),
        Column("order", "Load order", 7, "Lower loads first"),
        Column("depends", "Loads after", 12, "Objects that must be accepted first"),
        Column("brd", "BRD", 12, "BRD requirement"),
        Column("questions", "Open decisions", 14, "Decisions that affect the object"),
    ] + review(), rows, description="One row per legacy data object with the proposed decision and the four "
                                    "criteria of the BRD (BRID 1.1a)"))

    rows = [{"code": o["code"], "name": o["name"], "owner": o["owner"], "steward": o["steward"]}
            for o in cat.objects if o["decision"] != "EXCLUDED"]
    reviewed(wb.sheet("Data owners", [
        Column("code", "Object", 8, "Data object code"),
        Column("name", "Data object", 34, "What the object holds"),
        Column("owner", "Business owner (proposed)", 34, "Signs the decision (G1), the code maps (G2) and the "
               "acceptance (G6)"),
        Column("steward", "Data steward (proposed)", 30, "Prepares the code maps, resolves data-quality issues, "
               "fixes records at source"),
        Column("owner_name", "Named business owner", 24, "Name given by BDOI"),
        Column("steward_name", "Named data steward", 24, "Name given by BDOI"),
    ] + review(), rows, description="Business owner and data steward of every object; BDOI names the people (DMQ01)"))

    rows = [{"code": o["code"], "name": o["name"], "bound": o["bound"], "history": o["history"],
             "delta": o["delta"], "format": o["format"], "controls": o["controls"],
             "due": cat.milestone_text(o["due"])} for o in cat.objects if o["layouts"]]
    reviewed(wb.sheet("Extract planning", [
        Column("code", "Object", 8, "Data object code"),
        Column("name", "Data object", 30, "What the object holds"),
        Column("volume", "Volume (BDOI to fill)", 14, "Row count per source system at the last extract"),
        Column("bound", "Planning bound", 18, "Known bound from the BRDs"),
        Column("history", "History depth", 26, "Which records and how far back"),
        Column("delta", "Delta frequency", 26, "Extracts after the first full one"),
        Column("format", "Format", 10, "CSV or XLSX, with a control file"),
        Column("controls", "Control totals", 30, "Measures of the control file"),
        Column("due", "Due", 26, "Milestone of the first full extract and of the final one"),
    ] + review(), rows, description="What BDOI extracts per object, how much, how far back and by when"))

    wb.sheet("Milestones", [Column("id", "Milestone", 10, "Milestone ID"),
                            Column("what", "What is due", 80, "Content of the milestone"),
                            Column("when", "When", 16, "Date")],
             [dict(zip(["id", "what", "when"], m)) for m in cat.milestones],
             description="Milestones used in the Due and Needed-by columns")

    rows = [{"topic": a, "rule": b} for a, b in cat.how_to_fill] + \
           [{"topic": a, "rule": b} for a, b in cat.data.get("file_rules_extra") or []]
    reviewed(wb.sheet("File rules", [Column("topic", "Topic", 20, "Topic of the rule"),
                                     Column("rule", "Rule", 100, "What every extract file follows")] + review(),
                      rows, description="Rules of every extract and control file: the How to fill sheet of the "
                                        "console workbook, then the rules of the migration"))

    rows = []
    for layout in cat.layouts.values():
        o = cat.by_code.get(layout.object, {})
        rows.append({"layout": layout.code, "title": layout.title, "object": layout.object,
                     "decision": CLASS_LABEL.get(o.get("decision", ""), ""), "sheet": layout.sheet_name,
                     "template": layout.template, "columns": len(layout.columns),
                     "mandatory": sum(1 for c in layout.columns if c["mandatory"] == "Y"),
                     "key": ", ".join(layout.key), "hash": f"{HASH_LABEL.get(layout.hash_rule, layout.hash_rule)}",
                     "amounts": ", ".join(layout.amounts) or "-", "version": layout.version})
    wb.sheet("Load templates", [
        Column("layout", "Layout", 8, "Layout code; starts the file name"),
        Column("title", "Content", 36, "What one row of the file holds"),
        Column("object", "Object", 8, "Data object"),
        Column("decision", "Decision", 12, "Proposed decision of the object"),
        Column("sheet", "Sheet", 30, "Sheet of this workbook and of the console workbook"),
        Column("template", "CSV template", 20, "Template the console exports (header row only)"),
        Column("columns", "Columns", 8, "Columns of the layout", kind="number"),
        Column("mandatory", "Mandatory", 9, "Mandatory columns", kind="number"),
        Column("key", "Key", 26, "Legacy key of a row"),
        Column("hash", "Hash total", 20, "How the hash total of the control file is computed"),
        Column("amounts", "Amount totals", 26, "Amount columns totalled per currency in the control file"),
        Column("version", "Version", 7, "Layout version in force", kind="number"),
    ], rows, description="The extract layouts in force; each has a sheet below and a CSV template exported by the "
                         "Migration Console (Layouts and Rules)")

    for layout in cat.layouts.values():
        rows = []
        for c in layout.columns:
            allowed = f"Code map {c['map_set']}" if c.get("map_set") else client_text(c.get("allowed_values"))
            rows.append({"seq": int(c["seq"]), "name": c["name"], "description": client_text(c["description"]),
                         "type": TYPE_LABEL.get(c["data_type"], c["data_type"]), "length": c.get("length") or "",
                         "mandatory": c["mandatory"], "allowed": allowed, "format": client_text(c.get("format")),
                         "example": client_text(c.get("example")), "validation": client_text(c.get("validation"))})
        reviewed(wb.sheet(layout.sheet_name, [
            Column("seq", "No.", 5, "Column position in the file", kind="number"),
            Column("name", "Column", 22, "Column name in the header row of the file, exactly as written"),
            Column("description", "Description", 40, "What the column holds"),
            Column("type", "Type", 10, "Text, Code, Date, Timestamp, Amount (2 decimals), Integer, Decimal, Flag (Y "
                   "or N)"),
            Column("length", "Length", 7, "Maximum characters; for numbers the digits and decimals"),
            Column("mandatory", "Mandatory", 9, "Y always; N optional; C when its condition holds (Validation)",
                   values=["Y", "N", "C"]),
            Column("allowed", "Allowed values or code map", 30, "Closed list of values, or the code map that maps "
                   "the legacy value"),
            Column("format", "Format", 18, "How the value is written in the file"),
            Column("example", "Example", 18, "A made-up example"),
            Column("validation", "Validation", 34, "Check applied when the file is validated"),
        ] + review(), rows, description=f"Layout {layout.code} v{layout.version} of object {layout.object}: "
                                        f"{layout.title}. Key {', '.join(layout.key)}. Template {layout.template}."))

    fields = cat.data["control_fields"]
    wb.sheet("Control file", [
        Column("no", "No.", 5, "Column position", kind="number"),
        Column("name", "Column", 16, "Column name of the control-file template"),
        Column("what", "What it holds", 40, "Content"),
        Column("mandatory", "Mandatory", 9, "Y, N or C (conditional)"),
        Column("format", "Format", 40, "How the value is written"),
        Column("example", "Example", 26, "A made-up example"),
    ], [{"no": i, "name": f[0], "what": f[1], "mandatory": f[2], "format": f[3], "example": f[4]}
        for i, f in enumerate(fields, start=1)],
        description="The control file sent with every data file (<data file name>.ctl.csv): one row per measure")
    wb.sheet("Control example", [Column(f[0], f[0], w, f[1]) for f, w in
                                 zip(fields, [7, 7, 9, 26, 18, 18, 14, 14, 16, 9, 16, 30])],
             [list(r) for r in cat.data["control_example"]], description="Example control file of F01C (made-up "
                                                                         "values)")
    rows = []
    for layout in cat.layouts.values():
        base = {"layout": layout.code, "object": layout.object}
        rows.append({**base, "measure": "ROW_COUNT", "column": "-", "currency": "-",
                     "rule": "Rows of the data file, header excluded"})
        rows.append({**base, "measure": "HASH_TOTAL", "column": ", ".join(layout.hash_columns or layout.key),
                     "currency": "-", "rule": HASH_LABEL.get(layout.hash_rule, layout.hash_rule)})
        for amount in layout.amounts:
            rows.append({**base, "measure": "AMOUNT_TOTAL", "column": amount, "currency": "Each currency",
                         "rule": "Sum per currency" + (" and component" if layout.code == "F01C" else "")})
        rows.append({**base, "measure": "SHA256", "column": "-", "currency": "-",
                     "rule": "SHA-256 of the data file as sent"})
    wb.sheet("Control totals", [
        Column("layout", "Layout", 8, "Layout code"), Column("object", "Object", 8, "Data object"),
        Column("measure", "Measure", 14, "Control measure", values=["ROW_COUNT", "HASH_TOTAL", "AMOUNT_TOTAL",
                                                                     "SHA256"]),
        Column("column", "Column", 26, "Column the measure applies to"),
        Column("currency", "Currency", 13, "Per currency or not"),
        Column("rule", "How it is computed", 36, "Computation"),
        Column("bdoi", "Value from BDOI", 16, "From the control file"),
        Column("bibs", "Value in BIBS", 16, "Received, staged or loaded value (Reconciliation Summary)"),
        Column("diff", "Difference", 12, "BDOI value minus BIBS value"),
        Column("status", "Status", 12, "Matched, Break or Explained", values=["MATCHED", "BREAK", "EXPLAINED"],
               status=True),
    ], rows, description="Control totals per layout; the tie-out sheet of reconciliation levels L1 to L3 in each run")

    rows = [{"code": s["code"], "name": client_text(s["name"]), "values": cat.map_label(s["code"]),
             "used": s["used_by"], "owner": s["owner_title"], "steward": s["steward_title"],
             "due": cat.map_due(s["code"])} for s in cat.map_sets]
    reviewed(wb.sheet("Code maps", [
        Column("code", "Code map", 22, "Code map set"),
        Column("name", "What is mapped", 44, "Legacy codes mapped"),
        Column("values", "BIBS values", 26, "The BIBS values the codes map to"),
        Column("used", "Used by layouts", 24, "Layouts whose columns use the set"),
        Column("owner", "Approves (G2)", 26, "Business owner who approves each version"),
        Column("steward", "Prepares", 24, "Data steward who prepares the entries"),
        Column("due", "First approved version due", 22, "Milestone"),
    ] + review(), rows, description="Code map sets of the Migration Console (BRID 3.1): one per domain, versioned and "
                                    "approved by the business owner"))
    wb.sheet("Code map template", [
        Column("set", "Code map", 20, "Code map set; the console exports one sheet per set, named after it"),
        Column("source_system", "source_system", 13, "Legacy system of the code", values=SOURCES),
        Column("legacy_code", "legacy_code", 16, "Code as stored in legacy"),
        Column("legacy_description", "legacy_description", 28, "Description in legacy"),
        Column("qualifier", "qualifier", 12, "Attribute that splits one legacy code into several BIBS values "
               "(package code map: RISK_CODE, INSURER or SI_BAND); empty otherwise"),
        Column("qualifier_value", "qualifier_value", 18, "Value of the qualifier (a code, or a from-to amount band)"),
        Column("action", "action", 10, "MAP to a BIBS value, DEFAULT to the default of the set, REJECT the rows, "
               "CREATE a new BIBS value", values=["MAP", "DEFAULT", "REJECT", "CREATE"]),
        Column("target_code", "target_code", 18, "BIBS value; mandatory for MAP and CREATE"),
        Column("remarks", "remarks", 34, "Reason for DEFAULT, REJECT or CREATE; condition of a conditional entry"),
        Column("rows", "Rows using it", 11, "From the profiling of the extracts (not part of the file)"),
    ], [dict(zip(["set"] + CODE_MAP_COLUMNS + ["rows"], [str(x) for x in r])) for r in cat.data["map_example"]],
        description="Columns of a code map version as exported and imported in Excel on the Code Maps screen "
                    "(the columns source_system to remarks; illustrative rows)")

    rows = [{"code": r["code"], "scope": r["layout_scope"], "columns": client_text(r["columns"]),
             "kind": KIND_LABEL.get(r["kind"], r["kind"]), "rule": client_text(r["description"]),
             "severity": SEVERITY_LABEL.get(r["severity"], r["severity"]), "message": client_text(r["message"]),
             "fixed": client_text(r["fixed_by"])} for r in cat.rules]
    reviewed(wb.sheet("Validation rules", [
        Column("code", "Rule", 8, "Rule ID shown with each issue"),
        Column("scope", "Layouts", 12, "Layouts checked"),
        Column("columns", "Columns", 22, "Columns checked"),
        Column("kind", "Kind", 14, "Kind of rule"),
        Column("rule", "Rule", 50, "What is checked"),
        Column("severity", "Severity", 22, "Error: the row is not loaded; Warning: loaded and reported"),
        Column("message", "Message", 40, "Message of the issue; {name} is filled in"),
        Column("fixed", "Fixed by", 24, "Who corrects a failing row"),
    ] + review(), rows, description="Data-quality rules applied to every staged row (Layouts and Rules, Data-Quality "
                                    "Rules). Thresholds: master data 0.5 percent rejected or waived; financial "
                                    "objects none"))
    rows = [{"layout": m["layout_code"], "column": m["column_name"], "masking": MASK_LABEL.get(m["rule"], m["rule"])}
            for m in cat.masking]
    reviewed(wb.sheet("Masking", [
        Column("layout", "Layout", 8, "Layout code"), Column("column", "Column", 22, "Column masked"),
        Column("masking", "Masking outside production", 36, "How the value is replaced (the same value every time)"),
    ] + review(), rows, description="Personal data masked when a file is received in any environment other than "
                                    "production"))

    # Cut-over plan
    cut = cat.cutover
    roles = {r[0]: r[1] for r in cut["roles"]}
    phases = {p[0]: p[1] for p in cut["phases"]}
    reviewed(wb.sheet("Cut-over tasks", [
        Column("id", "Task", 8, "Task ID"), Column("phase", "Phase", 16, "Phase of the cut-over"),
        Column("day", "Day", 7, "Day relative to go-live (T)"), Column("time", "Start (PHT)", 8, "Planned start"),
        Column("task", "Task", 58, "What is done"), Column("owner", "Owner", 8, "Accountable role (sheet Cut-over "
                                                                                  "roles)"),
        Column("hours", "Hours", 6, "Planned elapsed hours", kind="number"),
        Column("pred", "After", 16, "Tasks that must be complete first"),
        Column("verification", "Verification", 40, "How completion is proven"),
        Column("checkpoint", "Checkpoint", 11, "Go / no-go checkpoint or milestone"),
        Column("assignee", "Assignee (name)", 16, "Named person"),
        Column("status", "Status", 12, "Task status", values=["NOT STARTED", "IN PROGRESS", "DONE", "BLOCKED", "N/A"],
               status=True),
    ] + review(), [{"id": t[0], "phase": f"{t[1]} {phases[t[1]]}", "day": t[2], "time": t[3], "task": t[4],
                    "owner": t[5], "hours": t[6], "pred": t[7], "verification": t[8], "checkpoint": t[9],
                    "status": "NOT STARTED"} for t in cut["tasks"]],
        description="The production cut-over plan from T-30 to the close of the opening-balance adjustments"))
    wb.sheet("Cut-over roles", [Column("code", "Code", 8, "Owner code of the task plan"),
                                Column("role", "Role", 70, "Role"), Column("org", "Organisation", 24, "Organisation"),
                                Column("name", "Name and phone (to fill)", 30, "Named person")],
             [dict(zip(["code", "role", "org"], r)) for r in cut["roles"]], description="Owner roles of the task plan")
    wb.sheet("Cut-over calendar", [Column("day", "Day", 10, "Day relative to go-live (T)"),
                                   Column("date", "Date (T = 3-Jan-2028)", 20, "Date for the proposed go-live"),
                                   Column("note", "Note", 80, "Holiday or year-end note")],
             [dict(zip(["day", "date", "note"], c)) for c in cut["calendar"]],
             description="Relative days on the calendar of the proposed go-live (DMQ25, DMQ39)")
    rows = [{"id": c["id"], "when": c["when"], "decision": c["decision"], "n": i, "criterion": x}
            for c in cut["checkpoints"] for i, x in enumerate(c["criteria"], start=1)]
    reviewed(wb.sheet("Go-no-go checkpoints", [
        Column("id", "Checkpoint", 12, "Checkpoint"), Column("when", "When", 10, "Day and time"),
        Column("decision", "Decision", 30, "What is decided"), Column("n", "No.", 5, "Criterion number"),
        Column("criterion", "Criterion", 70, "Criterion that must be met"),
        Column("value", "Measured value", 18, "Value at the decision"),
        Column("met", "Met", 8, "Y or N", values=["Y", "N"]),
    ] + review(), rows, description="Go / no-go checkpoints and their criteria"))
    reviewed(wb.sheet("Freeze windows", [
        Column("what", "Freeze", 20, "What is frozen"), Column("from", "From", 22, "Start"),
        Column("to", "Until", 12, "End"), Column("scope", "Scope", 50, "What may not change"),
        Column("exception", "Exception route", 46, "How an urgent change is handled"),
    ] + review(), [dict(zip(["what", "from", "to", "scope", "exception"], f)) for f in cut["freeze_windows"]],
        description="Freeze windows of the legacy systems and of BIBS"))
    reviewed(wb.sheet("Rollback", [
        Column("id", "Step", 7, "Step"), Column("name", "Name", 16, "Step name"),
        Column("action", "Action", 90, "What is done"), Column("owner", "Owner", 8, "Role"),
        Column("done", "Done at", 14, "Date and time"),
    ] + review(), [dict(zip(["id", "name", "action", "owner"], r)) for r in cut["rollback"]],
        description="Rollback procedure (only on NO-GO, before the point of no return)"))
    wb.sheet("Communication", [
        Column("n", "No.", 5, "Item"), Column("message", "Message", 50, "What is said"),
        Column("audience", "Audience", 34, "Who receives it"), Column("channel", "Channel", 26, "How"),
        Column("when", "When", 12, "Day"), Column("owner", "Owner", 8, "Role"), Column("sent", "Sent on", 14, "Date"),
    ], [dict(zip(["n", "message", "audience", "channel", "when", "owner"], c)) for c in cut["communication"]],
        description="Communication plan of the cut-over")
    wb.sheet("Hypercare", [
        Column("function", "Function", 26, "Support function"), Column("who", "Who", 40, "Team"),
        Column("hours", "Coverage", 36, "Hours"), Column("escalation", "Escalates to", 24, "Escalation"),
        Column("name", "Names and phone (to fill)", 30, "Named people"),
    ], [dict(zip(["function", "who", "hours", "escalation"], r)) for r in cut["hypercare_roster"]] +
        [{"function": f"Exit criterion {i}", "who": c} for i, c in enumerate(cut["hypercare_exit"], start=1)],
        description="Hypercare roster and exit criteria")
    rows = [{"system": s, "n": i, "criterion": c, "status": "OPEN"}
            for s, items in cut["decommissioning"].items() for i, c in enumerate(items, start=1)]
    reviewed(wb.sheet("Decommissioning", [
        Column("system", "Legacy system", 18, "System or the legacy context"), Column("n", "No.", 5, "Criterion"),
        Column("criterion", "Criterion", 80, "Criterion"), Column("evidence", "Evidence", 26, "Report, log or "
                                                                                                "sign-off"),
        Column("signed", "Signed by and date", 20, "Signer"),
        Column("status", "Status", 11, "Status", values=["OPEN", "DONE", "N/A"], status=True),
    ] + review(), rows, description="Checklist per legacy system and for the legacy context (BRID 12.1; DMQ27)"))

    # Reconciliation sign-off per object
    rows = []
    for o in cat.objects:
        g = cat.recon_group(o["code"])
        if g is None:
            continue
        s = cat.seed_objects[o["code"]]
        financial = str(s.get("financial")).lower() == "true"
        rows.append({"code": o["code"], "name": o["name"], "l1": g["l1"], "l2": g["l2"], "l3": g["l3"], "l4": g["l4"],
                     "l5": g["business"], "approver": "Head, Comptrollership" if financial else o["owner"],
                     "sample": g["sample"]})
    reviewed(wb.sheet("Reconciliation sign-off", [
        Column("code", "Object", 8, "Data object"), Column("name", "Data object", 28, "What the object holds"),
        Column("l1", "L1 counts", 26, "What the count level compares"),
        Column("l2", "L2 amounts", 26, "What the amount level compares"),
        Column("l3", "L3 hash totals", 20, "What the hash level compares"),
        Column("l4", "L4 fields", 26, "What the field level compares"),
        Column("l5", "L5 or business check", 34, "GL level of the financial objects, or the business check"),
        Column("sample", "Business verification sample (G6)", 44, "What the data owner checks on the BIBS screens"),
        Column("approver", "Signs G5", 22, "Reconciliation approver (Comptrollership for financial objects)"),
        Column("cycle", "Run", 16, "Trial migration 1 to 4, dress rehearsal or production",
               values=["Trial migration 1", "Trial migration 2", "Trial migration 3", "Trial migration 4",
                       "Dress rehearsal", "Production"]),
        Column("result", "Result", 14, "Matched, or explained with approval", values=["Matched", "Explained",
                                                                                         "Break"]),
        Column("g5", "G5 signed by and date", 20, "Reconciliation approver"),
        Column("g6", "G6 signed by and date", 20, "Data owner and Data Migration Lead"),
    ] + review(), rows, description="Reconciliation and acceptance of every object with a load template: what each "
                                    "level proves and who signs (Handbook Part C)"))

    # Decisions and proposals
    rows = [{"id": d[0], "register": cat.register_refs.get(d[0], "-"), "topic": d[1], "question": d[2],
             "blocks": d[3], "due": cat.milestone_text(d[4]), "owner": d[5], "status": d[6],
             "answer": d[7] if len(d) > 7 else ""} for d in cat.decisions]
    wb.sheet("Open decisions", [
        Column("id", "ID", 9, "Question (DMQnn) or register ID (DCR-nnn)"),
        Column("register", "Register", 12, "Item of the BRD discrepancy and clarification register"),
        Column("topic", "Topic", 22, "Subject"), Column("question", "Decision needed", 56, "What BDOI decides"),
        Column("blocks", "What waits for it", 26, "Objects, gates or dates that wait for the answer"),
        Column("due", "Needed by", 20, "Milestone and date"), Column("owner", "BDOI owner", 22, "Who answers"),
        Column("status", "Status", 13, "Open, Partly answered, Answered, or Recommended (answered with a "
               "recommendation that BDOI still confirms)", values=["OPEN", "PARTIAL", "ANSWERED", "RECOMMENDED"],
               status=True),
        Column("answer", "Answer", 44, "BDOI answer and date"),
    ], rows, description="Decisions of BDOI that the migration waits for, with the date each is needed by")
    rows = [{"ref": p[0], "topic": p[1], "rule": p[2], "reason": p[3], "requested": p[4], "decisions": p[5],
             "due": p[6]} for p in cat.proposals]
    ws = wb.sheet("Proposed rules", [
        Column("ref", "Ref", 10, "Proposal reference"), Column("topic", "Topic", 24, "Subject"),
        Column("rule", "Proposed rule", 60, "The rule proposed by the project team"),
        Column("reason", "Reason", 40, "Why it is proposed"),
        Column("requested", "Decision requested", 36, "What BDOI is asked to decide"),
        Column("decisions", "Open decisions", 14, "Decisions it answers"), Column("due", "Needed by", 18, "Date"),
        Column("bdoi", "BDOI decision", 18, "Agree, Agree with change, Disagree or Need more information",
               values=DECISION_VALUES),
        Column("bdoi_comment", "Change or comment", 40, "The change asked for, or the comment"),
        Column("decided_by", "Decided by", 20, "Name and unit"),
        Column("decided_on", "Decided on", 13, "Date of the decision (dd-mmm-yyyy)", kind="date"),
    ], rows, description="Proposed business rules and clarifications for confirmation (Handbook chapter of the same "
                         "name)")
    date_sheets.append(ws)


def build_workbook(out_dir: Path | None = None) -> Path:
    import signoff_pack  # noqa: PLC0415

    pack = signoff_pack.Pack(PACK_DIR / "pack.yaml")
    return signoff_pack.build_workbook(pack, extend=workbook_sheets, out_dir=out_dir)


# ============================================================================ CLI


def report(cat: Catalogue) -> str:
    return (f"BRD-13 catalogue: {len(cat.objects)} objects, {len(cat.layouts)} layouts, "
            f"{sum(len(x.columns) for x in cat.layouts.values())} template columns, {len(cat.map_sets)} code maps, "
            f"{len(cat.rules)} data-quality rules, {len(cat.masking)} masked columns, {len(cat.decisions)} open "
            f"decisions, {len(cat.proposals)} proposed rules, {len(cat.cutover['tasks'])} cut-over tasks, "
            f"{len(cat.reports)} reports")


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="check the catalogue and the sign-off pack only")
    ap.add_argument("--out", metavar="DIR", help="write the workbook into DIR instead of the release-set folder")
    args = ap.parse_args(argv)
    import signoff_pack  # noqa: PLC0415

    cat = catalogue()
    problems = cat.check()
    pack = signoff_pack.Pack(PACK_DIR / "pack.yaml")
    problems += [f"pack: {p}" for p in pack.check()]
    for p in problems:
        print(p)
    print(report(cat))
    print(signoff_pack.report(pack))
    if args.check:
        return 1 if problems else 0
    print(f"xlsx: {build_workbook(Path(args.out) if args.out else None)}")
    return 1 if problems else 0


if __name__ == "__main__":
    raise SystemExit(main())
