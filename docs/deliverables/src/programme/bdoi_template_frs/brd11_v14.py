"""Version 1.4 of the User Access Maintenance FRS in BDOI's template (product owner review of v1.3).

Used by build_brd11_bdoi_frs.py and build_brd11_workbooks.py; the data is in brd11_v14.yaml:

- the legend "How to read this document" in the front matter and the colour code of the requirements (black: in the
  BRD; green: not in the BRD, offered by the iNXT BrokerVerse OOTB platform per the Feature List vs OOTB; orange: change
  request), applied where each point is stated - the build stops when a point is not found;
- the simplified and end-to-end process flows (pictures of flows/brd11, rendered by flows/brd11/render.sh), each on
  a landscape page of its own;
- Annex X.2 (programme-wide non-functional rows of the other BRDs and addenda) and Annexes AA.2 to AA.4 (change
  requests with their effort, the points offered by the OOTB platform, the in-scope items to deliver);
- the shading of the v1.4 changes in the review copy.
"""
from __future__ import annotations

import copy
from pathlib import Path

import yaml
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

import colour_code as cc

HERE = Path(__file__).resolve().parent
V14 = yaml.safe_load((HERE / "brd11_v14.yaml").read_text(encoding="utf-8"))
ANNEX = {"nfr": "Annex X.2", "cr": "Annex AA.2", "ootb": "Annex AA.3", "deliver": "Annex AA.4", "scope": "Annex AA.5",
         "bp": "Annex AA.6"}
COLOURS = cc.COLOURS
ACTIVITIES = ["Analysis and design", "Backend", "Frontend", "Testing", "Documentation and UAT support"]


def days(effort) -> float:
    return round(sum(float(x) for x in effort), 2)


def cr_total() -> float:
    return round(sum(days(c["effort"]) for c in V14["crs"]), 2)


def deliver_total() -> float:
    return round(sum(days(d["effort"]) for d in V14["brd_to_deliver"]), 2)


def fmt(x: float) -> str:
    return f"{x:g}"


# ------------------------------------------------------------------------------------------------- colour
colour_run = cc.colour_run
colour_par = cc.colour_par


def _text(el) -> str:
    return "".join(t.text or "" for t in el.iter(qn("w:t")))


def apply_colours(b, fr_blocks: dict) -> dict:
    """Colours every point of V14['points']; returns {ref: paragraphs coloured}. Stops when a point is not found."""
    body = b.doc.element.body
    paras = list(body.iter(qn("w:p")))
    texts = [_text(p) for p in paras]
    done: dict[str, int] = {}
    missing = []
    for pt in V14["points"]:
        label = pt.get("label")
        col = None if label else COLOURS[pt["colour"]]
        n = 0
        first = True
        for item in pt.get("items") or []:
            block = fr_blocks.get(item)
            if not block:
                missing.append(f"{pt['ref']}: {item}")
                continue
            for el in block:
                for p in ([el] if el.tag == qn("w:p") else list(el.iter(qn("w:p")))):
                    if label:
                        if _text(p).strip():
                            cc.add_label(b, p, label)
                            n += 1
                            break
                        continue
                    colour_par(p, col)
                    n += 1
                if label and n:
                    break
        for t in pt.get("text") or []:
            hit = [p for p, s in zip(paras, texts) if t in s]
            if not hit:
                missing.append(f"{pt['ref']}: '{t}'")
            for p in hit:
                if label:
                    if first:
                        cc.add_label(b, p, label)
                        n += 1
                    first = False
                    break
                colour_par(p, col)
                n += 1
        done[pt["ref"]] = n
    if missing:
        raise SystemExit("colour code: points not found:\n  " + "\n  ".join(missing))
    b.stats["coloured"] = sum(done.values())
    return done


