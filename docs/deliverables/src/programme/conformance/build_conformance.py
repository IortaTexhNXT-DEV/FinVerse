"""Builds the End-to-End Conformance Register (programme level, BRD-00):
Programme/Quality/BIBS_Conformance_BRD-00_End-to-End_Conformance_Register_v<version>.xlsx.

Inputs (this folder):
  data/fr_evidence.json      requirements of every FRS with their acceptance criteria and the automated checks that
                             cover them, with the result of the last full build (collect_fr_evidence.py)
  data/wt_results.json       the live run of the walkthroughs of the sign-off packs (collect_walkthroughs.py)
  data/steps_plan.json       the walkthrough script of the UAT users workbook (plan_steps.py)
  data/steps_run.json        the screen checks of the steps no pack walkthrough performs (run_steps.cjs)
  data/cycle_run.json        the cross-BRD broking cycle (cycle.cjs)
  data/persona_run.json      the persona access check (persona_expected.py, persona_access.cjs)
  data/screen_labels.json    the menu name of every screen (persona_expected.py)
  step_checks.yaml           the screens of each step of the script and the blocked steps
  assessment.yaml            the review of the results: step notes, requirement decisions, the fixes made, the open
                             gaps with their proposals and the corrections of the FRS texts

Usage: python3 build_conformance.py
"""

from __future__ import annotations

import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parents[5]
HERE = Path(__file__).resolve().parent
DATA = HERE / "data"
sys.path.insert(0, str(ROOT / "tools/deliverables"))
import brand  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

TITLE = "End-to-End Conformance Register"
RESULTS = ["Conformant", "Partly conformant", "Not conformant", "Not testable yet"]
STEP_RESULTS = ["Pass", "Fail", "Blocked", "Not run"]
MODULES = {
    "BRD-00": "Platform (cross-BRD requirements)", "BRD-01": "New Business", "BRD-02": "Operations",
    "BRD-03": "Product Maintenance", "BRD-04": "Collections", "BRD-05": "Accounting, Disbursement and ACSL",
    "BRD-06": "Renewal", "BRD-07": "Claims", "BRD-08": "Employee Benefits", "BRD-09": "Customer Service Facility",
    "BRD-10": "Sanctions Screening", "BRD-11": "User Access Maintenance", "BRD-12": "Submitted Policies",
    "BRD-13": "Data Migration",
}


def load(name: str, default=None):
    p = DATA / name
    return json.loads(p.read_text(encoding="utf-8")) if p.exists() else default


import check_pack  # noqa: E402

GUARDS = [p for label, p in check_pack.BUILD_STATUS + check_pack.TECHNICAL
          if label not in ("built", "build", "designed")]
UNREADABLE = "The step could not be completed: a record that an earlier step creates was not there"


def clean(text: object) -> str:
    """Business wording of a recorded observation: the message of a step that stopped on a technical error is given
    in business words; addresses of the screens and internal names are left out."""
    s = re.sub(r"https?://\S+", "", str(text or ""))
    s = re.sub(r"\s+", " ", s).strip()
    if re.search(r"\b(?:select|psql|undefined|locator|timeout \d+ms|Cannot read properties)\b", s, re.I):
        return UNREADABLE if re.search(r"select|psql|undefined", s, re.I) else (
            "The step could not be completed: the screen did not offer the expected field or button in time")
    for guard in GUARDS:
        s = guard.sub("", s)
    return re.sub(r"\s{2,}", " ", s).strip()[:600]


