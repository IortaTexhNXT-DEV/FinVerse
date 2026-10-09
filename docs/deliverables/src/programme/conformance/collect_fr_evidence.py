"""Collects the functional requirements of every FRS with their acceptance criteria, and the automated checks of
the platform that cover each requirement, into fr_evidence.json (input of build_conformance.py).

A check covers a requirement when its source names the requirement, or when it exercises a class of the platform
whose source names the requirement (the class under test, a class of the platform the check uses, or the service
behind a screen address the check calls and the classes of its module that service uses). The name of a
check is the first sentence of its description, in business words; its result comes from the test reports of the
last full build (backend/target/surefire-reports, frontend vitest JSON when given).

Usage: python3 collect_fr_evidence.py [--vitest results.json]
"""

from __future__ import annotations

import argparse
import glob
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
FRS = sorted(glob.glob(str(ROOT / "docs/deliverables/src/BRD-*/FRS_*.md"))) + [
    str(ROOT / "docs/deliverables/src/BRD-13_Data_Migration/HANDBOOK_BRD13_DATA_MIGRATION.md")]
FR_ID = re.compile(r"FR-[A-Z]{2,3}-\d{3}")
BRD_ID = re.compile(r"(?<!['\"])\b((?:BR[A-Z]+|[A-Z]{3,5}ID)\.\d{3}|BR-\d{3}|(?:FRBS|BASAU|DIS|ACSL|BRID_MIG) \d+\.\d+(?:\.\d+)?)\b")
JAVA_MAIN = ROOT / "backend/src/main/java"
JAVA_TEST = ROOT / "backend/src/test/java"
FRONT = ROOT / "frontend/src"
PACKAGE = "com.iortatechnxt.brokerverse"


def brd_of(path: str) -> str:
    m = re.search(r"BRD-(\d\d)", path)
    return f"BRD-{m.group(1)}"


def requirements() -> list[dict]:
    out = []
    for f in FRS:
        text = Path(f).read_text(encoding="utf-8")
        for block in re.findall(r"```fr\n(.*?)```", text, re.S):
            d = yaml.safe_load(block)
            out.append({
                "brd": brd_of(f),
                "id": d["id"],
                "title": d.get("title", ""),
                "screens": d.get("screens", "") if isinstance(d.get("screens"), str) else ", ".join(d.get("screens") or []),
                "priority": d.get("priority", ""),
                "acceptance": [str(a) for a in (d.get("acceptance") or [])],
                "brd_ids": sorted(set(BRD_ID.findall(" ".join(map(str, d.get("brd") or []))))),
            })
    return out


def first_sentence(doc: str) -> str:
    doc = re.sub(r"\{@(?:code|link) ([^}]*)\}", r"\1", doc)
    doc = re.sub(r"<[^>]+>", "", doc)
    doc = " ".join(line.strip(" */") for line in doc.splitlines()).strip()
    m = re.match(r"(.+?[.:;])(\s|$)", doc)
    return (m.group(1) if m else doc)[:240].rstrip(":;").strip()


def words(name: str) -> str:
    base = re.sub(r"(IT|Test|Tests)$", "", name)
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", base).lower()


def java_classes() -> tuple[dict[str, set[str]], dict[str, str]]:
    """Main classes (simple name -> FR ids of its source) and simple name -> fully qualified name."""
    frs: dict[str, set[str]] = {}
    fqn: dict[str, str] = {}
    for f in JAVA_MAIN.rglob("*.java"):
        name = f.stem
        text = f.read_text(encoding="utf-8")
        ids = set(FR_ID.findall(text)) | set(BRD_ID.findall(text))
        if ids:
            frs.setdefault(name, set()).update(ids)
        pkg = re.search(r"^package ([\w.]+);", text, re.M)
        if pkg:
            fqn[name] = f"{pkg.group(1)}.{name}"
    return frs, fqn


def api_frs(main_frs: dict[str, set[str]], fqn: dict[str, str]) -> list[tuple[str, set[str]]]:
    """Base path of each REST controller with the FR ids of the controller and of the classes of its module it uses
    (an integration check that calls the path exercises them)."""
    out = []
    for f in JAVA_MAIN.rglob("*Controller.java"):
        text = f.read_text(encoding="utf-8")
        head = text.split(" class ", 1)[0]
        bases = re.findall(r'@RequestMapping\(\s*(?:value\s*=\s*|path\s*=\s*)?\{?\s*"(/[^"]+)"', head)
        if not bases:
            continue
        module = re.sub(r"^(" + re.escape(PACKAGE) + r"\.\w+).*$", r"\1", fqn.get(f.stem, ""))
        ids = set(main_frs.get(f.stem, set()))
        for name in set(re.findall(r"\b([A-Z]\w+)\b", text)):
            if name in main_frs and fqn.get(name, "").startswith(module + "."):
                ids |= main_frs[name]
        out += [(b.rstrip("/"), ids) for b in bases]
    return out


