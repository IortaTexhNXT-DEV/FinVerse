"""Shared colour code of the BDOI-template FRS packs (User Access Maintenance, Renewal, Product Maintenance, Cashiering).

Rules of the product owner (11-Oct-2026). OOTB is the iNXT BrokerVerse OOTB platform (repo IortaTexhNXT-DEV/BDOI-OOTB,
branch brokerverse-platform), tested in its code, with the Feature List vs OOTB workbook (column "OOTB platform") as a
cross-check; it is NOT the BIBS build of this repository.

  black  - in the BRD (or a programme-wide NFR): in scope; gaps against OOTB are delivery work, not change requests;
  green  - not in the BRD, from BDOI's FRS (or the project), offered by iNXT BrokerVerse OOTB: no cost;
  orange - not in the BRD and not (or only partly) in OOTB: change request with effort (partial: the missing part);
  grey   - label "[Integration scope]" (EIAM / Entra ID SSO, UIDM-ISC, AD / LDAP, any interface) or
           "[Data Migration scope]" (BRD-13): not a change request; listed separately;
  blue   - best practice in no BRD, FRS or OOTB: separate table with indicative effort (recommendation only).

Helpers for the builders:
  COLOURS, LABELS, OOTB_HEADER        constants (hex colours without '#', grey labels, Fit-Gap column header)
  colour_run(r, hex), colour_par(p, hex)   colour a w:r / every text run of a w:p
  add_label(b, p, kind)               append the grey label run ("integration" / "migration") to a w:p
  legend_rows(annex)                  the five rows of the legend table, with the pack's annex references
  legend_table(b, rows, size)         the legend table (via b.table) with the colour and label cells coloured
"""
from __future__ import annotations

from docx.oxml import OxmlElement
from docx.oxml.ns import qn

COLOURS = {"black": "000000", "green": "2E7D32", "orange": "E65100", "grey": "616161", "blue": "1565C0"}
LABELS = {"integration": "[Integration scope]", "migration": "[Data Migration scope]"}
OOTB_HEADER = "What iNXT BrokerVerse offers out of the box"
OOTB_NAME = "iNXT BrokerVerse OOTB platform"

# children of w:rPr that come after w:color (schema order)
RPR_AFTER_COLOR = ["spacing", "w", "kern", "position", "sz", "szCs", "highlight", "u", "effect", "bdr", "shd",
                   "fitText", "vertAlign", "rtl", "cs", "em", "lang", "eastAsianLayout", "specVanish", "oMath"]


def colour_run(r, hexcol: str):
    rpr = r.find(qn("w:rPr"))
    if rpr is None:
        rpr = OxmlElement("w:rPr")
        r.insert(0, rpr)
    for c in rpr.findall(qn("w:color")):
        rpr.remove(c)
    c = OxmlElement("w:color")
    c.set(qn("w:val"), hexcol)
    for child in rpr:
        if child.tag.split("}")[1] in RPR_AFTER_COLOR:
            child.addprevious(c)
            return
    rpr.append(c)


def colour_par(p, hexcol: str) -> None:
    for r in p.iter(qn("w:r")):
        if r.find(qn("w:t")) is not None:
            colour_run(r, hexcol)


def add_label(b, p, kind: str) -> None:
    """Adds the grey label ([Integration scope] or the data-migration one) as a short paragraph after p; BDOI text untouched."""
    import copy  # noqa: PLC0415
    lab = OxmlElement("w:p")
    ppr = p.find(qn("w:pPr"))
    if ppr is not None:
        ppr = copy.deepcopy(ppr)
        for tag in ("w:numPr", "w:sectPr"):
            for el in ppr.findall(qn(tag)):
                ppr.remove(el)
        lab.append(ppr)
    lab.append(b.run(LABELS[kind], bold=True, size=8, color=COLOURS["grey"]))
    p.addnext(lab)


def legend_rows(annex: dict) -> list[list[str]]:
    """annex: {'ootb': 'Annex AA.3', 'cr': 'Annex AA.2', 'deliver': 'Annex AA.4', 'scope': 'Annex AA.5',
    'bp': 'Annex AA.6', 'nfr': 'Annex X.2'} - the places of the pack where each list is."""
    return [
        ["Black", "In the BRD - in scope", f"A requirement of the BRD, or a programme-wide non-functional requirement of another BRD or addendum ({annex.get('nfr', 'Annex X')}). Where the {OOTB_NAME} does not meet it in full, the work is in scope ({annex['deliver']}).", "In scope."],
        ["Green", "Not in the BRD - offered by iNXT BrokerVerse OOTB", f"Not asked by any BRD; added by BDOI's FRS or the project, and offered by the {OOTB_NAME}, usually behind a setting BDOI confirms. Listed in {annex['ootb']}.", "No change request, no cost."],
        ["Orange", "Change request", f"Not asked by any BRD and not offered by the {OOTB_NAME}, or only in part (effort for the missing part). A change request with its effort ({annex['cr']} and the CR Effort Estimate workbook), also when already available for BDOI in the BIBS system.", "Delivered only when BDOI approves the change request."],
        ["Grey", "Label [Integration scope] or [Data Migration scope]", f"An interface with an external system (Microsoft Entra ID / EIAM sign-in, UIDM-ISC provisioning, directory, any other interface), or a data migration item ({LABELS['migration']}, BRD-13). Covered by those workstreams ({annex['scope']}).", "Not a change request; no cost here."],
        ["Blue", "Best-practice recommendation", f"An insurance-broking practice in no BRD, not in BDOI's FRS and not in the {OOTB_NAME}, in a separate table ({annex['bp']}).", "Recommendation only; indicative effort."],
    ]


def _text(el) -> str:
    return "".join(t.text or "" for t in el.iter(qn("w:t")))


def legend_table(b, rows: list[list[str]], widths=(0.8, 1.9, 3.5, 1.2), size=8.5):
    """The legend table: b.table(header, rows, widths, size=...) of the pack's builder, then the colour cells coloured
    (green, orange, blue; the grey label) and the first column bold."""
    tbl = b.table(["Colour", "Meaning", "What it covers", "Cost"], rows, list(widths), size=size)
    cols = {"Green": COLOURS["green"], "Orange": COLOURS["orange"], "Blue": COLOURS["blue"],
            "Grey": COLOURS["grey"]}
    for tr in tbl.findall(qn("w:tr"))[1:]:
        tcs = tr.findall(qn("w:tc"))
        name = _text(tcs[0]).strip()
        if name in cols:
            for tc in tcs[:2]:
                for p in tc.iter(qn("w:p")):
                    colour_par(p, cols[name])
        for p in tcs[0].iter(qn("w:p")):
            for r in p.iter(qn("w:r")):
                rpr = r.find(qn("w:rPr"))
                if rpr is not None and rpr.find(qn("w:b")) is None:
                    rpr.insert(1, OxmlElement("w:b"))
    return tbl
