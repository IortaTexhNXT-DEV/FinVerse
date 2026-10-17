"""Refreshes the catalogue snapshot behind the Data Dictionary and the entity relationship diagrams (BRD-00).

The snapshot (data/catalog.json and data/descriptions.json) is what build_secdata_pack.py reads, so the documents
can be rebuilt without a database. Refresh it whenever the production schema changes:

  python docs/deliverables/src/programme/security_data/extract_catalog.py --db bibs_dd_refresh --create --drop \
      [--as-postgres]

Steps
  1. --create: creates an empty PostgreSQL 16 database (the cluster must hold the role brokerverse_runtime, see
     DEPLOYMENT.md "Database roles"; it is created when missing);
  2. applies the production schema migrations of backend/src/main/resources/db/migration in version order, one
     transaction per file, with the placeholders of application.yml (runtime role, history table) substituted;
     the seed migrations (db/seed) are never applied, and the application is not started;
  3. reads tables, columns, types, nullability, defaults, keys, foreign keys, check constraints, indexes, triggers
     and comments from the system catalogue into data/catalog.json;
  4. reads the class documentation of each persistent entity (the first comment block above the class) into
     data/descriptions.json, keyed by table, with the package that owns the table;
  5. --drop: drops the database again.

--as-postgres runs psql through `su postgres -c` (a host where only the postgres account has peer access).
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
MIGRATIONS = REPO / "backend" / "src" / "main" / "resources" / "db" / "migration"
JAVA = REPO / "backend" / "src" / "main" / "java" / "com" / "iortatechnxt" / "brokerverse"
PLACEHOLDERS = {"${runtime_role}": "brokerverse_runtime", "${flyway:table}": "flyway_schema_history"}
HISTORY = ("create table flyway_schema_history(installed_rank int primary key, version varchar(50), "
           "description varchar(200), type varchar(20), script varchar(1000), checksum int, installed_by varchar(100), "
           "installed_on timestamp default now(), execution_time int, success boolean)")

CATALOG_SQL = """
select json_agg(t order by t.name) from (
 select c.relname as name, obj_description(c.oid, 'pg_class') as comment,
  (select json_agg(json_build_object('name', a.attname, 'pos', a.attnum, 'type', format_type(a.atttypid, a.atttypmod),
      'nullable', not a.attnotnull, 'default', pg_get_expr(d.adbin, d.adrelid), 'identity', a.attidentity,
      'generated', a.attgenerated, 'comment', col_description(c.oid, a.attnum)) order by a.attnum)
     from pg_attribute a left join pg_attrdef d on d.adrelid = a.attrelid and d.adnum = a.attnum
     where a.attrelid = c.oid and a.attnum > 0 and not a.attisdropped) as columns,
  (select json_agg(json_build_object('name', k.conname, 'type', k.contype, 'def', pg_get_constraintdef(k.oid),
      'cols', (select json_agg(a.attname order by x.ord) from unnest(k.conkey) with ordinality x(n, ord)
               join pg_attribute a on a.attrelid = k.conrelid and a.attnum = x.n),
      'ref_table', case when k.contype = 'f' then k.confrelid::regclass::text end,
      'ref_cols', (select json_agg(a.attname order by x.ord) from unnest(k.confkey) with ordinality x(n, ord)
                   join pg_attribute a on a.attrelid = k.confrelid and a.attnum = x.n),
      'on_delete', k.confdeltype) order by k.contype, k.conname)
     from pg_constraint k where k.conrelid = c.oid) as constraints,
  (select json_agg(json_build_object('name', i.relname, 'unique', x.indisunique, 'primary', x.indisprimary,
      'def', pg_get_indexdef(x.indexrelid)) order by i.relname)
     from pg_index x join pg_class i on i.oid = x.indexrelid where x.indrelid = c.oid) as indexes,
  (select json_agg(json_build_object('name', g.tgname, 'def', pg_get_triggerdef(g.oid)) order by g.tgname)
     from pg_trigger g where g.tgrelid = c.oid and not g.tgisinternal) as triggers
 from pg_class c where c.relnamespace = 'public'::regnamespace and c.relkind in ('r', 'p')
   and c.relname <> 'flyway_schema_history') t