def shade_changed(b, shd_el) -> int:
    """In the review copy: the paragraphs whose text was changed in version 1.4 in the data of earlier versions."""
    n = 0
    for p in b.doc.element.body.iter(qn("w:p")):
        s = _text(p)
        if any(t in s for t in V14["changed_texts"]):
            for r in p.iter(qn("w:r")):
                if r.find(qn("w:t")) is None:
                    continue
                rpr = r.find(qn("w:rPr"))
                if rpr is None:
                    rpr = OxmlElement("w:rPr")
                    r.insert(0, rpr)
                if rpr.find(qn("w:shd")) is None:
                    after = [c for c in rpr if c.tag.split("}")[1] in ("fitText", "vertAlign", "rtl", "cs", "em",
                                                                       "lang", "eastAsianLayout", "specVanish")]
                    (after[0].addprevious(shd_el()) if after else rpr.append(shd_el()))
            n += 1
    return n


# ------------------------------------------------------------------------------------------------- legend
def legend_elements(b, heading_model) -> list:
    """'How to read this document' with the colour code (front matter, after the revision log)."""
    from build_brd11_bdoi_frs import strip_ids  # noqa: PLC0415
    lg = V14["legend"]
    head = strip_ids(copy.deepcopy(heading_model))
    for el in list(head):
        if el.tag != qn("w:pPr"):
            head.remove(el)
    head.append(b.run("How to read this document"))
    els = [head, b.para(lg["intro"], space_after=120)]
    rows = cc.legend_rows(ANNEX)
    rows[2][2] = rows[2][2].replace(", also when already available for BDOI in the BIBS system", "")
    rows[3][1] = rows[3][1].replace("Label [Integration scope]",
                                    f"Label [Integration scope], {cc.LABELS['integration_uidm']}")
    rows[3][2] += (f" {cc.LABELS['integration_uidm']} marks the request steps that BDOI's reviewers place in UIDM-ISC "
                   "(\"c/o UIDM\", Annex Q.6).")
    tbl = cc.legend_table(b, rows)
    els += [tbl, b.para("", space_after=0)]
    els.append(b.para("Other conventions: FRUM.nnn are the functional requirements of section 3; BRD IDs are those of "
                      "the User Access Maintenance BRD; CR-UA-nnn are the change requests of Annex AA.2, OB-UA-nn the "
                      "points offered by the iNXT BrokerVerse OOTB platform (Annex AA.3), BRD-UA-nn the BRD items to "
                      "deliver (Annex AA.4), IS-UA-nn / DM-UA-nn the integration and data migration items (Annex "
                      "AA.5) and BP-UA-nn the best-practice recommendations (Annex AA.6); UAM-nnn and the like are the rows of the Feature List vs OOTB workbook; Q-nn are the observations of Annex Q. Figures are "
                      "numbered in the order they appear; screenshots of Annex J are numbered J.n.", size=9,
                      space_after=120))
    return els


# ------------------------------------------------------------------------------------------------- pictures
def sect_par(model_sect, landscape: bool):
    """A paragraph that ends a section, with the page set-up of model_sect turned to the given orientation."""
    sect = copy.deepcopy(model_sect)
    for t in sect.findall(qn("w:titlePg")):
        sect.remove(t)
    pg = sect.find(qn("w:pgSz"))
    w, h = int(pg.get(qn("w:w"))), int(pg.get(qn("w:h")))
    pg.set(qn("w:w"), str(max(w, h) if landscape else min(w, h)))
    pg.set(qn("w:h"), str(min(w, h) if landscape else max(w, h)))
    if landscape:
        pg.set(qn("w:orient"), "landscape")
    elif qn("w:orient") in pg.attrib:
        del pg.attrib[qn("w:orient")]
    p = OxmlElement("w:p")
    ppr = OxmlElement("w:pPr")
    ppr.append(sect)
    p.append(ppr)
    return p


def governing_sect(anchor):
    """The sectPr of the section that holds anchor (the next section break in the body, or the body's last)."""
    el = anchor
    while el is not None:
        s = el.find(f"{qn('w:pPr')}/{qn('w:sectPr')}") if el.tag == qn("w:p") else None
        if s is not None:
            return s
        if el.tag == qn("w:sectPr"):
            return el
        el = el.getnext()
    return anchor.getparent().find(qn("w:sectPr"))


