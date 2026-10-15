#!/usr/bin/env python3
"""Foreign keys without a supporting index.

Builds the final schema from the Flyway scripts (backend/src/main/resources/db/migration and
db/seed, applied in version order into a throw-away PostgreSQL cluster, with the Flyway
placeholders filled in), or reads an already migrated database (--dsn), and lists every foreign
key whose columns are not the leading columns of an index (in any order, partial indexes do not
count). With --migration it writes the migration that adds the missing indexes:

    create index if not exists ix_<table>_<col> on <table>(<col>);

Usage:
    tools/db/fk_index_check.py                      # report; exit 1 when an index is missing
    tools/db/fk_index_check.py --migration V2819__foreign_key_indexes.sql
    tools/db/fk_index_check.py --dsn postgresql://user:pw@localhost/brokerverse

The cluster is made with the PostgreSQL binaries on the PATH or under /usr/lib/postgresql/*/bin;
as root it runs as the postgres user.
"""

import argparse
import glob
import hashlib
import os
import pwd
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import time

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DB_DIR = os.path.join(ROOT, "backend", "src", "main", "resources", "db")
LOCATIONS = ("migration", "seed")
PLACEHOLDERS = {"runtime_role": "brokerverse_runtime", "flyway:table": "flyway_schema_history"}
MAX_IDENTIFIER = 63

MISSING_SQL = """
select n.nspname, t.relname,
       array_to_string(array(select a.attname from unnest(c.conkey) with ordinality k(num, pos)
                             join pg_attribute a on a.attrelid = c.conrelid and a.attnum = k.num
                             order by k.pos), ',')
from pg_constraint c
join pg_class t on t.oid = c.conrelid
join pg_namespace n on n.oid = t.relnamespace
where c.contype = 'f'
  and n.nspname not in ('pg_catalog', 'information_schema')
  and not exists (
    select 1 from pg_index i
    where i.indrelid = c.conrelid and i.indpred is null
      and (string_to_array(i.indkey::text, ' ')::int2[])[1:cardinality(c.conkey)] @> c.conkey
      and (string_to_array(i.indkey::text, ' ')::int2[])[1:cardinality(c.conkey)] <@ c.conkey)
order by 1, 2, 3
"""


def scripts():
    """The Flyway scripts in the order a new database applies them."""
    found = []
    for location in LOCATIONS:
        for path in glob.glob(os.path.join(DB_DIR, location, "V*__*.sql")):
            version = re.match(r"V([0-9_.]+)__", os.path.basename(path)).group(1)
            key = tuple(int(p) for p in re.split(r"[._]", version) if p)
            found.append((key, path))
    return [path for _, path in sorted(found)]


def binary(name):
    on_path = shutil.which(name)
    if on_path:
        return on_path
    candidates = sorted(glob.glob(f"/usr/lib/postgresql/*/bin/{name}"))
    if not candidates:
        sys.exit(f"PostgreSQL binary {name} not found")
    return candidates[-1]


def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


class Cluster:
    """A throw-away cluster; as root the server runs as the postgres user."""

    def __init__(self):
        self.dir = tempfile.mkdtemp(prefix="fkcheck-")
        self.port = free_port()
        self.prefix = []
        if os.geteuid() == 0:
            owner = pwd.getpwnam("postgres")
            os.chown(self.dir, owner.pw_uid, owner.pw_gid)
            self.prefix = ["runuser", "-u", "postgres", "--"]
        self.data = os.path.join(self.dir, "data")

    def __enter__(self):
        run = lambda *cmd: subprocess.run(self.prefix + list(cmd), check=True, capture_output=True)
        run(binary("initdb"), "-D", self.data, "-U", "postgres", "-A", "trust", "--no-sync")
        run(binary("pg_ctl"), "-D", self.data, "-w", "-l", os.path.join(self.dir, "log"),
            "-o", f"-p {self.port} -k {self.dir} -c fsync=off -c listen_addresses=''", "start")
        return self

    def __exit__(self, *exc):
        subprocess.run(self.prefix + [binary("pg_ctl"), "-D", self.data, "-m", "immediate",
                                      "stop"], capture_output=True)
        shutil.rmtree(self.dir, ignore_errors=True)

    def dsn(self):
        return f"host={self.dir} port={self.port} user=postgres dbname=postgres"


def psql(dsn, sql=None, path=None):
    cmd = [binary("psql"), dsn, "-X", "-q", "-1", "-v", "ON_ERROR_STOP=1", "-A", "-t", "-F", "|"]
    if path is not None:
        text = open(path, encoding="utf-8").read()
        for key, value in PLACEHOLDERS.items():
            text = text.replace("${" + key + "}", value)
        sql = text
    done = subprocess.run(cmd, input=sql, text=True, capture_output=True)
    if done.returncode != 0:
        sys.exit(f"{path or 'query'} failed:\n{done.stderr}")
    return done.stdout


def migrate(dsn):
    psql(dsn, sql=f"create role {PLACEHOLDERS['runtime_role']};"
                  " create table flyway_schema_history (installed_rank int);")
    for path in scripts():
        psql(dsn, path=path)


def index_name(table, columns):
    name = "ix_" + table + "_" + "_".join(columns)
    if len(name) <= MAX_IDENTIFIER:
        return name
    digest = hashlib.sha1(name.encode()).hexdigest()[:8]
    return name[: MAX_IDENTIFIER - 9] + "_" + digest


def missing(dsn):
    rows = []
    for line in psql(dsn, sql=MISSING_SQL).splitlines():
        if line.strip():
            schema, table, columns = line.split("|")
            qualified = table if schema == "public" else f"{schema}.{table}"
            rows.append((qualified, tuple(columns.split(","))))
    return sorted(set(rows))


def statements(rows):
    return [
        f"create index if not exists {index_name(t.split('.')[-1], c)} on {t}({', '.join(c)});"
        for t, c in rows
    ]


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--dsn", help="read this migrated database instead of building one")
    parser.add_argument("--migration", help="write the missing indexes to this migration file")
    args = parser.parse_args()
    started = time.time()
    if args.dsn:
        rows = missing(args.dsn)
    else:
        with Cluster() as cluster:
            migrate(cluster.dsn())
            rows = missing(cluster.dsn())
    lines = statements(rows)
    for line in lines:
        print(line)
    print(f"-- {len(lines)} foreign key(s) without an index ({time.time() - started:.0f} s)",
          file=sys.stderr)
    if args.migration:
        path = args.migration
        if not os.path.isabs(path):
            path = os.path.join(DB_DIR, "migration", path)
        with open(path, "w", encoding="utf-8") as out:
            out.write("-- Indexes on the foreign key columns that had none (tools/db/fk_index_check.py):\n"
                      "-- deletes and key updates of the parent row and the joins from the parent\n"
                      "-- no longer scan the child table.\n")
            out.write("\n".join(lines) + "\n")
    return 1 if lines and not args.migration else 0


if __name__ == "__main__":
    sys.exit(main())
