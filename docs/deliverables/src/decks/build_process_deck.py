"""Builds the BDOI business process reverse KT deck (deliverable 41, v2.0).

The deck gives BDOI back the project team's understanding of how BDOI works, department by department and end to
end: the current (As-Is) process as walked on the floor, the pain points the business users raised, the To-Be the
BRDs ask for and how it answers those pain points, what the BRDs do not cover yet, and the broker-industry practices
that go beyond the BRDs, with a roadmap for streamlining and automation, data migration and integrations. It does not
report build progress.

Content lives next to this script:

* ``process_deck/deck.yaml``: title, the understanding chapter, the holistic chapter, the department parts, the
  cross-cutting chapter (data migration, integrations, shared capabilities) and the roadmap;
* ``process_deck/areas/*.yaml``: one file per business area (at a glance, floor-walk notes, As-Is swimlanes,
  pain points, To-Be swimlane, before / after, what the BRD does not cover, best practice, speaker notes).

Sources of the content: the BDOI Day 1 business overview of 21-Sep-2026, the floor walks of 22 to 25-Sep-2026 and
their notes (Ref OV-, NB-, PM-, CA-...), the high-level process flows confirmed by the BDOI process owners, the BRDs
(PDF page), the discrepancy register (DCR-nnn) and the design documents of the repository.

The swimlanes and enterprise maps are generated as Graphviz sources in ``figures/*.dot`` and rendered to
``figures/*.png`` with the BDO colour tokens (tools/deliverables).

    python docs/deliverables/src/decks/build_process_deck.py              # deck
    python docs/deliverables/src/decks/build_process_deck.py --previews [dir]   # deck + slide PNGs
    python docs/deliverables/src/decks/build_process_deck.py --figures-only
"""

from __future__ import annotations

import argparse
import math
import tempfile
import sys
from pathlib import Path
from typing import Any

import yaml
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Emu, Inches, Pt

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
sys.path.insert(0, str(ROOT / "tools" / "deliverables"))
sys.path.insert(0, str(HERE))

import brand  # noqa: E402
from bdoi_docx import render_dot  # noqa: E402
from bdoi_pptx import MARGIN, W, H, BdoiDeck, _rgb  # noqa: E402
import process_figures as pf  # noqa: E402

DATA = HERE / "process_deck"
FIG = HERE / "figures"
VERSION = "2.0"
OUT = brand.out_dir("BRD-00", "Decks") / brand.output_name(
    "Deck", "BRD-00", "Business Process AsIs Envisioned BestPractice", VERSION, "pptx")

CONTENT_W = W - 2 * MARGIN
TOP = Inches(1.4)
BOTTOM = H - Inches(0.62)  # top of the footer rule

SEVERITY = {  # level of a gap or open point: (fill, font)
    "High": (brand.DANGER_BG, brand.DANGER),
    "Medium": (brand.AMBER_BG, brand.AMBER),
    "Low": (brand.DIRTY_WHITE, brand.MUTED),
    "Parked": ("EDE7F6", "4527A0"),
    "Answered": (brand.SUCCESS_BG, brand.SUCCESS),
}
COVERAGE = {  # how far the BRD covers a floor-walk item or a practice: (label, fill, font)
    "IN_BRD": ("In the BRD", brand.SUCCESS_BG, brand.SUCCESS),
    "PARTLY": ("Partly in BRD", brand.AMBER_BG, brand.AMBER),
    "NOT_IN_BRD": ("Not in BRD", brand.DANGER_BG, brand.DANGER),
    "BEYOND": ("Beyond the BRD", brand.BG_BLUE, brand.HEADER_BLUE),
    "CONFIRM": ("BDOI to confirm", "EDE7F6", "4527A0"),
}
HEARD = {  # kind of a floor-walk note: (label, fill, font)
    "asis": ("As-Is", brand.DIRTY_WHITE, brand.NEAR_BLACK),
    "pain": ("Pain point", brand.DANGER_BG, brand.DANGER),
    "want": ("User aspiration", brand.BG_BLUE, brand.HEADER_BLUE),
    "rule": ("Business rule", brand.SUCCESS_BG, brand.SUCCESS),
    "open": ("Open point", "EDE7F6", "4527A0"),
}
LEVEL = {  # benefit or effort level in the roadmap tables: (fill, font)
    "High": (brand.SUCCESS_BG, brand.SUCCESS),
    "Medium": (brand.AMBER_BG, brand.AMBER),
    "Low": (brand.DIRTY_WHITE, brand.MUTED),
}


def _short(a: dict) -> str:
    """Area name for slide titles (a long name would wrap the title onto two lines)."""
    return a.get("short", a["title"])


def load(path: Path) -> Any:
    return yaml.safe_load(path.read_text(encoding="utf-8"))


def _shape(s, kind, x, y, w, h):
    return s.shapes.add_shape(kind, int(x), int(y), int(w), int(h))


def _textbox(s, x, y, w, h):
    return s.shapes.add_textbox(int(x), int(y), int(w), int(h))


def _table(s, rows, cols, x, y, w, h):
    return s.shapes.add_table(rows, cols, int(x), int(y), int(w), int(h))


def chunks(rows: list, size: int) -> list[list]:
    """Splits rows into pages of at most ``size`` rows, balanced (7 rows at size 6 give 4 + 3, not 6 + 1)."""
    if len(rows) <= size:
        return [rows]
    pages = math.ceil(len(rows) / size)
    per = math.ceil(len(rows) / pages)
    return [rows[i:i + per] for i in range(0, len(rows), per)]


def paged(title: str, i: int, n: int) -> str:
    return title if n == 1 else f"{title} ({i + 1}/{n})"


# ====================================================================== deck with extra layouts

