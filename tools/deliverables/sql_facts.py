"""Reference rows read from the Flyway migrations of the platform, without a database.

The configuration inputs workbook of a drop (drop_closure.py) lists every system parameter, list of values,
notification event and document template that the platform delivers, and every master table that the SIT seed data
fills, so that each of them has an owner and one route into production. This module reads them from the SQL files:

* ``rows(table)``: the rows of ``insert into <table> (columns) values (...), (...)`` statements of
  ``db/migration`` in version order, with the ``update <table> set ... where <key> = '...'`` statements applied;
  a row is a dict of the literal values (strings unquoted, ``null`` as None, other literals as text);
* ``columns(table)``: the columns of ``create table <table>`` and later ``alter table <table> add column``;
* ``seed_tables()``: the tables that the seed profile (``db/seed``) inserts rows into.

Inserts written as ``insert ... select`` are not read (none of the tables read here uses them for its rows).
"""

from __future__ import annotations

import re
from functools import lru_cache
from pathlib import Path

import brand
from code_facts import matching, split_top

DB = brand.REPO_ROOT / "backend" / "src" / "main" / "resources" / "db"
MIGRATIONS = DB / "migration"
SEED = DB / "seed"


def _version(p: Path) -> tuple[int, ...]:
    m = re.match(r"V(\d+(?:_\d+)*)__", p.name)
    return tuple(int(x) for x in m.group(1).split("_")) if m else (0,)


def _strip_comments(sql: str) -> str:
    """The SQL text without -- and /* */ comments (string literals kept)."""
    out, i, n = [], 0, len(sql)
    while i < n:
        c = sql[i]
        if c == "'":
            j = i + 1
            while j < n:
                if sql[j] == "'" and j + 1 < n and sql[j + 1] == "'":
                    j += 2
                    continue
                if sql[j] == "'":
                    break
                j += 1
            out.append(sql[i:j + 1])
            i = j + 1
        elif sql.startswith("--", i):
            k = sql.find("\n", i)
            i = n if k < 0 else k
        elif sql.startswith("/*", i):
            i = sql.index("*/", i) + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def _literal(cell: str) -> str | None:
    cell = cell.strip()
    if "||" in cell and cell.startswith("'"):  # 'a' || 'b': the concatenated text
        return "".join(_literal(p) or "" for p in split_top(cell, "|") if p.strip())
    if cell.lower() == "null":
        return None
    if cell.startswith("'") and cell.endswith("'"):
        return cell[1:-1].replace("''", "'")
    return cell


@lru_cache(maxsize=4)
def _files(folder: Path) -> tuple[tuple[Path, str], ...]:
    return tuple((p, _strip_comments(p.read_text(encoding="utf-8")))
                 for p in sorted(folder.glob("V*.sql"), key=_version))


def _statements(table: str, folder: Path = MIGRATIONS):
    """(kind, text) of the insert and update statements of a table, in migration order."""
    pat = re.compile(rf"\b(insert\s+into|update)\s+{re.escape(table)}\b", re.I)
    for _, sql in _files(folder):
        for m in pat.finditer(sql):
            end = sql.find(";", m.end())
            # a ; inside a string literal: extend to the ; that closes the statement
            while end >= 0 and sql[m.start():end].count("'") % 2:
                end = sql.find(";", end + 1)
            yield m.group(1).lower().split()[0], sql[m.end():end if end >= 0 else len(sql)]


