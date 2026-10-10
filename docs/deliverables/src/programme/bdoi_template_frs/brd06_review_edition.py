"""Business Unit review edition of the Renewal FRS in BDOI's template (version 1.1).

Used by build_brd06_bdoi_frs.py: the summary for the Business Unit Review and the references (Introduction), the
process flows and life-cycles of section Process Flow, the menu by persona of Annex AA, and Annexes AD to AK, AN, AP
and AQ; with the self-checks of the e-mail texts (found word for word in the platform) and of the open questions
(every earlier question classified once, none that BDOI's FRS answers kept open). Content: brd06_review.yaml and
brd06_flows.yaml.
"""

from __future__ import annotations

import copy
import re
from pathlib import Path

import openpyxl
import yaml
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

import brd11_figures
import brd_common_final as final

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[4]
RN = REPO / "docs" / "deliverables" / "src" / "BRD-06_Renewal"
OUT_PROG = REPO / "docs" / "deliverables" / "out" / "Programme"
BACKEND = REPO / "backend" / "src" / "main"
JAVA = BACKEND / "java" / "com" / "iortatechnxt" / "brokerverse"
REV = yaml.safe_load((HERE / "brd06_review.yaml").read_text(encoding="utf-8"))
FLOWS = yaml.safe_load((HERE / "brd06_flows.yaml").read_text(encoding="utf-8"))["figures"]
INPUTS_XLSX = OUT_PROG / "BDOI_Inputs" / "BIBS_Inputs_BRD-00_BDOI_Requirements_and_Inputs_by_BRD_v1.2.xlsx"
ORDER = ["summary", "flow", "AA", "AD", "AH"]


# ------------------------------------------------------------------------------------------- figures
def render_figures(work: Path) -> dict[str, Path]:
    work.mkdir(parents=True, exist_ok=True)
    return {f["id"]: brd11_figures.render(f, work) for f in FLOWS}


def figure_numbers() -> dict[str, int]:
    figs = sorted(FLOWS, key=lambda f: ORDER.index(f["where"]))
    return {f["id"]: i for i, f in enumerate(figs, 1)}


def figure_elements(b, figs: dict[str, Path], fid: str, max_h=8.4) -> list:
    spec = next(f for f in FLOWS if f["id"] == fid)
    n = figure_numbers()[fid]
    b.stats["figures_added"] += 1
    return [b.picture(figs[fid], max_w=7.0, max_h=max_h),
            b.para(f"Figure {n} - {spec['caption']}", italic=True, size=8.5, jc="center", space_after=40),
            b.para(f"How to read it: {spec['read']} Traces to: {spec['brd']}.", size=8.5, space_after=160)]


# ------------------------------------------------------------------------------------------- shared data
def inputs_tab() -> list[dict]:
    """The BRD-06 rows of the programme workbook BDOI Requirements and Inputs by BRD v1.2 (same IDs)."""
    wb = openpyxl.load_workbook(INPUTS_XLSX, read_only=True)
    out = []
    for r in wb["BRD-06 Renewal"].iter_rows(min_row=5, values_only=True):
        if r[0]:
            out.append({"id": r[0], "type": r[2], "need": r[3], "why": r[4], "proposal": r[5], "source": r[6],
                        "owner": r[7], "due": r[8].strftime("%d-%b-%Y") if hasattr(r[8], "strftime") else r[8],
                        "priority": r[9], "status": r[10]})
    wb.close()
    return out


def classify_items() -> dict[str, str]:
    """Every reference of our sources classified once: open / input / answered / assumption / dependency."""
    oi = REV["open_items"]
    where: dict[str, str] = {}

    def put(ref, kind):
        if ref in where and where[ref] != kind:
            raise SystemExit(f"{ref} is classified twice ({where[ref]}, {kind})")
        where[ref] = kind
    for o in oi["open"]:
        for ref in (x.strip() for x in o["refs"].split(";")):
            if re.match(r"[A-Z]", ref) and ref not in ("BDOFC", "CBG-HOME"):
                put(ref, "open")
    for ref in oi["inputs"]:
        put(ref, "input")
    for ref in oi["answered"]:
        put(ref, "answered")
    for a in oi["assumptions"]:
        put(a[0], "assumption")
    for d in oi["dependencies"]:
        put(d[0], "dependency")
    return where


