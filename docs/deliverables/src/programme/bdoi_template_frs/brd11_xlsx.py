"""Workbook helper of the BRD-11 review workbooks: the BIBS workbook (tools/deliverables/bdoi_xlsx.py) with the
drop-down lists on a visible Lists sheet, and self-contained fill-in template sheets (templates standard of the
client, 28-Sep-2026): title block, column guide band directly above the header, mandatory headers marked *, an
example row in grey italics, drop-downs, frozen panes and print set-up; plus a Start here index sheet.
"""

from __future__ import annotations

import sys
from pathlib import Path
from typing import Any, Sequence

from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter
from openpyxl.worksheet.datavalidation import DataValidation

REPO = Path(__file__).resolve().parents[5]
sys.path.insert(0, str(REPO / "tools" / "deliverables"))
import brand  # noqa: E402
from bdoi_xlsx import BdoiWorkbook, Column, _SheetInfo  # noqa: E402

THIN = Side(style="thin", color=brand.BORDER)
BOX = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)
HEAD = PatternFill("solid", fgColor=brand.HEADER_BLUE)
BAND = PatternFill("solid", fgColor=brand.BG_BLUE)
GUIDE = PatternFill("solid", fgColor="F2F2F2")
EXAMPLE = PatternFill("solid", fgColor="EDEDED")
FONT = brand.FONT


def font(size=10, bold=False, colour=brand.TEXT, italic=False):
    return Font(name=FONT, size=size, bold=bold, color=colour, italic=italic)


