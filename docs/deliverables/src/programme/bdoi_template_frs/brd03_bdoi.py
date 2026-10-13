"""BDOI view of the Product Maintenance FRS v1.3 (product owner direction of 11/12-Oct-2026), as done for BRD-11 v1.4.

The FRS revolves around three sources only: the Product Maintenance BRD (with BRD-00 and the programme NFRs), BDOI's
own FRS and the iNXT BrokerVerse OOTB platform. BIBS gives the full-page screenshots only, so that the reader can
picture the functions and the flows. The data is in brd03_v13_bdoi.yaml:

  * the screenshot of each screen of Annex G (whole window, header and menu, DEV environment, realistic fictitious
    data; one per figure, each with its caption on one page: a wide screen on a landscape page);
  * the business walkthroughs of Annex K with fictitious people, each step illustrated by its full-page screenshot;
  * the phrases of the added text that still described the build (rewritten) or only described it (dropped).

The page helpers reuse the section handling of brd11_v14.py (portrait / landscape sections).
"""
from __future__ import annotations

import math
import sys
from pathlib import Path

import yaml
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import brd11_v14 as u  # noqa: E402  (sect_par, landscape_page, page_size, drop_breaks_after_sections)
import brd_common_final as final  # noqa: E402

BDOI = yaml.safe_load((HERE / "brd03_v13_bdoi.yaml").read_text(encoding="utf-8"))
SCREENS = HERE / BDOI["screens_dir"]
CORE = None


def bind(core) -> None:
    global CORE
    CORE = core


def shot(slug: str) -> Path:
    png = SCREENS / f"{slug}.png"
    if not png.exists():
        raise SystemExit(f"full-page screenshot missing: {png}")
    return png


def rewrite(s: str) -> str:
    for old, new in BDOI.get("replace") or []:
        s = s.replace(old, new)
    return s


def is_wide(png: Path) -> bool:
    """A wide screen gets a landscape page; BDOI['portrait_screens'] stay portrait (scaled to the page width)."""
    from PIL import Image  # noqa: PLC0415
    if png.stem in (BDOI.get("portrait_screens") or []):
        return False
    with Image.open(png) as im:
        w, h = im.size
    return w / h > 1.15


def _ppr(el):
    ppr = el.find(qn("w:pPr"))
    if ppr is None:
        ppr = OxmlElement("w:pPr")
        el.insert(0, ppr)
    return ppr


def _picture(b, png: Path, max_w: float, max_h: float):
    pic = final.bordered(b.picture(png, max_w=max_w, max_h=max_h))
    ppr = _ppr(pic)
    for tag in ("w:keepNext", "w:keepLines"):
        if ppr.find(qn(tag)) is None:
            CORE.ppr_insert(ppr, OxmlElement(tag))
    return pic


def _caption(b, text: str):
    p = b.para(text, italic=True, size=8, jc="center", space_after=60)
    sp = p.find(f"{qn('w:pPr')}/{qn('w:spacing')}")
    sp.set(qn("w:before"), "60")
    return p


def figure_with_intro(b, model_sect, intro: list, png: Path, caption: str) -> list:
    """A screen section whose heading and introduction share the page of its screenshot: the picture is scaled to the
    room left under the introduction (a wide screen on a landscape page of its own), so the figure and its caption
    are never split and the introduction never ends alone on a page."""
    wide = is_wide(png)
    w, h = u.page_size(model_sect, wide)
    used = 0.0
    for k, el in enumerate(intro):
        text = "".join(t.text or "" for t in el.iter(qn("w:t")))
        used += 0.55 if k == 0 else 0.14 + 0.19 * max(1, math.ceil(len(text) * 0.078 / w))
        ppr = _ppr(el)
        if ppr.find(qn("w:keepNext")) is None:
            CORE.ppr_insert(ppr, OxmlElement("w:keepNext"))
    pic = _picture(b, png, w - 0.2, h - 0.9 - used - (0 if wide else 0.7))
    cap = _caption(b, caption)
    b.stats["page_figures"] += 1
    if wide:
        return u.landscape_page(model_sect, intro + [pic, cap])
    return intro + [pic, cap]


def figure_run(b, model_sect, figures: list[tuple[Path, str]]) -> list:
    """Several page figures in a row, one per page: consecutive wide screens share one landscape section (a page
    each); a tall screen starts a new portrait page."""
    out: list = []
    group: list = []

    def close_group():
        if group:
            out.extend(u.landscape_page(model_sect, list(group)))
            group.clear()

    after_landscape = False
    for png, caption in figures:
        wide = is_wide(png)
        w, h = u.page_size(model_sect, wide)
        pic = _picture(b, png, w - 0.2, h - 0.8)
        cap = _caption(b, caption)
        b.stats["page_figures"] += 1
        if wide:
            if group:
                CORE.ppr_insert(_ppr(pic), OxmlElement("w:pageBreakBefore"))
            group.extend([pic, cap])
            after_landscape = True
        else:
            close_group()
            if not after_landscape:
                CORE.ppr_insert(_ppr(pic), OxmlElement("w:pageBreakBefore"))
            after_landscape = False
            out.extend([pic, cap])
    close_group()
    return out


def drop_breaks_after_sections(b) -> int:
    return u.drop_breaks_after_sections(b)


def apply_docx(b, protected: set[str]) -> dict:
    """The phrases of brd03_v13_bdoi.yaml in the added text (BDOI's paragraphs never touched); drop list removed."""
    stats = {"pm_view_rewritten": 0, "pm_view_dropped": 0}
    for p in list(b.doc.element.body.iter(qn("w:p"))):
        if p.getparent() is None:
            continue
        s = "".join(t.text or "" for t in p.iter(qn("w:t")))
        if not s.strip() or s in protected:
            continue
        if any(d in s for d in BDOI.get("drop") or []):
            tr = next((x for x in p.iterancestors() if x.tag == qn("w:tr")), None)
            target = tr if tr is not None else p
            if target.getparent() is not None:
                target.getparent().remove(target)
                stats["pm_view_dropped"] += 1
            continue
        new = rewrite(s)
        if new == s:
            continue
        runs = list(p.iter(qn("w:t")))
        for t in runs:
            t.text = rewrite(t.text or "")
        if "".join(t.text or "" for t in runs) != new:
            runs[0].text = new
            runs[0].set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
            for t in runs[1:]:
                t.text = ""
        stats["pm_view_rewritten"] += 1
    return stats