class ProcessDeck(BdoiDeck):
    """BdoiDeck plus the pictorial layouts of this deck. Uses the toolkit primitives only."""

    # -------------------------------------------------------------- helpers

    def _text(self, slide, x, y, w, h, *args, **kwargs):
        return super()._text(slide, int(x), int(y), int(w), int(h), *args, **kwargs)

    def _rect(self, slide, x, y, w, h, *args, **kwargs):
        return super()._rect(slide, int(x), int(y), int(w), int(h), *args, **kwargs)

    @property
    def slide(self):
        return self.prs.slides[len(self.prs.slides) - 1]

    def notes(self, text: str | list[str]) -> None:
        if isinstance(text, list):
            text = "\n".join(text)
        self.slide.notes_slide.notes_text_frame.text = text.strip()

    def page(self, title: str):
        s = self._slide()
        self._chrome(s, title)
        return s

    def _chrome(self, slide, title: str) -> None:
        """The toolkit chrome, with the title stepped down from 26 pt when it would wrap onto the yellow rule."""
        super()._chrome(slide, title)
        size = 26 if len(title) <= 50 else 23 if len(title) <= 58 else 20
        if len(title) > 66:
            print(f"warning: slide {self._n} title is {len(title)} characters: {title}")
        if size != 26:
            for shape in slide.shapes:
                if shape.has_text_frame and shape.text_frame.text == title:
                    for r in shape.text_frame.paragraphs[0].runs:
                        r.font.size = Pt(size)
                    break

    def para(self, s, x, y, w, h, runs, size=12, anchor=MSO_ANCHOR.TOP, align=None, space=4):
        """Text box with paragraphs; each paragraph is a list of (text, bold, colour) runs or a str."""
        box = _textbox(s, x, y, w, h)
        tf = box.text_frame
        tf.word_wrap = True
        tf.vertical_anchor = anchor
        tf.margin_left = tf.margin_right = Inches(0.05)
        tf.margin_top = tf.margin_bottom = Inches(0.02)
        first = True
        for p_runs in runs:
            p = tf.paragraphs[0] if first else tf.add_paragraph()
            first = False
            p.space_after = Pt(space)
            if align:
                p.alignment = align
            if isinstance(p_runs, str):
                p_runs = [(p_runs, False, brand.TEXT)]
            for text, bold, colour in p_runs:
                r = p.add_run()
                r.text = text
                r.font.name, r.font.size, r.font.bold = brand.FONT, Pt(size), bold
                r.font.color.rgb = _rgb(colour)
        return box

    def chip(self, s, x, y, text, fill, font, w=Inches(1.3), h=Inches(0.32), size=11):
        shape = _shape(s, MSO_SHAPE.ROUNDED_RECTANGLE, x, y, w, h)
        shape.fill.solid()
        shape.fill.fore_color.rgb = _rgb(fill)
        shape.line.color.rgb = _rgb(font)
        shape.line.width = Pt(0.75)
        shape.shadow.inherit = False
        tf = shape.text_frame
        tf.margin_left = tf.margin_right = Inches(0.04)
        tf.margin_top = tf.margin_bottom = 0
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = text
        r.font.name, r.font.size, r.font.bold = brand.FONT, Pt(size), True
        r.font.color.rgb = _rgb(font)
        return shape

    def circle(self, s, x, y, text, d=Inches(0.36), fill=brand.YELLOW, font=brand.NEAR_BLACK, size=12):
        shape = _shape(s, MSO_SHAPE.OVAL, x, y, d, d)
        shape.fill.solid()
        shape.fill.fore_color.rgb = _rgb(fill)
        shape.line.color.rgb = _rgb(brand.NEAR_BLACK)
        shape.line.width = Pt(0.75)
        shape.shadow.inherit = False
        tf = shape.text_frame
        tf.margin_left = tf.margin_right = tf.margin_top = tf.margin_bottom = 0
        tf.vertical_anchor = MSO_ANCHOR.MIDDLE
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = str(text)
        r.font.name, r.font.size, r.font.bold = brand.FONT, Pt(size), True
        r.font.color.rgb = _rgb(font)
        return shape

    def box(self, s, x, y, w, h, fill, line=None, radius=True):
        shape = _shape(s, MSO_SHAPE.ROUNDED_RECTANGLE if radius else MSO_SHAPE.RECTANGLE, x, y, w, h)
        if radius:
            shape.adjustments[0] = 0.08
        shape.fill.solid()
        shape.fill.fore_color.rgb = _rgb(fill)
        if line:
            shape.line.color.rgb = _rgb(line)
            shape.line.width = Pt(1)
        else:
            shape.line.fill.background()
        shape.shadow.inherit = False
        return shape

    def arrow(self, s, x, y, w, h, fill=brand.HEADER_BLUE):
        shape = _shape(s, MSO_SHAPE.RIGHT_ARROW, x, y, w, h)
        shape.fill.solid()
        shape.fill.fore_color.rgb = _rgb(fill)
        shape.line.fill.background()
        shape.shadow.inherit = False
        return shape

    def chevrons(self, s, x, y, w, h, labels, fill=brand.BG_BLUE, font=brand.HEADER_BLUE, size=12):
        n = len(labels)
        step = w / n
        for i, label in enumerate(labels):
            shape = _shape(s, MSO_SHAPE.CHEVRON if i else MSO_SHAPE.PENTAGON,
                                       int(x + i * step), y, int(step + Inches(0.12)), h)
            shape.fill.solid()
            shape.fill.fore_color.rgb = _rgb(fill if i % 2 == 0 else brand.WHITE)
            shape.line.color.rgb = _rgb(brand.HEADER_BLUE)
            shape.line.width = Pt(1)
            shape.shadow.inherit = False
            # the label sits in its own text box: a chevron's own text area is too narrow
            tip = Inches(0.2)
            self.para(s, x + i * step + (tip if i else Inches(0.04)), y, step - tip - Inches(0.04), h,
                      [[(label, True, font)]], size=size, anchor=MSO_ANCHOR.MIDDLE, align=PP_ALIGN.CENTER,
                      space=0)

    def grid_table(self, s, x, y, w, headers, rows, widths, size=12, row_h=Inches(0.5), fills=None,
                   bold_cols=(), header_fill=brand.HEADER_BLUE):
        """Native table. ``fills`` maps (row, col) -> (fill, font) for heat-map cells."""
        n_rows, n_cols = len(rows) + 1, len(headers)
        shape = _table(s, n_rows, n_cols, x, y, w, row_h * n_rows)
        tbl = shape.table
        scale = w / sum(widths)
        for i, wd in enumerate(widths):
            tbl.columns[i].width = Emu(int(wd * scale))
        tbl.rows[0].height = Inches(0.4)
        for r in range(1, n_rows):
            tbl.rows[r].height = int(row_h)
        for r in range(n_rows):
            for c in range(n_cols):
                cell = tbl.cell(r, c)
                value = headers[c] if r == 0 else rows[r - 1][c]
                cell.margin_left = cell.margin_right = Inches(0.07)
                cell.margin_top = cell.margin_bottom = Inches(0.03)
                cell.vertical_anchor = MSO_ANCHOR.MIDDLE
                tf = cell.text_frame
                tf.word_wrap = True
                p = tf.paragraphs[0]
                run = p.add_run()
                run.text = str(value)
                run.font.name, run.font.size = brand.FONT, Pt(size)
                run.font.bold = r == 0 or c in bold_cols
                cell.fill.solid()
                if r == 0:
                    cell.fill.fore_color.rgb = _rgb(header_fill)
                    run.font.color.rgb = _rgb(brand.WHITE)
                elif fills and (r - 1, c) in fills:
                    fill, font = fills[(r - 1, c)]
                    cell.fill.fore_color.rgb = _rgb(fill)
                    run.font.color.rgb = _rgb(font)
                    run.font.bold = True
                    p.alignment = PP_ALIGN.CENTER
                else:
                    cell.fill.fore_color.rgb = _rgb(brand.BG_BLUE if r % 2 == 0 else brand.WHITE)
                    run.font.color.rgb = _rgb(brand.TEXT)
        return tbl

    # -------------------------------------------------------------- layouts

    def diagram(self, title: str, png: Path, caption: str, notes) -> None:
        from PIL import Image

        s = self.page(title)
        area_w, area_h = CONTENT_W, Inches(5.17)
        with Image.open(png) as im:
            iw, ih = im.size
        scale = min(area_w / iw, area_h / ih)
        w, h = int(iw * scale), int(ih * scale)
        s.shapes.add_picture(str(png), MARGIN + (area_w - w) // 2, Inches(1.36), width=w, height=h)
        if caption:
            self._text(s, MARGIN, Inches(1.36) + h + Inches(0.02), CONTENT_W, Inches(0.34), caption, size=12,
                       italic=True, colour=brand.MUTED, align=PP_ALIGN.CENTER)
        self.notes(notes)

    def card(self, s, x, y, w, h, head, body, chip=None, head_h=Inches(0.5), size=12, head_size=13,
             head_fill=brand.HEADER_BLUE, fill=brand.WHITE):
        """Card with a coloured header; ``body`` is a list of paragraphs (str or runs); ``chip`` is
        (label, fill, font)."""
        self.box(s, x, y, w, h, fill, brand.BORDER)
        self.box(s, x, y, w, head_h, head_fill, radius=False)
        self.para(s, x + Inches(0.08), y, w - Inches(0.16), head_h, [[(head, True, brand.WHITE)]], size=head_size,
                  anchor=MSO_ANCHOR.MIDDLE, space=0)
        by = y + head_h + Inches(0.08)
        if chip:
            label, cf, ct = chip
            self.chip(s, x + Inches(0.1), by, label, cf, ct, w=Inches(1.7), h=Inches(0.28), size=11)
            by += Inches(0.36)
        runs = []
        for p in body:
            if isinstance(p, str):
                runs.append([("•  ", False, brand.HEADER_BLUE), (p, False, brand.TEXT)])
            else:
                runs.append(p)
        self.para(s, x + Inches(0.08), by, w - Inches(0.16), y + h - by - Inches(0.04), runs, size=size, space=3)

    def cards(self, title: str, items: list[dict], cols: int = 3, notes="", intro: str = "", size=12,
              head_size=13, rows: int | None = None) -> None:
        """Grid of cards: {head, body: [..], chip: key of COVERAGE or LEVEL, note}."""
        s = self.page(title)
        y0 = TOP
        if intro:
            self.para(s, MARGIN, TOP - Inches(0.05), CONTENT_W, Inches(0.5), [[(intro, False, brand.NEAR_BLACK)]],
                      size=13, space=0)
            y0 = TOP + Inches(0.5)
        gap = Inches(0.18)
        cw = (CONTENT_W - gap * (cols - 1)) / cols
        nrows = rows or math.ceil(len(items) / cols)
        ch = (BOTTOM - Inches(0.1) - y0 - gap * (nrows - 1)) / nrows
        for i, it in enumerate(items):
            x = MARGIN + (i % cols) * (cw + gap)
            y = y0 + (i // cols) * (ch + gap)
            chip = None
            if it.get("chip"):
                chip = COVERAGE.get(it["chip"]) or HEARD.get(it["chip"])
            self.card(s, x, y, cw, ch, it["head"], it.get("body", []), chip=chip, size=size, head_size=head_size)
        self.notes(notes or [f"{it['head']}: " + "; ".join(b if isinstance(b, str) else "".join(r[0] for r in b)
                                                       for b in it.get("body", [])) for it in items])

    def stats(self, title: str, tiles: list[dict], notes, foot: str = "") -> None:
        """Big-number tiles: {value, label, source}."""
        s = self.page(title)
        cols = 4
        rows = math.ceil(len(tiles) / cols)
        gap = Inches(0.2)
        tw = (CONTENT_W - gap * (cols - 1)) / cols
        avail = BOTTOM - TOP - (Inches(0.5) if foot else Inches(0.1))
        th = (avail - gap * (rows - 1)) / rows
        for i, t in enumerate(tiles):
            x = MARGIN + (i % cols) * (tw + gap)
            y = TOP + (i // cols) * (th + gap)
            self.box(s, x, y, tw, th, brand.BG_BLUE)
            self._text(s, x + Inches(0.15), y + Inches(0.08), tw - Inches(0.3), Inches(0.75), str(t["value"]),
                       size=30, bold=True, colour=brand.HEADER_BLUE)
            self.para(s, x + Inches(0.15), y + Inches(0.85), tw - Inches(0.3), th - Inches(1.25),
                      [[(t["label"], True, brand.NEAR_BLACK)]], size=13, space=0)
            if t.get("source"):
                self.para(s, x + Inches(0.15), y + th - Inches(0.42), tw - Inches(0.3), Inches(0.38),
                          [[(t["source"], False, brand.MUTED)]], size=10, anchor=MSO_ANCHOR.BOTTOM, space=0)
        if foot:
            self.para(s, MARGIN, BOTTOM - Inches(0.45), CONTENT_W, Inches(0.4), [[(foot, False, brand.MUTED)]],
                      size=11, space=0)
        self.notes(notes)

    def agenda(self, title: str, tiles: list[dict]) -> None:
        s = self.page(title)
        cols = 4
        gap = Inches(0.16)
        tw = (CONTENT_W - gap * (cols - 1)) / cols
        rows = math.ceil(len(tiles) / cols)
        th = min(Inches(1.18), (BOTTOM - TOP - Inches(0.1) - gap * (rows - 1)) / rows)
        for i, t in enumerate(tiles):
            x = MARGIN + (i % cols) * (tw + gap)
            y = TOP + (i // cols) * (th + gap)
            hl = t.get("highlight")
            self.box(s, x, y, tw, th, brand.HEADER_BLUE if hl else brand.WHITE, brand.HEADER_BLUE)
            colour = brand.WHITE if hl else brand.HEADER_BLUE
            self._text(s, x + Inches(0.08), y + Inches(0.04), Inches(0.62), Inches(0.5), t["num"], size=20,
                       bold=True, colour=brand.YELLOW if hl else brand.CTA_BLUE)
            self.para(s, x + Inches(0.66), y + Inches(0.06), tw - Inches(0.72), th - Inches(0.4),
                      [[(t["title"], True, colour)]], size=13)
            if t.get("sub"):
                self._text(s, x + Inches(0.1), y + th - Inches(0.36), tw - Inches(0.2), Inches(0.32), t["sub"],
                           size=11, colour=brand.BG_BLUE if hl else brand.MUTED)

    def part(self, p: dict) -> None:
        """Divider of a department part, listing its chapters."""
        s = self._slide()
        self._rect(s, 0, 0, W, H, brand.HEADER_BLUE)
        self._text(s, MARGIN, Inches(1.5), Inches(11.5), Inches(0.6), f"Part {p['num']}", size=20, bold=True,
                   colour=brand.YELLOW)
        size = 36 if len(p["title"]) <= 42 else 28
        self._text(s, MARGIN, Inches(2.05), Inches(11.5), Inches(1.0), p["title"], size=size, bold=True,
                   colour=brand.WHITE, anchor=MSO_ANCHOR.TOP)
        self._rect(s, MARGIN, Inches(3.15), Inches(1.6), Pt(4), brand.YELLOW)
        self.para(s, MARGIN, Inches(3.35), Inches(11.5), Inches(0.9), [[(p["subtitle"], False, brand.BG_BLUE)]],
                  size=16)
        y = Inches(4.35)
        for ch in p.get("chapters", []):
            self.para(s, MARGIN, y, Inches(11.5), Inches(0.4),
                      [[(f"{ch['num']}.  ", True, brand.YELLOW), (ch["title"], True, brand.WHITE),
                        (f"   {ch['sub']}", False, brand.FIELD_BLUE)]], size=15, space=0)
            y += Inches(0.42)
        self._text(s, MARGIN, H - Inches(0.6), Inches(6), Inches(0.35), f"{brand.FOOTER_TEXT} | {self.title_text}",
                   size=9, colour=brand.FIELD_BLUE)
        self.notes(p.get("notes", p["subtitle"]))

    def glance(self, a: dict) -> None:
        s = self.page(f"{a['num']}. {_short(a)}: at a glance")
        colw = (CONTENT_W - Inches(0.4)) / 3
        y0, hh = TOP, Inches(3.95)
        # card 1: department and roles
        x = MARGIN
        self.box(s, x, y0, colw, hh, brand.WHITE, brand.BORDER)
        self.box(s, x, y0, colw, Inches(0.45), brand.HEADER_BLUE, radius=False)
        self._text(s, x + Inches(0.12), y0, colw, Inches(0.45), "Department and roles", size=14, bold=True,
                   colour=brand.WHITE, anchor=MSO_ANCHOR.MIDDLE)
        runs = [[(a["department"], True, brand.NEAR_BLACK)]] + [[("•  ", False, brand.HEADER_BLUE),
                                                              (r, False, brand.TEXT)] for r in a["roles"]]
        self.para(s, x + Inches(0.1), y0 + Inches(0.55), colw - Inches(0.2), hh - Inches(0.6), runs, size=12,
                  space=3)
        # card 2: key figures
        x = MARGIN + colw + Inches(0.2)
        self.box(s, x, y0, colw, hh, brand.WHITE, brand.BORDER)
        self.box(s, x, y0, colw, Inches(0.45), brand.HEADER_BLUE, radius=False)
        self._text(s, x + Inches(0.12), y0, colw, Inches(0.45), "Volumes and business rules", size=14, bold=True,
                   colour=brand.WHITE, anchor=MSO_ANCHOR.MIDDLE)
        figs = a.get("figures", [])
        fy = y0 + Inches(0.55)
        tile_h = Inches(0.78)
        for k, (value, label) in enumerate(figs[:4]):
            ty = fy + k * (tile_h + Inches(0.08))
            self.box(s, x + Inches(0.1), ty, colw - Inches(0.2), tile_h, brand.BG_BLUE)
            self._text(s, x + Inches(0.18), ty, Inches(1.55), tile_h, str(value), size=20, bold=True,
                       colour=brand.HEADER_BLUE, anchor=MSO_ANCHOR.MIDDLE)
            self.para(s, x + Inches(1.72), ty, colw - Inches(1.85), tile_h, [label], size=12,
                      anchor=MSO_ANCHOR.MIDDLE, space=0)
        # card 3: how we learned it
        x = MARGIN + 2 * (colw + Inches(0.2))
        self.box(s, x, y0, colw, hh, brand.WHITE, brand.BORDER)
        self.box(s, x, y0, colw, Inches(0.45), brand.HEADER_BLUE, radius=False)
        self._text(s, x + Inches(0.12), y0, colw, Inches(0.45), "How we learned it", size=14, bold=True,
                   colour=brand.WHITE, anchor=MSO_ANCHOR.MIDDLE)
        fw = a["session"]
        pocs = fw["pocs"]
        if len(pocs) > 150:  # the full list is in the notes and on the floor-walk coverage slide
            names = [n.strip() for n in pocs.replace(";", ",").split(",")]
            shown = ", ".join(names[:6])
            pocs = f"{shown} and {len(names) - 6} more (floor-walk coverage slide)"
        runs = [[("Floor walk: ", True, brand.HEADER_BLUE), (fw["when"], False, brand.NEAR_BLACK)],
                [("Process owners: ", True, brand.HEADER_BLUE), (pocs, False, brand.TEXT)]]
        runs += [[("•  ", False, brand.HEADER_BLUE), (t, False, brand.TEXT)] for t in a["sources"]]
        self.para(s, x + Inches(0.1), y0 + Inches(0.55), colw - Inches(0.2), hh - Inches(0.6), runs, size=11,
                  space=4)
        # process in one line
        cy = y0 + hh + Inches(0.22)
        self._text(s, MARGIN, cy, CONTENT_W, Inches(0.3), "The process in one line", size=12, bold=True,
                   colour=brand.MUTED)
        size = 12 if max(len(c) for c in a["chevrons"]) <= 12 else 11
        self.chevrons(s, MARGIN, cy + Inches(0.32), CONTENT_W, Inches(0.72), a["chevrons"], size=size)
        self.notes(a.get("notes", {}).get("glance", "") + f"\n\nFloor walk: {fw['when']}; {fw['pocs']}."
                   + "\nSources: " + "; ".join(a["sources"]))

    def heard(self, a: dict) -> None:
        """Floor-walk notes of the area: Ref, topic, kind, what we heard, what BDOI wants."""
        items = a["heard"]
        pages = chunks(items, 7)
        for i, rows_in in enumerate(pages):
            s = self.page(paged(f"{a['num']}. {_short(a)}: what we heard on the floor", i, len(pages)))
            headers = ["Ref", "Topic", "Type", "What we heard (As-Is)", "What the business wants (To-Be)"]
            rows, fills = [], {}
            for k, h in enumerate(rows_in):
                label, fill, font = HEARD[h["kind"]]
                rows.append([h["ref"], h["topic"], label, h["asis"], h.get("want") or "-"])
                fills[(k, 2)] = (fill, font)
            row_h = min(Inches(0.9), (BOTTOM - TOP - Inches(0.5)) / max(1, len(rows)))
            self.grid_table(s, MARGIN, TOP, CONTENT_W, headers, rows, [0.8, 1.9, 1.35, 4.6, 3.4], size=11,
                            row_h=row_h, fills=fills, bold_cols=(0,))
        notes = [a.get("notes", {}).get("heard", "Notes of the floor walk and the discovery sessions; the Ref is "
                                                 "the row of the floor-walk notes workbook.")]
        notes += [f"{h['ref']} {h['topic']} ({HEARD[h['kind']][0]}): {h['asis']}"
                  + (f" Wants: {h['want']}" if h.get("want") else "") for h in items]
        self.notes(notes)

    def pain_legend(self, a: dict) -> None:
        s = self.page(f"{a['num']}. {_short(a)}: pain points today")
        pains = a["pains"]
        cols = 2
        rows = math.ceil(len(pains) / cols)
        gap = Inches(0.3)
        cw = (CONTENT_W - gap) / cols
        rh = min(Inches(1.25), (Inches(5.35)) / rows)
        for i, p in enumerate(pains):
            c, r = i // rows, i % rows
            x, y = MARGIN + c * (cw + gap), TOP + r * rh
            self.circle(s, x, y + Inches(0.04), p["n"], d=Inches(0.42), size=14)
            self.para(s, x + Inches(0.52), y, cw - Inches(0.55), rh - Inches(0.05),
                      [[(p["title"], True, brand.NEAR_BLACK)],
                       [(p["text"], False, brand.TEXT)],
                       [(p["ref"], False, brand.CTA_BLUE)]], size=12, space=1)
        notes = [a.get("notes", {}).get("pains", "Pain points marked on the As-Is swimlane.")]
        notes += [f"{p['n']}. {p['title']}: {p['text']} ({p['ref']})" for p in pains]
        self.notes(notes)

    def before_after(self, a: dict, rows_key: str = "after", suffix: str = "how the BRD answers",
                     heads: tuple = ("Pain point today", "To-Be in the BRD", "Benefit")) -> None:
        """Pain point -> answer -> benefit rows ({n, bibs, measure}), seven a slide."""
        pains = {p["n"]: p for p in a["pains"]}
        pages = chunks(a[rows_key], 7)
        for pi, rows in enumerate(pages):
            s = self.page(paged(f"{a['num']}. {_short(a)}: {suffix}", pi, len(pages)))
            head_y = TOP - Inches(0.02)
            xb, wb = MARGIN + Inches(0.5), Inches(3.7)
            xa, wa = xb + wb + Inches(0.45), Inches(5.2)
            xm, wm = xa + wa + Inches(0.12), CONTENT_W - (xa + wa + Inches(0.12) - MARGIN)
            for x, w, t in zip((xb, xa, xm), (wb, wa, wm), heads):
                self._text(s, x, head_y, w, Inches(0.3), t, size=12, bold=True, colour=brand.HEADER_BLUE)
            rh = Inches(5.0) / 7
            for i, r in enumerate(rows):
                y = TOP + Inches(0.32) + i * rh
                bh = rh - Inches(0.08)
                self.circle(s, MARGIN, y + (bh - Inches(0.36)) / 2, r["n"], size=12)
                self.box(s, xb, y, wb, bh, brand.DIRTY_WHITE, brand.BORDER)
                self.para(s, xb + Inches(0.05), y, wb - Inches(0.1), bh, [pains[r["n"]]["title"]], size=12,
                          anchor=MSO_ANCHOR.MIDDLE, space=0)
                self.arrow(s, xb + wb + Inches(0.08), y + bh / 2 - Inches(0.12), Inches(0.3), Inches(0.24))
                self.box(s, xa, y, wa, bh, brand.BG_BLUE, brand.CTA_BLUE)
                self.para(s, xa + Inches(0.05), y, wa - Inches(0.1), bh, [r["bibs"]], size=11,
                          anchor=MSO_ANCHOR.MIDDLE, space=0)
                self.para(s, xm, y, wm, bh, [[(r.get("measure") or "-", True, brand.SUCCESS)]], size=11,
                          anchor=MSO_ANCHOR.MIDDLE, space=0)
        notes = [a.get("notes", {}).get(rows_key, "")]
        notes += [f"{r['n']}. {pains[r['n']]['title']} -> {r['bibs']}"
                  + (f" {heads[2]}: {r['measure']}." if r.get("measure") else "") for r in a[rows_key]]
        self.notes(notes)

    def missed(self, a: dict | None, title: str | None = None, rows_in: list | None = None, notes: str = "",
               per_page: int = 7) -> None:
        """What the BRD does not cover (yet): coverage, item, what we saw, BRD position, recommendation, ref."""
        rows_in = rows_in if rows_in is not None else a["missed"]
        title = title or f"{a['num']}. {_short(a)}: gaps in the BRD"
        pages = chunks(rows_in, per_page)
        for i, part in enumerate(pages):
            s = self.page(paged(title, i, len(pages)))
            headers = ["Coverage", "Item", "What we saw on the floor", "What the BRD says", "Our recommendation",
                       "Ref"]
            rows, fills = [], {}
            for k, g in enumerate(part):
                label, fill, font = COVERAGE[g["cov"]]
                rows.append([label, g["item"], g["seen"], g["brd"], g["rec"], g["ref"]])
                fills[(k, 0)] = (fill, font)
            row_h = min(Inches(0.9), (BOTTOM - TOP - Inches(0.5)) / max(1, len(rows)))
            self.grid_table(s, MARGIN, TOP, CONTENT_W, headers, rows, [1.15, 2.0, 3.0, 2.6, 3.2, 1.15], size=11,
                            row_h=row_h, fills=fills, bold_cols=(1,))
        n = [notes or (a.get("notes", {}).get("missed", "") if a else "")]
        n += [f"[{COVERAGE[g['cov']][0]}] {g['item']}. Seen: {g['seen']} BRD: {g['brd']} Recommendation: "
              f"{g['rec']} ({g['ref']})" for g in rows_in]
        self.notes(n)

    def best_practice(self, a: dict, chips: bool = True) -> None:
        """Practice cards; ``chips`` shows how far the BRD covers each (off in the floor-walk edition)."""
        items = a["best"]
        pages = chunks(items, 6)
        for pi, part in enumerate(pages):
            s = self.page(paged(f"{a['num']}. {_short(a)}: best practice", pi, len(pages)))
            cols = 3
            gap = Inches(0.18)
            cw = (CONTENT_W - gap * (cols - 1)) / cols
            rows = math.ceil(len(part) / cols)
            ch = (Inches(5.35) - gap * (rows - 1)) / rows
            for i, b in enumerate(part):
                x = MARGIN + (i % cols) * (cw + gap)
                y = TOP + (i // cols) * (ch + gap)
                self.box(s, x, y, cw, ch, brand.WHITE, brand.BORDER)
                self.box(s, x, y, cw, Inches(0.62), brand.HEADER_BLUE, radius=False)
                self.para(s, x + Inches(0.08), y, cw - Inches(0.16), Inches(0.62),
                          [[(b["practice"], True, brand.WHITE)]], size=13, anchor=MSO_ANCHOR.MIDDLE, space=0)
                lx = x + Inches(0.1)
                if chips:
                    label, fill, font = COVERAGE[b["status"]]
                    self.chip(s, lx, y + Inches(0.7), label, fill, font, w=Inches(1.75), h=Inches(0.28), size=11)
                    lx = x + Inches(1.95)
                if b.get("lever"):
                    self.para(s, lx, y + Inches(0.68), x + cw - lx - Inches(0.1), Inches(0.32),
                              [[(b["lever"], True, brand.CTA_BLUE)]], size=11, anchor=MSO_ANCHOR.MIDDLE, space=0)
                self.para(s, x + Inches(0.08), y + Inches(1.02), cw - Inches(0.16), ch - Inches(1.05),
                          [[("Industry practice: ", True, brand.HEADER_BLUE), (b["what"], False, brand.TEXT)],
                           [("For BDOI: ", True, brand.HEADER_BLUE), (b["rec"], False, brand.TEXT)]], size=12,
                          space=4)
        notes = [a.get("notes", {}).get("best", "Practices of insurance brokers that fit BDOI, beyond what the BRD "
                                                 "asks; the chip shows how far the BRD already covers each one.")]
        notes += [f"{b['practice']}" + (f" [{COVERAGE[b['status']][0]}]" if chips else "")
                  + f". {b['what']} For BDOI: {b['rec']}" for b in items]
        self.notes(notes)

    def table_slide(self, title: str, t: dict, fill_col: int | None = None, fill_map: dict | None = None,
                    per_page: int = 8, size: int = 11) -> None:
        """Generic table from {headers, widths, rows, notes, intro}; ``fill_map`` colours column ``fill_col``."""
        pages = chunks(t["rows"], per_page)
        for i, part in enumerate(pages):
            s = self.page(paged(title, i, len(pages)))
            y = TOP
            if t.get("intro"):
                self.para(s, MARGIN, TOP - Inches(0.05), CONTENT_W, Inches(0.45),
                          [[(t["intro"], False, brand.NEAR_BLACK)]], size=13, space=0)
                y = TOP + Inches(0.45)
            fills = {}
            if fill_col is not None and fill_map:
                for k, r in enumerate(part):
                    key = str(r[fill_col])
                    if key in fill_map:
                        fills[(k, fill_col)] = fill_map[key]
            row_h = min(Inches(0.8), (BOTTOM - y - Inches(0.5)) / max(1, len(part)))
            self.grid_table(s, MARGIN, y, CONTENT_W, t["headers"], part, t["widths"], size=size, row_h=row_h,
                            fills=fills, bold_cols=tuple(t.get("bold_cols", (0,))))
            if t.get("foot"):
                self.para(s, MARGIN, BOTTOM - Inches(0.42), CONTENT_W, Inches(0.38),
                          [[(t["foot"], False, brand.MUTED)]], size=11, space=0)
        self.notes(t.get("notes", ""))

    def lifecycle(self, title: str, lc: dict) -> None:
        """Seven stages as chevrons, the systems used at each stage, and the three flows."""
        s = self.page(title)
        self.chevrons(s, MARGIN, TOP, CONTENT_W, Inches(0.8), lc["stages"], size=11)
        n = len(lc["stages"])
        step = CONTENT_W / n
        y = TOP + Inches(0.9)
        self._text(s, MARGIN, y, CONTENT_W, Inches(0.3), lc.get("row1", "Systems and tools used today"), size=12,
                   bold=True, colour=brand.MUTED)
        for i, sy in enumerate(lc["systems"]):
            self.box(s, MARGIN + i * step + Inches(0.04), y + Inches(0.32), step - Inches(0.08), Inches(0.72),
                     brand.DIRTY_WHITE, brand.BORDER)
            self.para(s, MARGIN + i * step + Inches(0.08), y + Inches(0.32), step - Inches(0.16), Inches(0.72),
                      [sy], size=11, anchor=MSO_ANCHOR.MIDDLE, align=PP_ALIGN.CENTER, space=0)
        y += Inches(1.18)
        self._text(s, MARGIN, y, CONTENT_W, Inches(0.3), lc.get("row2", "Owner"), size=12, bold=True,
                   colour=brand.MUTED)
        for i, ow in enumerate(lc["owners"]):
            self.para(s, MARGIN + i * step + Inches(0.04), y + Inches(0.28), step - Inches(0.08), Inches(0.5),
                      [[(ow, True, brand.HEADER_BLUE)]], size=11, align=PP_ALIGN.CENTER, space=0)
        y += Inches(0.85)
        fw = (CONTENT_W - Inches(0.36)) / 3
        fh = BOTTOM - y - Inches(0.1)
        for k, f in enumerate(lc["flows"]):
            x = MARGIN + k * (fw + Inches(0.18))
            self.card(s, x, y, fw, fh, f["head"], f["body"], head_h=Inches(0.42), size=12)
        self.notes(lc.get("notes", ""))

    def refs_figure(self, title: str, r: dict) -> None:
        """One transaction, several references: boxes for each system's number, and what it causes."""
        s = self.page(title)
        n = len(r["refs"])
        gap = Inches(0.35)
        bw = (Inches(8.0) - gap * (n - 1)) / n
        y = TOP + Inches(0.1)
        for i, ref in enumerate(r["refs"]):
            x = MARGIN + i * (bw + gap)
            self.box(s, x, y, bw, Inches(1.5), brand.BG_BLUE, brand.HEADER_BLUE)
            self.para(s, x + Inches(0.06), y + Inches(0.06), bw - Inches(0.12), Inches(1.4),
                      [[(ref["system"], True, brand.HEADER_BLUE)], [(ref["number"], True, brand.NEAR_BLACK)],
                       [(ref["who"], False, brand.MUTED)]], size=12, align=PP_ALIGN.CENTER, space=2)
            if i < n - 1:
                self.arrow(s, x + bw + Inches(0.04), y + Inches(0.62), gap - Inches(0.08), Inches(0.26))
        self.para(s, MARGIN, y + Inches(1.6), Inches(8.0), Inches(0.4), [[(r["caption"], False, brand.MUTED)]],
                  size=11, space=0)
        # effects
        ey = y + Inches(2.1)
        self._text(s, MARGIN, ey, Inches(8), Inches(0.35), "What it causes today", size=14, bold=True,
                   colour=brand.HEADER_BLUE)
        runs = [[("•  ", False, brand.HEADER_BLUE), (t, False, brand.TEXT)] for t in r["effects"]]
        self.para(s, MARGIN, ey + Inches(0.4), Inches(8.0), BOTTOM - ey - Inches(0.5), runs, size=13, space=4)
        # right panel
        px = MARGIN + Inches(8.3)
        pw = CONTENT_W - Inches(8.3)
        ph = (BOTTOM - TOP - Inches(0.28)) / 2
        self.card(s, px, TOP, pw, ph, r.get("brd_title", "To-Be in the BRD"), r["brd"], head_h=Inches(0.45))
        self.card(s, px, TOP + ph + Inches(0.18), pw, ph, r.get("rec_title", "Our recommendation"), r["rec"],
                  head_h=Inches(0.45),
                  head_fill=brand.CTA_BLUE)
        self.notes(r.get("notes", ""))

    def horizons(self, title: str, r: dict) -> None:
        """Roadmap in horizons: columns with a period, a theme and items; enablers underneath."""
        s = self.page(title)
        cols = r["columns"]
        gap = Inches(0.2)
        cw = (CONTENT_W - gap * (len(cols) - 1)) / len(cols)
        ch = Inches(4.0)
        shades = [brand.DIRTY_WHITE, brand.BG_BLUE, brand.FIELD_BLUE, brand.CTA_BLUE]
        for i, c in enumerate(cols):
            x = MARGIN + i * (cw + gap)
            fill = shades[i % len(shades)]
            font = brand.WHITE if fill == brand.CTA_BLUE else brand.HEADER_BLUE
            self.box(s, x, TOP, cw, ch, brand.WHITE, brand.BORDER)
            self.box(s, x, TOP, cw, Inches(0.8), fill, brand.HEADER_BLUE, radius=False)
            self.para(s, x + Inches(0.1), TOP, cw - Inches(0.2), Inches(0.8),
                      [[(c["title"], True, font)], [(c["when"], False, font)]], size=13, anchor=MSO_ANCHOR.MIDDLE,
                      space=0)
            runs = [[(c["theme"], True, brand.NEAR_BLACK)]]
            runs += [[("•  ", False, brand.HEADER_BLUE), (t, False, brand.TEXT)] for t in c["items"]]
            self.para(s, x + Inches(0.1), TOP + Inches(0.88), cw - Inches(0.2), ch - Inches(0.92), runs, size=12,
                      space=3)
            if i < len(cols) - 1:
                self.arrow(s, x + cw - Inches(0.02), TOP + Inches(0.26), gap + Inches(0.04), Inches(0.3))
        y = TOP + ch + Inches(0.15)
        self.box(s, MARGIN, y, CONTENT_W, BOTTOM - y - Inches(0.08), brand.BG_BLUE, brand.HEADER_BLUE)
        self._text(s, MARGIN + Inches(0.12), y + Inches(0.03), CONTENT_W, Inches(0.32), r["enablers_title"],
                   size=13, bold=True, colour=brand.HEADER_BLUE)
        half = math.ceil(len(r["enablers"]) / 2)
        for k in range(2):
            items = r["enablers"][k * half:(k + 1) * half]
            runs = [[("•  ", False, brand.HEADER_BLUE), (t, False, brand.NEAR_BLACK)] for t in items]
            self.para(s, MARGIN + Inches(0.12) + k * CONTENT_W / 2, y + Inches(0.36),
                      CONTENT_W / 2 - Inches(0.25), BOTTOM - y - Inches(0.5), runs, size=11, space=2)
        self.notes(r.get("notes", ""))

    def principles(self, title: str, items: list[dict], notes: str) -> None:
        """Numbered principles in a row of circles with text below (streamlining levers)."""
        s = self.page(title)
        n = len(items)
        cw = CONTENT_W / n
        for i, it in enumerate(items):
            x = MARGIN + i * cw
            d = Inches(0.9)
            self.circle(s, x + (cw - d) / 2, TOP + Inches(0.1), str(i + 1), d=d, fill=brand.HEADER_BLUE,
                        font=brand.WHITE, size=24)
            self.para(s, x + Inches(0.08), TOP + Inches(1.1), cw - Inches(0.16), Inches(0.5),
                      [[(it["head"], True, brand.HEADER_BLUE)]], size=15, align=PP_ALIGN.CENTER, space=0)
            self.para(s, x + Inches(0.1), TOP + Inches(1.6), cw - Inches(0.2), Inches(1.3),
                      [[(it["text"], False, brand.TEXT)]], size=12, align=PP_ALIGN.CENTER, space=0)
            self.box(s, x + Inches(0.1), TOP + Inches(2.95), cw - Inches(0.2), BOTTOM - TOP - Inches(3.05),
                     brand.BG_BLUE)
            runs = [[("At BDOI", True, brand.HEADER_BLUE)]]
            runs += [[("•  ", False, brand.HEADER_BLUE), (t, False, brand.NEAR_BLACK)] for t in it["examples"]]
            self.para(s, x + Inches(0.16), TOP + Inches(3.0), cw - Inches(0.32), BOTTOM - TOP - Inches(3.15), runs,
                      size=11, space=3)
        self.notes(notes)

    def maturity(self, title: str, m: dict, notes: str) -> None:
        s = self.page(title)
        levels = m["levels"]
        label_w = Inches(2.3)
        x0 = MARGIN + label_w
        gw = CONTENT_W - label_w
        colw = gw / len(levels)
        # scale header
        for i, lv in enumerate(levels):
            x = x0 + i * colw
            self.box(s, x + Inches(0.03), TOP, colw - Inches(0.06), Inches(0.62), brand.BG_BLUE, radius=False)
            self.para(s, x + Inches(0.05), TOP, colw - Inches(0.1), Inches(0.62),
                      [[(f"{i + 1}  ", True, brand.HEADER_BLUE), (lv, False, brand.NEAR_BLACK)]], size=11,
                      anchor=MSO_ANCHOR.MIDDLE, align=PP_ALIGN.CENTER, space=0)
        themes = m["themes"]
        rh = Inches(3.7) / len(themes)
        y0 = TOP + Inches(0.72)
        marks = [("now", brand.MUTED, "Today"), ("bibs", brand.CTA_BLUE, "To-Be in the BRDs"),
                 ("best", brand.YELLOW, "Best practice")]
        for k, t in enumerate(themes):
            y = y0 + k * rh
            if k % 2 == 0:
                self._rect(s, MARGIN, y, CONTENT_W, rh, brand.DIRTY_WHITE)
            self._text(s, MARGIN + Inches(0.05), y, label_w, rh, t["theme"], size=13, bold=True,
                       colour=brand.HEADER_BLUE, anchor=MSO_ANCHOR.MIDDLE)

            def cx(v):
                return int(x0 + (v - 0.5) * colw)

            d = Inches(0.3)
            cy = int(y + rh / 2)
            self._rect(s, cx(t["now"]), cy - Pt(1.5), cx(t["bibs"]) - cx(t["now"]), Pt(3), brand.FIELD_BLUE)
            for key, colour, _ in marks:
                v = t[key]
                off = {"now": 0, "bibs": 0, "best": 0}[key]
                self.circle(s, cx(v) - d // 2 + off, cy - d // 2, "", d=d, fill=colour, size=9)
        # legend and basis
        ly = y0 + len(themes) * rh + Inches(0.12)
        lx = MARGIN
        for _, colour, text in marks:
            self.circle(s, lx, ly, "", d=Inches(0.26), fill=colour)
            self._text(s, lx + Inches(0.3), ly - Inches(0.03), Inches(2.2), Inches(0.32), text, size=12,
                       colour=brand.TEXT)
            lx += Inches(2.3)
        self.para(s, MARGIN + Inches(7.0), ly - Inches(0.05), CONTENT_W - Inches(7.0), Inches(0.5),
                  [[(m["basis"], False, brand.MUTED)]], size=11)
        n = [notes] + [f"{t['theme']}: today {t['now']}, To-Be {t['bibs']}, best practice {t['best']}. {t['why']}"
                       for t in themes]
        self.notes(n)

    def heat_counts(self, title: str, rows: list[list], notes: str) -> None:
        """BRD x severity counts from the discrepancy register, as a heat map."""
        s = self.page(title)
        headers = ["Area", "BRD", "High", "Medium", "Low", "Open", "Answered"]
        fills = {}
        mx = max(r[2] for r in rows) or 1
        for i, r in enumerate(rows):
            for c, (base, font) in ((2, (brand.DANGER_BG, brand.DANGER)), (3, (brand.AMBER_BG, brand.AMBER)),
                                    (4, (brand.DIRTY_WHITE, brand.MUTED))):
                if r[c]:
                    fills[(i, c)] = (base, font)
            if r[2] >= 0.75 * mx:
                fills[(i, 2)] = (brand.DANGER, brand.WHITE)
        self.grid_table(s, MARGIN, TOP, Inches(8.2), headers, rows, [3.4, 1.2, 0.9, 0.9, 0.9, 0.9, 1.1],
                        size=12, row_h=Inches(0.36), fills=fills)
        self.para(s, MARGIN + Inches(8.45), TOP, CONTENT_W - Inches(8.45), Inches(5.3), notes.split("\n"),
                  size=12, space=6)
        self.notes(notes + "\nSource: docs/deliverables/src/registers/discrepancy_register.yaml. An item "
                           "that touches several BRDs is counted under each of them.")


# ====================================================================== figures

def render(name: str, text: str) -> Path:
    path = pf.write(FIG / f"{name}.dot", text)
    return render_dot(path, dpi=200)


def area_figures(a: dict) -> dict[str, Path]:
    base = f"{a['id']}"
    out = {"asis": render(f"{base}_asis", pf.swimlane(f"{base}_asis", a["asis"], "asis",
                                                      f"{a['title']}: As-Is swimlane. {a['asis']['caption']}")),
           "tobe": render(f"{base}_envisioned", pf.swimlane(f"{base}_envisioned", a["tobe"], "tobe",
                                                            f"{a['title']}: To-Be swimlane in the BRD. "
                                                            f"{a['tobe']['caption']}"))}
    for k, extra in enumerate(a.get("asis_extra", []) or []):
        name = f"{base}_asis_{k + 2}"
        out[f"asis_{k + 2}"] = render(name, pf.swimlane(name, extra, "asis",
                                                        f"{a['title']}: As-Is swimlane. {extra['caption']}"))
    return out


def asis_map_spec(m: dict, areas: dict[str, dict]) -> dict:
    """Areas above and below, today's systems in the middle; one line per dependency."""
    nodes, edges = [], []
    n_areas = max(len(m["areas_top"]), len(m["areas_bottom"]))
    gx = (pf.CANVAS_W - 8) / n_areas
    w, h = gx - 8, 50.0
    for key, y in (("areas_top", 26.0), ("areas_bottom", 272.0)):
        for i, aid in enumerate(m[key]):
            a = areas.get(aid)
            if a is None:
                continue
            nodes.append({"id": aid, "x": 6 + i * gx, "y": y, "w": w, "h": h,
                          "text": f"{a['num']}  {_short(a)}", "kind": "area", "size": 11.5,
                          "pain": [len(a["pains"])]})
    sg = (pf.CANVAS_W - 6) / len(m["systems"])
    for i, sy in enumerate(m["systems"]):
        nodes.append({"id": sy["id"], "x": 4 + i * sg, "y": 150.0, "w": sg - 8, "h": 54, "text": sy["text"],
                      "kind": "legacy", "size": 11.5})
    top = set(m["areas_top"])
    for aid, targets in m["links"].items():
        for t in targets if aid in areas else []:
            ports = ("s", "n") if aid in top else ("n", "s")
            edges.append({"a": aid, "b": t, "style": "solid", "colour": "@MUTED", "arrow": False, "ports": ports,
                          "width": 0.9})
    bands = [{"x": 0, "y": 0, "w": pf.CANVAS_W, "h": 84, "fill": "@WHITE", "label": m["top_label"],
              "label_w": 320},
             {"x": 0, "y": 124, "w": pf.CANVAS_W, "h": 90, "fill": "#EEF3F8"},
             {"x": 0, "y": 248, "w": pf.CANVAS_W, "h": 100, "fill": "@WHITE", "label": m["bottom_label"],
              "label_w": 320, "label_pos": "bottom"}]
    return {"bands": bands, "nodes": nodes, "edges": edges,
            "legend": [["area", "Business area (chapter)"], ["legacy", "Today's system, file or channel"],
                       ["pain", "Number of pain points in the chapter"]]}


def envisioned_map_spec(m: dict) -> dict:
    nodes, edges = [], []
    ex_w, ex_h, x0 = 120.0, 52.0, 236.0
    for i, x in enumerate(m["externals"]):
        nodes.append({"id": x["id"], "x": 0, "y": 6 + i * 65.0, "w": ex_w, "h": ex_h, "text": x["text"],
                      "sub": x.get("sub"), "kind": x.get("kind", "external"), "size": 12})
    nodes.append({"id": "chan", "x": 134, "y": 6, "w": 92, "h": 312, "text": m["channels"], "kind": "screen",
                  "size": 12})
    for xid, label in m["links"]:
        edges.append({"a": xid, "b": "chan", "style": "dashed" if label == "parked" else "solid",
                      "ports": ("e", "w")})
    x = x0 + 6
    for i, g in enumerate(m["groups"]):
        gid = f"g{i}"
        nodes.append({"id": gid, "x": x, "y": 26, "w": g["w"], "h": 38, "text": g["title"], "kind": "group",
                      "size": 11.5})
        nodes.append({"id": gid + "m", "x": x, "y": 68, "w": g["w"], "h": 148, "text": g["modules"],
                      "kind": "module", "size": 11})
        x += g["w"] + 4
    n = len(m["services"])
    span = pf.CANVAS_W - x0 - 12
    sw = (span - 3 * (n - 1)) / n
    for i, sv in enumerate(m["services"]):
        nodes.append({"id": f"s{i}", "x": x0 + 6 + i * (sw + 3), "y": 246, "w": sw, "h": 42, "text": sv,
                      "kind": "service", "size": 11})
    nodes.append({"id": "host", "x": x0 + 6, "y": 293, "w": span, "h": 24, "text": m["hosting"],
                  "kind": "module", "size": 11})
    bands = [{"x": x0, "y": 0, "w": pf.CANVAS_W - x0, "h": 222, "fill": "@DIRTY_WHITE",
              "label": "Business functions in one system (BRD scope)", "label_w": 360},
             {"x": x0, "y": 224, "w": pf.CANVAS_W - x0, "h": 98, "fill": "@BG_BLUE",
              "label": "Shared capabilities every function uses", "label_w": 360}]
    return {"bands": bands, "nodes": nodes, "edges": edges,
            "legend": [["external", "External party"], ["group", "Business function group"],
                       ["service", "Shared capability"], ["parked", "Interface still to be specified"]]}


def value_chain_spec(m: dict) -> dict:
    nodes, edges = [], []
    w, h, gx = 136.0, 120.0, 146.6
    stages = m["stages"]
    for i, st in enumerate(stages):
        row, k = divmod(i, 6)
        col = k if row == 0 else 5 - k
        nodes.append({"id": f"v{i}", "x": 2 + col * gx, "y": 14 + row * 172, "w": w, "h": h, "text": st["text"],
                      "sub": [st["owner"], "To-Be: " + st["tobe"]], "kind": "stage", "size": 12.5})
    for i in range(len(stages) - 1):
        edges.append({"a": f"v{i}", "b": f"v{i + 1}", "width": 2.0})
    return {"nodes": nodes, "edges": edges,
            "legend": [["stage", "Stage: BDOI owner and the To-Be of the BRDs"]]}


def holistic_figures(h: dict, areas: list[dict]) -> dict[str, Path]:
    by_key = {a["key"]: a for a in areas}
    specs = {"asis_map": asis_map_spec(h["asis_map"], by_key),
             "envisioned_map": envisioned_map_spec(h["envisioned_map"]),
             "value_chain": value_chain_spec(h["value_chain"])}
    out = {key: render(f"holistic_{key}", pf.free_layout(f"holistic_{key}", spec, h[key]["caption"]))
           for key, spec in specs.items()}
    for key in ("e2e_1", "e2e_2"):
        out[key] = render(f"holistic_{key}", pf.swimlane(f"holistic_{key}", h[key], "e2e", h[key]["caption"]))
    return out


# ====================================================================== build

AREA_KEYS = ("id", "key", "num", "title", "brd", "scope", "department", "roles", "figures", "session", "sources",
             "chevrons", "heard", "asis", "pains", "tobe", "after", "missed", "best")


def _check_swimlane(name: str, diagram: str, spec: dict, pains: set) -> list[str]:
    errs = []
    lanes = {ln["id"] for ln in spec["lanes"]}
    steps = {s["id"] for s in spec["steps"]}
    for s in spec["steps"]:
        if s["lane"] not in lanes:
            errs.append(f"{name} {diagram}: step {s['id']} in unknown lane {s['lane']}")
        if not isinstance(s["text"], str):
            errs.append(f"{name} {diagram}: step {s['id']} text is not a string")
        for p in s.get("pain", []) or []:
            if p not in pains:
                errs.append(f"{name} {diagram}: step {s['id']} marks unknown pain point {p}")
    for e in spec.get("edges", []):
        for end in e[:2]:
            if end not in steps:
                errs.append(f"{name} {diagram}: edge {e} names unknown step {end}")
    return errs


def validate(a: dict, name: str) -> list[str]:
    """Checks one area file: keys, cross-references and text cells (YAML turns 'a: b' or 'x?' into odd types)."""
    errs = [f"{name}: missing key {k}" for k in AREA_KEYS if k not in a]
    if errs:
        return errs
    pains = {p["n"] for p in a["pains"]}
    diagrams = [("asis", a["asis"]), ("tobe", a["tobe"])]
    diagrams += [(f"asis_{k + 2}", x) for k, x in enumerate(a.get("asis_extra", []) or [])]
    for diagram, spec in diagrams:
        errs += _check_swimlane(name, diagram, spec, pains)
    marked = {p for d, spec in diagrams if d.startswith("asis") for s in spec["steps"] for p in (s.get("pain") or [])}
    errs += [f"{name}: pain point {p} is not marked on an As-Is diagram" for p in sorted(pains - marked)]
    errs += [f"{name}: before / after row {r['n']} has no pain point" for r in a["after"] if r["n"] not in pains]
    for h in a["heard"]:
        if h.get("kind") not in HEARD or not all(isinstance(h.get(k), str) for k in ("ref", "topic", "asis")):
            errs.append(f"{name}: floor-walk note {h.get('ref')} is incomplete")
        if h.get("want") is not None and not isinstance(h["want"], str):
            errs.append(f"{name}: floor-walk note {h.get('ref')} want is not text")
    for g in a["missed"]:
        for k in ("cov", "item", "seen", "brd", "rec", "ref"):
            if not isinstance(g.get(k), str):
                errs.append(f"{name}: not-covered item {g.get('item')} field {k} is not text")
        if g.get("cov") not in COVERAGE:
            errs.append(f"{name}: not-covered item {g.get('item')} has unknown coverage {g.get('cov')}")
    for b in a["best"]:
        if b.get("status") not in COVERAGE or not all(isinstance(b.get(k), str) for k in ("practice", "what", "rec")):
            errs.append(f"{name}: best practice {b.get('practice')} is incomplete")
    return errs


def load_areas() -> list[dict]:
    areas, errs = [], []
    for p in sorted((DATA / "areas").glob("*.yaml")):
        try:
            a = load(p)
        except yaml.YAMLError as exc:
            errs.append(f"{p.name}: {exc}")
            continue
        errs += validate(a, p.name)
        areas.append(a)
    if errs:
        raise SystemExit("Content errors:\n  " + "\n  ".join(errs))
    return sorted(areas, key=lambda a: a["num"])


def register_counts(areas: list[dict]) -> list[list]:
    reg = load(ROOT / "docs/deliverables/src/registers/discrepancy_register.yaml")
    rows = []
    for a in areas:
        if not a["brd"].startswith("BRD-"):
            continue
        key = "BRD-%02d" % int(a["brd"].split("-")[1])
        items = [i for i in reg["items"] if key in i["brds"]]
        rows.append([_short(a), a["brd"], sum(i["sev"] == "High" for i in items),
                     sum(i["sev"] == "Medium" for i in items), sum(i["sev"] == "Low" for i in items),
                     sum(i["status"] == "Open" for i in items), sum(i["status"] != "Open" for i in items)])
    return rows


def area_chapter(deck: ProcessDeck, a: dict, figs: dict[str, Path]) -> None:
    deck.glance(a)
    deck.heard(a)
    deck.diagram(f"{a['num']}. {_short(a)}: As-Is process" + (" (1/%d)" % (1 + len(a.get("asis_extra") or []))
                                                             if a.get("asis_extra") else ""),
                 figs["asis"], a["asis"]["caption"], a["asis"]["notes"])
    extras = a.get("asis_extra") or []
    for k, extra in enumerate(extras):
        deck.diagram(f"{a['num']}. {_short(a)}: As-Is process ({k + 2}/{len(extras) + 1})", figs[f"asis_{k + 2}"],
                     extra["caption"], extra["notes"])
    if a.get("variants"):
        v = a["variants"]
        deck.table_slide(f"{a['num']}. {_short(a)}: {v['title']}", v, per_page=v.get("per_page", 6))
    deck.pain_legend(a)
    deck.diagram(f"{a['num']}. {_short(a)}: To-Be in the BRD", figs["tobe"], a["tobe"]["caption"],
                 a["tobe"]["notes"])
    deck.before_after(a)
    deck.missed(a)
    deck.best_practice(a)


def build(previews: str | None = None, figures_only: bool = False) -> Path:
    d = load(DATA / "deck.yaml")
    areas = load_areas()
    by_key = {a["key"]: a for a in areas}
    hol = holistic_figures(d["holistic"], areas)
    figs = {a["id"]: area_figures(a) for a in areas}
    if figures_only:
        return FIG

    deck = ProcessDeck(d["title"], version=VERSION, date=d["date"], subtitle=d["subtitle"])
    deck.title()
    deck.notes(d["notes"]["title"])

    # ---- opening: purpose, sources, floor-walk coverage, agenda, reading guide
    u = d["understanding"]
    deck.cards(u["purpose"]["title"], u["purpose"]["cards"], cols=3, notes=u["purpose"]["notes"],
               intro=u["purpose"]["intro"], size=15, head_size=15)
    deck.cards(u["summary"]["title"], u["summary"]["cards"], cols=3, notes=u["summary"]["notes"], size=14)
    deck.stats(u["sources"]["title"], u["sources"]["tiles"], u["sources"]["notes"], foot=u["sources"]["foot"])
    deck.table_slide(u["schedule"]["title"], u["schedule"], per_page=9)
    tiles = [{"num": "0", "title": "Our understanding of BDOI, end to end", "sub": "Holistic view",
              "highlight": True}]
    for p in d["parts"]:
        for k in p["areas"]:
            a = by_key[k]
            tiles.append({"num": str(a["num"]), "title": a["title"], "sub": f"Part {p['num']} | {a['brd']}"})
    for t in d["closing_tiles"]:
        tiles.append({**t, "highlight": True})
    deck.agenda("Agenda", tiles)
    deck.notes(d["notes"]["agenda"])
    deck.content("How to read this deck", d["reading"], size=15)
    deck.notes(d["notes"]["reading"])

    # ---- chapter 0: holistic
    h = d["holistic"]
    deck.section("0. Our understanding of BDOI", h["subtitle"])
    deck.notes(h["notes"])
    deck.stats(h["glance"]["title"], h["glance"]["tiles"], h["glance"]["notes"], foot=h["glance"].get("foot", ""))
    deck.cards(h["org"]["title"], h["org"]["cards"], cols=4, notes=h["org"]["notes"], size=11, head_size=12)
    deck.lifecycle(h["lifecycle"]["title"], h["lifecycle"])
    deck.diagram(h["e2e_1"]["title"], hol["e2e_1"], h["e2e_1"]["caption"], h["e2e_1"]["notes"])
    deck.diagram(h["e2e_2"]["title"], hol["e2e_2"], h["e2e_2"]["caption"], h["e2e_2"]["notes"])
    deck.diagram(h["asis_map"]["title"], hol["asis_map"], h["asis_map"]["caption"], h["asis_map"]["notes"])
    deck.refs_figure(h["refs"]["title"], h["refs"])
    deck.table_slide(h["handoffs"]["title"], h["handoffs"], per_page=7)
    deck.cards(h["challenges"]["title"], h["challenges"]["cards"], cols=2, notes=h["challenges"]["notes"])
    deck.diagram(h["envisioned_map"]["title"], hol["envisioned_map"], h["envisioned_map"]["caption"],
                 h["envisioned_map"]["notes"])
    deck.diagram(h["value_chain"]["title"], hol["value_chain"], h["value_chain"]["caption"],
                 h["value_chain"]["notes"])
    deck.maturity(h["maturity"]["title"], h["maturity"], h["maturity"]["notes"])

    # ---- department parts
    for p in d["parts"]:
        p = {**p, "chapters": [{"num": by_key[k]["num"], "title": by_key[k]["title"],
                                "sub": by_key[k]["brd"]} for k in p["areas"]]}
        deck.part(p)
        for k in p["areas"]:
            area_chapter(deck, by_key[k], figs[by_key[k]["id"]])

    # ---- cross-cutting: data migration, integrations, shared capabilities
    x = d["crosscut"]
    deck.part({**x["part"], "chapters": x["part"]["chapters"]})
    dm = x["dm"]
    deck.cards(dm["context"]["title"], dm["context"]["cards"], cols=3, notes=dm["context"]["notes"],
               intro=dm["context"].get("intro", ""))
    deck.table_slide(dm["sources"]["title"], dm["sources"], per_page=6)
    deck.table_slide(dm["objects"]["title"], dm["objects"], per_page=8)
    deck.lifecycle(dm["approach"]["title"], dm["approach"])
    deck.cards(dm["challenges"]["title"], dm["challenges"]["cards"], cols=3, notes=dm["challenges"]["notes"],
               size=11)
    deck.missed(None, title=dm["missed"]["title"], rows_in=dm["missed"]["rows"], notes=dm["missed"]["notes"])
    deck.best_practice({"num": dm["num"], "title": dm["short"], "best": dm["best"], "notes": dm.get("notes", {})})
    it = x["integration"]
    deck.cards(it["context"]["title"], it["context"]["cards"], cols=3, notes=it["context"]["notes"],
               intro=it["context"].get("intro", ""))
    deck.table_slide(it["inventory"]["title"], it["inventory"], per_page=7, size=10)
    deck.cards(it["challenges"]["title"], it["challenges"]["cards"], cols=3, notes=it["challenges"]["notes"],
               size=11)
    deck.missed(None, title=it["missed"]["title"], rows_in=it["missed"]["rows"], notes=it["missed"]["notes"])
    deck.best_practice({"num": it["num"], "title": it["short"], "best": it["best"], "notes": it.get("notes", {})})
    sc = x["shared"]
    deck.table_slide(sc["title"], sc, per_page=7)

    # ---- roadmap
    r = d["roadmap"]
    deck.part(r["part"])
    deck.principles(r["principles"]["title"], r["principles"]["items"], r["principles"]["notes"])
    deck.table_slide(r["automation"]["title"], r["automation"], fill_col=4, fill_map=LEVEL, per_page=7)
    deck.horizons(r["horizons"]["title"], r["horizons"])
    deck.table_slide(r["kpis"]["title"], r["kpis"], per_page=8)
    deck.maturity(r["maturity_title"], h["maturity"], r["maturity_notes"])
    deck.table_slide(r["top_missed"]["title"], r["top_missed"], fill_col=0, fill_map=SEVERITY, per_page=7)
    deck.heat_counts(r["register"]["title"], register_counts(areas), r["register"]["text"])
    deck.table_slide(r["open_points"]["title"], r["open_points"], per_page=9)
    deck.content(r["ask"]["title"], r["ask"]["items"], size=18)
    deck.notes(r["ask"]["notes"])

    out = deck.save(OUT)
    print(f"{out} ({deck._n} slides)")
    if previews:
        import render as rnd

        # previews are never written into the repository: they go to a scratch folder
        pdf = rnd.to_pdf(out, outdir=Path(previews))
        sheets = rnd.previews(pdf, dpi=60)
        print(f"previews: {sheets[0].parent if sheets else '-'}")
    return out


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--previews", nargs="?", const=str(Path(tempfile.gettempdir()) / "bibs_process_deck"),
                    help="render slide PNGs and contact sheets into this folder (default: a temp folder)")
    ap.add_argument("--figures-only", action="store_true", help="only generate and render the figures")
    args = ap.parse_args()
    build(previews=args.previews, figures_only=args.figures_only)
    return 0


if __name__ == "__main__":
    sys.exit(main())