def landscape_page(model_sect, els: list) -> list:
    """els on a landscape page of their own: the portrait section is closed before them, the landscape after."""
    return [sect_par(model_sect, landscape=False)] + els + [sect_par(model_sect, landscape=True)]


def landscape_size(model_sect) -> tuple[float, float]:
    """Usable width and height (inches) of the landscape page made from model_sect."""
    pg, mar = model_sect.find(qn("w:pgSz")), model_sect.find(qn("w:pgMar"))
    w, h = int(pg.get(qn("w:w"))), int(pg.get(qn("w:h")))
    long_, short = max(w, h), min(w, h)
    m = {k: int(mar.get(qn(f"w:{k}")) or 0) for k in ("top", "bottom", "left", "right", "header", "footer")}
    width = (long_ - m["left"] - m["right"]) / 1440
    height = (short - m["top"] - m["bottom"]) / 1440
    return width, height


# ------------------------------------------------------------------------------------------------- annexes
def programme_nfr(a):
    """Annex X.2: the programme-wide non-functional rows of the other BRDs and addenda that apply to access."""
    rows = V14["programme_nfr"]
    a.h2("X.2 Programme-wide security requirements of the other BRDs and addenda")
    a.p("The Collections addendum (section 6) states the security rules of BDOI's applications; the Claims and "
        "ReInsurance addenda carry the same audit rule 4.03 and the Operations addendum the segregation of duties. They "
        "apply to User Access Maintenance as BRD requirements (black in this document). Each row gives where this FRS "
        "meets it and its status against the iNXT BrokerVerse OOTB platform (Feature List vs OOTB); a row to deliver is "
        "in scope and is listed in Annex AA.4, not as a change request.")
    a.table(["NFR (source)", "Requirement", "Where in this FRS", "Status"], rows, [1.2, 2.9, 1.5, 1.8], size=7.5)
    a.b.stats["programme_nfr"] = len(rows)


def colour_column(tbl, cols: tuple[int, ...], hexcol: str, skip_last: bool = False):
    """The cells of the given columns of a table (header row excepted) in a colour of the colour code."""
    rows = tbl.findall(qn("w:tr"))[1:]
    for tr in rows[:-1] if skip_last else rows:
        tcs = tr.findall(qn("w:tc"))
        for c in cols:
            for p in tcs[c].iter(qn("w:p")):
                colour_par(p, hexcol)


def last_table(a):
    return next(el for el in reversed(a.els) if el.tag == qn("w:tbl"))


