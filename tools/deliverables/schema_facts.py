"""The final schema of the BIBS database for the data dictionary of a BRD (DDI).

    python tools/deliverables/schema_facts.py '^(sec_|idn_)'      # prints the tables that match

The schema is read from a scratch PostgreSQL database built by replaying every migration of
backend/src/main/resources/db/migration in version order (each in one transaction, as the migration tool runs them),
so that the dictionary shows the schema as it stands after the last migration, with the reference data the
migrations load (parameters, lists of values). The scratch database is rebuilt when the set of migrations changes.

Needs a local PostgreSQL 14+ server reachable with psql as the operating-system user postgres (or set
DDI_PSQL to the psql command line to use, for example "psql -h localhost -U postgres").
"""

from __future__ import annotations

import hashlib
import json
import os
import re
import shlex
import subprocess
import sys
from functools import lru_cache
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand  # noqa: E402

MIGRATIONS = brand.REPO_ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration"
DB = os.environ.get("DDI_DATABASE", "bibs_dictionary")
PLACEHOLDERS = {"${runtime_role}": "bibs_runtime", "${flyway:table}": "flyway_schema_history"}


def _psql_cmd(db: str) -> list[str]:
    if os.environ.get("DDI_PSQL"):
        return shlex.split(os.environ["DDI_PSQL"]) + ["-d", db]
    if os.geteuid() == 0:
        return ["su", "postgres", "-c", f"psql -d {db}"]
    return ["psql", "-d", db]


def _run(sql: str, db: str = DB, single: bool = False) -> str:
    cmd = _psql_cmd(db)
    flags = "-q -At -v ON_ERROR_STOP=1" + (" -1" if single else "")
    if cmd[0] == "su":
        cmd = cmd[:-1] + [cmd[-1] + " " + flags]
    else:
        cmd = cmd + shlex.split(flags)
    r = subprocess.run(cmd, input=sql, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(r.stderr.strip()[:400])
    return r.stdout


def _version(p: Path) -> tuple[int, ...]:
    return tuple(int(x) for x in re.match(r"V([\d_]+?)__", p.name).group(1).split("_"))


def migration_files() -> list[Path]:
    return sorted(MIGRATIONS.glob("V*__*.sql"), key=_version)


def _fingerprint(files: list[Path]) -> str:
    h = hashlib.sha256()
    for p in files:
        h.update(p.name.encode())
        h.update(p.read_bytes())
    return h.hexdigest()[:16]


def ensure_database() -> str:
    """Builds the scratch database when missing or out of date; returns the fingerprint of the migrations."""
    files = migration_files()
    fp = _fingerprint(files)
    try:
        current = _run("select shobj_description(oid, 'pg_database') from pg_database where datname = current_database()").strip()
    except RuntimeError:
        current = ""
    if current == fp:
        return fp
    _run(f"drop database if exists {DB}", db="postgres")
    _run(f"create database {DB}", db="postgres")
    _run("do $$ begin if not exists (select 1 from pg_roles where rolname = 'bibs_runtime') then "
         "create role bibs_runtime; end if; end $$; create table flyway_schema_history(id int);")
    for p in files:
        sql = p.read_text(encoding="utf-8")
        for k, v in PLACEHOLDERS.items():
            sql = sql.replace(k, v)
        try:
            _run(sql, single=True)
        except RuntimeError as e:
            raise SystemExit(f"migration {p.name} failed in the scratch database: {e}") from e
    _run("drop table flyway_schema_history")
    _run(f"comment on database {DB} is '{fp}'")
    return fp


def query(sql: str) -> list[dict]:
    ensure_database()
    out = _run(f"select coalesce(json_agg(t), '[]') from ({sql}) t")
    return json.loads(out or "[]")


@lru_cache(maxsize=None)
def tables(pattern: str) -> list[dict]:
    """Tables whose name matches the regular expression: name, comment, estimated rows (none on a scratch base)."""
    return query(f"""select c.relname as name, obj_description(c.oid) as comment
        from pg_class c join pg_namespace n on n.oid = c.relnamespace
        where n.nspname = 'public' and c.relkind = 'r' and c.relname ~ '{pattern}' order by 1""")


def columns(table: str) -> list[dict]:
    return query(f"""select a.attnum as pos, a.attname as name, format_type(a.atttypid, a.atttypmod) as type,
        not a.attnotnull as nullable, pg_get_expr(d.adbin, d.adrelid) as default,
        a.attidentity as identity, col_description(a.attrelid, a.attnum) as comment
        from pg_attribute a left join pg_attrdef d on d.adrelid = a.attrelid and d.adnum = a.attnum
        where a.attrelid = 'public.{table}'::regclass and a.attnum > 0 and not a.attisdropped order by a.attnum""")


def constraints(table: str) -> list[dict]:
    return query(f"""select conname as name, contype as type, pg_get_constraintdef(oid) as definition,
        (select array_agg(attname order by attnum) from pg_attribute where attrelid = conrelid
         and attnum = any(conkey)) as cols, confrelid::regclass::text as ref_table
        from pg_constraint where conrelid = 'public.{table}'::regclass order by contype, conname""")


def indexes(table: str) -> list[dict]:
    return query(f"""select i.relname as name, ix.indisunique as unique, ix.indisprimary as primary,
        pg_get_indexdef(ix.indexrelid) as definition
        from pg_index ix join pg_class i on i.oid = ix.indexrelid
        where ix.indrelid = 'public.{table}'::regclass order by i.relname""")


def triggers(table: str) -> list[dict]:
    return query(f"""select tgname as name, pg_get_triggerdef(oid) as definition from pg_trigger
        where tgrelid = 'public.{table}'::regclass and not tgisinternal order by 1""")


def rows(sql: str) -> list[dict]:
    """Reference data loaded by the migrations (parameters, lists of values...)."""
    return query(sql)


def allowed_values(definition: str) -> list[str]:
    """CHECK ((status)::text = ANY ((ARRAY['A'::character varying, ...])::text[])) -> [A, ...]."""
    if "ARRAY[" not in definition and " IN (" not in definition.upper():
        return []
    return re.findall(r"'([^']*)'::", definition) or re.findall(r"'([^']*)'", definition)


def main(argv: list[str]) -> int:
    for t in tables(argv[0] if argv else "^sec_"):
        print(t["name"], len(columns(t["name"])))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