def java_checks(main_frs: dict[str, set[str]], apis: list[tuple[str, set[str]]] | None = None) -> list[dict]:
    checks = []
    for f in sorted(JAVA_TEST.rglob("*.java")):
        if not (f.stem.endswith("Test") or f.stem.endswith("IT")):
            continue
        text = f.read_text(encoding="utf-8")
        direct = set(FR_ID.findall(text)) | set(BRD_ID.findall(text))
        used = set(re.findall(rf"import {re.escape(PACKAGE)}\.[\w.]*\.(\w+);", text))
        same_pkg = set(re.findall(r"\b([A-Z]\w+)\b", text))
        target = re.sub(r"(IT|Test)$", "", f.stem)
        via = set()
        for cls in used | ({target} & same_pkg) | {target}:
            via |= main_frs.get(cls, set())
        for called in set(re.findall(r'"(/api/v1/[^"?{]*)', text)):
            for base, ids in apis or []:
                if called == base or called.startswith(base + "/"):
                    via |= ids
        doc = re.search(r"/\*\*(.*?)\*/\s*(?:@[\w.]+(?:\([^)]*\))?\s*)*(?:public\s+)?(?:final\s+)?class\s", text, re.S)
        name = first_sentence(doc.group(1)) if doc else words(f.stem).capitalize()
        module = f.relative_to(JAVA_TEST).parts[3] if len(f.relative_to(JAVA_TEST).parts) > 4 else ""
        checks.append({"id": f.stem, "kind": "backend", "module": module, "named": bool(doc),
                       "name": name or words(f.stem).capitalize(),
                       "direct": sorted(direct), "via": sorted(via - direct)})
    return checks


def front_checks() -> list[dict]:
    checks = []
    comp_frs = {}
    for f in FRONT.rglob("*.ts*"):
        if ".test." in f.name:
            continue
        source = f.read_text(encoding="utf-8")
        ids = set(FR_ID.findall(source)) | set(BRD_ID.findall(source))
        if ids:
            comp_frs[str(f.with_suffix("")).split(".")[0]] = ids
    for f in sorted(FRONT.rglob("*.test.ts*")):
        text = f.read_text(encoding="utf-8")
        direct = set(FR_ID.findall(text)) | set(BRD_ID.findall(text))
        base = str(f).split(".test.")[0]
        via = comp_frs.get(base, set())
        for imp in re.findall(r"from '\./([\w/]+)'", text):
            via |= comp_frs.get(str(f.parent / imp), set())
        desc = re.search(r"describe\('([^']+)'", text)
        name = desc.group(1) if desc else words(f.name.split(".")[0])
        rel = f.relative_to(FRONT).parts
        checks.append({"id": str(f.relative_to(FRONT)), "kind": "frontend", "named": bool(desc),
                       "module": rel[1] if rel[0] == "features" and len(rel) > 2 else rel[0],
                       "name": name[0].upper() + name[1:],
                       "direct": sorted(direct), "via": sorted(via - direct)})
    return checks


def backend_results() -> dict[str, str]:
    out = {}
    for f in glob.glob(str(ROOT / "backend/target/surefire-reports/TEST-*.xml")):
        try:
            root = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        name = root.get("name", "").rsplit(".", 1)[-1]
        bad = int(root.get("failures", 0)) + int(root.get("errors", 0))
        out[name] = "failed" if bad else "passed"
    return out


def frontend_results(path: str | None) -> dict[str, str]:
    if not path or not Path(path).exists():
        return {}
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    out = {}
    for r in data.get("testResults", []):
        rel = str(Path(r["name"]).resolve().relative_to(FRONT.resolve()))
        out[rel] = "passed" if r.get("status") == "passed" else "failed"
    return out


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--vitest")
    args = parser.parse_args()
    main_frs, fqn = java_classes()
    checks = java_checks(main_frs, api_frs(main_frs, fqn)) + front_checks()
    results = backend_results() | frontend_results(args.vitest)
    for c in checks:
        c["result"] = results.get(c["id"], "not run")
    reqs = requirements()
    for r in reqs:
        keys = {r["id"], *r["brd_ids"]}
        r["direct"] = [c["id"] for c in checks if keys & set(c["direct"])]
        r["via"] = [c["id"] for c in checks if keys & set(c["via"]) and c["id"] not in r["direct"]]
    out = HERE / "data" / "fr_evidence.json"
    out.parent.mkdir(exist_ok=True)
    out.write_text(json.dumps({"requirements": reqs, "checks": {c["id"]: c for c in checks}}, indent=1),
                   encoding="utf-8")
    covered = sum(1 for r in reqs if r["direct"] or r["via"])
    print(f"{len(reqs)} requirements, {sum(len(r['acceptance']) for r in reqs)} acceptance criteria, "
          f"{len(checks)} checks, {covered} requirements with a check")


if __name__ == "__main__":
    main()
