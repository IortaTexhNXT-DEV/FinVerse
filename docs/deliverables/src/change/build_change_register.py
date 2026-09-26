"""Builds the Change Management Register workbook and its Word summary (BRD-00, programme level).

The register lists every deviation from the BRDs that goes through change control, with the effort in man-days to
close it. It is for documentation and change control; it does not change the platform build or the FRS baseline.

Usage
  python docs/deliverables/src/change/build_change_register.py --check      # checks only
  python docs/deliverables/src/change/build_change_register.py              # build both files
  python docs/deliverables/src/change/build_change_register.py --previews   # also page previews

Inputs (this folder)
  * change_register.yaml          the register rows, lists, process, CCB, CR form, effort basis, decisions;
  * CHANGE_MANAGEMENT_SUMMARY.md  Word source in the bdoi_docx format; lines <!-- cr:<name> --> are replaced by tables
                                  built from the YAML file and {{name}} tokens by figures (see placeholders()).

Outputs (set here, not from the drop map: docs/deliverables/out/Programme/Change_Management/)
  * BIBS_Change_Register_BRD-00_Change_Management_v<version>.xlsx
  * BIBS_Change_Register_BRD-00_Change_Management_Summary_v<version>.docx
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

import yaml
from openpyxl.formatting.rule import CellIsRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.pagebreak import Break

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[3]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_docx import BdoiDocument, lint_source, load_source, meta_from, render_body  # noqa: E402
from bdoi_xlsx import HEADER_ROW, BdoiWorkbook, Column, _SheetInfo  # noqa: E402

OUT = brand.OUT_DIR / brand.DROPS["Programme"]["folder"] / "Change_Management"
DOC = "CHANGE_MANAGEMENT_SUMMARY.md"
XLSX_NAME = "BIBS_Change_Register_BRD-00_Change_Management_v{v}.xlsx"
DOCX_NAME = "BIBS_Change_Register_BRD-00_Change_Management_Summary_v{v}.docx"
PLACEHOLDER = re.compile(r"^<!--\s*cr:(\w+)\s*(.*?)-->\s*$")
TOKEN = re.compile(r"\{\{(\w+)\}\}")

DROPS = ["Drop 0", "Drop 1", "Drop 2", "Programme", "Phase 2"]
BRDS = [f"BRD-{n:02d}" for n in range(14)]
PRIORITIES = ["High", "Medium", "Low"]
BRD_NAMES = {
    "BRD-00": "Core Replacement (umbrella) and programme", "BRD-01": "New Business", "BRD-02": "Operations",
    "BRD-03": "Product Maintenance", "BRD-04": "Collections", "BRD-05": "Accounting, Disbursement and ACSL",
    "BRD-06": "Renewal", "BRD-07": "Claims", "BRD-08": "Employee Benefits", "BRD-09": "Customer Servicing Facility",
    "BRD-10": "Sanction Screening", "BRD-11": "User Access Maintenance", "BRD-12": "Submitted Policies",
    "BRD-13": "Data Migration",
}
REQUIRED = ("id", "drop", "brd", "ref", "fr", "module", "type", "req", "built", "reason", "impact", "depts", "eff",
            "prio", "status", "xref", "target", "src")
# Filler words of the writing standard (docs/deliverables/README.md).
FILLER =("seamless", "robust", "comprehensive", "leverage", "cutting-edge", "state-of-the-art", "best-in-class",
          "world-class")

THIN = Side(style="thin", color=brand.BORDER)
BOX = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
HEAD_FILL = PatternFill("solid", fgColor=brand.HEADER_BLUE)
BAND_FILL = PatternFill("solid", fgColor=brand.BG_BLUE)
LABEL_FILL = PatternFill("solid", fgColor=brand.DIRTY_WHITE)


def font(size: float = 10, bold: bool = False, colour: str = brand.TEXT, italic: bool = False) -> Font:
    return Font(name=brand.FONT, size=size, bold=bold, color=colour, italic=italic)


def load() -> dict[str, Any]:
    return yaml.safe_load((HERE / "change_register.yaml").read_text(encoding="utf-8"))


# ------------------------------------------------------------------------------------------------ derived values

def total(r: dict[str, Any]) -> int:
    return int(sum(r["eff"]))


def size_class(d: dict[str, Any], days: int) -> str:
    if days == 0:
        return "0"
    for name, lo, hi in d["sizes"]:
        if lo <= days <= hi:
            return name
    return "XL"


def basis(d: dict[str, Any], days: int) -> str:
    return "0 - acceptance only" if days == 0 else f"{size_class(d, days)} ({days})"


def first_sentence(text: str, limit: int = 0) -> str:
    m = re.match(r"(.+?[.;])(\s|$)", text)
    s = (m.group(1) if m else text).rstrip(";").rstrip(".")
    if limit and len(s) > limit:
        s = s[:limit].rsplit(" ", 1)[0].rstrip(",;") + " ..."
        return s
    return s + "."


# ------------------------------------------------------------------------------------------------ checks

def check(d: dict[str, Any]) -> list[str]:
    problems: list[str] = []
    rows = d["rows"]
    ids = [r.get("id") for r in rows]
    for i, rid in enumerate(ids, start=1):
        if rid != f"CR-{i:04d}":
            problems.append(f"{rid}: out of sequence (expected CR-{i:04d})")
    known = set(ids)
    for r in rows:
        rid = r.get("id")
        for key in REQUIRED:
            if r.get(key) in (None, ""):
                problems.append(f"{rid}: missing {key}")
        if r.get("drop") not in DROPS:
            problems.append(f"{rid}: drop {r.get('drop')}")
        if r.get("brd") not in BRDS:
            problems.append(f"{rid}: brd {r.get('brd')}")
        if r.get("type") not in d["types"]:
            problems.append(f"{rid}: type {r.get('type')}")
        if r.get("status") not in d["statuses"]:
            problems.append(f"{rid}: status {r.get('status')}")
        if r.get("target") not in d["targets"]:
            problems.append(f"{rid}: target {r.get('target')}")
        if r.get("prio") not in PRIORITIES:
            problems.append(f"{rid}: priority {r.get('prio')}")
        if r.get("owner") and r["owner"] not in d["owners"]:
            problems.append(f"{rid}: owner {r['owner']}")
        eff = r.get("eff")
        if not (isinstance(eff, list) and len(eff) == 4 and all(isinstance(x, int) and x >= 0 for x in eff)):
            problems.append(f"{rid}: eff must be four whole numbers")
        elif r.get("status") == "IMPL" and sum(eff) != 0:
            problems.append(f"{rid}: implemented rows carry 0 man-days")
        elif r.get("status") != "IMPL" and r.get("type") not in ("RED", "DEC", "DIFF", "PH2", "NFR", "MIG") \
                and sum(eff) == 0:
            problems.append(f"{rid}: open {r.get('type')} row with 0 man-days")
        if "alt" in r and not isinstance(r["alt"], int):
            problems.append(f"{rid}: alt must be a whole number")
        if "p." not in str(r.get("ref", "")) and r.get("brd") != "BRD-00" and "Drop plan" not in str(r.get("ref")):
            problems.append(f"{rid}: BRD reference without a page")
        text = " ".join(str(v) for v in r.values())
        for ref in re.findall(r"CR-\d{4}", text):
            if ref not in known:
                problems.append(f"{rid}: unknown cross-reference {ref}")
        problems += [f"{rid}: {p}" for p in wording(text)]
    for dec in d["decisions"]:
        for ref in re.findall(r"CR-\d{4}", dec[1]):
            if ref not in known:
                problems.append(f"decision {dec[0]}: unknown {ref}")
    return problems


def wording(text: str) -> list[str]:
    out = []
    low = text.lower()
    for w in FILLER:
        if re.search(rf"(?<![a-z0-9_]){re.escape(w)}(?![a-z0-9_])", low):
            out.append(f"word not allowed: {w}")
    return out


# ------------------------------------------------------------------------------------------------ workbook

class CrWorkbook(BdoiWorkbook):
    """BdoiWorkbook with free-form sheets, a reading guide in place of the README and a document control sheet."""

    def __init__(self, d: dict[str, Any], *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.d = d
        self._readme.title = "How to read"
        self._control = self.wb.create_sheet("Document control")
        self._sheets.append(_SheetInfo("Document control", "Version history, review and approval, distribution; the "
                                                           "statement that the register does not change the build "
                                                           "or the FRS", [], None))

    def custom(self, name: str, description: str, landscape: bool = True):
        ws = self.wb.create_sheet(name)
        ws.sheet_view.showGridLines = False
        ws["A1"] = name
        ws["A1"].font = font(14, True, brand.HEADER_BLUE)
        ws["A2"] = description
        ws["A2"].font = font(9, False, brand.MUTED)
        ws.row_dimensions[1].height = 24
        self._print_setup(ws, None)
        ws.page_setup.orientation = "landscape" if landscape else "portrait"
        ws.page_setup.paperSize = ws.PAPERSIZE_A4
        self._sheets.append(_SheetInfo(name, description, [], None))
        return ws

    # ---- reading guide (replaces the generated README; keeps its column table)
    def _build_readme(self) -> None:
        d = self.d
        ws = self._readme
        ws.sheet_view.showGridLines = False
        for col, w in zip("ABCD", (26, 30, 80, 44)):
            ws.column_dimensions[col].width = w
        ws["A1"] = "How to read this register"
        ws["A1"].font = font(14, True, brand.HEADER_BLUE)
        ws["A2"] = d["meta"]["statement"]
        ws["A2"].font = font(10, True, brand.NEAR_BLACK)
        r = 4
        guide = [
            "One row per deviation from a BRD requirement, merged across sources (FRS recorded differences, specs, "
            "designs, the discrepancy register v1.2, the alignment pack, the cross-BRD decisions and the BDOI answers "
            "of 26 September 2026). The Source column says where each deviation is recorded.",
            "Filter the Register sheet by Drop, BRD, Deviation type, Priority or CR status. The Summary sheet recounts "
            "from the Register, so it stays correct when rows are added or a status changes.",
            "Effort is in man-days for the work still needed to close the deviation as proposed (Effort basis sheet). "
            "It is 0 when the behaviour is built and only needs acceptance. 'BRD as written' gives the indicative "
            "effort to build the BRD text instead, where the CCB may want to compare.",
            "A CR moves Draft -> For CCB review -> Approved or Rejected -> Implemented. Rows marked 'Implemented (as "
            "built, for acceptance)' describe built behaviour that differs from the BRD: the CCB records the "
            "acceptance; no build remains.",
            "The register does not change the FRS. An approved CR reaches the FRS only through the next FRS revision, "
            "which cites the CR ID.",
        ]
        r = self._heading(ws, r, "Reading guide")
        for g in guide:
            ws.cell(row=r, column=1, value="•").font = font(10, True, brand.CTA_BLUE)
            ws.cell(row=r, column=1).alignment = Alignment(horizontal="right", vertical="top")
            c = ws.cell(row=r, column=2, value=g)
            c.font = font(10)
            c.alignment = Alignment(wrap_text=True, vertical="top")
            ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=4)
            ws.row_dimensions[r].height = 30
            r += 1
        r += 1
        r = self._heading(ws, r, "Deviation types")
        r = table(ws, r, ["Type", "Meaning"], [[d["types"][k], d["type_meaning"][k]] for k in d["types"]],
                  [1, 2], merge_last_to=4)
        r += 1
        r = self._heading(ws, r, "CR status")
        r = table(ws, r, ["Status", "Meaning"], [[d["statuses"][k], d["status_meaning"][k]] for k in d["statuses"]],
                  [1, 2], merge_last_to=4)
        r += 1
        r = self._heading(ws, r, "Priority")
        r = table(ws, r, ["Priority", "Meaning"], [[k, v] for k, v in d["priorities"].items()], [1, 2],
                  merge_last_to=4)
        r += 1
        r = self._heading(ws, r, "Size classes (effort basis)")
        r = table(ws, r, ["Class", "Man-days"], [["0", "Built; acceptance only"]] +
                  [[n, f"{lo} to {hi}" if hi < 9999 else f"more than {lo - 1}"] for n, lo, hi in d["sizes"]],
                  [1, 2], merge_last_to=4)
        r += 1
        r = self._heading(ws, r, "Columns of the data sheets")
        rows = []
        for s in self._sheets:
            for col in s.columns:
                rows.append([s.name, col.header, col.description, ", ".join(col.values) if col.values else ""])
        r = table(ws, r, ["Sheet", "Column", "Meaning", "Allowed values"], rows, [1, 2, 3, 4])
        self._print_setup(ws, None)
        ws.page_setup.orientation = "landscape"
        ws.page_setup.paperSize = ws.PAPERSIZE_A4

    @staticmethod
    def _heading(ws, r: int, text: str) -> int:
        ws.cell(row=r, column=1, value=text).font = font(12, True, brand.HEADER_BLUE)
        return r + 1

    # ---- document control
    def build_control(self) -> None:
        d, m = self.d, self.d["meta"]
        ws = self._control
        ws.sheet_view.showGridLines = False
        for col, w in zip("ABCDEF", (22, 16, 26, 26, 26, 50)):
            ws.column_dimensions[col].width = w
        ws["A1"] = "Document control"
        ws["A1"].font = font(14, True, brand.HEADER_BLUE)
        ws["A3"] = m["statement"]
        ws["A3"].font = font(11, True, brand.HEADER_BLUE)
        ws["A3"].fill = BAND_FILL
        ws.merge_cells("A3:F3")
        ws.row_dimensions[3].height = 22
        ws["A3"].alignment = Alignment(vertical="center", indent=1)
        r = 5
        facts = [("Document", f"{m['title']} ({m['doc_id']})"), ("Version", m["version"]), ("Date", m["date"]),
                 ("Status", "Issued for BDOI review"), ("Status as of", m["status_as_of"]),
                 ("Baseline", m["baseline"]),
                 ("Word summary", DOCX_NAME.format(v=m["version"])),
                 ("Owner of the register", "Project Manager (iorta TechNXT) on behalf of the Change Control Board")]
        for k, v in facts:
            ws.cell(row=r, column=1, value=k).font = font(10, True, brand.MUTED)
            c = ws.cell(row=r, column=2, value=v)
            c.font = font(10)
            c.alignment = Alignment(wrap_text=True, vertical="top")
            ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=6)
            for col in range(1, 7):
                ws.cell(row=r, column=col).border = Border(bottom=Side(style="thin", color=brand.BORDER_LIGHT))
            r += 1
        r += 1
        ws.cell(row=r, column=1, value="Version history").font = font(12, True, brand.HEADER_BLUE)
        r = table(ws, r + 1, ["Version", "Date", "Author", "Reviewer", "Approver", "Change"],
                  [[m["version"], "26 Sep 2026", "iorta TechNXT Business Analyst lead",
                    "iorta TechNXT Solution Architect", "BIBS Product Owner (pending)",
                    f"First issue: {len(d['rows'])} deviations from BRD-00 to BRD-13 with effort in man-days"]],
                  [1, 2, 3, 4, 5, 6])
        r += 1
        ws.cell(row=r, column=1, value="Review and approval").font = font(12, True, brand.HEADER_BLUE)
        r = table(ws, r + 1, ["Role", "Organisation", "Name", "Signature", "Date", "Purpose"],
                  [["BIBS Product Owner", "BDOI", "", "", "", "Chair of the CCB; approves the register"],
                   ["Program Manager, Business Project Services", "BDO Unibank ESG", "", "", "",
                    "Plan, drops and effort"],
                   ["Head, Comptrollership", "BDOI", "", "", "", "Posting, GL, tax and BIR items"],
                   ["Chief Compliance Officer", "BDOI", "", "", "", "Screening, access and retention items"],
                   ["Project Manager", "iorta TechNXT", "", "", "", "Register owner; CCB secretary"]],
                  [1, 2, 3, 4, 5, 6], height=26)
        r += 1
        ws.cell(row=r, column=1, value="Distribution").font = font(12, True, brand.HEADER_BLUE)
        r = table(ws, r + 1, ["Recipient", "Organisation", "Purpose"],
                  [["Owners of BRD-1 to BRD-13 (department heads and product owners)", "BDOI",
                    "Review the CRs of their BRD; decision owners"],
                   ["BDOI IT (integration owners)", "BDOI", "Interface and infrastructure CRs"],
                   ["Project team", "iorta TechNXT", "Impact analysis, scheduling, FRS revisions"]],
                  [1, 2, 3], merge_last_to=6)
        self._print_setup(ws, None)
        ws.page_setup.orientation = "landscape"
        ws.page_setup.paperSize = ws.PAPERSIZE_A4

    def _build_cover(self) -> None:
        super()._build_cover()
        ws = self._cover
        for row in ws.iter_rows(min_col=2, max_col=3):
            b, c = row
            if b.value == "README":
                b.value = "How to read"
                b.hyperlink = "#'How to read'!A1"
                c.value = "Reading guide, deviation types, statuses, priorities, size classes and column definitions"
        # Store the heights of wrapped rows, so that the logo anchored below them keeps its place in every viewer.
        for row in range(1, ws.max_row + 1):
            text = str(ws[f"C{row}"].value or "")
            if len(text) > 90 and ws[f"B{row}"].value:
                ws.row_dimensions[row].height = 14 * (1 + len(text) // 90)

    def save(self, path: str | Path) -> Path:
        self.build_control()
        self._build_cover()
        self._build_readme()
        order = [self._cover, self._control, self._readme] + \
            [self.wb[s.name] for s in self._sheets if s.name != "Document control"] + [self._lists]
        self.wb._sheets = order
        self.wb.active = 0
        props = self.wb.properties
        props.title = self.title
        props.creator = brand.VENDOR
        props.subject = brand.SYSTEM
        props.keywords = "Change Register, BRD-00, BIBS, BDOI"
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self.wb.save(path)
        return path


def table(ws, r: int, headers: list[str], rows: list[list[Any]], cols: list[int], merge_last_to: int | None = None,
          height: float | None = None, number_cols: tuple[int, ...] = ()) -> int:
    """Writes a small BDOI table at row r in the given columns; returns the next free row."""
    last = merge_last_to or cols[-1]
    for h, c in zip(headers, cols):
        cell = ws.cell(row=r, column=c, value=h)
        cell.font = font(10, True, brand.WHITE)
        cell.fill = HEAD_FILL
        cell.border = BOX
        cell.alignment = Alignment(wrap_text=True, vertical="center")
    if merge_last_to and merge_last_to > cols[-1]:
        for c in range(cols[-1] + 1, merge_last_to + 1):
            ws.cell(row=r, column=c).fill = HEAD_FILL
            ws.cell(row=r, column=c).border = BOX
        ws.merge_cells(start_row=r, start_column=cols[-1], end_row=r, end_column=last)
    r += 1
    for i, row in enumerate(rows):
        for j, (v, c) in enumerate(zip(row, cols)):
            cell = ws.cell(row=r, column=c, value=v)
            cell.font = font(10, j == 0)
            cell.alignment = Alignment(wrap_text=True, vertical="top",
                                       horizontal="right" if j in number_cols else None)
            cell.border = BOX
            if i % 2 == 1:
                cell.fill = BAND_FILL
        if merge_last_to and merge_last_to > cols[-1]:
            for c in range(cols[-1] + 1, merge_last_to + 1):
                ws.cell(row=r, column=c).border = BOX
                if i % 2 == 1:
                    ws.cell(row=r, column=c).fill = BAND_FILL
            ws.merge_cells(start_row=r, start_column=cols[-1], end_row=r, end_column=last)
        if height:
            ws.row_dimensions[r].height = height
        r += 1
    return r


REG_COLUMNS = [
    ("id", "CR ID", 9, "Change request identifier, CR-nnnn"),
    ("drop", "Drop", 9, "BDOI drop of the item (Drop 0 set-up and migration, Drop 1 transactional, Drop 2 "
                        "independent), Programme (cross-drop) or Phase 2"),
    ("brd", "BRD", 8, "Primary BRD; other BRDs involved are named in the BRD requirement column"),
    ("ref", "BRD requirement ID(s) and page", 24, "BRD requirement IDs with the PDF page of the source BRD"),
    ("fr", "FRS FR ID(s)", 16, "Functional requirement IDs of the FRS, or the FRS section that records the item"),
    ("module", "Module / screen", 19, "BIBS module, screen, job or port concerned"),
    ("type", "Deviation type", 18, "Kind of deviation (How to read)"),
    ("req", "BRD requirement", 30, "What the BRD asks, in short"),
    ("built", "As built / proposed behaviour", 42, "What BIBS does today, or the proposed behaviour"),
    ("reason", "Reason", 22, "BDOI decision, open question or technical constraint behind the deviation"),
    ("impact", "Business impact", 22, "Effect on the business process, money, control or users"),
    ("depts", "Affected departments", 18, "BDOI departments affected"),
    ("a", "Analysis (md)", 7, "Man-days of analysis still needed"),
    ("b", "Build (md)", 7, "Man-days of build (code, configuration, unit and integration tests)"),
    ("t", "Test (md)", 7, "Man-days of SIT test preparation, execution and regression"),
    ("d", "Doc. (md)", 7, "Man-days of documentation (FRS, design, test plan, guides)"),
    ("total", "Total (md)", 7, "Sum of the four effort columns (formula)"),
    ("basis", "Size basis", 12, "Size class and chosen number: S 1-3, M 4-10, L 11-25, XL over 25; 0 = acceptance "
                                "only (formula)"),
    ("alt", "BRD as written (md)", 9, "Indicative man-days to build the BRD text instead of the proposed behaviour "
                                      "(blank where not applicable)"),
    ("prio", "Priority", 9, "High, Medium or Low (How to read)"),
    ("status", "CR status", 16, "Status in the change-control process"),
    ("owner", "Decision owner (BDOI)", 22, "BDOI role that decides the CR"),
    ("xref", "Register / question refs", 18, "Discrepancy register items (DCR-), question IDs and decisions (D1-D10)"),
    ("target", "Target drop / release", 17, "Build wave or release in which the CR is closed"),
    ("src", "Source of the record", 24, "Where the deviation is recorded (FRS section, spec, design, register, "
                                        "alignment pack)"),
]
FIRST_PAGE_LAST_COL = 11  # columns A-K on the first printed page, L-Y (with A repeated) on the second


def reg_rows(d: dict[str, Any]) -> list[dict[str, Any]]:
    out = []
    for r in d["rows"]:
        a, b, t, doc = r["eff"]
        out.append({
            "id": r["id"], "drop": r["drop"], "brd": r["brd"], "ref": r["ref"], "fr": r["fr"], "module": r["module"],
            "type": d["types"][r["type"]], "req": r["req"], "built": r["built"], "reason": r["reason"],
            "impact": r["impact"], "depts": r["depts"], "a": a, "b": b, "t": t, "d": doc, "total": None,
            "basis": None, "alt": r.get("alt"), "prio": r["prio"], "status": d["statuses"][r["status"]],
            "owner": d["owners"][r.get("owner") or r["brd"]], "xref": r["xref"], "target": d["targets"][r["target"]],
            "src": r["src"],
        })
    return out


def col_letter(key: str) -> str:
    return get_column_letter([c[0] for c in REG_COLUMNS].index(key) + 1)


def build_workbook(d: dict[str, Any]) -> Path:
    m = d["meta"]
    wb = CrWorkbook(d, m["title"], doc_type=m["doc_type"], brd=m["brd"], version=m["version"], date=m["date"],
                    subtitle=m["statement"])
    wb.legend = [(d["statuses"][k], d["status_meaning"][k]) for k in ("DRAFT", "CCB", "APPR", "REJ")] + \
        [("Implemented", f"Shown as \"{d['statuses']['IMPL']}\". {d['status_meaning']['IMPL']}")]
    wb.cover_notes = [f"Status as of {m['status_as_of']}. Baseline: {m['baseline']}."]
    values = {"drop": DROPS, "brd": BRDS, "type": list(d["types"].values()), "prio": PRIORITIES,
              "status": list(d["statuses"].values()), "target": list(d["targets"].values())}
    cols = [Column(k, h, w, desc, values=values.get(k),
                   kind="number" if k in ("a", "b", "t", "d", "total", "alt") else "text")
            for k, h, w, desc in REG_COLUMNS]
    rows = reg_rows(d)
    ws = wb.sheet("Register", cols, rows,
                  description=f"{len(rows)} deviations from the BRDs for change control. {m['statement']}")
    n = len(rows)
    first, last = HEADER_ROW + 1, HEADER_ROW + n
    A, D, TOT, BAS = col_letter("a"), col_letter("d"), col_letter("total"), col_letter("basis")
    for r in range(first, last + 1):
        ws[f"{TOT}{r}"] = f"=SUM({A}{r}:{D}{r})"
        ws[f"{BAS}{r}"] = (f'=IF({TOT}{r}=0,"0 - acceptance only",IF({TOT}{r}<=3,"S",IF({TOT}{r}<=10,"M",'
                           f'IF({TOT}{r}<=25,"L","XL")))&" ("&{TOT}{r}&")")')
    # Smaller type for the register, numbers centred, status and priority colours.
    num_keys = {"a", "b", "t", "d", "total", "alt"}
    for r in range(HEADER_ROW, last + 1):
        for c, (k, *_rest) in enumerate(REG_COLUMNS, start=1):
            cell = ws.cell(row=r, column=c)
            if r > HEADER_ROW:
                cell.font = font(9, k in ("id", "total"), brand.TEXT)
                if k in num_keys or k in ("drop", "brd", "prio"):
                    cell.alignment = Alignment(horizontal="center", vertical="top", wrap_text=True)
            else:
                cell.font = font(9, True, brand.WHITE)
    ws.row_dimensions[HEADER_ROW].height = 40
    rng = lambda key: f"{col_letter(key)}{first}:{col_letter(key)}{last + 500}"  # noqa: E731
    status_fills = {
        d["statuses"]["IMPL"]: (brand.SUCCESS_BG, brand.SUCCESS),
        d["statuses"]["CCB"]: (brand.AMBER_BG, brand.AMBER),
        d["statuses"]["DRAFT"]: (brand.BG_BLUE, brand.HEADER_BLUE),
        d["statuses"]["APPR"]: (brand.SUCCESS_BG, brand.SUCCESS),
        d["statuses"]["REJ"]: (brand.DIRTY_WHITE, brand.MUTED),
    }
    for v, (fill, fnt) in status_fills.items():
        ws.conditional_formatting.add(rng("status"), CellIsRule(
            operator="equal", formula=[f'"{v}"'], stopIfTrue=True,
            fill=PatternFill("solid", fgColor=fill, bgColor=fill), font=Font(color=fnt, bold=True)))
    for v, (fill, fnt) in {"High": (brand.DANGER_BG, brand.DANGER), "Medium": (brand.AMBER_BG, brand.AMBER)}.items():
        ws.conditional_formatting.add(rng("prio"), CellIsRule(
            operator="equal", formula=[f'"{v}"'], stopIfTrue=True,
            fill=PatternFill("solid", fgColor=fill, bgColor=fill), font=Font(color=fnt, bold=True)))
    # Status and priority fills take precedence over the row banding added by BdoiWorkbook.sheet().
    mine = {rng("status"), rng("prio")}
    ranked = sorted(((str(cf.sqref) in mine, rule) for cf in ws.conditional_formatting for rule in cf.rules),
                    key=lambda x: not x[0])
    for i, (_, rule) in enumerate(ranked, start=1):
        rule.priority = i
    # A3 landscape, two pages across (description | effort and governance), CR ID repeated on both.
    ws.page_setup.paperSize = ws.PAPERSIZE_A3
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToWidth = 2
    ws.page_setup.fitToHeight = 0
    ws.page_setup.pageOrder = "overThenDown"
    ws.print_title_cols = "A:A"
    ws.col_breaks.append(Break(id=FIRST_PAGE_LAST_COL))
    ws.freeze_panes = "C5"

    build_summary(wb, d, n)
    build_effort(wb, d, n)
    build_process(wb, d)
    build_form(wb, d)
    OUT.mkdir(parents=True, exist_ok=True)
    return wb.save(OUT / XLSX_NAME.format(v=m["version"]))


def reg_range(key: str, n: int) -> str:
    c = col_letter(key)
    return f"Register!${c}${HEADER_ROW + 1}:${c}${HEADER_ROW + n + 500}"


def build_summary(wb: CrWorkbook, d: dict[str, Any], n: int) -> None:
    ws = wb.custom("Summary", "Counts and man-days by drop, BRD, deviation type, status and priority. Formulas read "
                              "the Register sheet, so the figures follow any change to it.")
    st = d["statuses"]
    R = lambda k: reg_range(k, n)  # noqa: E731
    r = 4

    def block(title: str, key: str, items: list[tuple[str, str]], extra: str = "") -> int:
        nonlocal r
        ws.cell(row=r, column=1, value=title).font = font(12, True, brand.HEADER_BLUE)
        r += 1
        heads = ["Value", "Description", "CRs", "High", "Draft", "For CCB review", "Implemented", "Analysis (md)",
                 "Build (md)", "Test (md)", "Doc. (md)", "Total (md)"]
        for c, h in enumerate(heads, start=1):
            cell = ws.cell(row=r, column=c, value=h)
            cell.font = font(9, True, brand.WHITE)
            cell.fill = HEAD_FILL
            cell.border = BOX
            cell.alignment = Alignment(wrap_text=True, vertical="center", horizontal="center" if c > 2 else None)
        ws.row_dimensions[r].height = 28
        r += 1
        top = r
        for i, (val, desc) in enumerate(items):
            crit = f'{R(key)},"{val}"'
            vals = [val, desc, f"=COUNTIFS({crit})", f'=COUNTIFS({crit},{R("prio")},"High")',
                    f'=COUNTIFS({crit},{R("status")},"{st["DRAFT"]}")',
                    f'=COUNTIFS({crit},{R("status")},"{st["CCB"]}")',
                    f'=COUNTIFS({crit},{R("status")},"{st["IMPL"]}")']
            vals += [f"=SUMIFS({R(k)},{crit})" for k in ("a", "b", "t", "d", "total")]
            for c, v in enumerate(vals, start=1):
                cell = ws.cell(row=r, column=c, value=v)
                cell.font = font(9, c in (1, 12))
                cell.border = BOX
                cell.alignment = Alignment(wrap_text=True, vertical="top", horizontal="center" if c > 2 else None)
                if i % 2 == 1:
                    cell.fill = BAND_FILL
            r += 1
        vals = ["Total", extra] + [f"=SUM({get_column_letter(c)}{top}:{get_column_letter(c)}{r - 1})"
                                   for c in range(3, 13)]
        for c, v in enumerate(vals, start=1):
            cell = ws.cell(row=r, column=c, value=v)
            cell.font = font(9, True, brand.HEADER_BLUE)
            cell.fill = LABEL_FILL
            cell.border = BOX
            cell.alignment = Alignment(horizontal="center" if c > 2 else None, vertical="top")
        r += 2
        return r

    for i, w in enumerate([30, 40, 8, 8, 8, 10, 12, 10, 10, 10, 10, 11], start=1):
        ws.column_dimensions[get_column_letter(i)].width = w
    block("By drop", "drop", [(x, brand.DROPS[x]["title"] if x in brand.DROPS else "Reinsurance module (BDOI answer A3)")
                              for x in DROPS])
    block("By BRD", "brd", [(b, BRD_NAMES[b]) for b in BRDS])
    block("By deviation type", "type", [(v, d["type_meaning"][k].split(".")[0]) for k, v in d["types"].items()])
    block("By CR status", "status", [(v, d["status_meaning"][k].split(".")[0]) for k, v in d["statuses"].items()])
    block("By priority", "prio", [(k, v.split(".")[0]) for k, v in d["priorities"].items()])
    ws.freeze_panes = "A4"
    ws.page_setup.fitToWidth = 1
    ws.print_title_rows = "1:2"


def build_effort(wb: CrWorkbook, d: dict[str, Any], n: int) -> None:
    ws = wb.custom("Effort basis", "How the man-days are estimated: team roles, the man-day, what is included and "
                                   "excluded, and the size classes.", landscape=False)
    for col, w in zip("ABCD", (4, 30, 70, 16)):
        ws.column_dimensions[col].width = w
    r = 4
    ws.cell(row=r, column=1, value="Assumptions").font = font(12, True, brand.HEADER_BLUE)
    r += 1
    for i, a in enumerate(d["assumptions"], start=1):
        ws.cell(row=r, column=1, value=i).font = font(10, True, brand.CTA_BLUE)
        ws.cell(row=r, column=1).alignment = Alignment(vertical="top", horizontal="center")
        c = ws.cell(row=r, column=2, value=a)
        c.font = font(10)
        c.alignment = Alignment(wrap_text=True, vertical="top")
        ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=4)
        ws.row_dimensions[r].height = 28 if len(a) > 110 else 15
        r += 1
    r += 1
    ws.cell(row=r, column=1, value="Team roles").font = font(12, True, brand.HEADER_BLUE)
    r = table(ws, r + 1, ["Role", "Work counted in the estimate"], d["roles"], [2, 3], merge_last_to=4)
    r += 1
    ws.cell(row=r, column=1, value="Size classes").font = font(12, True, brand.HEADER_BLUE)
    r = table(ws, r + 1, ["Class", "Man-days", "CRs"],
              [["0", "Built; acceptance only", f'=COUNTIFS({reg_range("total", n)},0)']] +
              [[nm, f"{lo} to {hi}" if hi < 9999 else f"more than {lo - 1}",
                f'=COUNTIFS({reg_range("total", n)},">={lo}",{reg_range("total", n)},"<={hi}")']
               for nm, lo, hi in d["sizes"]], [2, 3, 4], number_cols=(2,))
    r += 1
    ws.cell(row=r, column=1, value="Man-days by activity (all CRs)").font = font(12, True, brand.HEADER_BLUE)
    r = table(ws, r + 1, ["Activity", "Meaning", "Man-days"],
              [["Analysis", "Impact analysis, workshops, rules and layouts", f"=SUM({reg_range('a', n)})"],
               ["Build", "Code, configuration, unit and integration tests", f"=SUM({reg_range('b', n)})"],
               ["Test", "SIT cases written and run, regression", f"=SUM({reg_range('t', n)})"],
               ["Documentation", "FRS, design, test plan, guides", f"=SUM({reg_range('d', n)})"],
               ["Total", "", f"=SUM({reg_range('total', n)})"]], [2, 3, 4], number_cols=(2,))
    ws.page_setup.fitToWidth = 1


def build_process(wb: CrWorkbook, d: dict[str, Any]) -> None:
    ws = wb.custom("Change control", "Change-control process for deviations and new change requests, with the "
                                     "roles and the proposed Change Control Board (CCB).")
    for col, w in zip("ABCDE", (18, 62, 34, 30, 18)):
        ws.column_dimensions[col].width = w
    r = 4
    ws.cell(row=r, column=1, value="Process: request, impact analysis, CCB, approval, schedule, implement, verify, "
                                   "close").font = font(12, True, brand.HEADER_BLUE)
    r = table(ws, r + 1, ["Step", "What happens", "Who", "Output", "Time"], d["process"], [1, 2, 3, 4, 5])
    r += 1
    ws.cell(row=r, column=1, value="Change Control Board (proposed membership)").font = font(12, True,
                                                                                           brand.HEADER_BLUE)
    r = table(ws, r + 1, ["Member", "Organisation", "Role on the board"], [[a, b, c] for a, b, c in d["ccb"]],
              [2, 3, 4], merge_last_to=5)
    r += 1
    rules = [
        "Quorum: the chair (or a named delegate), the Program Manager and the decision owner of each CR on the agenda.",
        "The CCB meets every two weeks from October 2026 to go-live, and ad hoc for High priority items that block a "
        "drop.",
        "A CR above 25 man-days, or one that moves a drop or the go-live date, needs the Program Manager's approval.",
        "Approved CRs reach the FRS only through an FRS revision that cites the CR ID; the FRS baseline is not "
        "changed by this register.",
        "Rejected CRs keep their row with the reason; the BRD text stands.",
    ]
    ws.cell(row=r, column=1, value="Rules of the board").font = font(12, True, brand.HEADER_BLUE)
    r += 1
    for rule in rules:
        ws.cell(row=r, column=1, value="•").alignment = Alignment(horizontal="right", vertical="top")
        c = ws.cell(row=r, column=2, value=rule)
        c.font = font(10)
        c.alignment = Alignment(wrap_text=True, vertical="top")
        ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=5)
        r += 1
    ws.page_setup.fitToWidth = 1


def build_form(wb: CrWorkbook, d: dict[str, Any]) -> None:
    ws = wb.custom("CR form", "Change request form: one per CR; the Project Manager files the signed form with the "
                              "register.", landscape=False)
    for col, w in zip("ABC", (22, 36, 56)):
        ws.column_dimensions[col].width = w
    r = 4
    section = None
    for sec, field, guide in d["form"]:
        if sec != section:
            section = sec
            r += 1 if r > 4 else 0
            cell = ws.cell(row=r, column=1, value=sec)
            cell.font = font(11, True, brand.WHITE)
            cell.fill = HEAD_FILL
            ws.merge_cells(start_row=r, start_column=1, end_row=r, end_column=3)
            r += 1
        lab = ws.cell(row=r, column=1, value=field)
        lab.font = font(9, True, brand.NEAR_BLACK)
        lab.fill = LABEL_FILL
        lab.alignment = Alignment(wrap_text=True, vertical="top")
        lab.border = BOX
        val = ws.cell(row=r, column=2, value=None)
        val.border = BOX
        ws.cell(row=r, column=3).border = BOX
        ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=3)
        hint = ws.cell(row=r, column=2, value=guide)
        hint.font = font(8, False, brand.PLACEHOLDER, italic=True)
        hint.alignment = Alignment(wrap_text=True, vertical="top")
        big = field in ("What the BRD asks", "Change requested or as-built behaviour", "Business impact",
                        "Conditions", "Reason")
        ws.row_dimensions[r].height = 48 if big else 24
        r += 1
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 1


# ------------------------------------------------------------------------------------------------ Word summary

def figures(d: dict[str, Any]) -> dict[str, Any]:
    rows = d["rows"]
    tot = sum(total(r) for r in rows)
    open_rows = [r for r in rows if r["status"] != "IMPL"]
    by_drop = Counter()
    for r in rows:
        by_drop[r["drop"]] += total(r)
    return {
        "last_id": rows[-1]["id"],
        "total_crs": len(rows),
        "total_md": tot,
        "open_crs": len(open_rows),
        "impl_crs": len(rows) - len(open_rows),
        "high_crs": sum(1 for r in rows if r["prio"] == "High"),
        "high_open": sum(1 for r in open_rows if r["prio"] == "High"),
        "zero_crs": sum(1 for r in rows if total(r) == 0),
        "ccb_crs": sum(1 for r in rows if r["status"] == "CCB"),
        "draft_crs": sum(1 for r in rows if r["status"] == "DRAFT"),
        "analysis_md": sum(r["eff"][0] for r in rows),
        "build_md": sum(r["eff"][1] for r in rows),
        "test_md": sum(r["eff"][2] for r in rows),
        "doc_md": sum(r["eff"][3] for r in rows),
        "xl_crs": sum(1 for r in rows if total(r) > 25),
        "l_crs": sum(1 for r in rows if 11 <= total(r) <= 25),
        "top_drop": max(by_drop, key=by_drop.get),
        "top_drop_md": max(by_drop.values()),
        "int_md": sum(total(r) for r in rows if r["type"] == "INT"),
        "park_md": sum(total(r) for r in rows if r["type"] == "PARK"),
        "int_park_pct": round(100 * sum(total(r) for r in rows if r["type"] in ("INT", "PARK")) / max(tot, 1)),
        "version": d["meta"]["version"],
        "statement": d["meta"]["statement"],
        "status_as_of": d["meta"]["status_as_of"],
    }


def md_table(headers: list[str], rows: list[list[Any]], widths: list[float], caption: str, size: float = 8.5,
             bold_first: bool = True, status: str | None = None) -> list[str]:
    opts = f'widths={",".join(str(w) for w in widths)} caption="{caption}" size={size}'
    if bold_first:
        opts += " bold=first"
    if status:
        opts += f" status={status}"
    out = [f"<!-- table: {opts} -->", "| " + " | ".join(headers) + " |", "|" + "---|" * len(headers)]
    for r in rows:
        out.append("| " + " | ".join(str(c).replace("|", "/").replace("\n", " ") for c in r) + " |")
    out.append("")
    return out


def group_rows(d: dict[str, Any], key: str, order: list[str], label=None, desc=None) -> list[list[Any]]:
    rows = d["rows"]
    out = []
    for v in order:
        sel = [r for r in rows if (label(r) if label else r[key]) == v]
        if not sel:
            continue
        out.append([v] + ([desc(v)] if desc else []) + [
            len(sel), sum(1 for r in sel if r["prio"] == "High"), sum(1 for r in sel if r["status"] != "IMPL"),
            sum(r["eff"][0] for r in sel), sum(r["eff"][1] for r in sel), sum(r["eff"][2] for r in sel),
            sum(r["eff"][3] for r in sel), sum(total(r) for r in sel)])
    tot = [sum(x[i] for x in out) for i in range(len(out[0]) - 8, len(out[0]))]
    out.append(["**Total**"] + ([""] if desc else []) + [f"**{x}**" for x in tot])
    return out


GROUP_HEADERS = ["CRs", "High", "Open", "Analysis", "Build", "Test", "Doc.", "Total md"]


def top20(d: dict[str, Any]) -> list[dict[str, Any]]:
    weight = {"High": 3, "Medium": 2, "Low": 1}
    rows = [r for r in d["rows"] if r["status"] != "IMPL"]
    rows.sort(key=lambda r: (-(total(r) * weight[r["prio"]]), -total(r), r["id"]))
    return rows[:20]


def placeholders(d: dict[str, Any]) -> dict[str, list[str]]:
    types = d["types"]
    st = d["statuses"]
    return {
        "by_drop": md_table(["Drop", "Scope"] + GROUP_HEADERS,
                            group_rows(d, "drop", DROPS, desc=lambda v: brand.DROPS[v]["title"].split(" - ")[-1]
                                       if v in brand.DROPS else "Reinsurance module"),
                            [1.6, 3.3, 0.8, 0.8, 0.9, 1.4, 1, 0.9, 0.9, 1.3], "Deviations and man-days by drop"),
        "by_type": md_table(["Deviation type"] + GROUP_HEADERS,
                            group_rows(d, "type", list(types.values()), label=lambda r: types[r["type"]]),
                            [4.6, 0.9, 0.9, 0.9, 1.1, 1, 0.9, 0.9, 1.2], "Deviations and man-days by type"),
        "by_status": md_table(["CR status"] + GROUP_HEADERS,
                              group_rows(d, "status", list(st.values()), label=lambda r: st[r["status"]]),
                              [4.6, 0.9, 0.9, 0.9, 1.1, 1, 0.9, 0.9, 1.2], "Deviations and man-days by CR status"),
        "by_brd": md_table(["BRD", "Name"] + GROUP_HEADERS,
                           group_rows(d, "brd", BRDS, desc=lambda v: BRD_NAMES[v]),
                           [1.3, 3.5, 0.8, 0.8, 0.9, 1.4, 1, 0.9, 0.9, 1.3], "Deviations and man-days by BRD"),
        "top20": md_table(["CR", "BRD", "BRD asks", "As built / proposed", "Man-days", "Priority", "Status"],
                          [[r["id"], r["brd"], first_sentence(r["req"], 90), first_sentence(r["built"], 120),
                            f"{total(r)} ({size_class(d, total(r))})", r["prio"], st[r["status"]]]
                           for r in top20(d)],
                          [1.4, 1.3, 5.0, 6.3, 1.4, 1.3, 1.9],
                          "Open CRs ranked by man-days x priority weight (High 3, Medium 2, Low 1); full text in the Register", size=7),
        "decisions": md_table(["Topic", "CRs", "Decision needed", "Decision owner", "By"],
                              [list(x) for x in d["decisions"]], [2.4, 2.6, 6.4, 3.6, 1.6],
                              "Decisions needed from BDOI", size=8),
        "process": md_table(["Step", "What happens", "Who", "Output", "Time"], [list(x) for x in d["process"]],
                            [1.9, 6.6, 3.6, 3.2, 1.9], "Change-control process", size=8),
        "ccb": md_table(["Member", "Organisation", "Role on the board"], [list(x) for x in d["ccb"]],
                        [5.5, 3.2, 7.8], "Change Control Board (proposed)", size=8.5),
        "assumptions": [f"- {a}" for a in d["assumptions"]] + [""],
        "roles": md_table(["Role", "Work counted in the estimate"], [list(x) for x in d["roles"]], [4.5, 12],
                          "Team roles in the estimates", size=8.5),
    }


def expanded_source(d: dict[str, Any], src: Path) -> Path:
    blocks = placeholders(d)
    figs = figures(d)
    lines: list[str] = []
    for line in src.read_text(encoding="utf-8").splitlines():
        m = PLACEHOLDER.match(line.strip())
        if m:
            name = m.group(1)
            if name not in blocks:
                raise KeyError(f"unknown placeholder cr:{name}")
            lines.extend(blocks[name])
            continue
        lines.append(TOKEN.sub(lambda t: str(figs[t.group(1)]), line))
    tmp = src.with_name(f"_{src.stem}.expanded.md")
    tmp.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return tmp


def build_doc(d: dict[str, Any], pdf: bool = True, keep_pdf: bool = False) -> tuple[Path, Path | None]:
    tmp = expanded_source(d, HERE / DOC)
    try:
        front, lines = load_source(tmp)
        doc = BdoiDocument(meta_from(front), h1_page_break=bool(front.get("h1_page_break", True)), base_dir=HERE)
        doc.cover()
        doc.front_matter()
        render_body(doc, lines)
        target = OUT / DOCX_NAME.format(v=d["meta"]["version"])
        return doc.publish(target, pdf=pdf, keep_pdf=keep_pdf)
    finally:
        tmp.unlink(missing_ok=True)


# ------------------------------------------------------------------------------------------------ main

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--check", action="store_true", help="checks only")
    ap.add_argument("--previews", action="store_true", help="render page previews of both outputs")
    ap.add_argument("--no-pdf", action="store_true", help="skip the PDF pass of the Word document")
    args = ap.parse_args(argv)
    d = load()
    problems = check(d)
    tmp = expanded_source(d, HERE / DOC)
    try:
        problems += [f"{DOC}: {p}" for p in lint_source(tmp)]
        body = tmp.read_text(encoding="utf-8")
        problems += [f"{DOC}: {p}" for p in wording(body.split("\n---", 2)[-1])]
    finally:
        tmp.unlink(missing_ok=True)
    for p in problems:
        print(p)
    if problems:
        return 1
    f = figures(d)
    print(f"check: {f['total_crs']} CRs, {f['total_md']} man-days, {f['open_crs']} open, {f['impl_crs']} implemented "
          f"- OK")
    if args.check:
        return 0
    import render

    xlsx = build_workbook(d)
    print(f"xlsx: {xlsx}")
    if args.previews:
        pdf = render.to_pdf(xlsx)
        render.previews(pdf)
        print(f"xlsx pages: {render.page_count(pdf)}")
        pdf.unlink(missing_ok=True)
    docx_path, pdf_path = build_doc(d, pdf=not args.no_pdf, keep_pdf=args.previews)
    print(f"docx: {docx_path}")
    if pdf_path and args.previews:
        print(f"docx pages: {render.page_count(pdf_path)}")
        render.previews(pdf_path)
        pdf_path.unlink(missing_ok=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
