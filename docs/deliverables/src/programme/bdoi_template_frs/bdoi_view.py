"""BDOI view of the BDOI-template FRS packs (product owner direction of 11-Oct-2026).

The FRS and its workbooks revolve around the BRD, BDOI's own FRS and the iNXT BrokerVerse OOTB platform; the BIBS build
is not described (BIBS stays only as BDOI's name for the system). The phrases of bdoi_view.yaml are rewritten as
requirement wording in the added text of an FRS (BDOI's own paragraphs are never touched) and in every text cell of the
workbooks of the packs. Used by build_bdoi_frs_pm.py and the
workbook builders of those packs.
"""
from __future__ import annotations

import copy
import re
from pathlib import Path

import docx
import yaml
from docx.oxml.ns import qn

HERE = Path(__file__).resolve().parent
RULES = yaml.safe_load((HERE / "bdoi_view.yaml").read_text(encoding="utf-8"))
_REGEX = [(re.compile(p), r) for p, r in RULES.get("regex") or []]


def rewrite(s: str) -> str:
    for old, new in RULES["replace"]:
        s = s.replace(old, new)
    for rx, new in _REGEX:
        s = rx.sub(new, s)
    return s


def _text(p) -> str:
    return "".join(t.text or "" for t in p.iter(qn("w:t")))


def source_texts(path: Path) -> set[str]:
    """The paragraph texts of BDOI's FRS (never rewritten)."""
    d = docx.Document(str(path))
    return {_text(p) for p in d.element.body.iter(qn("w:p"))}


def apply_docx(doc, protected: set[str]) -> dict:
    """Rewrites the added text of the document; removes the paragraphs (or table rows) of the drop list."""
    stats = {"bdoi_view_rewritten": 0, "bdoi_view_dropped": 0}
    for p in list(doc.element.body.iter(qn("w:p"))):
        if p.getparent() is None:
            continue
        s = _text(p)
        if not s.strip() or s in protected:
            continue
        if any(d in s for d in RULES.get("drop") or []):
            tr = next((x for x in p.iterancestors() if x.tag == qn("w:tr")), None)
            target = tr if tr is not None else p
            if target.getparent() is not None:
                target.getparent().remove(target)
                stats["bdoi_view_dropped"] += 1
            continue
        new = rewrite(s)
        if new == s:
            continue
        runs = list(p.iter(qn("w:t")))
        for t in runs:                       # phrase within one run: the formatting of each run is kept
            t.text = rewrite(t.text or "")
        if _text(p) != new:                  # phrase across runs: the text goes to the first run
            runs[0].text = new
            runs[0].set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
            for t in runs[1:]:
                t.text = ""
        stats["bdoi_view_rewritten"] += 1
    return stats


def patch_workbooks(folder_part: str = "BDOI_Template_FRS") -> None:
    """Every openpyxl workbook saved under a path holding folder_part has its text cells rewritten first."""
    from openpyxl.workbook.workbook import Workbook  # noqa: PLC0415
    if getattr(Workbook.save, "_bdoi_view", False):
        return
    original = Workbook.save

    def save(self, filename):
        if folder_part in str(filename) and "/Quality/" not in str(filename) and "/Comparisons/" not in str(filename):
            normalise_workbook(self, Path(str(filename)).name)
        if folder_part in str(filename):
            for ws in self.worksheets:
                for row in ws.iter_rows():
                    for c in row:
                        if isinstance(c.value, str) and not c.value.startswith("="):
                            new = rewrite(c.value)
                            if new != c.value:
                                c.value = new
        return original(self, filename)

    save._bdoi_view = True
    Workbook.save = save


# ------------------------------------------------------------------------------------- one style for the workbooks
_MD_HEAD = re.compile(r"(?i)man-?days?|\bmd\b|effort|^total\b|^low\b|^high\b|analysis and design|^backend|^frontend"
                      r"|^testing|^documentation")
_NOT_MD = re.compile(r"(?i)cost|php|rate|(?<!20)%|covers|no\.|count|status|assessment|activit|note|source|basis")
_COST = re.compile(r"(?i)\bcost\b")


