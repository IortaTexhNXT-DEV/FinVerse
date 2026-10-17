"""Schema snapshot behind the BIBS Database Design and Database Inventory.

Applies the Flyway schema migrations (backend/src/main/resources/db/migration) to a temporary PostgreSQL 16 database,
reads the system catalogue (tables, columns, constraints, indexes, triggers, sequences, views) into
data/catalog.json and the entity class documentation into data/descriptions.json, then drops the database:

  python3 docs/deliverables/src/programme/database_design/extract_schema.py --db bibs_dbd_tmp [--as-postgres]

The migration runner and the catalogue query are those of security_data/extract_catalog.py.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "security_data"))
import extract_catalog as ec  # noqa: E402

SEQ_SQL = ("select coalesce(json_agg(json_build_object('name', s.sequencename, 'start', s.start_value, "
           "'increment', s.increment_by, 'owned_by', (select d.refobjid::regclass::text || '.' || a.attname "
           "from pg_depend d join pg_attribute a on a.attrelid = d.refobjid and a.attnum = d.refobjsubid "
           "join pg_class c on c.relname = s.sequencename and c.relkind = 'S' "
           "where d.objid = c.oid and d.deptype in ('a','i') limit 1)) order by s.sequencename), '[]') "
           "from pg_sequences s where s.schemaname = 'public'")


def main(argv=None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default="bibs_dbd_tmp")
    ap.add_argument("--as-postgres", action="store_true")
    a = ap.parse_args(argv)
    try:
        n = ec.apply_migrations(a.db, a.as_postgres)
        cat = json.loads(ec.psql(["-d", a.db], a.as_postgres, stdin=ec.CATALOG_SQL))
        views = json.loads(ec.psql(["-d", a.db, "-c", ec.VIEWS_SQL], a.as_postgres) or "null") or []
        seqs = json.loads(ec.psql(["-d", a.db, "-c", SEQ_SQL], a.as_postgres))
        last = sorted(ec.MIGRATIONS.glob("V*__*.sql"), key=ec.version_key)[-1].name
        snap = {"migrations": n, "last_migration": last, "tables": cat, "views": views, "sequences": seqs}
        (HERE / "data").mkdir(exist_ok=True)
        (HERE / "data" / "catalog.json").write_text(json.dumps(snap, indent=1, sort_keys=True) + "\n", encoding="utf-8")
        (HERE / "data" / "descriptions.json").write_text(json.dumps(ec.descriptions(), indent=1, sort_keys=True) + "\n",
                                                         encoding="utf-8")
        print(f"{n} migrations to {last}; {len(cat)} tables, {len(views)} views, {len(seqs)} sequences")
    finally:
        ec.psql(["-d", "postgres", "-c", f'drop database if exists "{a.db}"'], a.as_postgres)
        print(f"dropped {a.db}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