def check_classification(where: dict[str, str]) -> int:
    """Every open question, clarification, assumption and dependency of the BIBS FRS and every row of the BRD-06
    inputs tab is classified."""
    frs = (RN / "FRS_BRD06_RENEWAL.md").read_text(encoding="utf-8")
    needed = set(re.findall(r"\bCLR-RN-\d\d\b", frs))
    needed |= set(re.findall(r"^\| (RQ\d\d|DMQ\d\d|SP SQ10|OOS-1|A-RN-\d\d|D-RN-\d\d) \|", frs, re.M))
    needed |= {r["id"] for r in inputs_tab()}
    missing = sorted(needed - set(where))
    if missing:
        raise SystemExit(f"open items not classified: {missing}")
    return len(needed)


def _corpus() -> dict[str, str]:
    files = {}
    for d in ("renewal", "messaging", "placement"):
        for p in (JAVA / d).rglob("*.java"):
            files[p.relative_to(JAVA).as_posix()] = p.read_text(encoding="utf-8")
    for p in (BACKEND / "resources" / "db" / "migration").glob("*.sql"):
        t = p.read_text(encoding="utf-8")
        if "doc_template" in t and ("RNW_" in t or "HOLD_COVER_REQUEST" in t):
            files["sql/" + p.name] = t
    return files


def check_emails() -> int:
    """Every quoted text is found in the platform, and every class of the renewal package that sends a notice, an
    alert or an e-mail is quoted in Annex AE."""
    items = REV["emails"]["items"]
    files = _corpus()
    allcode = "\n".join(files.values())
    for i in items:
        for frag in i["code"]:
            if frag not in allcode:
                raise SystemExit(f"{i['id']}: text {frag!r} not found in the platform")
    quoted = set()
    for rel, text in files.items():
        if any(frag in text for i in items for frag in i["code"]):
            quoted.add(rel)
    quoted |= {s for i in items for s in i.get("source", [])}
    for rel, text in files.items():
        if rel.startswith("renewal/") and re.search(r"new RenewalNotices\.Text\(|alerts\.raise\(|queueEmail\(|"
                                                    r"new AlertSignal\(", text) and rel not in quoted:
            raise SystemExit(f"notice source {rel} not quoted in Annex AE")
    notif = yaml.safe_load((RN / "pack" / "notifications.yaml").read_text(encoding="utf-8"))["notifications"]
    cat = REV["emails"]["catalogue"]
    ids = {i["id"] for i in items}
    for n in notif:
        if n["id"] not in cat or any(x not in ids for x in cat[n["id"]]):
            raise SystemExit(f"notification {n['id']} of the BIBS FRS not quoted in Annex AE")
    return len(items)


# ------------------------------------------------------------------------------------------- Introduction
def heading2(b):
    for p in b.doc.paragraphs:
        if p.text.strip().startswith("Document Preparation") and p.style.name == "Heading 2":
            return p._p
    raise SystemExit("Heading 2 not found")


def key_numbers(b) -> list[list[str]]:
    s = b.stats
    items = s["bdoi_items"] + s["new_subitems"] + s["new_item_subitems"]
    return [
        ["BRD requirement IDs mapped (42 BRRN and the lines of the main BRD), each to at least one FR item",
         str(s["brd_ids"]), "Business Requirements Mapping; Annex AM"],
        ["FR entries (BDOI's 44 and FRRN.045 to FRRN.049)", str(s["bdoi_entries"] + s["new_items"]),
         "Functional Requirements"],
        ["FR items and sub-items (BDOI's 143 and the added ones)", str(items), "Functional Requirements"],
        ["Acceptance criteria", str(s["ac"]), "Functional Requirements"],
        ["Figures added", str(len(FLOWS)), "This summary; Process Flow; Annexes AA, AD, AH"],
        ["Screens", str(s["screens"]), "Annex AA"],
        ["Messages shown to the user", str(s["messages"]), "Annex AB"],
        ["Notices, alerts and e-mails", str(s["emails"]), "Annex AE"],
        ["Documents and outputs", str(s["documents"]), "Annex AF"],
        ["Reports / scheduled runs", f"{s['reports']} / {s['schedules']}", "Annex AG"],
        ["Integrations", str(s["integrations"]), "Annex AH"],
        ["Open questions not answered in BDOI's FRS / answered and not repeated",
         f"{s['open_kept']} / {s['answered']}", "Annex AN"],
        ["Observations and points for decision", str(s["observations"]), "Annex AO"],
        ["Review checklist points", str(s["checklist"]), "Annex AQ"],
    ]