AREAS = {
    "account": "Accounts", "accounts": "Accounts", "accounting": "Accounting", "accounting-engine": "Accounting events",
    "acsl": "ACSL", "adjustment": "Adjustment", "admin": "User administration", "alert": "Alerts", "alerts": "Alerts",
    "api": "Platform services", "approval": "Approvals", "assets": "Fixed assets", "attachment": "Documents",
    "audit": "Audit trail", "auth": "Sign-in", "booking": "Booking", "brokerclaims": "Claims",
    "brokingsetup": "Broking set-up", "budget": "Budget", "bulk": "Bulk uploads", "cache": "Platform services",
    "cashiering": "Cashiering", "catalog": "Product catalogue", "closing": "Period closing", "coa": "Chart of accounts",
    "collections": "Collections", "commission": "Commission", "common": "Platform services",
    "components": "Common screen elements", "config": "Configuration", "configpromo": "Configuration promotion",
    "crm": "Clients", "csf": "Customer Service Facility", "dashboard": "Dashboards", "disbursement": "Disbursement",
    "docgen": "Business documents", "eb": "Employee Benefits", "events": "Business events", "finreport": "Financial reports",
    "fixedasset": "Fixed assets", "frbs": "Financial reports", "gl": "General ledger", "integration": "Interfaces",
    "investment": "Investments", "issuance": "Policy issuance", "journal": "Journals", "journaltools": "Journals",
    "legacy-inquiry": "Legacy inquiry", "lov": "Lists of values", "masters": "Master data", "messaging": "Notifications",
    "migration": "Data migration", "navigation": "Menus", "nbadmin": "User Access Maintenance",
    "nbreport": "New Business reports", "nbreports": "New Business reports", "nonpackage": "Non-package requests",
    "operations": "Operations", "opsintegration": "Operations", "opsledger": "Invoice ledger",
    "organization": "Organisation", "party": "Parties", "payables": "Payables", "payrequest": "Payment requests",
    "period": "Accounting periods", "placement": "Placement", "prodrecon": "Production reconciliation",
    "productmaint": "Product Maintenance", "profile": "User profile", "proposals": "Proposals", "quotation": "Quotations",
    "quotations": "Quotations", "receivables": "Receivables", "remittance": "Remittance", "renewal": "Renewal",
    "report": "Reports", "reports": "Reports", "screening": "Sanctions screening", "security": "Access control",
    "session": "Sign-in", "setup": "Set-up", "sharedstate": "Platform services", "storage": "Documents",
    "subledger": "Sub-ledgers", "submitted": "Submitted policies", "system": "System settings", "tax": "Tax",
    "workflow": "Workflow", "workspace": "My Work", "utils": "Common screen elements",
}
TECHNICAL = re.compile(r"\b(?:HTTP|API|api|JSON|SQL|database|PostgreSQL|Redis|server|endpoint|DTO|JDBC|JWT|Kafka|"
                       r"controller|service|repository|class|DOCX|XLSX|CSV|token|cache|adapters?|ports?|V\d{2,4}|wave|W\d|IT)\b")


def business_name(check: dict) -> str:
    """The name of an automated check in business words: its description without references and technical terms,
    or the business area it checks."""
    name = re.sub(r"\s*\([^)]*\)", "", str(check.get("name") or "")).strip(" .")
    area = AREAS.get(check.get("module") or "", "Platform")
    flagged = any(p.search(name) for _, p in check_pack.BUILD_STATUS + check_pack.TECHNICAL)
    if not check.get("named") or not name or flagged or TECHNICAL.search(name) or len(name) < 12:
        return f"automated check of {area}"
    return f"{name} ({area})"


def dedupe_shots() -> dict[str, str]:
    """Keeps one file of identical screenshots (the first by name): name of each screenshot -> file kept."""
    kept: dict[str, str] = {}
    out: dict[str, str] = {}
    for f in sorted((HERE / "shots").glob("*.png")):
        digest = hashlib.md5(f.read_bytes()).hexdigest()  # noqa: S324 - content identity, not security
        if digest in kept:
            out[f.name] = kept[digest]
            f.unlink()
        else:
            kept[digest] = f.name
            out[f.name] = f.name
    return out


# ------------------------------------------------------------------ walkthrough results