"""
VIEWS_SQL = ("select json_agg(json_build_object('name', viewname) order by viewname) from pg_views "
             "where schemaname = 'public'")


def psql(args: list[str], as_postgres: bool, stdin: str | None = None) -> str:
    cmd = ["psql", "-X", "-q", "-At", "-v", "ON_ERROR_STOP=1", *args]
    if as_postgres:
        cmd = ["su", "postgres", "-c", " ".join(shlex.quote(c) for c in cmd)]
    res = subprocess.run(cmd, input=stdin, capture_output=True, text=True, check=False)
    if res.returncode != 0:
        raise RuntimeError(res.stderr.strip() or res.stdout.strip())
    return res.stdout


def version_key(path: Path) -> tuple[int, ...]:
    m = re.match(r"V(\d+(?:[._]\d+)*)__", path.name)
    return tuple(int(x) for x in re.split(r"[._]", m.group(1))) if m else (10**9,)


def apply_migrations(db: str, as_postgres: bool) -> int:
    exists = psql(["-d", "postgres", "-c", "select 1 from pg_roles where rolname = 'brokerverse_runtime'"], as_postgres)
    if not exists.strip():
        psql(["-d", "postgres", "-c", "create role brokerverse_runtime nologin"], as_postgres)
    psql(["-d", "postgres", "-c", f'create database "{db}"'], as_postgres)
    psql(["-d", db, "-c", HISTORY], as_postgres)
    files = sorted(MIGRATIONS.glob("V*__*.sql"), key=version_key)
    for f in files:
        text = f.read_text(encoding="utf-8")
        for k, v in PLACEHOLDERS.items():
            text = text.replace(k, v)
        try:
            psql(["-d", db, "--single-transaction"], as_postgres, stdin=text)
        except RuntimeError as exc:
            raise RuntimeError(f"{f.name}: {exc}") from exc
    return len(files)


def snake(name: str) -> str:
    return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", name).lower()


def clean_doc(doc: str) -> str:
    doc = re.sub(r"\s*\n\s*\*\s?", " ", doc).strip()
    return re.sub(r"\s@(param|return|see|since|author)\b.*", "", doc).strip()


def descriptions() -> dict[str, dict]:
    out: dict[str, dict] = {}
    for path in sorted(JAVA.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        m = re.search(r'@Table\(\s*name\s*=\s*"([a-z0-9_]+)"', text)
        if not m:
            continue
        package = path.relative_to(JAVA).parts[0]
        cm = re.search(r"/\*\*((?:(?!\*/).)*)\*/\s*(?:@[^\n]*\n\s*)*(?:public\s+)?(?:abstract\s+)?(?:final\s+)?class\s",
                       text, re.S)
        out[m.group(1)] = {"package": package, "doc": clean_doc(cm.group(1)) if cm else ""}
    return out


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--db", required=True, help="database name (created with --create)")
    ap.add_argument("--create", action="store_true", help="create the database and apply the schema migrations")
    ap.add_argument("--drop", action="store_true", help="drop the database at the end")
    ap.add_argument("--as-postgres", action="store_true", help="run psql as the postgres account (su)")
    args = ap.parse_args(argv)
    try:
        if args.create:
            print(f"applied {apply_migrations(args.db, args.as_postgres)} schema migrations to {args.db}")
        catalog = json.loads(psql(["-d", args.db], args.as_postgres, stdin=CATALOG_SQL))
        views = json.loads(psql(["-d", args.db, "-c", VIEWS_SQL], args.as_postgres) or "null") or []
        (HERE / "data").mkdir(exist_ok=True)
        snapshot = {"migrations": len(list(MIGRATIONS.glob("V*__*.sql"))), "tables": catalog, "views": views}
        (HERE / "data" / "catalog.json").write_text(json.dumps(snapshot, indent=1, sort_keys=True) + "\n",
                                                  encoding="utf-8")
        (HERE / "data" / "descriptions.json").write_text(json.dumps(descriptions(), indent=1, sort_keys=True) + "\n",
                                                       encoding="utf-8")
        print(f"catalog: {len(catalog)} tables, {len(views)} views")
    finally:
        if args.drop:
            psql(["-d", "postgres", "-c", f'drop database if exists "{args.db}"'], args.as_postgres)
            print(f"dropped {args.db}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
