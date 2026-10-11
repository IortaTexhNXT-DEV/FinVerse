"""BDOI view of the BDOI-template FRS packs (product owner direction of 11-Oct-2026).

The FRS and its workbooks revolve around the BRD, BDOI's own FRS and the iNXT BrokerVerse OOTB platform; the BIBS build
is not described (BIBS stays only as BDOI's name for the system). The phrases of bdoi_view.yaml are rewritten as
requirement wording in the added text of an FRS (BDOI's own paragraphs are never touched) and in every text cell of the
workbooks of the packs. Used by build_brd06_bdoi_frs.py, build_bdoi_frs_pm.py, build_brd02_bdoi_frs.py and the
workbook builders of those packs.
"""
from __future__ import annotations

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