def walkthrough_rows(assess: dict, labels: dict[str, str]) -> tuple[list[dict], dict[str, str]]:
    plan = load("steps_plan.json", [])
    wt = {(r["brd"], r["walkthrough"], r["step"]): r for r in load("wt_results.json", [])}
    runs = {(r["brd"], r["step"]): r for r in load("steps_run.json", [])}
    curated = yaml.safe_load((HERE / "step_checks.yaml").read_text(encoding="utf-8"))
    notes = assess.get("step_notes") or {}
    shots = dedupe_shots()
    gap_notes = assess.get("step_gaps") or {}
    rows = []
    status_of: dict[str, str] = {}

    def label(path: str) -> str:
        return labels.get(path) or labels.get(re.sub(r"/:[^/]+", "", path)) or "record page"

    pending_refs = []
    for p in plan:
        brd, step = p["brd"], str(p["step"])
        key = f"{brd} {step}"
        m = re.match(r"(WT-[A-Z])\.(\d+)$", step)
        r = wt.get((brd, m.group(1), int(m.group(2)))) if m else None
        spec = (curated.get(brd) or {}).get(step) or {}
        evidence, observed, shot = "", "", ""
        if r and r["status"] != "not run":
            result = "Pass" if r["status"] == "pass" else "Fail"
            evidence = "Walkthrough performed on the SIT/UAT data by the persona"
            if r["status"] == "pass":
                observed = f"{clean(r.get('screen'))}: {clean(r.get('observed'))}".strip(": ")
            else:
                observed = clean(r.get("error")) or "The step could not be completed"
            shot = r.get("screenshot") or ""
        elif spec.get("blocked"):
            result, observed = "Blocked", spec["blocked"]
            evidence = "Scope of the release"
        elif (brd, step) in runs:
            run = runs[(brd, step)]
            result = {"pass": "Pass", "fail": "Fail", "blocked": "Blocked"}.get(run["status"], "Not run")
            evidence = "Screens opened by the persona's sign-in and checked for the step's actions"
            parts = []
            for c in run.get("checks") or []:
                name = label(c["screen"])
                if c["ok"]:
                    parts.append(f"{name} opens")
                else:
                    miss = ", ".join(c.get("missing") or [])
                    said = [clean(c.get("problem")), f"does not show {miss}" if miss else ""]
                    parts.append(f"{name}: " + "; ".join(x for x in said if x))
            observed = "; ".join(parts)
            if spec.get("gap"):
                observed = "; ".join(x for x in (observed, gap_notes.get(spec["gap"], "")) if x)
            if not observed:
                observed = clean(run.get("note"))
            shot = ", ".join(f"{brd}_{step}_{i}.png".replace(" ", "_")
                             for i, c in enumerate(run.get("checks") or [], start=1) if not c["ok"])
            if spec.get("refs"):
                pending_refs.append((len(rows), spec["refs"]))
        else:
            result, observed = "Not run", "No automated walkthrough of the step in this run"
        if key in notes:
            n = notes[key]
            result = n.get("result", result)
            observed = n.get("observed", observed)
            if n.get("evidence"):
                evidence = n["evidence"]
        shot = ", ".join(dict.fromkeys(shots.get(x.strip(), x.strip()) for x in shot.split(",") if x.strip()))
        status_of[key] = result
        rows.append({"brd": brd, "walkthrough": p["walkthrough"], "step": step, "persona": p["persona"],
                     "user": p["user"], "action": p["action"], "expected": p["expected"], "result": result,
                     "observed": observed, "evidence": evidence, "screenshot": shot})
    # A BRD-00 journey step also takes the result of the BRD walkthroughs it is made of.
    for i, refs in pending_refs:
        failed = []
        for ref in refs:
            brd, _, wt_part = ref.partition("/")
            m = re.match(r"(WT-[A-Z]|P\d|UF-[A-Z])(?:\.(\d+)-(\d+))?$", wt_part)
            if not m:
                continue
            for k, v in status_of.items():
                kb, ks = k.split(" ", 1)
                if kb != brd or not ks.startswith(m.group(1) + "."):
                    continue
                n = int(ks.split(".")[1]) if ks.split(".")[1].isdigit() else 0
                if m.group(2) and not int(m.group(2)) <= n <= int(m.group(3)):
                    continue
                if v == "Fail":
                    failed.append(f"{brd} {ks}")
        row = rows[i]
        row["evidence"] += "; and the BRD walkthrough steps the journey is made of"
        if failed and row["result"] == "Pass":
            row["result"] = "Fail"
        if failed:
            row["observed"] += f"; failed steps of the BRD walkthroughs: {', '.join(failed[:8])}"
        status_of[f"{row['brd']} {row['step']}"] = row["result"]
    return rows, status_of


