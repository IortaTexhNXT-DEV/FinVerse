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
    tbl = cc.legend_table(b, cc.legend_rows(ANNEX))
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
        "data migration are not change requests (Annex AA.5). Each is a candidate change request for BDOI's decision, also when it was "
        "already available for BDOI in the BIBS system (column Prepared for BIBS?). The effort is in man-days of 8 hours, estimated "
        "against the OOTB platform (for a Partial feature, the missing part only) with a margin of plus or minus 20%. "
        "The detail by "
        "activity, the assumptions and the cost (at the rate BDOI enters) are in the CR Effort Estimate workbook of "
        "BRD-11; the status of each request is kept in the Change Request Register.")
    rows = []
    for c in V14["crs"]:
        rows.append([c["id"], c["title"], c["frs"], c["fl"], " ".join(c["why"].split()), c["built"].split(" (")[0],
                     c["priority"], fmt(days(c["effort"]))])
    rows.append(["Total", f"{len(V14['crs'])} change requests", "", "", "", "", "", fmt(cr_total())])
    a.table(["CR No.", "Change", "FRS reference", "Feature List / OOTB", "Why it is needed", "Prepared for BIBS?",
             "Priority", "Man-days"], rows, [0.75, 1.05, 0.95, 1.05, 1.95, 0.8, 0.6, 0.6], size=7.5)
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
        "are part of the scope, not change requests, and are delivered before UAT; most are already available for BDOI "
        "in the BIBS system. The effort is given for planning only.")
    rows = [[d["id"], d["title"], d["ref"], d["fl"], d["scope"], fmt(days(d["effort"]))] for d in V14["brd_to_deliver"]]
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
        "those workstreams: they are neither change requests nor counted in the efforts of Annex AA.2 and AA.4.")
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
                gaps=sum(1 for d in V14["brd_to_deliver"] if days(d["effort"]) > 0), days=fmt(cr_total()))
    return [p.format(**vals) for p in V14["change_points"]]