@lru_cache(maxsize=None)
def rows(table: str, key: str = "code") -> tuple[dict[str, str | None], ...]:
    """Rows of a reference table, keyed by `key` (a later insert of the same key replaces the row)."""
    found: dict[str, dict[str, str | None]] = {}
    for kind, body in _statements(table):
        if kind == "insert":
            m = re.match(r"\s*\(", body)
            if not m:
                continue
            close = matching(body, m.end() - 1)
            cols = [c.strip().lower() for c in body[m.end():close].split(",")]
            rest = body[close + 1:]
            v = re.match(r"\s*values\s*", rest, re.I)
            if not v:
                sel = re.match(r"\s*select\s+", rest, re.I)
                fv = re.search(r"\bfrom\s*\(\s*values\s*", rest, re.I)
                if sel and fv:  # insert ... select v.a, 'x', v.b from (values (...), ...) as v(a, b)
                    exprs = split_top(rest[sel.end():fv.start()])
                    open_ = rest.rfind("(", 0, fv.end())
                    close = matching(rest, open_)
                    alias = re.match(r"\s*(?:as\s+)?(\w+)\s*\(([^)]*)\)", rest[close + 1:], re.I)
                    if alias and len(exprs) == len(cols):
                        names = [a.strip().lower() for a in alias.group(2).split(",")]
                        vals, i = rest[fv.end():close], 0
                        while i < len(vals) and vals[i] == "(":
                            j = matching(vals, i)
                            src = dict(zip(names, (_literal(c) for c in split_top(vals[i + 1:j]))))
                            row = {}
                            for col, e in zip(cols, exprs):
                                ref = re.fullmatch(rf"{alias.group(1)}\.(\w+)", e.strip())
                                row[col] = src.get(ref.group(1).lower()) if ref else _literal(e)
                            if key in row:
                                found[str(row[key])] = row
                            s = re.match(r"\s*,\s*", vals[j + 1:])
                            if not s:
                                break
                            i = j + 1 + s.end()
                    continue
                if sel:  # insert ... select <literals> [where not exists ...]: one row
                    cells = split_top(rest[sel.end():])
                    if cells:
                        cells[-1] = re.split(r"\s(?:where|from)\b", cells[-1], maxsplit=1, flags=re.I)[0]
                    if len(cells) == len(cols) and not any(re.search(r"\bfrom\b", c, re.I) for c in cells[:-1]):
                        row = dict(zip(cols, (_literal(c) for c in cells)))
                        if key in row:
                            found[str(row[key])] = row
                continue
            i = v.end()
            while i < len(rest) and rest[i] == "(":
                j = matching(rest, i)
                cells = [_literal(c) for c in split_top(rest[i + 1:j])]
                row = dict(zip(cols, cells))
                if key in row:
                    found[str(row[key])] = row
                i = j + 1
                s = re.match(r"\s*,\s*", rest[i:])
                if not s:
                    break
                i += s.end()
        else:
            m = re.match(r"\s*set\s+(.*?)\s+where\s+(.*)$", body, re.S | re.I)
            if not m:
                continue
            assigns = {}
            for part in split_top(m.group(1)):
                col, _, val = part.partition("=")
                val = val.strip()
                if val.startswith("'") or val.lower() in ("true", "false", "null") or re.fullmatch(r"-?\d+(\.\d+)?", val):
                    assigns[col.strip().lower()] = _literal(val)
            where = m.group(2)
            keys = re.findall(rf"\b{key}\s*=\s*'([^']*)'", where)
            ins = re.search(rf"\b{key}\s+in\s*\(([^)]*)\)", where)
            if ins:
                keys += [_literal(k) for k in ins.group(1).split(",")]
            for k in keys:
                if k in found:
                    found[k].update(assigns)
    return tuple(found.values())


@lru_cache(maxsize=None)
def columns(table: str) -> tuple[str, ...]:
    """Columns of a table from its create table statement and later add column statements."""
    cols: list[str] = []
    for _, sql in _files(MIGRATIONS):
        m = re.search(rf"\bcreate\s+table\s+(?:if\s+not\s+exists\s+)?{re.escape(table)}\s*\(", sql, re.I)
        if m:
            body = sql[m.end():matching(sql, m.end() - 1)]
            for part in split_top(body):
                word = part.split()[0].lower() if part.split() else ""
                if word and word not in ("constraint", "primary", "unique", "foreign", "check", "exclude"):
                    cols.append(word)
        for a in re.finditer(rf"\balter\s+table\s+(?:if\s+exists\s+)?{re.escape(table)}\b([^;]*);", sql, re.I):
            cols += [c.lower() for c in re.findall(r"add\s+column\s+(?:if\s+not\s+exists\s+)?(\w+)", a.group(1), re.I)]
    return tuple(dict.fromkeys(cols))


@lru_cache(maxsize=1)
def seed_tables() -> tuple[str, ...]:
    """Tables the seed profile inserts rows into (db/seed)."""
    found = set()
    for _, sql in _files(SEED):
        found.update(t.lower() for t in re.findall(r"\binsert\s+into\s+(\w+)", sql, re.I))
    return tuple(sorted(found))


if __name__ == "__main__":
    for t, k in (("sys_parameter", "param_key"), ("lov_type", "code"), ("msg_notification_event", "code"),
                 ("doc_template", "code")):
        print(t, len(rows(t, k)))
    print("seed tables", len(seed_tables()))