# ------------------------------------------------------------------ requirements


def requirement_rows(assess: dict, wt_rows: list[dict], labels: dict[str, str]) -> tuple[list[dict], list[dict]]:
    ev = load("fr_evidence.json")
    checks = ev["checks"]
    decisions = assess.get("requirements") or {}
    extra = assess.get("extra_checks") or {}
    gaps = {fr: g for g in assess.get("gaps") or [] for fr in g.get("frs") or []}
    cycle_frs = defaultdict(list)
    cycle = load("cycle_run.json", {"steps": []})
    for s in cycle.get("steps") or []:
        for fr in (assess.get("cycle_frs") or {}).get(s["no"], []):
            cycle_frs[fr].append(s)
    # Walkthrough steps of the packs by the requirements of their screen (pack/screens/*.yaml).
    screen_frs: dict[str, list[str]] = {}
    for f in (ROOT / "docs/deliverables/src").glob("BRD-*/pack/screens/*.yaml"):
        for s in yaml.safe_load(f.read_text(encoding="utf-8")).get("screens") or []:
            screen_frs[s["id"]] = s.get("frs") or []
    wt_by_fr = defaultdict(list)
    for r in load("wt_results.json", []):
        for fr in screen_frs.get(r.get("screen_id") or "", []):
            wt_by_fr[fr].append(r)
    persona = load("persona_run.json", [])
    shown = set()
    for p in persona:
        shown |= set(p.get("observed") or [])
    by_label = {v.split(" > ", 1)[-1].lower(): k for k, v in labels.items()}
    reports = load("report_runs.json", {})
    # Screens opened by the personas in the screen checks of the walkthrough script, with the steps.
    step_screens = defaultdict(set)
    for run in load("steps_run.json", []):
        for c in run.get("checks") or []:
            if c["ok"]:
                step_screens[re.sub(r"/:[^/]+$", "", c["screen"]) if ":" in c["screen"] else c["screen"]].add(
                    f"{run['brd']} {run['step']}")
    persona_ok = bool(persona) and all(p["ok"] for p in persona)
    persona_frs = set(assess.get("persona_frs") or [])

    def fr_paths(screens: str) -> set[str]:
        out = set()
        for part in re.split(r";", screens or ""):
            name = re.sub(r"\(.*?\)", "", part).split(">")[-1].strip().lower()
            if name in by_label:
                out.add(by_label[name])
        return out

    rows, summary = [], []
    for fr in ev["requirements"]:
        fid = fr["id"]
        direct = [checks[c] for c in fr["direct"] + list(extra.get(fid, [])) if c in checks]
        via = [checks[c] for c in fr["via"] if c in checks]
        passed = [c for c in direct if c["result"] == "passed"]
        failed = [c for c in direct if c["result"] == "failed"]
        via_passed = [c for c in via if c["result"] == "passed"]
        steps = wt_by_fr.get(fid, [])
        steps_ok = [s for s in steps if s["status"] == "pass"]
        steps_bad = [s for s in steps if s["status"] == "fail"]
        cyc = cycle_frs.get(fid, [])
        cyc_ok = [s for s in cyc if s["status"] == "pass"]
        screens_on = fr_paths(fr["screens"]) & shown
        script_steps = sorted({st for p in fr_paths(fr["screens"]) for st in step_screens.get(p, set())})
        text = " ".join([fr["title"], fr["screens"], *fr["acceptance"]])
        runs = sorted({c for c in re.findall(r"\b[A-Z]{2,4}(?:-[A-Z0-9]+)+\b", text)
                       if c in reports and reports[c].get("status") == 200})
        if fid in persona_frs and persona_ok:
            cyc_ok = cyc_ok + [{"no": "persona access"}]
        evidence = []
        if passed:
            evidence.append("Automated checks: " + "; ".join(sorted({business_name(c) for c in passed})[:4]))
        if via_passed and not passed:
            evidence.append("Automated checks of the function behind it: " + "; ".join(sorted({business_name(c) for c in via_passed})[:3]))
        if steps_ok:
            evidence.append("Walkthrough steps passed: " + ", ".join(sorted({f"{s['brd']} {s['walkthrough']}.{s['step']}" for s in steps_ok})[:6]))
        if script_steps and not steps_ok:
            evidence.append("Walkthrough script steps passed on its screens: " + ", ".join(script_steps[:6]))
            steps_ok = [{"brd": x} for x in script_steps]
        if any(s["no"] != "persona access" for s in cyc_ok):
            evidence.append("Broking cycle steps passed: " + ", ".join(s["no"] for s in cyc_ok if s["no"] != "persona access"))
        if fid in persona_frs and persona_ok:
            evidence.append("Persona access: every persona signs in to the menu of its FRS; a screen outside it is refused")
        if runs:
            evidence.append("Reports run on the SIT/UAT data: " + ", ".join(
                f"{c} ({reports[c].get('data', 0)} rows)" for c in runs[:4]))
            cyc_ok = cyc_ok + [{"no": c} for c in runs]
        if screens_on and not (passed or steps_ok):
            evidence.append("Screen shown to its personas: " + ", ".join(sorted(labels.get(p, p) for p in screens_on)[:3]))
        reason = ""
        if fid in decisions:
            result, reason = decisions[fid]["result"], decisions[fid]["reason"]
        elif fid in gaps:
            g = gaps[fid]
            result = g.get("result", "Not conformant")
            reason = f"Open gap {g['id']}: {g['gap']}"
        elif failed:
            result = "Not conformant"
            reason = "Automated check failing: " + "; ".join(business_name(c) for c in failed)
        elif (passed and (steps_ok or cyc_ok or not steps_bad)) or (via_passed and (steps_ok or cyc_ok)):
            result = "Conformant"
            if steps_bad:
                reason = "A walkthrough step on its screen failed for a reason outside the requirement (see Walkthrough results)"
        elif passed or via_passed or steps_ok or cyc_ok or screens_on:
            result = "Partly conformant"
            reason = ("Shown working, but no automated check covers each acceptance criterion yet; the criteria are "
                      "confirmed in UAT with the test plan")
        else:
            result = "Not testable yet"
            reason = ("No automated check or walkthrough step of this run reaches it; it is tested in UAT with the "
                      "test case of the test plan")
        summary.append({"brd": fr["brd"], "id": fid, "result": result, "acs": len(fr["acceptance"]), "reason": reason})
        acs = fr["acceptance"] or ["(no acceptance criterion written)"]
        for n, ac in enumerate(acs, start=1):
            rows.append({"brd": fr["brd"], "fr": fid, "title": fr["title"], "priority": fr["priority"], "ac": n,
                         "criterion": ac, "evidence": "\n".join(evidence) or "None in this run", "result": result,
                         "reason": reason})
    return rows, summary