class ReviewWorkbook(BdoiWorkbook):
    """BdoiWorkbook whose drop-down lists are on a visible sheet named Lists (one titled column per list)."""

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._lists.title = "Lists"
        self._lists.sheet_state = "visible"
        self._list_cache: dict[tuple, str] = {}
        self.next_list_name = "Values"

    def _list_ref(self, values: Sequence[str]) -> str:
        key = tuple(values)
        if key in self._list_cache:
            return self._list_cache[key]
        self._list_col += 1
        col = self._list_col
        letter = get_column_letter(col)
        head = self._lists.cell(row=1, column=col, value=self.next_list_name)
        head.font = font(10, True, brand.WHITE)
        head.fill = HEAD
        self._lists.column_dimensions[letter].width = max(18, min(48, max(len(str(v)) for v in values) + 4))
        for i, v in enumerate(values, start=2):
            self._lists.cell(row=i, column=col, value=v).font = font(10)
        ref = f"'Lists'!${letter}$2:${letter}${len(values) + 1}"
        self._list_cache[key] = ref
        return ref

    def sheet(self, name, columns, rows, description="", freeze_first_column=True):
        for c in columns:
            if c.values:
                self.next_list_name = c.header
                self._list_ref(c.values)
        return super().sheet(name, columns, rows, description, freeze_first_column)

    def save(self, path):
        self._lists.freeze_panes = "A2"
        self._print_setup(self._lists, "1:1")
        return super().save(path)

    # ------------------------------------------------------------------ fill-in template sheet
    def template_sheet(self, name: str, title: str, block: list[tuple[str, str]], cols: list[dict],
                       rows: Sequence[dict[str, Any]], example: dict[str, Any] | None, description: str = ""):
        """cols: key, header, width, mandatory (bool), format, values (list), what. Returns the worksheet and the
        number of the first data row."""
        ws = self.wb.create_sheet(name[:31])
        last = get_column_letter(len(cols))
        ws["A1"] = title
        ws["A1"].font = font(14, True, brand.HEADER_BLUE)
        ws.row_dimensions[1].height = 24
        r = 2
        for label, value in block:
            ws.cell(row=r, column=1, value=label).font = font(9, True, brand.NEAR_BLACK)
            ws.cell(row=r, column=2, value=value).font = font(9, False, brand.TEXT)
            ws.merge_cells(start_row=r, start_column=2, end_row=r, end_column=len(cols))
            ws.cell(row=r, column=2).alignment = Alignment(wrap_text=True, vertical="top")
            ws.row_dimensions[r].height = 15 if len(str(value)) < 150 else 28
            r += 1
        r += 1
        guide_rows = [("Mandatory", lambda c: "Mandatory *" if c.get("mandatory") else "Optional"),
                      ("Format", lambda c: c.get("format", "Text")),
                      ("Allowed values", lambda c: ", ".join(c["values"]) if c.get("values") else "Free text"),
                      ("What to enter", lambda c: c.get("what", ""))]
        for label, fn in guide_rows:
            for ci, c in enumerate(cols, start=1):
                cell = ws.cell(row=r, column=ci, value=(f"{label}: " if ci == 1 else "") + str(fn(c)))
                cell.font = font(8, ci == 1, brand.MUTED, italic=ci != 1)
                cell.fill = GUIDE
                cell.border = BOX
                cell.alignment = Alignment(wrap_text=True, vertical="top")
            ws.row_dimensions[r].height = 24 if label != "What to enter" else 40
            r += 1
        header_row = r
        for ci, c in enumerate(cols, start=1):
            cell = ws.cell(row=r, column=ci, value=c["header"] + (" *" if c.get("mandatory") else ""))
            cell.font = font(10, True, brand.WHITE)
            cell.fill = HEAD
            cell.border = BOX
            cell.alignment = Alignment(wrap_text=True, vertical="center")
            ws.column_dimensions[get_column_letter(ci)].width = c.get("width", 18)
        ws.row_dimensions[r].height = 30
        r += 1
        if example:
            for ci, c in enumerate(cols, start=1):
                v = example.get(c["key"], "")
                if ci == 1:
                    v = f"Example – overwrite or delete: {v}" if v else "Example – overwrite or delete"
                cell = ws.cell(row=r, column=ci, value=v)
                cell.font = font(9, False, "808080", italic=True)
                cell.fill = EXAMPLE
                cell.border = BOX
                cell.alignment = Alignment(wrap_text=True, vertical="top")
            r += 1
        first = r
        for i, row in enumerate(rows):
            for ci, c in enumerate(cols, start=1):
                v = row.get(c["key"])
                cell = ws.cell(row=r, column=ci, value=v)
                cell.font = font(9)
                cell.border = BOX
                cell.alignment = Alignment(wrap_text=True, vertical="top")
                if i % 2 == 1:
                    cell.fill = BAND
                if c.get("kind") == "date" and v is not None:
                    cell.number_format = "dd-mmm-yyyy"
            r += 1
        ext = max(r - 1, first) + 300
        for ci, c in enumerate(cols, start=1):
            if c.get("values"):
                self.next_list_name = c["header"]
                dv = DataValidation(type="list", formula1=self._list_ref(c["values"]), allow_blank=True,
                                    showErrorMessage=True, errorTitle=c["header"],
                                    error=f"Choose a value from the list ({c['header']}).")
                ws.add_data_validation(dv)
                letter = get_column_letter(ci)
                dv.add(f"{letter}{first}:{letter}{ext}")
        ws.freeze_panes = ws.cell(row=header_row + 1, column=2)
        ws.auto_filter.ref = f"A{header_row}:{last}{max(r - 1, header_row)}"
        self._print_setup(ws, f"{header_row}:{header_row}")
        ws.sheet_view.zoomScale = 90
        columns = [Column(c["key"], c["header"], c.get("width", 18), c.get("what", ""), values=c.get("values"))
                   for c in cols]
        self._sheets.append(_SheetInfo(ws.title, description or title, columns, len(rows)))
        return ws, first, header_row

    def index_sheet(self, name: str, title: str, intro: str, header: list[str], rows: list[list[Any]],
                    widths: list[float], link_col: int | None = None, status_col: int | None = None,
                    status_values: Sequence[str] | None = None):
        """A Start here / index sheet; link_col holds sheet names turned into links "Open →"."""
        ws = self.wb.create_sheet(name[:31])
        ws["A1"] = title
        ws["A1"].font = font(14, True, brand.HEADER_BLUE)
        ws["A2"] = intro
        ws["A2"].font = font(9, False, brand.MUTED)
        ws["A2"].alignment = Alignment(wrap_text=True, vertical="top")
        ws.merge_cells(start_row=2, start_column=1, end_row=2, end_column=len(header))
        ws.row_dimensions[2].height = 42
        hr = 4
        for ci, (h, w) in enumerate(zip(header, widths), start=1):
            cell = ws.cell(row=hr, column=ci, value=h)
            cell.font = font(10, True, brand.WHITE)
            cell.fill = HEAD
            cell.border = BOX
            cell.alignment = Alignment(wrap_text=True, vertical="center")
            ws.column_dimensions[get_column_letter(ci)].width = w
        for ri, row in enumerate(rows, start=hr + 1):
            for ci, v in enumerate(row, start=1):
                cell = ws.cell(row=ri, column=ci, value=v)
                cell.font = font(9)
                cell.border = BOX
                cell.alignment = Alignment(wrap_text=True, vertical="top")
                if (ri - hr) % 2 == 0:
                    cell.fill = BAND
            if link_col:
                target = ws.cell(row=ri, column=link_col).value
                cell = ws.cell(row=ri, column=link_col, value="Open →")
                cell.hyperlink = f"#'{target}'!A1"
                cell.font = font(9, True, brand.CTA_BLUE)
        if status_col and status_values:
            self.next_list_name = header[status_col - 1]
            dv = DataValidation(type="list", formula1=self._list_ref(status_values), allow_blank=True)
            ws.add_data_validation(dv)
            letter = get_column_letter(status_col)
            dv.add(f"{letter}{hr + 1}:{letter}{hr + len(rows) + 50}")
        ws.freeze_panes = ws.cell(row=hr + 1, column=2)
        self._print_setup(ws, f"{hr}:{hr}")
        self._sheets.append(_SheetInfo(ws.title, title, [Column(f"c{i}", h, w, "") for i, (h, w) in
                                                       enumerate(zip(header, widths))], len(rows)))
        return ws
