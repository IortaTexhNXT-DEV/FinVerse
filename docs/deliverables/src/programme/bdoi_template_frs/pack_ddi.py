"""Data Dictionary (DDI) of a BRD pack: one workbook with the sheets Tables, Columns, Lists of Values, Parameters,
Status Models and Indexes. The schema is the final schema after every migration (tools/deliverables/schema_facts.py);
the reference data is what the migrations load. Names are written in capitals, as in the BIBS Database Inventory.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

import api_facts as af
import schema_facts as sf
from pack_common import REPO, Column, save_workbook, workbook

sys.path.insert(0, str(REPO / "docs" / "deliverables" / "src" / "programme" / "security_data"))
import build_secdata_pack as sd  # noqa: E402


def _fk_of(cons: list[dict], col: str) -> dict | None:
    return next((c for c in cons if c["type"] == "f" and c["cols"] and c["cols"][0] == col and len(c["cols"]) == 1),
                None)


def _text(K, text: str) -> str:
    """A description in business words: a content module may reword the texts of the schema (DDI_TEXT)."""
    for old, new in getattr(K, "DDI_TEXT", {}).items():
        text = text.replace(old, new)
    return text


def _label(code: str, K) -> str:
    return K.ENUM_LABELS.get(code) or code.replace("_", " ").capitalize()


def _parameter_labels(K) -> tuple[dict, list, str, str]:
    """Labels of the parameters on their settings screen, the keys of that screen and the maker-checker wording.
    A content module gives PARAMETER_LABELS (key -> label), SETTINGS_SCREEN_KEYS and PARAMETER_MAKER_CHECKER; without
    them the User Access settings screen of BRD-11 is read."""
    if hasattr(K, "PARAMETER_LABELS"):
        special_text, other_text = K.PARAMETER_MAKER_CHECKER
        return K.PARAMETER_LABELS, list(getattr(K, "SETTINGS_SCREEN_KEYS", [])), special_text, other_text
    ui = (REPO / "frontend" / "src" / "features" / "useraccess" / "uamSettings.ts").read_text(encoding="utf-8")
    labels = dict(re.findall(r"^\s+(\w+): '((?:[^'\\]|\\.)*)',$", ui, re.M)) | dict(
        re.findall(r'^\s+(\w+): "([^"]*)",$', ui, re.M))
    uam_keys = re.findall(r"^\s+'([A-Z_]+)',$", ui.split("UAM_SETTING_KEYS")[1].split("];")[0], re.M)
    return labels, uam_keys, "Yes: UAM Settings, approved by Information Security", \
        "Yes: System Parameters, approved by another user"


def write(K, out: Path) -> tuple[Path, dict]:
    tables = [t["name"] for t in sf.tables(K.TABLE_PATTERN)]
    unknown = [t for t in tables if t not in K.TABLES]
    gone = [t for t in K.TABLES if t not in tables]
    if unknown or gone:
        raise SystemExit(f"data dictionary: tables without a purpose {unknown}; purposes without a table {gone}")
    t_rows, c_rows, i_rows, check_lists = [], [], [], []
    for t in tables:
        module, purpose, go_live, year = K.TABLES[t]
        cols = sf.columns(t)
        cons = sf.constraints(t)
        pk = next((c for c in cons if c["type"] == "p"), None)
        t_rows.append({"t": t.upper(), "m": module, "p": purpose, "pk": ", ".join(x.upper() for x in pk["cols"])
                       if pk else "-", "n": len(cols), "v0": go_live, "v1": year})
        uniques = {c["cols"][0] for c in cons if c["type"] == "u" and c["cols"] and len(c["cols"]) == 1}
        for col in cols:
            fk = _fk_of(cons, col["name"])
            checks = [c for c in cons if c["type"] == "c" and c["cols"] and col["name"] in c["cols"]]
            allowed = None
            texts = []
            for c in checks:
                vals = sd.allowed_values(c["definition"]) if len(c["cols"]) == 1 else None
                if vals:
                    allowed = vals
                    check_lists.append((t, col["name"], vals))
                    texts.append("Allowed: " + ", ".join(vals))
                else:
                    texts.append(sd.check_text(c["definition"]))
            if col["name"] in uniques:
                texts.append("Unique")
            typ = sd.fmt_type(col["type"])
            m = re.match(r"(\w[\w ]*?)(?:\((\d+(?:,\d+)?)\))?$", typ)
            c_rows.append({
                "t": t.upper(), "c": col["name"].upper(), "dt": m.group(1) if m else typ,
                "len": m.group(2) if m and m.group(2) else "-", "null": "Yes" if col["nullable"] else "No",
                "def": sd.fmt_default(col) or "-", "chk": "; ".join(texts) or "-",
                "fk": f"{fk['ref_table'].upper()}" if fk else "-",
                "desc": _text(K, sd.column_description({"name": t}, col, fk, allowed))})
        for ix in sf.indexes(t):
            cols_txt = re.search(r"\((.*)\)", ix["definition"]).group(1)
            where = re.search(r"\bWHERE (.*)$", ix["definition"])
            i_rows.append({"t": t.upper(), "i": ix["name"].upper(),
                           "k": "Primary key" if ix["primary"] else "Unique" if ix["unique"] else "Index",
                           "c": sd.upper_identifiers(cols_txt.split(")")[0]).upper(),
                           "w": sd.check_text("CHECK (" + where.group(1) + ")") if where else "-"})
    # lists of values: the LOV tables, the enumerations of the screens and the allowed values of the checks
    l_rows = []
    lov = sf.rows("select type_code, code, label, record_status, effective_to from lov_value where type_code in ("
                  + ", ".join(repr(x) for x in K.LOV_TYPES) + ") order by type_code, sort_order, code")
    for r in lov:
        l_rows.append({"l": r["type_code"], "src": "List of values (maintained)", "c": r["code"], "lb": r["label"],
                       "a": "Yes" if r["record_status"] in ("ACTIVE", "AUTHORIZED") and not r["effective_to"]
                       else "No"})
    for name, enum in K.ENUM_LISTS:
        vals = af.enum_values(enum)
        if not vals:
            raise SystemExit(f"data dictionary: enumeration {enum} not found")
        for v in vals:
            l_rows.append({"l": name, "src": "Fixed list", "c": v, "lb": _label(v, K),
                           "a": "No" if v in K.INACTIVE_CODES else "Yes"})
    seen = set()
    for t, col, vals in check_lists:
        key = (col, tuple(vals))
        if key in seen:
            continue
        seen.add(key)
        for v in vals:
            l_rows.append({"l": f"{t.upper()}.{col.upper()}", "src": "Allowed values of the column", "c": v,
                           "lb": _label(v, K), "a": "No" if v in K.INACTIVE_CODES else "Yes"})
    # parameters
    p_rows = []
    labels, special, special_text, other_text = _parameter_labels(K)
    for r in sf.rows(f"select param_key, param_value, value_type, category, description, min_value, max_value "
                     f"from sys_parameter where {K.PARAMETER_FILTER} order by param_key"):
        rng = "-"
        if r["min_value"] is not None or r["max_value"] is not None:
            rng = f"{r['min_value'] if r['min_value'] is not None else '-'} to " \
                  f"{r['max_value'] if r['max_value'] is not None else '-'}"
        elif r["value_type"] == "BOOLEAN":
            rng = "true / false"
        p_rows.append({"k": r["param_key"], "lb": labels.get(r["param_key"], "-"), "ty": r["value_type"],
                       "d": r["param_value"] or "(empty)", "r": rng,
                       "mc": special_text if r["param_key"] in special else other_text,
                       "ds": _text(K, sd.upper_identifiers(r["description"]))})
    missing = [k for k in special if k not in {p["k"] for p in p_rows}]
    if missing:
        raise SystemExit(f"data dictionary: settings of the screen without a parameter row: {missing}")
    # status models
    s_rows = []
    statuses = getattr(K, "STATUSES", None)
    if statuses is None:
        import brd11_v15_content as C  # noqa: PLC0415
        statuses = C.STATUSES
    for entity, rows in statuses.items():
        for status, meaning, nxt in rows:
            s_rows.append({"e": entity, "s": status, "m": meaning, "n": nxt})
    for entity, (enum, rows) in K.WORKFLOWS.items():
        for code, status, _, nxt in rows:
            s_rows.append({"e": f"{entity} (stored code)", "s": code, "m": status, "n": nxt})

    wb = workbook("Data Dictionary", "Data dictionary", K.BRD, K.version("DDI"), K.DATE,
                  f"{K.NAME}: tables, columns, lists of values, parameters, status models and indexes")
    wb.sheet("Tables", [Column("t", "Table", 30), Column("m", "Module", 20), Column("p", "Purpose", 60),
                        Column("pk", "Primary key", 18), Column("n", "Columns", 9, kind="number"),
                        Column("v0", "Rows at go-live", 14), Column("v1", "Rows after one year", 16)], t_rows,
             description=getattr(K, "VOLUME_NOTE", "Every table of the module, with the estimated volume for about 530 users."))
    wb.sheet("Columns", [Column("t", "Table", 28), Column("c", "Column", 28), Column("dt", "Data type", 14),
                         Column("len", "Length / precision", 12), Column("null", "Null", 7),
                         Column("def", "Default", 20), Column("chk", "Constraints and allowed values", 40),
                         Column("fk", "Foreign key to", 22), Column("desc", "Description", 40)], c_rows,
             description="Every column of the tables, as the schema stands after the last change of the release.")
    wb.sheet("Lists of Values", [Column("l", "Code list", 30), Column("src", "Kind", 22), Column("c", "Code", 26),
                                 Column("lb", "Label", 36), Column("a", "Active", 8, values=["Yes", "No"])], l_rows,
             description="Maintained lists of values, fixed lists of the screens and allowed values of the columns.")
    wb.sheet("Parameters", [Column("k", "Key", 34), Column("lb", "Label on screen", 34), Column("ty", "Type", 12),
                            Column("d", "Default", 22), Column("r", "Range", 12),
                            Column("mc", "Maker-checker", 30), Column("ds", "Description", 60)], p_rows,
             description="Parameters of the module; a change applies only after another user approves it.")
    wb.sheet("Status Models", [Column("e", "Entity", 30), Column("s", "Status", 22), Column("m", "Meaning", 46),
                               Column("n", "Next statuses", 40)], s_rows,
             description="Statuses of each entity and the statuses it can move to.")
    wb.sheet("Indexes", [Column("t", "Table", 28), Column("i", "Index", 40), Column("k", "Kind", 12),
                         Column("c", "Columns", 40), Column("w", "Condition", 30)], i_rows,
             description="Primary keys, unique keys and indexes.")
    target = save_workbook(wb, out / K.file_name("DDI"))
    return target, {"Tables": len(t_rows), "Columns": len(c_rows), "Lists of Values": len(l_rows),
                    "Parameters": len(p_rows), "Status Models": len(s_rows), "Indexes": len(i_rows)}