# ------------------------------------------------------------------ workbook


def short_reason(reason: str) -> str:
    """The reason of a requirement not testable yet, in a few words for the summary."""
    if reason.startswith("No automated check"):
        return "not reached by this run, tested in UAT"
    if reason.startswith("Open gap"):
        return reason.split(":")[0].replace("Open gap", "outside the release, gap")
    if "BDOI" in reason:
        return "waits for a BDOI decision"
    return reason.split(";")[0][:60]


def main() -> None:
    assess = yaml.safe_load((HERE / "assessment.yaml").read_text(encoding="utf-8"))
    version = str(assess.get("version", "1.0"))
    labels = load("screen_labels.json", {})
    wt_rows, _ = walkthrough_rows(assess, labels)
    fr_rows, fr_summary = requirement_rows(assess, wt_rows, labels)

    wb = BdoiWorkbook(TITLE, doc_type="Conformance register", brd="BRD-00", version=version,
                      date=assess.get("date"), subtitle="All BRDs: walkthroughs, broking cycle, requirements and access")
    wb.legend = [("Conformant", "Shown working by automated checks and the walkthrough or cycle"),
                 ("Partly conformant", "Shown working in part; the rest is confirmed in UAT"),
                 ("Not conformant", "Missing or not working; see Open gaps"),
                 ("Not testable yet", "Cannot be shown on the platform today; the reason is given")]
    wb.cover_notes = [assess.get("scope", "")]

    # Summary
    by_brd = defaultdict(Counter)
    acs = Counter()
    reasons = defaultdict(Counter)
    for s in fr_summary:
        by_brd[s["brd"]][s["result"]] += 1
        acs[s["brd"]] += s["acs"]
        if s["result"] == "Not testable yet":
            reasons[s["brd"]][short_reason(s["reason"])] += 1
    wt_by_brd = defaultdict(Counter)
    for r in wt_rows:
        wt_by_brd[r["brd"]][r["result"]] += 1
    summary_rows = []
    for brd in sorted(by_brd):
        c = by_brd[brd]
        w = wt_by_brd[brd]
        why = "; ".join(f"{k} ({v})" for k, v in reasons[brd].most_common(3))
        summary_rows.append({"brd": brd, "module": MODULES.get(brd, ""), "frs": sum(c.values()), "acs": acs[brd],
                             "ok": c["Conformant"], "partly": c["Partly conformant"], "nok": c["Not conformant"],
                             "nt": c["Not testable yet"], "why": why or "-",
                             "steps": sum(w.values()), "pass": w["Pass"], "fail": w["Fail"], "blocked": w["Blocked"]})
    tot = {k: sum(r[k] for r in summary_rows) for k in ("frs", "acs", "ok", "partly", "nok", "nt", "steps", "pass", "fail", "blocked")}
    summary_rows.append({"brd": "Total", "module": "", **tot, "why": ""})
    wb.sheet("Summary", [
        Column("brd", "BRD", 9, "Business requirements document"),
        Column("module", "Module", 28, "Business area of the BRD"),
        Column("frs", "FRs", 8, "Functional requirements of its FRS", kind="number"),
        Column("acs", "Acceptance criteria", 12, "Acceptance criteria of those requirements", kind="number"),
        Column("ok", "Conformant", 11, "Requirements shown working", kind="number"),
        Column("partly", "Partly", 9, "Requirements shown working in part", kind="number"),
        Column("nok", "Not conformant", 11, "Requirements missing or not working", kind="number"),
        Column("nt", "Not testable yet", 11, "Requirements that cannot be shown on the platform today", kind="number"),
        Column("why", "Why not testable yet", 50, "The main reasons, with the number of requirements"),
        Column("steps", "Walkthrough steps", 11, "Steps of the walkthrough script of the BRD", kind="number"),
        Column("pass", "Passed", 9, "Steps that passed", kind="number"),
        Column("fail", "Failed", 9, "Steps that failed", kind="number"),
        Column("blocked", "Blocked", 9, "Steps outside the release or depending on an external party", kind="number"),
    ], summary_rows, description="Per BRD: the requirements by conformance result and the walkthrough steps by result")

    wb.sheet("Walkthrough results", [
        Column("brd", "BRD", 9, "BRD of the walkthrough"),
        Column("walkthrough", "Walkthrough", 30, "Walkthrough of the UAT walkthrough script"),
        Column("step", "Step", 9, "Step number in the script"),
        Column("persona", "Persona", 22, "Persona doing the step"),
        Column("user", "Sign-in", 12, "SIT/UAT user that did the step"),
        Column("action", "Action", 46, "What the persona does"),
        Column("expected", "Expected result", 40, "What the persona should see"),
        Column("result", "Result", 10, "Result of the step", values=STEP_RESULTS, status=True),
        Column("observed", "Observed result", 50, "What the screen showed after the step"),
        Column("evidence", "How it was run", 30, "How the step was run"),
        Column("screenshot", "Screenshot", 22, "Screenshot of a failed step (screens folder of the register)"),
    ], wt_rows, description="The 367 steps of the walkthrough script, run on fresh SIT/UAT data")

    cycle = load("cycle_run.json", {"steps": []})
    cycle_rows = []
    for s in cycle.get("steps") or []:
        base = {"no": s["no"], "area": s["area"], "persona": s["persona"], "action": s["action"],
                "expected": s["expected"], "observed": clean(s.get("observed"))}
        if not s.get("amounts"):
            cycle_rows.append({**base, "amount": "", "exp": None, "obs": None,
                               "result": "Pass" if s["status"] == "pass" else "Fail"})
        for a in s.get("amounts") or []:
            cycle_rows.append({**base, "amount": a["name"], "exp": a["expected"], "obs": a["observed"],
                               "result": "Pass" if a["ok"] else "Fail"})
    for extra in assess.get("cycle_notes") or []:
        cycle_rows.append({"no": extra["step"], "area": extra["area"], "persona": extra.get("persona", ""),
                           "action": extra["action"], "expected": extra.get("expected", ""), "observed": extra["observed"],
                           "amount": "", "exp": None, "obs": None, "result": extra["result"]})
    wb.sheet("Broking cycle", [
        Column("no", "Step", 7, "Step of the cycle"),
        Column("area", "Area", 22, "Business area"),
        Column("persona", "Personas", 22, "Personas doing the step"),
        Column("action", "Action", 40, "What is done"),
        Column("expected", "Expected", 40, "Expected outcome, by the rules of the FRS"),
        Column("amount", "Amount checked", 30, "Amount checked against the FRS rule"),
        Column("exp", "Expected amount", 14, "Amount by the FRS rule (PHP)", kind="amount"),
        Column("obs", "Observed amount", 14, "Amount on the platform (PHP)", kind="amount"),
        Column("result", "Result", 10, "Result", values=STEP_RESULTS, status=True),
        Column("observed", "Observed", 50, "What the platform shows"),
    ], cycle_rows, description=f"One client from onboarding to month-end on fresh SIT/UAT data (account {cycle.get('arn') or '-'}, invoice {cycle.get('invoice') or '-'})")

    wb.sheet("FR conformance", [
        Column("brd", "BRD", 9, "BRD of the FRS"),
        Column("fr", "FR", 12, "Functional requirement"),
        Column("title", "Requirement", 40, "Title of the requirement"),
        Column("priority", "Priority", 11, "Priority in the FRS"),
        Column("ac", "AC", 5, "Number of the acceptance criterion", kind="number"),
        Column("criterion", "Acceptance criterion", 50, "Acceptance criterion as written in the FRS"),
        Column("evidence", "Evidence", 50, "Automated checks, walkthrough and cycle steps that show it"),
        Column("result", "Result", 16, "Conformance result", values=RESULTS, status=True),
        Column("reason", "Reason", 40, "Why the result is not Conformant"),
    ], fr_rows, description="Every acceptance criterion of every FRS with its evidence and result")

    persona_rows = []
    for p in load("persona_run.json", []):
        if p.get("error"):
            result, note = "Fail", f"Sign-in failed: {clean(p['error'])}"
        else:
            result = "Pass" if p["ok"] else "Fail"
            note = ""
            if p.get("denial"):
                d = p["denial"]
                note = f"{labels.get(d['screen'], d['screen'])} opened by its address: " + (
                    "no-access message shown" if d["denied"] else "the screen opened")
        persona_rows.append({
            "brd": p["brd"], "persona": p["persona"], "user": p["user"],
            "expected": len(p["expected"]), "shown": len(p.get("observed") or []),
            "missing": ", ".join(labels.get(x, x) for x in p.get("missing") or []) or "-",
            "extra": ", ".join(labels.get(x, x) for x in p.get("extra") or []) or "-",
            "denial": note, "result": result})
    wb.sheet("Persona access", [
        Column("brd", "BRD", 9, "BRD of the persona table"),
        Column("persona", "Persona", 28, "Persona of the FRS persona table"),
        Column("user", "Sign-in", 12, "SIT/UAT user of the persona (UAT users workbook)"),
        Column("expected", "Menu screens in the FRS", 12, "Screens of the persona's menu in the FRS (Menu by persona)", kind="number"),
        Column("shown", "Menu screens shown", 12, "Screens the sidebar showed after sign-in", kind="number"),
        Column("missing", "Missing from the menu", 40, "Screens of the FRS menu the sidebar did not show"),
        Column("extra", "Not in the FRS menu", 40, "Screens the sidebar showed that the FRS menu does not give"),
        Column("denial", "Screen outside the menu", 40, "A screen outside the persona's menu opened by its address"),
        Column("result", "Result", 10, "Result", values=STEP_RESULTS, status=True),
    ], persona_rows, description="Each persona signs in; its sidebar is compared with the FRS menu of the persona")

    wb.sheet("Fixes made", [
        Column("id", "No.", 7, "Fix number"),
        Column("brd", "BRD", 9, "BRD concerned"),
        Column("area", "Area", 22, "Business area"),
        Column("found", "What was found", 50, "What was missing or not working"),
        Column("fix", "Fix", 50, "What was changed"),
        Column("check", "Verified by", 40, "The check that shows it now works"),
    ], assess.get("fixes") or [], description="Problems and missing pieces fixed during the conformance run")

    wb.sheet("Open gaps", [
        Column("id", "No.", 7, "Gap number"),
        Column("brd", "BRD", 9, "BRD concerned"),
        Column("frs_text", "Requirements", 26, "Requirements affected"),
        Column("gap", "Gap", 50, "What is missing"),
        Column("impact", "Impact", 40, "Effect on the business and on UAT"),
        Column("proposal", "Proposal", 60, "Proposed way to close the gap"),
        Column("size", "Size", 10, "Indicative size", values=["Small", "Medium", "Large"]),
    ], [{**g, "frs_text": ", ".join(g.get("frs") or []) or g.get("frs_text", "-")} for g in assess.get("gaps") or []],
        description="Gaps too large to fix during the run, with a proposal each")

    wb.sheet("FRS corrections", [
        Column("id", "No.", 7, "Correction number"),
        Column("brd", "BRD", 9, "FRS concerned"),
        Column("where", "Where", 26, "Requirement, screen or walkthrough step"),
        Column("current", "FRS text today", 45, "What the FRS says"),
        Column("proposed", "Proposed text", 45, "What it should say to match the platform"),
        Column("reason", "Reason", 40, "Why"),
    ], assess.get("frs_corrections") or [], description="Texts of the FRS that differ from the platform; for the next issue of the FRS")

    for ws in wb.wb.worksheets:
        for row in ws.iter_rows(values_only=True):
            for v in row:
                for label, pat in check_pack.BUILD_STATUS + check_pack.TECHNICAL:
                    m = pat.search(str(v or ""))
                    if m:
                        print(f"wording to review ({label}: {m.group(0)}) in {ws.title}: {str(v)[:120]}")
    out = brand.out_path("BRD-00", "Quality", brand.output_name("Conformance", "BRD-00", TITLE, version, "xlsx"))
    print(wb.save(out))
    print({k: v for k, v in tot.items()})


if __name__ == "__main__":
    main()