def add_summary(b, figs):
    from build_brd06_bdoi_frs import DOC, find_par, ppr_insert, strip_ids  # noqa: PLC0415
    s = REV["summary"]
    model_h2 = heading2(b)
    model_body = find_par(b, "This document once approved")._p

    def h2(text, page=False):
        h = strip_ids(copy.deepcopy(model_h2))
        for el in list(h):
            if el.tag != qn("w:pPr"):
                h.remove(el)
        ppr = h.find(qn("w:pPr"))
        if page and ppr is not None and ppr.find(qn("w:pageBreakBefore")) is None:
            ppr_insert(ppr, OxmlElement("w:pageBreakBefore"))
        h.append(b.run(text))
        return h

    def body(text):
        return b.clone_par(model_body, " ".join(str(text).split()))

    def sub(text):
        return b.para(text, bold=True, keep_next=True, size=10.5, color="014EA9", space_after=60)
    intro = DOC["introduction"]
    els = [h2(intro["references_title"]), body(intro["references_intro"]),
           b.table(["Document", "Owner", "Use in this FRS"], intro["references"], [3.6, 1.5, 2.3], size=8),
           b.para("")]
    els += [h2("Summary for the Business Unit Review", page=True), body(s["intro"])]
    els += [sub("Scope on a page"), b.table(s["scope_header"], s["scope"], [2.7, 2.2, 2.5], size=8), b.para("")]
    els += [sub("The process from end to end")] + figure_elements(b, figs, "brd06_fig_e2e", max_h=8.0)
    els += [sub("Key numbers"), body(s["numbers_intro"]),
            b.table(["Item", "Number", "Where"], b.key_numbers, [3.8, 1.0, 2.6], size=8.5), b.para("")]
    q = {r["no"].split(" (")[-1].rstrip(")"): r["no"] for r in b.q_rows if "(" in r["no"]}
    rows = []
    for no, text, refs, who in s["decisions"]:
        where = "; ".join(q.get(r.strip(), r.strip()) if re.fullmatch(r"C\d\d", r.strip()) else "Annex " + r.strip()
                          for r in refs.split(","))
        rows.append([str(no), text, where, who])
    els += [sub("Decisions the Business Unit must take"), body(s["decisions_intro"]),
            b.table(["No.", "Decision", "Annex AO / AM", "Decided by"], rows, [0.4, 3.7, 1.5, 1.8], size=8),
            b.para("")]
    els += [sub("How to review this document"), body(s["review_intro"]),
            b.table(s["review_header"], s["review"], [2.0, 2.4, 2.4, 0.6], size=8), b.para("")]
    els += [body(s["workbooks_intro"]),
            b.table(["Workbook", "What it holds"], s["workbooks"], [2.2, 5.2], size=8.5), b.para("")]
    els.append(body(f"Before signing, the Business Unit confirms the {len(REV['checklist']['rows'])} points of the "
                    f"review checklist in Annex AQ."))
    anchor = find_par(b, "Business Requirements Mapping", "Heading 1")._p
    for el in els:
        anchor.addprevious(el)


# ------------------------------------------------------------------------------------------- Process Flow
def add_process_flows(b, figs):
    from build_brd06_bdoi_frs import find_par  # noqa: PLC0415
    head = find_par(b, "Process Flow", "Heading 1")._p
    el = head.getnext()
    while el is not None and el.find(".//" + qn("w:drawing")) is None and el.find(".//" + qn("w:pict")) is None \
            and el.find(".//" + qn("w:object")) is None:
        el = el.getnext()
    if el is None:
        raise SystemExit("BDOI's process flow figure not found")
    els = [b.para(""), b.para("Flows added in version 1.1", bold=True, size=11, color="014EA9", keep_next=True),
           b.para("BDOI's flowchart above is kept. The figures below draw every process of the BRD from end to end, "
                  "one column per persona and a column for what the system does by itself, followed by the status "
                  "life-cycles of the renewal account, the letters, the hold cover and the uploads. Where the flow "
                  "differs from BDOI's flowchart (paid-off and RMU loans), the decision is in Annex AO.")]
    for f in FLOWS:
        if f["where"] == "flow":
            els += figure_elements(b, figs, f["id"], max_h=8.6 if f["kind"] == "swimlane" else 5.0)
    b.insert_after(el, els)