def cr_annex(a):
    """Annexes AA.2 to AA.4: change requests with their effort, points offered by the OOTB platform, items to deliver."""
    b = a.b
    a.h2("AA.2 Change requests identified at version 1.4")
    a.p("The points of this FRS that are in no BRD and that the iNXT BrokerVerse OOTB platform does not offer (orange "
        "in this document), checked in the code of the platform (brokerverse-platform) with the Feature List vs "
        "OOTB as a cross-check: not available, only in part, or BDOI-specific. Interfaces with external systems and "
        "data migration are not change requests (Annex AA.5). Each is a candidate change request for BDOI's decision. The "
        "effort is in man-days of 8 hours, estimated "
        "against the OOTB platform (for a Partial feature, the missing part only) with a margin of plus or minus 20%. "
        "The detail by "
        "activity, the assumptions and the cost (at the rate BDOI enters) are in the CR Effort Estimate workbook of "
        "BRD-11; the status of each request is kept in the Change Request Register.")
    rows = []
    for c in V14["crs"]:
        rows.append([c["id"], c["title"], c["frs"], c["fl"], " ".join(c["why"].split()), c["priority"],
                     fmt(days(c["effort"]))])
    rows.append(["Total", f"{len(V14['crs'])} change requests", "", "", "", "", fmt(cr_total())])
    a.table(["CR No.", "Change", "FRS reference", "Feature List / OOTB", "Why it is needed", "Priority", "Man-days"],
            rows, [0.75, 1.15, 1.0, 1.2, 2.5, 0.6, 0.6], size=7.5)
    colour_column(last_table(a), (0, 1), COLOURS["orange"], skip_last=True)
    a.h2("AA.3 Beyond the BRD, offered by the iNXT BrokerVerse OOTB platform (no change request)")
    a.p("The points of this FRS that no BRD asks for and that the iNXT BrokerVerse OOTB platform offers, found in its "
        "code (column OOTB evidence) and Available in the Feature List vs OOTB (green in this document). They come at no cost; most sit "
        "behind a setting that BDOI confirms (Annex M.10). Two of them were candidate change requests in the "
        "register of version 1.3 and are closed there.")
    a.table(["No.", "Point", "FRS reference", "What BDOI gets", "Feature List / OOTB", "OOTB evidence (code)"],
            [[o[0], o[1], o[2], o[3], o[5], o[6]] for o in V14["ootb"]], [0.7, 1.1, 1.0, 2.2, 1.2, 1.6], size=7.5)
    colour_column(last_table(a), (0, 1), COLOURS["green"])
    a.h2("AA.4 BRD requirements to deliver (in scope, no change request)")
    a.p("Requirements of the BRDs (black in this document, Annex P and X.2) that the iNXT BrokerVerse OOTB platform "
        "does not meet in full: the matching feature of the Feature List vs OOTB is Not available or Partial. They "
        "are part of the scope, not change requests, and are delivered before UAT. The effort is given for planning only. "
        "The request steps that BDOI's reviewers place in UIDM-ISC (raise, correct, cancel and view a request, bulk "
        "requests, the approver's decision and the notices of those steps; Annex Q.6) are not delivered in the system: "
        "they are moved to Annex AA.5 (IS-UA-06 to IS-UA-09) and their effort is removed from this list.")
    rows = [[d["id"], d["title"], d["ref"], d["fl"], d["scope"], fmt(days(d["effort"]))] for d in V14["brd_to_deliver"]]
    rows.append(["Total", f"{len(rows)} items", "", "", "", fmt(deliver_total())])
    a.table(["No.", "Item", "BRD reference", "Feature List / OOTB", "What is delivered", "Man-days"], rows,
            [0.75, 1.3, 1.4, 1.1, 2.3, 0.6], size=7.5)
    a.p("Also BRD requirements, offered by the iNXT BrokerVerse OOTB platform (black in this document; in scope, no "
        "effort): points first listed as beyond the BRD that a BRD line asks for, read with BRD-00 and the "
        "programme-wide NFRs of the addenda (product owner decision of 11-Oct-2026). The OOTB platform offers them, "
        "so they are delivered without a change request and without effort.")
    a.table(["No.", "Point", "FRS reference", "BRD line", "What BDOI gets", "Feature List / OOTB"],
            [[o[0], o[1], o[2], o[3], o[4], o[5]] for o in V14["brd_ootb"]], [0.7, 1.0, 0.9, 2.0, 1.6, 1.2], size=7.5)
    a.h2("AA.5 Covered by the Integration and Data Migration scope (not change requests)")
    a.p("The points of this FRS that are interfaces with BDOI's external systems (Microsoft Entra ID / EIAM sign-in, "
        "UIDM-ISC provisioning, any other interface) or data migration (BRD-13). They are labelled "
        f"{cc.LABELS['integration']} or {cc.LABELS['migration']} in grey where they are stated and are delivered by "
        "those workstreams: they are neither change requests nor counted in the efforts of Annex AA.2 and AA.4. The "
        "request steps that BDOI's reviewers place in UIDM-ISC (\"c/o UIDM\", Annex Q.6) are labelled "
        f"{cc.LABELS['integration_uidm']} (IS-UA-06 to IS-UA-09).")
    a.table(["No.", "Point", "FRS reference", "Scope", "Workstream", "Feature List / OOTB", "Note"],
            V14["integration"], [0.7, 1.4, 1.0, 2.1, 0.8, 0.9, 0.7], size=7.5)
    lab = last_table(a)
    for tr in lab.findall(qn("w:tr"))[1:]:
        for p in tr.findall(qn("w:tc"))[0].iter(qn("w:p")):
            colour_par(p, COLOURS["grey"])
    a.h2("AA.6 Best-practice recommendations (not in the BRD, the FRS or OOTB)")
    a.p("Insurance-broking practices relevant to user access that are in no BRD, not in BDOI's FRS and not in the "
        "iNXT BrokerVerse OOTB platform (blue in this document). They are recommendations for BDOI's consideration, "
        "not commitments; the effort is indicative, in man-days, and is not part of the change request total.",
        color=COLOURS["blue"])
    a.table(["ID", "Practice", "Why it matters for a broker", "Priority", "Indicative man-days"],
            [[r[0], r[1], r[2], r[3], fmt(r[4])] for r in V14["best_practice"]], [0.75, 1.5, 3.6, 0.7, 0.9], size=7.5)
    colour_column(last_table(a), (0, 1, 2), COLOURS["blue"])
    b.stats["crs"] = len(V14["crs"])
    b.stats["ootb_items"] = len(V14["ootb"])
    b.stats["brd_ootb_items"] = len(V14["brd_ootb"])