def normalise_workbook(wb, filename: str) -> dict:
    """The same look for every client workbook of the four packs (whatever builder wrote it): Arial throughout; on
    each sheet the print set-up (landscape, one page wide, header row repeated), the file name and page in the header
    and footer, the header row frozen and filtered, long texts wrapped; man-days as 0.00 and dates as dd-Mmm-yyyy;
    the named cells DayRate and Accuracy on the Assumptions sheet of an effort estimate."""
    import datetime as _dt  # noqa: PLC0415

    from openpyxl.styles import Alignment, Font  # noqa: PLC0415
    from openpyxl.utils import get_column_letter  # noqa: PLC0415
    from openpyxl.worksheet.properties import PageSetupProperties  # noqa: PLC0415

    blue, muted = "004EA8", "656565"
    effort = "Effort_Estimate" in filename
    out = {"fonts": 0, "formats": 0, "wrapped": 0, "filters": 0, "frozen": 0}
    for ws in wb.worksheets:
        # fonts
        for row in ws.iter_rows():
            for c in row:
                if c.value is not None and c.font is not None and c.font.name != "Arial":
                    f = copy.copy(c.font)
                    c.font = Font(name="Arial", sz=f.sz, b=f.b, i=f.i, color=f.color, u=f.u, strike=f.strike,
                                  vertAlign=f.vertAlign)
                    out["fonts"] += 1
        # header and footer: the title and the file name on top, classification, page and version below
        ws.oddHeader.center.text = "&F"
        ws.oddHeader.center.size = 8
        ws.oddHeader.center.color = muted
        if not ws.oddFooter.center.text:
            ws.oddFooter.left.text = "Confidential – BDOI"
            ws.oddFooter.center.text = "Page &P of &N"
            for part in (ws.oddFooter.left, ws.oddFooter.center, ws.oddFooter.right):
                part.size = 8
                part.color = muted
        ps = ws.page_setup
        if ws.title != "Cover":
            ps.orientation = "landscape"
        ps.fitToWidth = 1
        ps.fitToHeight = 0
        ws.sheet_properties.pageSetUpPr = PageSetupProperties(fitToPage=True)
        # a very wide sheet prints on A3 landscape, so that one page wide stays readable
        total = sum((ws.column_dimensions[get_column_letter(k)].width or 13) for k in range(1, ws.max_column + 1))
        if ws.title != "Cover" and total > 260:
            ps.paperSize = ws.PAPERSIZE_A3
        if ws.title in ("Cover",):
            continue
        # the header row: the first row with two or more cells on the header blue
        header = None
        for r in range(1, min(ws.max_row, 25) + 1):
            n = sum(1 for k in range(1, ws.max_column + 1)
                    if ws.cell(r, k).value is not None and (ws.cell(r, k).fill.fgColor.rgb or "")[-6:] == blue)
            if n >= 2:
                header = r
                break
        if header is None:
            continue
        last_col = max(k for k in range(1, ws.max_column + 1) if ws.cell(header, k).value is not None)
        if not ws.print_title_rows:
            ws.print_title_rows = f"{header}:{header}"
        if ws.freeze_panes is None:
            ws.freeze_panes = f"A{header + 1}"
            out["frozen"] += 1
        if not ws.auto_filter.ref and ws.max_row > header and ws.title != "Lists":
            ws.auto_filter.ref = f"A{header}:{get_column_letter(last_col)}{ws.max_row}"
            out["filters"] += 1
        heads = {k: str(ws.cell(header, k).value or "") for k in range(1, last_col + 1)}
        md_cols = {k for k, h in heads.items() if _MD_HEAD.search(h) and not _NOT_MD.search(h)} if effort else \
            {k for k, h in heads.items() if re.search(r"(?i)man-?days?", h) and not _NOT_MD.search(h)}
        kv = last_col <= 3 and ws.title in ("Summary",)
        for row in ws.iter_rows(min_row=header + 1, max_row=ws.max_row, max_col=last_col):
            for c in row:
                v = c.value
                if v is None:
                    continue
                numeric = (isinstance(v, (int, float)) and not isinstance(v, bool)) or (isinstance(v, str) and v.startswith("="))
                label = str(ws.cell(c.row, 1).value or "")
                is_md = c.column in md_cols or (kv and c.column > 1 and re.search(r"(?i)man-?day", label)
                                                and not _NOT_MD.search(label))
                is_md = is_md or (effort and kv and c.column > 1 and re.search(r"(?i)^range (low|high)", label))
                is_cost = effort and (_COST.search(heads.get(c.column, "")) or (kv and _COST.search(label)))
                if numeric and is_cost and c.number_format in ("General", "0.00", "0.0#"):
                    c.number_format = "#,##0.00"
                    out["formats"] += 1
                elif numeric and (is_md or (effort and c.number_format == "0.0#")) \
                        and c.number_format in ("General", "0.0#", "0.0", "0"):
                    c.number_format = "0.00"
                    out["formats"] += 1
                if isinstance(v, (_dt.date, _dt.datetime)) and c.number_format != "dd-mmm-yyyy":
                    c.number_format = "dd-mmm-yyyy"
                    out["formats"] += 1
                if isinstance(v, str) and not v.startswith("=") and not (c.alignment and c.alignment.wrap_text):
                    width = ws.column_dimensions[get_column_letter(c.column)].width or 10
                    if len(v) > width * 1.1:
                        a = copy.copy(c.alignment)
                        c.alignment = Alignment(horizontal=a.horizontal, vertical=a.vertical or "top", wrap_text=True,
                                                indent=a.indent)
                        out["wrapped"] += 1
    # the named cells of the effort estimate
    if effort and "Assumptions" in wb.sheetnames:
        from openpyxl.workbook.defined_name import DefinedName  # noqa: PLC0415
        ws = wb["Assumptions"]
        names = set(wb.defined_names.keys()) if hasattr(wb.defined_names, "keys") else set()
        for r in range(1, ws.max_row + 1):
            label = str(ws.cell(r, 1).value or "")
            for name, rx in (("DayRate", r"(?i)day ?rate|daily rate"), ("Accuracy", r"(?i)range|accuracy")):
                if name not in names and re.search(rx, label):
                    dn = DefinedName(name, attr_text=f"Assumptions!$B${r}")
                    try:
                        wb.defined_names[name] = dn
                    except TypeError:
                        wb.defined_names.append(dn)
                    names.add(name)
    return out