# ------------------------------------------------------------------------------------------- new annexes
class NewAnnexes:
    def __init__(self, a, figs):
        self.a = a
        self.b = a.b
        self.figs = figs

    def menus(self, a):
        k = self.b.stats["screens"] + 1
        a.h2(f"AA.{k} Menu by persona")
        a.p("The Renewal entries of the menu of each persona: the menu shows an entry only when one of the user's "
            "profiles holds its function (FRRN.001.02).")
        a.els += figure_elements(self.b, self.figs, "brd06_fig_menus", max_h=5.0)

    # AD
    def workflow(self):
        w = REV["workflow"]
        a = self.a
        a.h1("Annex AD – Workflow and Approvals")
        a.p(" ".join(w["intro"].split()))
        a.els += figure_elements(self.b, self.figs, "brd06_fig_approvals", max_h=5.5)
        a.table(w["header"], w["rows"], [1.0, 1.3, 1.3, 1.4, 1.5, 1.0], size=7.5)
        a.p(w["matrix_intro"], bold=True, keep_next=True)
        a.table(w["matrix_header"], w["matrix"], [2.2] + [0.74] * 7, size=7.5)
        a.p(" ".join(w["escalation"].split()))
        self.b.stats["workflow_rows"] = len(w["rows"])

    # AE
    def emails(self):
        e = REV["emails"]
        a = self.a
        a.h1("Annex AE – E-mail and Notification Texts")
        a.p(" ".join(e["intro"].split()))
        a.p(e["common"]["notice"])
        rows = [[i["id"], i["name"], i["channel"], i["recipients"]] for i in e["items"]]
        a.table(["No.", "Notice, alert or e-mail", "Channel", "Recipients"], rows, [0.6, 2.4, 1.6, 2.8], size=7.5)
        for i in e["items"]:
            a.h2(f"{i['id']} {i['name']}")
            body = str(i["body"]).replace("\\n", "\n")
            rows = [["Trigger", i["trigger"]], ["Recipients", i["recipients"]], ["Channel", i["channel"]],
                    ["Subject / title", i["subject"]], ["Text", body], ["Attachments", i["attachments"]],
                    ["Protection", e["common"]["protection_mail"] if i["channel"].startswith("E-mail")
                     and i["attachments"] != "None" else "No attachment; the notice opens the record only for a "
                                                         "signed-in user who holds the right."],
                    ["Frequency", i["frequency"]]]
            rows.append(["Wording", i.get("bdoi") or "Wording of the system today, proposed by iorta TechNXT for BDOI "
                                                     "approval (requirements collection workbook, sheet E-mail "
                                                     "wordings, item " + i["id"] + ")"])
            rows.append(["Traces to", f"{i['brd']}; {i['frrn']}"])
            a.kv(rows)
        a.p("Not sent as a message:", bold=True, keep_next=True)
        a.table(["Item", "How it is handled"], e["not_sent"], [2.0, 5.4], size=8)
        self.b.stats["emails"] = len(e["items"])

    # AF
    def documents(self):
        from build_brd06_bdoi_frs import clean  # noqa: PLC0415
        d = REV["documents"]
        a = self.a
        docs = yaml.safe_load((RN / "pack" / "documents.yaml").read_text(encoding="utf-8"))["documents"]
        a.h1("Annex AF – Document Prints and Output Formats")
        a.p(" ".join(d["intro"].split()))
        a.p(" ".join(d["file_name_rule"].split()))
        k = 0
        for doc in docs:
            k += 1
            extra = d["items"][doc["id"]]
            a.h2(f"AF.{k} {clean(doc['name'])} ({doc['id']})")
            fields = "\n".join(f"{n}. {clean(f)}" for n, f in enumerate(doc["fields"], 1))
            a.kv([["Purpose", extra["purpose"]], ["Recipients", extra["recipients"]],
                  ["Format", clean(doc["format"])], ["Produced from", clean(doc["produced"])],
                  ["Protection", clean(doc["protected"])], ["Fields in order", fields],
                  ["BDOI reference", extra["bdoi"]],
                  ["Layout", "Current layout of the system below; where BDOI's FRS gives the template, BDOI's "
                             f"template prevails; otherwise the Business Unit confirms it or gives its layout "
                             f"(requirements collection workbook, item {extra['rc']})"]], size=8)
            png = RN / "screenshots" / f"{doc['shot']}.png"
            if not png.exists():
                raise SystemExit(f"image missing: {png}")
            from PIL import Image  # noqa: PLC0415
            with Image.open(png) as im:
                portrait = im.height >= im.width
            # an A4 page at the width of every document print; a wide extract at the width of the screenshots
            if portrait:
                a.els.append(final.document_image(a.b, png))
            else:
                # a wide extract in two halves, left columns above the right ones, so that its text stays legible
                for part in _halves(png, a.work):
                    a.els.extend(a.screenshot_parts(part))
            a.p(f"Figure AF.{k}: current layout of the {clean(doc['name'])}", italic=True, size=8, jc="center")
        for doc in d["extra"]:
            k += 1
            a.h2(f"AF.{k} {doc['name']} ({doc['id']})")
            fields = "\n".join(f"{n}. {f}" for n, f in enumerate(doc["fields"], 1))
            given = "given" in doc["bdoi"]
            a.kv([["Purpose", doc["purpose"]], ["Recipients", doc["recipients"]], ["Format", doc["format"]],
                  ["Produced from", doc["produced"]], ["Protection", doc["protected"]],
                  ["Fields in order", fields], ["BDOI reference", doc["bdoi"]],
                  ["Layout", ("BDOI's template of this FRS; the Business Unit confirms it" if given else
                              "Layout to be provided by the Business Unit") +
                   f" (requirements collection workbook, item {doc['rc']})"]], size=8)
        self.b.stats["documents"] = k
        names = {clean(x["name"]) for x in docs}
        if len(names) != len(docs):
            raise SystemExit("documents of the BIBS set missing in Annex AF")

    # AG
    def reports(self):
        r = REV["reports"]
        a = self.a
        a.h1("Annex AG – Reports and Schedules")
        a.p(" ".join(r["intro"].split()))
        a.table(r["header"], r["rows"], [1.2, 0.9, 1.1, 1.4, 0.8, 0.6, 0.6, 0.9], size=7)
        a.p(r["schedules_intro"], bold=True, keep_next=True)
        a.table(r["schedules_header"], r["schedules"], [1.1, 0.9, 2.0, 1.3, 1.1, 1.0], size=7.5)
        self.b.stats["reports"] = len(r["rows"])
        self.b.stats["schedules"] = len(r["schedules"])
        ours = re.findall(r"^\| (RNW-[A-Z-]+) \| ([^|]+)\|", (RN / "FRS_BRD06_RENEWAL.md").read_text(encoding="utf-8"),
                          re.M)
        if len(ours) > len(r["rows"]):
            raise SystemExit("Annex AG lacks reports of the BIBS FRS")

    # AH
    def integrations(self):
        w = REV["integrations"]
        a = self.a
        a.h1("Annex AH – Integrations")
        n = figure_numbers()["brd06_fig_context"]
        a.p(" ".join(w["intro"].split()) + f" (Figure {n}).")
        a.els += figure_elements(self.b, self.figs, "brd06_fig_context", max_h=5.0)
        a.table(w["header"], w["rows"], [1.2, 0.5, 1.4, 0.8, 0.7, 0.8, 1.0, 1.0], size=7)
        self.b.stats["integrations"] = len(w["rows"])

    # AJ
    def nfr(self):
        x = REV["nfr"]
        a = self.a
        a.h1("Annex AJ – Non-functional Requirements")
        a.p(" ".join(x["intro"].split()))
        a.table(x["header"], x["rows"], [0.7, 0.8, 2.1, 1.9, 0.9, 1.0], size=7)
        reg = openpyxl.load_workbook(OUT_PROG / "Requirements" /
                                     "BIBS_NFR_BRD-00_Non_Functional_Requirements_Register_v1.0.xlsx", read_only=True)
        ids = {r[0] for r in reg["NFR register"].iter_rows(min_row=5, values_only=True) if r[0]}
        reg.close()
        for row in x["rows"]:
            for ref in re.findall(r"NFR-[A-Z]{3}-\d\d", row[5]):
                if ref not in ids:
                    raise SystemExit(f"{row[0]}: {ref} not in the NFR register")
        self.b.stats["nfr_rows"] = len(x["rows"])

    # AK
    def data_setup(self):
        y = REV["data_setup"]
        a = self.a
        a.h1("Annex AK – Data Set-up and Migration at Go-live")
        a.p(" ".join(y["intro"].split()))
        a.table(y["header"], y["rows"], [1.5, 1.1, 1.2, 1.0, 1.0, 0.8, 0.8], size=7.5)
        a.p(" ".join(y["not_loaded"].split()))
        self.b.stats["data_rows"] = len(y["rows"])

    # AN
    def open_items(self):
        o = REV["open_items"]
        a = self.a
        where = a.where
        a.h1("Annex AN – Assumptions, Dependencies and Open Questions")
        a.p(" ".join(o["intro"].split()))
        a.h2("AN.1 Assumptions")
        a.table(["No.", "Our reference", "Assumption", "Related"],
                [[f"AN.1.{i}", r[0], r[1], r[2]] for i, r in enumerate(o["assumptions"], 1)], [0.6, 0.9, 4.3, 1.6])
        a.h2("AN.2 Dependencies")
        a.table(["No.", "Our reference", "Dependency", "Needed for"],
                [[f"AN.2.{i}", r[0], r[1], r[2]] for i, r in enumerate(o["dependencies"], 1)], [0.6, 0.9, 4.3, 1.6])
        a.h2("AN.3 Open questions not answered in BDOI's FRS")
        rows = []
        for i, q in enumerate(o["open"], 1):
            ref = q["refs"] if q["refs"] not in ("BDOFC", "CBG-HOME") else "New in v1.1"
            rows.append([f"AN.3.{i}", ref, q["q"], q["why"], q["fmt"], q["owner"],
                         q["by"].strftime("%d-%b-%Y") if hasattr(q["by"], "strftime") else str(q["by"])])
        a.table(["No.", "Our reference", "Question", "Why (BDOI's FRS)", "Format of the answer", "Owner", "Needed by"],
                rows, [0.6, 0.9, 2.1, 1.6, 1.1, 0.8, 0.7], size=7)
        n_answered = sum(1 for k in where.values() if k == "answered")
        a.p(f"{n_answered} earlier questions, clarifications and assumptions are answered by BDOI's FRS and are not "
            f"repeated here (sheet Answered in the BDOI FRS of the Test Cases and Traceability workbook); "
            f"{sum(1 for k in where.values() if k == 'input')} items are values, templates, data or signatures the "
            f"Business Unit provides (requirements collection workbook).", italic=True, size=9)
        clash = [r for r, k in where.items() if k == "open" and r in o["answered"]]
        if clash:
            raise SystemExit(f"open questions answered in BDOI's FRS: {clash}")
        self.b.stats["open_kept"] = len(o["open"])
        self.b.stats["answered"] = n_answered
        self.b.stats["assumptions"] = len(o["assumptions"])
        self.b.stats["dependencies"] = len(o["dependencies"])

    # AP
    def change_control(self):
        c = REV["change_control"]
        a = self.a
        a.h1("Annex AP – Change Control after Sign-off")
        a.p(" ".join(c["intro"].split()))
        a.p("What signing freezes", bold=True, keep_next=True)
        a.bullets(c["freezes"])
        a.p(" ".join(c["not_frozen"].split()))
        a.p("How a change is raised and decided", bold=True, keep_next=True)
        a.table(c["steps_header"], c["steps"], [0.8, 4.2, 2.6, 2.4], size=8)
        a.p(" ".join(c["candidates"].split()))

    # AQ
    def checklist(self):
        c = REV["checklist"]
        a = self.a
        a.h1("Annex AQ – Business Unit Review Checklist")
        a.p(" ".join(c["intro"].split()))
        rows = [[str(i), r[0], r[1], r[2], r[3], "☐"] for i, r in enumerate(c["rows"], 1)]
        a.table(c["header"], rows, [0.5, 1.6, 2.4, 2.9, 1.8, 0.6], size=8)
        self.b.stats["checklist"] = len(rows)


def _halves(png: Path, work: Path) -> list[Path]:
    """A very wide extract (more than four times as wide as high) cut into its left and right halves."""
    from PIL import Image  # noqa: PLC0415
    with Image.open(png) as im:
        if im.width <= 4 * im.height:
            return [png]
        work.mkdir(parents=True, exist_ok=True)
        mid = im.width // 2
        out = []
        for k, box in enumerate(((0, 0, mid, im.height), (mid, 0, im.width, im.height))):
            target = work / f"{png.stem}-part{k + 1}.png"
            im.crop(box).save(target, optimize=True)
            out.append(target)
        return out