def change_summary_points(e2e_no: int, nfr_rows: int) -> list[str]:
    vals = dict(green=len(V14["ootb"]), orange=len(V14["crs"]), e2e=e2e_no, nfr=nfr_rows,
                integration=len(V14["integration"]), bp=len(V14["best_practice"]),
                gaps=sum(1 for d in V14["brd_to_deliver"] if days(d["effort"]) > 0), days=fmt(cr_total()),
                deliver_days=fmt(deliver_total()))
    return [p.format(**vals) for p in V14["change_points"]]


# ------------------------------------------------------------------------------------------------- BDOI view
# Second product owner review of v1.4 (11-Oct-2026): the FRS revolves around the BRD, BDOI's FRS and the OOTB platform
# only; BIBS gives the full-page screenshots (brd11_v14_bdoi.yaml).
BDOI = yaml.safe_load((HERE / "brd11_v14_bdoi.yaml").read_text(encoding="utf-8"))
SCREENS = HERE / BDOI["screens_dir"]


def shot(slug: str) -> Path:
    png = SCREENS / f"{slug}.png"
    if not png.exists():
        raise SystemExit(f"full-page screenshot missing: {png}")
    return png


def _set_text(p, new: str) -> None:
    """The whole text of a paragraph replaced: kept in its first text run (its formatting), the other runs emptied."""
    runs = list(p.iter(qn("w:t")))
    if not runs:
        return
    runs[0].text = new
    runs[0].set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
    for t in runs[1:]:
        t.text = ""


def _rewrite(s: str) -> str:
    for old, rep in BDOI["replace"]:
        s = s.replace(old, rep)
    return s


def bdoi_view(b, protected: set[str]) -> dict:
    """Rewrites the phrases of the added text that described the BIBS build, and removes the paragraphs and table rows
    that only described it or its test tools. protected: paragraph texts of BDOI's FRS, never touched."""
    stats = {"replaced": 0, "dropped": 0}
    body = b.doc.element.body
    for p in list(body.iter(qn("w:p"))):
        if p.getparent() is None:
            continue
        s = _text(p)
        if not s.strip() or s in protected:
            continue
        if any(d in s for d in BDOI["drop"]):
            tr = next((x for x in p.iterancestors() if x.tag == qn("w:tr")), None)
            target = tr if tr is not None else p
            if target.getparent() is not None:
                target.getparent().remove(target)
                stats["dropped"] += 1
            continue
        new = _rewrite(s)
        if new == s:
            continue
        for t in p.iter(qn("w:t")):
            t.text = _rewrite(t.text or "")
        if _text(p) != new:
            _set_text(p, new)
        stats["replaced"] += 1
    return stats


