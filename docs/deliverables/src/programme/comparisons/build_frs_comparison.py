"""Comparison of BDOI's Product Maintenance FRS with the BIBS FRS BRD-3, and the plan to bring ours into BDOI's format.

    python docs/deliverables/src/programme/comparisons/build_frs_comparison.py            # build the workbook
    python docs/deliverables/src/programme/comparisons/build_frs_comparison.py --check    # checks only

Sources (this folder):
  * bdoi_frs_brd03.yaml – BDOI's FRS v1.0 of 19-Aug-2026 (BDO ITG), extracted word for word from
    docs/source-documents: the business requirements mapping, the FRPM items, the annex field tables and the labels of
    the quotation slip templates embedded in Annex D and F;
  * brd03_frs_comparison.yaml – summary and recommendation, structure, pros and cons, BRD coverage, requirement mapping,
    conflicts and the incorporation plan;
  * brd03_frs_comparison_lists.yaml – only in ours, only in BDOI's, field mapping and the quotation slip comparison.
Our side is counted from the BIBS FRS BRD-3 source (docs/deliverables/src/BRD-03_Product_Maintenance): functional
requirements, rules, validations and acceptance criteria, and the screens of its sign-off pack.

Output: docs/deliverables/out/Programme/Comparisons/BIBS_Comparison_BRD-03_BDOI_FRS_vs_BIBS_FRS_Product_Maintenance_v1.0.xlsx
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

import yaml

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column  # noqa: E402

BDOI = HERE / "bdoi_frs_brd03.yaml"
DATA = HERE / "brd03_frs_comparison.yaml"
LISTS = HERE / "brd03_frs_comparison_lists.yaml"
PM_SRC = brand.SRC_DIR / "BRD-03_Product_Maintenance"
OUT_FOLDER = brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "Comparisons"

MATCH = ["Same", "Partial", "Different", "Not in ours"]
PLATFORM = ["Yes", "Partly", "No"]
ACTIONS = ["Adopt BDOI wording", "Keep ours", "Needs BDOI decision", "Platform change"]
COVERAGE = ["Full", "Partial", "By reference", "Not covered"]
RECOMMEND = ["Include", "Include as annex", "Keep as supporting document"]
BDOI_ACTIONS = ["Add to our FRS", "Platform change needed", "Ask BDOI"]
SIDES = ["BDOI's", "Ours", "Both", "Neither", "Not stated", "-"]
KINDS = ["Between the documents", "Within BDOI's FRS"]
DECISIONS = ["Open", "Decided", "Deferred"]
# Colours of the values (drop-downs keep the values; the colours reuse the pack's status palette).
COLOURS = {"SAME": "FIT", "DIFFERENT": "CHANGE", "NOT IN OURS": "GAP", "YES": "FIT", "PARTLY": "PARTIAL", "NO": "GAP",
           "FULL": "FIT", "BY REFERENCE": "N/A", "NOT COVERED": "GAP", "INCLUDE": "FIT", "INCLUDE AS ANNEX": "CONFIGURE",
           "KEEP AS SUPPORTING DOCUMENT": "N/A", "ADD TO OUR FRS": "CONFIGURE", "PLATFORM CHANGE NEEDED": "CHANGE",
           "ASK BDOI": "OPEN", "ADOPT BDOI WORDING": "CONFIGURE", "KEEP OURS": "FIT", "NEEDS BDOI DECISION": "OPEN",
           "PLATFORM CHANGE": "CHANGE", "DECIDED": "DONE", "DEFERRED": "ON HOLD"}


def load(path: Path) -> dict:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def norm(text: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", text.lower().replace("’", "'"))


# ============================================================================ our side, counted from the BRD-3 sources


def our_counts() -> dict:
    frs = (PM_SRC / "FRS_BRD03_PRODUCT_MAINTENANCE.md").read_text(encoding="utf-8")
    blocks = [yaml.safe_load(b) for b in re.findall(r"```fr\n(.*?)```", frs, re.S)]
    screens, fields, actions = 0, 0, 0
    for f in sorted((PM_SRC / "pack" / "screens").glob("*.yaml")):
        for s in load(f)["screens"]:
            screens += 1
            fields += len(s.get("fields", []))
            actions += len(s.get("actions", []))
    notes = load(PM_SRC / "pack" / "notifications.yaml")["notifications"]
    walks = load(PM_SRC / "pack" / "walkthroughs.yaml")["walkthroughs"]
    return {
        "frs": len(blocks),
        "rules": sum(len(b.get("rules") or []) for b in blocks),
        "validations": sum(len(b.get("validations") or []) for b in blocks),
        "acceptance": sum(len(b.get("acceptance") or []) for b in blocks),
        "screens": screens, "fields": fields, "actions": actions,
        "notifications": len(notes), "walkthroughs": len(walks),
        "words": len(frs.split()),
        "clarifications": len(re.findall(r"^\| CLR-PM-\d+", frs, re.M)),
    }


# Counts of the sign-off workbook and test plan v2.1 that are produced from the platform at their build.
SIGNOFF_V21 = {"messages": 276, "screen_rules": 84, "test_conditions": 138, "test_cases": 270}


# ============================================================================ checks


def check(bdoi: dict, data: dict, lists: dict) -> list[str]:
    errors: list[str] = []
    items = [i["id"] for f in bdoi["frpm"] for i in f["items"]]
    mapped = [m["id"] for m in data["mapping"]]
    if sorted(items) != sorted(mapped):
        errors.append(f"mapping rows {sorted(set(items) ^ set(mapped))} differ from BDOI's FRPM sub-items")
    brm = [b["id"] for b in bdoi["brm"]]
    cov = [c["id"] for c in data["coverage"]]
    if brm != cov:
        errors.append(f"coverage rows differ from BDOI's mapping table: {sorted(set(brm) ^ set(cov))}")
    for m in data["mapping"]:
        for key, allowed in (("match", MATCH), ("platform", PLATFORM), ("action", ACTIONS)):
            if m[key] not in allowed:
                errors.append(f"{m['id']}: {key} '{m[key]}' not in {allowed}")
    for c in data["coverage"]:
        for key in ("bdoi_status", "ours_status"):
            if c[key] not in COVERAGE:
                errors.append(f"{c['id']}: {key} '{c[key]}'")
    for c in data["conflicts"]:
        if c["kind"] not in KINDS or c["brd"] not in SIDES or c["platform"] not in SIDES:
            errors.append(f"{c['id']}: kind, brd or platform value not allowed")
    for r in lists["only_ours"]:
        if r["recommendation"] not in RECOMMEND:
            errors.append(f"only_ours {r['ref']}: {r['recommendation']}")
    for r in lists["only_bdoi"]:
        if r["action"] not in BDOI_ACTIONS:
            errors.append(f"only_bdoi {r['ref']}: {r['action']}")
    conflict_ids = {c["id"] for c in data["conflicts"]}
    for m in data["mapping"]:
        for ref in re.findall(r"\bC\d\d\b", m.get("action_detail", "")):
            if ref not in conflict_ids:
                errors.append(f"{m['id']}: unknown conflict {ref}")
    annex_fields = {"A": bdoi["annex_a"], "E": bdoi["annex_e"], "BC": bdoi["annex_b"] + bdoi["annex_c"]}
    for key, rows in annex_fields.items():
        names = {norm(r["field"]) for r in rows}
        for name, row in lists["fields"][key]["map"].items():
            if norm(name) not in names:
                errors.append(f"field map {key}: '{name}' is not a field of BDOI's annex")
            if row["match"] not in MATCH:
                errors.append(f"field map {key}: '{name}' match '{row['match']}'")
    return errors


# ============================================================================ rows


def field_rows(bdoi: dict, lists: dict) -> list[dict]:
    rows: list[dict] = []
    spec = lists["fields"]

    def add(annex: str, key: str, src: list[dict]) -> None:
        lookup = {norm(k): v for k, v in spec[key]["map"].items()}
        for f in src:
            ours = lookup.get(norm(f["field"]), spec[key]["default"])
            rows.append({"annex": annex, "field": f["field"], "type": f["type"], "lines": f["lines"],
                         "remarks": f["remarks"] or "-", "screen": ours["screen"], "our_field": ours["field"],
                         "mandatory": ours["mandatory"], "our_type": ours["type"], "match": ours["match"],
                         "difference": ours["difference"]})

    add("Annex A – Package product", "A", bdoi["annex_a"])
    for s in bdoi["sub_line_values"]:
        rows.append({"annex": "Annex A – Sub-line values", "field": f"Sub-Line ({s['line']})", "type": "Values",
                     "lines": s["line"], "remarks": s["values"], "screen": "SCR-PM-11 Products",
                     "our_field": "Cover types of the line (master data)", "mandatory": "-", "our_type": "Master list",
                     "match": "Partial", "difference": "Values not yet loaded (PQ02); load BDOI's list as cover types "
                                                       "(configuration templates CI-01, CI-02)."})
    add("Annex B – Non-package product", "BC", bdoi["annex_b"])
    add("Annex C – Non-package quotation request", "BC", bdoi["annex_c"])
    add("Annex E – Package request", "E", bdoi["annex_e"])
    for kind in ("non_package", "package"):
        cmp_ = lists["qs_compare"][kind]
        covered = {norm(x) for x in cmp_["covered"]}
        for t in bdoi["qs_templates"][kind]:
            labels = list(dict.fromkeys(t["labels"]))
            missing = [x for x in labels if norm(x) not in covered and norm(x) != norm("Law and Jurisdiction")]
            diff = (f"Labels of BDOI's template the BIBS slip does not fill: {', '.join(missing)}. "
                    if missing else "") + cmp_["extra"]
            rows.append({"annex": cmp_["annex"], "field": f"Template {t['template']}", "type": "Word template",
                         "lines": t["template"], "remarks": "; ".join(labels), "screen": cmp_["screen"],
                         "our_field": cmp_["ours"], "mandatory": "-", "our_type": "PDF",
                         "match": "Partial", "difference": diff})
    for o in spec["ours_only"]:
        rows.append({"annex": o["annex"], "field": "-", "type": "-", "lines": "-", "remarks": "-", "screen": o["screen"],
                     "our_field": o["field"], "mandatory": o["mandatory"], "our_type": o["type"], "match": "Different",
                     "difference": "Only in ours. " + o["difference"]})
    for i, r in enumerate(rows, start=1):
        r["no"] = i
    return rows


def summary_rows(bdoi: dict, data: dict, lists: dict, ours: dict, fields: list[dict]) -> list[dict]:
    s = data["summary"]
    rows: list[dict] = []

    def add(section, item, b="", o="", note=""):
        rows.append({"section": section, "item": item, "bdoi": str(b), "ours": str(o), "note": note})

    for p in s["purpose"]:
        add("What each document is for", p["doc"], p["text"] if p["doc"].startswith("BDOI") else "",
            p["text"] if not p["doc"].startswith("BDOI") else "")
    m = bdoi["meta"]
    add("Documents compared", "Document", f"{m['title']}, v{m['version']}, {m['date']}, prepared by {m['prepared_by']}, "
        f"project {m['project']}", data["meta"]["ours"]["title"], "BRD baseline: " + data["meta"]["ours"]["baseline"])
    cov = data["coverage"]
    bc, oc = Counter(c["bdoi_status"] for c in cov), Counter(c["ours_status"] for c in cov)
    n_items = sum(len(f["items"]) for f in bdoi["frpm"])
    add("Counts", "BRD requirement IDs (PMADD01-08, BRPM.001-022, BRPM.024)", len(cov), len(cov),
        "BRPM.023 exists in neither BRD version.")
    add("Counts", "BRD IDs met by the document's own requirements", bc["Full"] + bc["Partial"], oc["Full"] + oc["Partial"],
        "BDOI refers PMADD05, BRPM.001 and 002 to the User Access Maintenance FRS and leaves BRPM.020 open.")
    for st in COVERAGE:
        add("Counts", f"BRD IDs – coverage {st}", bc[st], oc[st], "Sheet 4.")
    add("Counts", "Functional requirements", f"{len(bdoi['frpm'])} (FRPM.001-021)", f"{ours['frs']} (FR-PM-001-081)")
    add("Counts", "Numbered requirement items", f"{n_items} sub-items (FRPM.nnn.nn)",
        f"{ours['rules']} rules, {ours['validations']} validations, {ours['acceptance']} acceptance criteria")
    add("Counts", "Screens, fields, actions", "List columns stated in the text; no screen specifications",
        f"{ours['screens']} screens, {ours['fields']} fields, {ours['actions']} actions")
    annex_n = len(bdoi["annex_a"]) + len(bdoi["annex_b"]) + len(bdoi["annex_c"]) + len(bdoi["annex_e"])
    add("Counts", "Field lists per product line (annexes)", f"{annex_n} field rows in Annex A, B, C, E; "
        f"{len(bdoi['sub_line_values'])} sub-line value lists; "
        f"{len(bdoi['qs_templates']['package']) + len(bdoi['qs_templates']['non_package'])} quotation slip templates",
        "Configurable field rules per line or product; draft document layouts")
    add("Counts", "Messages and notifications", "Notifications named in the text; no message texts",
        f"{SIGNOFF_V21['messages']} messages; {ours['notifications']} notifications")
    add("Counts", "Process flows and walkthroughs", f"{len(m['process_flows'])} swimlane process flows",
        f"Lifecycle and workflow figures; {ours['walkthroughs']} walkthroughs")
    add("Counts", "Test traceability", "None", f"{SIGNOFF_V21['test_conditions']} test conditions, "
        f"{SIGNOFF_V21['test_cases']} test cases")
    add("Counts", "Length", f"About {round(m['approx_words'], -2):,} words", f"About {round(ours['words'], -3):,} words "
        "of source, plus the screen specifications and catalogues")
    mc = Counter(x["match"] for x in data["mapping"])
    add("Counts", f"BDOI items compared with ours ({len(data['mapping'])} FRPM sub-items)",
        ", ".join(f"{k} {mc[k]}" for k in MATCH), "", "Sheet 5. 'Not in ours' items are mostly non-package and covered "
        "by the New Business FRS.")
    pc = Counter(x["platform"] for x in data["mapping"])
    add("Counts", "BDOI items the platform supports today", ", ".join(f"{k} {pc[k]}" for k in PLATFORM), "", "Sheet 5.")
    add("Counts", "Only in BDOI's FRS (items)", len(lists["only_bdoi"]), "", "Sheet 7.")
    add("Counts", "Only in ours (items)", "", len(lists["only_ours"]), "Sheet 6.")
    kc = Counter(c["kind"] for c in data["conflicts"])
    add("Counts", "Conflicts between the documents needing a BDOI decision", kc["Between the documents"], "",
        f"Sheet 8; plus {kc[KINDS[1]]} points to correct within BDOI's FRS.")
    fc = Counter(f["match"] for f in fields)
    non_pkg = sum(1 for f in fields if f["match"] == "Not in ours" and f["annex"].startswith(("Annex B", "Annex C")))
    add("Counts", f"Field comparison ({len(fields)} rows)", ", ".join(f"{k} {fc[k]}" for k in MATCH), "",
        f"Sheet 9. {non_pkg} of the 'Not in ours' rows are non-package fields (Annex B, C), outside the BRD-3 FRS; "
        "the sheet names the New Business screen that holds each where BIBS has one.")
    pros, cons = s["headline_pros"], s["headline_cons"]
    for i in range(max(len(pros["BDOI's FRS"]), len(pros["Our FRS"]))):
        add("Headline pros", str(i + 1), _at(pros["BDOI's FRS"], i), _at(pros["Our FRS"], i))
    for i in range(max(len(cons["BDOI's FRS"]), len(cons["Our FRS"]))):
        add("Headline cons", str(i + 1), _at(cons["BDOI's FRS"], i), _at(cons["Our FRS"], i))
    r = s["recommendation"]
    effort = sum(p["effort"] for p in data["plan"])
    add("Recommendation", r["title"], "", "", r["text"].strip() + f" Indicative effort: about {effort:.0f} person-days "
        "of the project team (sheet 10), plus BDOI's review.")
    for i, w in enumerate(r["why"], start=1):
        add("Recommendation", f"Why ({i})", "", "", w)
    for a in s["alternatives"]:
        add("Alternatives", a["option"], "", "", a["tradeoff"])
    for n in s["notes"]:
        add("Notes", "-", "", "", n)
    return rows


def _at(items: list, i: int) -> str:
    return items[i] if i < len(items) else ""


# ============================================================================ workbook


def build(bdoi: dict, data: dict, lists: dict) -> Path:
    for value, base in COLOURS.items():
        brand.STATUS_COLOURS.setdefault(value, brand.STATUS_COLOURS[base])
    meta = data["meta"]
    ours = our_counts()
    fields = field_rows(bdoi, lists)
    wb = BdoiWorkbook(meta["title"], doc_type=meta["doc_type"], brd=meta["brd"], version=meta["version"],
                      date=meta["date"], subtitle=meta["subtitle"])
    wb.legend = [("Same", "The two documents say the same"), ("Partial", "Partly the same; see the difference"),
                 ("Different", "The two documents say different things"), ("Not in ours", "Only in BDOI's FRS"),
                 ("Yes", "BIBS supports it today"), ("Partly", "BIBS supports part of it"),
                 ("No", "BIBS does not support it today"), ("Needs BDOI decision", "A decision of BDOI is needed")]
    wb.cover_notes = ["Short quotes of both documents are word for word."]

    wb.sheet("1 Summary", [
        Column("section", "Section", 20, "Part of the summary"),
        Column("item", "Item", 34, "What is described or counted"),
        Column("bdoi", "BDOI's FRS", 52, "BDOI's FRS (BDO ITG, v1.0, 19-Aug-2026)"),
        Column("ours", "Our FRS", 52, "BIBS FRS BRD-3 v2.1 (8-Oct-2026) and its sign-off pack"),
        Column("note", "Comment / recommendation", 70, "Comment, recommendation or trade-off"),
    ], summary_rows(bdoi, data, lists, ours, fields),
        description="What each document is for, headline pros and cons, counts and the recommended way forward")

    wb.sheet("2 Structure", [
        Column("bdoi", "BDOI section", 34, "Section of BDOI's FRS"),
        Column("ours", "Our chapter(s)", 40, "Chapter(s) of our FRS"),
        Column("bdoi_content", "What BDOI's contains", 44, "Content of BDOI's section"),
        Column("ours_content", "What ours contains", 44, "Content of our chapter(s)"),
        Column("depth", "Depth", 22, "Relative depth"),
        Column("assessment", "Assessment", 48, "What to do with the section in the merged document"),
    ], data["structure"], description="Section by section: BDOI's FRS against our FRS")

    pc_rows = []
    for g in data["pros_cons"]:
        for doc in ("BDOI's FRS", "Our FRS"):
            pc_rows.append({"group": g["group"], "doc": doc, "pros": g[doc]["pros"], "cons": g[doc]["cons"]})
    wb.sheet("3 Pros and cons", [
        Column("group", "Group", 26, "Aspect compared"),
        Column("doc", "Document", 14, "BDOI's FRS or our FRS", values=["BDOI's FRS", "Our FRS"]),
        Column("pros", "Pros", 70, "Real strengths of the document for this aspect"),
        Column("cons", "Cons", 70, "Real weaknesses of the document for this aspect"),
    ], pc_rows, description="Pros and cons of each document, grouped by aspect")

    brm = {b["id"]: b for b in bdoi["brm"]}
    cov_rows = [{**c, "requirement": brm[c["id"]]["requirement"], "bdoi_frs": brm[c["id"]]["bdoi_frs"]}
                for c in data["coverage"]]
    wb.sheet("4 BRD coverage", [
        Column("id", "BRD ID", 11, "Requirement ID of the Product Maintenance BRD or its addendum"),
        Column("requirement", "Business requirement (as in BDOI's mapping)", 46, "BRD text as quoted by BDOI's FRS"),
        Column("bdoi_frs", "BDOI FR IDs", 20, "Functional requirement IDs in BDOI's mapping table"),
        Column("bdoi_status", "BDOI coverage", 13, "How far BDOI's FRS meets the requirement", values=COVERAGE,
               status=True),
        Column("bdoi_note", "BDOI note", 44, "What BDOI's FRS says or leaves out"),
        Column("ours_frs", "Our FRs", 20, "Functional requirements of our FRS"),
        Column("ours_status", "Our coverage", 13, "How far our FRS meets the requirement", values=COVERAGE, status=True),
        Column("note", "Note", 44, "Comment"),
    ], cov_rows, description="Every BRD requirement ID of Product Maintenance: BDOI's FR IDs, our FRs and the coverage "
                             "in each")

    titles = {i["id"]: (i["title"], f["capability"]) for f in bdoi["frpm"] for i in f["items"]}
    map_rows = [{**m, "title": titles[m["id"]][0], "capability": titles[m["id"]][1], "also": m.get("also", "-")}
                for m in data["mapping"]]
    wb.sheet("5 Requirement mapping", [
        Column("id", "BDOI ID", 13, "FRPM sub-item of BDOI's FRS"),
        Column("capability", "BDOI capability", 20, "Capability of the FRPM item"),
        Column("title", "BDOI title", 24, "Title of the sub-item"),
        Column("quote", "BDOI text (short quote)", 44, "Short quote of BDOI's text"),
        Column("ours", "Our FR IDs", 18, "Matching functional requirements of our FRS"),
        Column("also", "Also in BIBS (other FRS)", 18, "Where BIBS covers it in another FRS"),
        Column("match", "Match", 12, "Same, partial, different or not in ours", values=MATCH, status=True),
        Column("difference", "Difference in plain words", 60, "What differs"),
        Column("platform", "BIBS today", 10, "Does BIBS support it today", values=PLATFORM, status=True),
        Column("action", "Proposed action", 18, "What to do", values=ACTIONS, status=True),
        Column("action_detail", "Action detail", 44, "Detail of the action; C-numbers refer to sheet 8"),
    ], map_rows, description="Every BDOI FRPM item against our FRs, with the platform status and the proposed action")

    wb.sheet("6 Only in ours", [
        Column("ref", "Our reference", 20, "FR, catalogue or chapter of our FRS"),
        Column("kind", "Kind", 18, "Kind of content"),
        Column("content", "Content", 60, "What it states"),
        Column("brd", "BRD", 14, "BRD requirement it serves"),
        Column("where", "Where it goes in BDOI's template", 40, "Section, FRPM item or annex"),
        Column("recommendation", "Recommendation", 18, "Include, include as annex or keep as supporting document",
               values=RECOMMEND, status=True),
    ], lists["only_ours"], description="Content of our FRS and pack that BDOI's FRS does not have, and where it goes")

    wb.sheet("7 Only in BDOI FRS", [
        Column("ref", "BDOI reference", 22, "Item, annex or flow of BDOI's FRS"),
        Column("item", "Item", 60, "What BDOI's FRS has that ours lacks or states differently"),
        Column("impact", "Impact", 44, "Effect on our FRS or on BIBS"),
        Column("action", "Action", 18, "Add to our FRS, platform change needed or ask BDOI", values=BDOI_ACTIONS,
               status=True),
        Column("detail", "Detail", 34, "Detail; C-numbers refer to sheet 8"),
    ], lists["only_bdoi"], description="Items in BDOI's FRS that are missing or different in ours")

    wb.sheet("8 Conflicts", [
        Column("id", "ID", 6, "Conflict number"),
        Column("kind", "Kind", 16, "Between the documents or within BDOI's FRS", values=KINDS),
        Column("topic", "Topic", 26, "What the conflict is about"),
        Column("bdoi", "BDOI's FRS says", 46, "BDOI's text (short quotes)"),
        Column("ours", "Our FRS says", 46, "Our text (short quotes)"),
        Column("brd", "Matches the BRD", 11, "Which side matches the BRD", values=SIDES),
        Column("brd_note", "BRD text", 40, "What the BRD says"),
        Column("platform", "Matches BIBS", 10, "Which side matches BIBS today", values=SIDES),
        Column("decision", "Decision needed from BDOI", 40, "The question for BDOI"),
        Column("recommendation", "Our recommendation", 44, "What the project team recommends"),
        Column("status", "Decision status", 11, "Recorded at the decision session", values=DECISIONS, status=True),
        Column("bdoi_decision", "BDOI decision", 30, "Recorded at the decision session"),
    ], [{**c, "status": "Open", "bdoi_decision": ""} for c in data["conflicts"]],
        description="Where the two documents say different things, and points to correct within BDOI's FRS")

    wb.sheet("9 Field comparison", [
        Column("no", "No.", 6, "Row number", kind="number"),
        Column("annex", "BDOI annex", 22, "Annex of BDOI's FRS"),
        Column("field", "BDOI field", 26, "Field or template in BDOI's annex"),
        Column("type", "BDOI type", 12, "Data type in BDOI's annex"),
        Column("lines", "Product lines", 28, "Product lines where BDOI's field applies"),
        Column("remarks", "BDOI remarks / values", 36, "Remarks, values or template labels in BDOI's annex"),
        Column("screen", "Our screen", 30, "Screen or document of BIBS"),
        Column("our_field", "Our field", 30, "Field on our screen"),
        Column("mandatory", "Mandatory (ours)", 14, "Mandatory in ours; BDOI's annexes do not state it"),
        Column("our_type", "Type (ours)", 16, "Type or list in ours"),
        Column("match", "Match", 12, "Same, partial, different or not in ours", values=MATCH, status=True),
        Column("difference", "Difference", 50, "What differs"),
    ], fields, description="BDOI Annex A to F fields and templates against our screen specifications",
        freeze_first_column=False)

    wb.sheet("10 Incorporation plan", [
        Column("part", "Part", 18, "A ID scheme, B section by section, C order of work, D ITG review, E platform changes"),
        Column("bdoi_section", "BDOI section", 30, "Section of BDOI's FRS"),
        Column("insert", "Content to insert from ours", 64, "What goes in"),
        Column("how", "How (format in BDOI's FRS)", 28, "Form it takes in BDOI's document"),
        Column("source", "Source in ours", 24, "Where it comes from"),
        Column("effort", "Effort (person-days)", 11, "Indicative effort of the project team", kind="number"),
        Column("itg", "BDOI ITG review", 30, "What BDOI ITG reviews or decides"),
    ], data["plan"], description="How our content is placed into BDOI's FRS format: ID scheme, sections, order of work, "
                                 "review and platform changes")
    path = OUT_FOLDER / meta["filename"]
    return wb.save(path)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="checks only")
    args = ap.parse_args()
    bdoi, data, lists = load(BDOI), load(DATA), load(LISTS)
    errors = check(bdoi, data, lists)
    for e in errors:
        print("error:", e)
    if errors:
        return 1
    if args.check:
        print("check: 0 errors")
        return 0
    path = build(bdoi, data, lists)
    print(f"wrote {path.relative_to(REPO)} ({path.stat().st_size // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