def page_size(model_sect, landscape: bool) -> tuple[float, float]:
    """Usable width and height (inches) of a page of the section model_sect in the given orientation."""
    pg, mar = model_sect.find(qn("w:pgSz")), model_sect.find(qn("w:pgMar"))
    w, h = int(pg.get(qn("w:w"))), int(pg.get(qn("w:h")))
    long_, short = max(w, h), min(w, h)
    m = {k: int(mar.get(qn(f"w:{k}")) or 0) for k in ("top", "bottom", "left", "right", "header", "footer")}
    pw, ph = (long_, short) if landscape else (short, long_)
    top = max(m["top"], m["header"] + 360)
    bottom = max(m["bottom"], m["footer"] + 360)
    return (pw - m["left"] - m["right"]) / 1440, (ph - top - bottom) / 1440


def is_wide(png: Path) -> bool:
    """A wide screen gets a landscape page; the screens of BDOI["portrait_screens"] stay on a portrait page (scaled to
    its width) where a landscape section would leave the end of the text before it alone on a page."""
    from PIL import Image  # noqa: PLC0415
    if png.stem in (BDOI.get("portrait_screens") or []):
        return False
    with Image.open(png) as im:
        w, h = im.size
    return w / h > 1.15


def page_figure(b, model_sect, png: Path, caption: str, stats_key: str = "page_figures") -> list:
    """One screenshot with its caption, alone on a page and scaled to fit it: on a landscape page of its own when the
    screen is wide, else at the top of a new portrait page. The picture keeps with its caption, so a figure is never
    split or continued on the next page."""
    from build_brd11_bdoi_frs import ppr_insert  # noqa: PLC0415
    import brd_common_final as final  # noqa: PLC0415
    wide = is_wide(png)
    w, h = page_size(model_sect, wide)
    pic = final.bordered(b.picture(png, max_w=w - 0.2, max_h=h - 0.8))
    ppr = pic.find(qn("w:pPr"))
    ppr_insert(ppr, OxmlElement("w:keepNext"))
    ppr_insert(ppr, OxmlElement("w:keepLines"))
    if not wide:
        ppr_insert(ppr, OxmlElement("w:pageBreakBefore"))
    cap = b.para(caption, italic=True, size=8, jc="center", space_after=60, space_before=60)
    b.stats[stats_key] = b.stats.get(stats_key, 0) + 1
    if wide:
        return landscape_page(model_sect, [pic, cap])
    return [pic, cap]


def figure_run(b, model_sect, figures: list[tuple[Path, str]], first_break: bool = True) -> list:
    """Several page figures in a row: consecutive wide screens share one landscape section (a page each), so no empty
    page comes between them; a tall screen starts a new portrait page."""
    from build_brd11_bdoi_frs import ppr_insert  # noqa: PLC0415
    import brd_common_final as final  # noqa: PLC0415
    out: list = []
    group: list = []

    def close_group():
        if group:
            out.extend(landscape_page(model_sect, list(group)))
            group.clear()

    after_landscape = False
    for png, caption in figures:
        wide = is_wide(png)
        w, h = page_size(model_sect, wide)
        pic = final.bordered(b.picture(png, max_w=w - 0.2, max_h=h - 0.8))
        ppr = pic.find(qn("w:pPr"))
        ppr_insert(ppr, OxmlElement("w:keepNext"))
        ppr_insert(ppr, OxmlElement("w:keepLines"))
        cap = b.para(caption, italic=True, size=8, jc="center", space_after=60, space_before=60)
        b.stats["page_figures"] = b.stats.get("page_figures", 0) + 1
        if wide:
            if group:
                ppr_insert(ppr, OxmlElement("w:pageBreakBefore"))
            group.extend([pic, cap])
            after_landscape = True
        else:
            close_group()
            if (first_break or out) and not after_landscape:
                ppr_insert(ppr, OxmlElement("w:pageBreakBefore"))
            after_landscape = False
            out.extend([pic, cap])
    close_group()
    return out


def figure_with_intro(b, model_sect, intro: list, png: Path, caption: str) -> list:
    """A screen section whose heading and introduction share the page of its screenshot: the picture is scaled to the
    room left under the introduction, so that the introduction never ends alone on a page before the figure."""
    import math  # noqa: PLC0415
    from build_brd11_bdoi_frs import ppr_insert  # noqa: PLC0415
    import brd_common_final as final  # noqa: PLC0415
    wide = is_wide(png)
    w, h = page_size(model_sect, wide)
    used = 0.0
    for k, el in enumerate(intro):
        text = _text(el)
        if k == 0:
            used += 0.55
        else:
            used += 0.14 + 0.19 * max(1, math.ceil(len(text) * 0.078 / w))
        ppr = el.find(qn("w:pPr"))
        if ppr is None:
            ppr = OxmlElement("w:pPr")
            el.insert(0, ppr)
        if ppr.find(qn("w:keepNext")) is None:
            ppr_insert(ppr, OxmlElement("w:keepNext"))
    pic = final.bordered(b.picture(png, max_w=w - 0.2, max_h=h - 0.9 - used - (0 if wide else 0.7)))
    ppr = pic.find(qn("w:pPr"))
    ppr_insert(ppr, OxmlElement("w:keepNext"))
    ppr_insert(ppr, OxmlElement("w:keepLines"))
    cap = b.para(caption, italic=True, size=8, jc="center", space_after=60, space_before=60)
    b.stats["page_figures"] = b.stats.get("page_figures", 0) + 1
    if wide:
        return landscape_page(model_sect, intro + [pic, cap])
    return intro + [pic, cap]   # kept together: on the page where the text before ends, or on the next


def drop_breaks_after_sections(b) -> int:
    """A paragraph that follows a section break (which already starts a new page) loses its own page break, so that no
    blank page follows a landscape page."""
    n = 0
    for p in list(b.doc.element.body.iter(qn("w:p"))):
        ppr = p.find(qn("w:pPr"))
        if ppr is None or ppr.find(qn("w:sectPr")) is None or len(p.attrib):   # only the breaks this builder adds
            continue
        typ = ppr.find(f"{qn('w:sectPr')}/{qn('w:type')}")
        if typ is not None and typ.get(qn("w:val")) in ("continuous", "evenPage", "oddPage"):
            continue
        nxt = p.getnext()
        while nxt is not None and nxt.tag == qn("w:p") and not _text(nxt).strip() and \
                nxt.find(f"{qn('w:pPr')}/{qn('w:sectPr')}") is None and not list(nxt.iter(qn("w:drawing"))):
            nxt.getparent().remove(nxt)   # an empty paragraph at the top of the new section only pushes the text
            n += 1
            nxt = p.getnext()
        if nxt is None or nxt.tag != qn("w:p"):
            continue
        for pbb in nxt.findall(f"{qn('w:pPr')}/{qn('w:pageBreakBefore')}"):
            pbb.getparent().remove(pbb)
            n += 1
        for br in list(nxt.iter(qn("w:br"))):
            if br.get(qn("w:type")) == "page":
                br.getparent().remove(br)
                n += 1
                break
    return n


def user_access_report_annex(a):
    """Annex AC: the User Access Report sample layout asked by BDOI's reviewer (comment on BRD 3.003.1.1)."""
    u = BDOI["user_access_report"]
    a.h1("Annex AC – User Access Report: Fields and Sample Layout")
    a.p(" ".join(u["intro"].split()))
    a.table(u["header"], u["rows"], [0.4, 1.7, 1.6, 1.1, 2.6], size=7.5)
    a.p("Formats and file name: " + u["formats"])
    model = a.b.doc.sections[-1]._sectPr
    a.els.extend(figure_run(a.b, model, [(shot(u["shot"]), f"Figure AC.1: {u['caption']}")]))
