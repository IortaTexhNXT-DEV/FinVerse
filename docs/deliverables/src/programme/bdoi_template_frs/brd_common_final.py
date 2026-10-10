"""Final-document steps shared by the BDOI-format FRS builders (Cashiering, User Access Maintenance, Product
Maintenance, Renewal): the BDOI-format FRS is the final document and stands alone.

- ``reference_findings``: references to another FRS left in a .docx or .xlsx (FR numbers of the earlier FRS, "BIBS
  reference", "reference FRS", "BIBS FRS", "our FRS", "BIBS test plan"); the builders stop when one is found.
- ``populate_toc``: the table of contents written into the .docx itself from the headings and the page numbers
  LibreOffice lays out, so that Word does not have to update the fields on opening (the TOC field is kept, so Word can
  still refresh it).
- ``render_pages``: every page of a PDF as PNG, for the page-by-page review.

Command line:
  python3 brd_common_final.py scan FILE...           counts of the references found in each file
  python3 brd_common_final.py toc FILE.docx...       populates the table of contents of each document
  python3 brd_common_final.py render FILE.pdf DIR    writes DIR/page-NNN.png
"""
from __future__ import annotations

import copy
import re
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path

# References to another FRS that the final document does not contain
REFERENCE_PATTERNS = {
    "FR number of another FRS": r"\bFR-(?:OP|PM|UA|RN|NB)-\d{3}\b",
    "BIBS reference": r"\bBIBS reference\b",
    "reference FRS / requirement": r"\breference (?:FRS|FR|requirements?)\b",
    "BIBS FRS / our FRS": r"\b(?:BIBS FRS|our FRS|FRS v2\.\d)\b",
    "BIBS test plan": r"\bBIBS test plan\b",
    "incorporated from": r"\bincorporated from the BIBS\b",
}


def _texts(path: Path) -> list[str]:
    """The text of every part of a .docx or .xlsx, one string per XML part (tags removed)."""
    out = []
    with zipfile.ZipFile(path) as z:
        for name in z.namelist():
            if name.endswith(".xml") and (name.startswith("word/") or name.startswith("xl/")):
                xml = z.read(name).decode("utf-8", "ignore")
                xml = re.sub(r"</w:p>|</si>|</c>|<w:tab/>", "\n", xml)
                out.append(re.sub(r"<[^>]+>", "", xml))
    return out


def reference_findings(path: Path, context: int = 70) -> dict[str, list[str]]:
    """Each kind of reference to another FRS found in a document, with a short context per occurrence."""
    found: dict[str, list[str]] = {}
    for text in _texts(Path(path)):
        for kind, pat in REFERENCE_PATTERNS.items():
            for m in re.finditer(pat, text):
                snippet = text[max(0, m.start() - context):m.end() + context].replace("\n", " / ")
                found.setdefault(kind, []).append(snippet)
    return found


def reference_count(path: Path) -> int:
    return sum(len(v) for v in reference_findings(path).values())


def check_no_references(paths) -> None:
    """Stops the build when a document still refers to another FRS."""
    problems = []
    for p in paths:
        for kind, hits in reference_findings(Path(p)).items():
            problems.append(f"{Path(p).name}: {len(hits)} x {kind}, e.g. ...{hits[0]}...")
    if problems:
        raise SystemExit("references to another FRS left in the final documents:\n  " + "\n  ".join(problems))


# --------------------------------------------------------------------------------------------- LibreOffice
class _Office:
    """A private headless LibreOffice reached through UNO."""

    def __enter__(self):
        import uno  # noqa: PLC0415

        self.uno = uno
        self.profile = Path(tempfile.mkdtemp(prefix="frs_lo_"))
        self.pipe = f"frsfinal{int(time.time() * 1000)}"
        self.proc = subprocess.Popen(
            ["soffice", f"-env:UserInstallation=file://{self.profile}", "--headless", "--norestore", "--invisible",
             f"--accept=pipe,name={self.pipe};urp;"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        local = uno.getComponentContext()
        resolver = local.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", local)
        ctx = None
        for _ in range(120):
            try:
                ctx = resolver.resolve(f"uno:pipe,name={self.pipe};urp;StarOffice.ComponentContext")
                break
            except Exception:  # noqa: BLE001
                time.sleep(1)
        if ctx is None:
            raise RuntimeError("LibreOffice did not start")
        self.desktop = ctx.ServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
        return self

    def prop(self, name, value):
        from com.sun.star.beans import PropertyValue  # noqa: PLC0415

        p = PropertyValue()
        p.Name, p.Value = name, value
        return p

    def open(self, path: Path):
        return self.desktop.loadComponentFromURL(self.uno.systemPathToFileUrl(str(path)), "_blank", 0,
                                                 (self.prop("Hidden", True),))

    def __exit__(self, *exc):
        try:
            self.desktop.terminate()
        except Exception:  # noqa: BLE001
            pass
        try:
            self.proc.wait(timeout=30)
        except subprocess.TimeoutExpired:
            self.proc.kill()
        shutil.rmtree(self.profile, ignore_errors=True)


def toc_entries(path: Path) -> list[tuple[int, str, str]]:
    """(level, heading text, page) of every line of the first table of contents, as LibreOffice lays it out."""
    with _Office() as lo:
        model = lo.open(path)
        idx = model.getDocumentIndexes()
        entries = []
        for _ in range(2):
            for i in range(idx.getCount()):
                idx.getByIndex(i).update()
            model.refresh()
        for i in range(idx.getCount()):
            ix = idx.getByIndex(i)
            if not ix.supportsService("com.sun.star.text.ContentIndex"):
                continue
            en = ix.getAnchor().createEnumeration()
            while en.hasMoreElements():
                par = en.nextElement()
                text = par.getString()
                style = par.ParaStyleName or ""
                m = re.search(r"(\d+)\s*$", style)
                if not text.strip() or "\t" not in text:
                    continue
                head, page = text.rsplit("\t", 1)
                entries.append((int(m.group(1)) if m else 1, head.replace("\t", " ").strip(), page.strip()))
            break
        model.close(True)
    return entries


def populate_toc(path: Path) -> int:
    """Writes the table of contents of a .docx from LibreOffice's layout and clears the update-on-open flag.
    Returns the number of entries."""
    from docx import Document  # noqa: PLC0415
    from docx.oxml.ns import qn  # noqa: PLC0415

    path = Path(path)
    entries = toc_entries(path)
    if not entries:
        raise SystemExit(f"{path.name}: no table of contents laid out")
    doc = Document(str(path))
    body = doc.element.body
    first = next((p for p in body.iter(qn("w:p"))
                  if any(re.match(r"\s*TOC\b", t.text or "") for t in p.iter(qn("w:instrText")))), None)
    if first is None:
        raise SystemExit(f"{path.name}: no TOC field")
    # the paragraphs of the field: from its begin to the end that closes it
    paras, depth, started = [], 0, False
    p = first
    while p is not None:
        if p.tag == qn("w:p"):
            paras.append(p)
            for fc in p.iter(qn("w:fldChar")):
                kind = fc.get(qn("w:fldCharType"))
                if kind == "begin":
                    depth += 1
                    started = True
                elif kind == "end":
                    depth -= 1
            if started and depth == 0:
                break
        p = p.getnext()
    content = first.getparent()
    instr = next(t.text for t in first.iter(qn("w:instrText")) if "TOC" in (t.text or ""))
    styles = {}
    for p in paras:
        ps = p.find(f"{qn('w:pPr')}/{qn('w:pStyle')}")
        if ps is not None and re.fullmatch(r"TOC\d", ps.get(qn("w:val")) or ""):
            styles.setdefault(int(ps.get(qn("w:val"))[3:]), p.find(qn("w:pPr")))
    base_ppr = styles.get(1) if styles else first.find(qn("w:pPr"))
    anchor = first.getprevious()
    for p in paras:
        content.remove(p)

    def el(tag, **attrs):
        e = content.makeelement(qn(tag), {qn(k): v for k, v in attrs.items()})
        return e

    def run_fld(kind):
        r = el("w:r")
        r.append(el("w:fldChar", **{"w:fldCharType": kind}))
        return r

    def run_text(text):
        r = el("w:r")
        t = el("w:t")
        t.text = text
        t.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
        r.append(t)
        return r

    new = []
    for i, (level, head, page) in enumerate(entries):
        p = el("w:p")
        ppr = copy.deepcopy(styles.get(level, base_ppr))
        ps = ppr.find(qn("w:pStyle"))
        if ps is None:
            ps = el("w:pStyle")
            ppr.insert(0, ps)
        ps.set(qn("w:val"), f"TOC{min(level, 9)}")
        tabs = ppr.find(qn("w:tabs"))
        if tabs is not None:
            ppr.remove(tabs)
        tabs = el("w:tabs")
        tabs.append(el("w:tab", **{"w:val": "right", "w:leader": "dot", "w:pos": "9630"}))
        ppr.append(tabs) if ppr.find(qn("w:rPr")) is None else ppr.find(qn("w:rPr")).addprevious(tabs)
        p.append(ppr)
        if i == 0:
            p.append(run_fld("begin"))
            r = el("w:r")
            it = el("w:instrText")
            it.text = instr
            it.set("{http://www.w3.org/XML/1998/namespace}space", "preserve")
            r.append(it)
            p.append(r)
            p.append(run_fld("separate"))
        p.append(run_text(head))
        r = el("w:r")
        r.append(el("w:tab"))
        p.append(r)
        p.append(run_text(page))
        if i == len(entries) - 1:
            p.append(run_fld("end"))
        new.append(p)
    if anchor is not None:
        for p in reversed(new):
            anchor.addnext(p)
    else:
        for k, p in enumerate(new):
            content.insert(k, p)
    settings = doc.settings.element
    for old in settings.findall(qn("w:updateFields")):
        settings.remove(old)
    doc.save(str(path))
    return len(entries)


def toc_levels_from_lo(path: Path) -> None:  # pragma: no cover - diagnostic
    for e in toc_entries(path):
        print(e)


# Guidance sentences of BDOI's FRS template (D051) that are instructions to the author, not content
TEMPLATE_GUIDANCE = re.compile(
    r"^\(?(?:State the major benefits that the implementation|List Stakeholders|Describe the dependencies between this "
    r"Application|List the use cases/business functions that are out of scope|List the system/organizational interfaces "
    r"that are out of scope|List the filenames of the Business Request)")


def finalise_docx(path: Path) -> dict:
    """The clean-up of a built .docx: the template's guidance sentences removed and every table fitted to the text
    width of its section. Returns what was changed."""
    from docx import Document  # noqa: PLC0415
    from docx.oxml.ns import qn  # noqa: PLC0415

    path = Path(path)
    doc = Document(str(path))
    body = doc.element.body
    removed = 0
    for p in list(body.iter(qn("w:p"))):
        text = "".join(x.text or "" for x in p.iter(qn("w:t"))).strip()
        if text and TEMPLATE_GUIDANCE.match(text) and p.find(f".//{qn('w:drawing')}") is None:
            p.getparent().remove(p)
            removed += 1

    def width_of(sect):
        sz, mar = sect.find(qn("w:pgSz")), sect.find(qn("w:pgMar"))
        if sz is None or mar is None:
            return None
        return int(sz.get(qn("w:w"))) - int(mar.get(qn("w:left"))) - int(mar.get(qn("w:right")))

    fitted = 0
    pending = []
    for el in body.iterchildren():
        if el.tag == qn("w:tbl"):
            pending.append(el)
        sect = el.find(f"{qn('w:pPr')}/{qn('w:sectPr')}") if el.tag == qn("w:p") else (
            el if el.tag == qn("w:sectPr") else None)
        if sect is None:
            continue
        limit = width_of(sect)
        for tbl in pending:
            grid = tbl.find(qn("w:tblGrid"))
            cols = grid.findall(qn("w:gridCol")) if grid is not None else []
            total = sum(int(c.get(qn("w:w")) or 0) for c in cols)
            ind = tbl.find(f"{qn('w:tblPr')}/{qn('w:tblInd')}")
            indent = int(ind.get(qn("w:w")) or 0) if ind is not None and ind.get(qn("w:type")) in (None, "dxa") else 0
            room = (limit or 0) - max(indent, 0)
            if not limit or total <= room + 15:
                continue
            f = room / total
            for c in cols:
                c.set(qn("w:w"), str(int(int(c.get(qn("w:w"))) * f)))
            for tcw in tbl.iter(qn("w:tcW")):
                if tcw.get(qn("w:type")) in (None, "dxa") and tcw.get(qn("w:w")):
                    tcw.set(qn("w:w"), str(int(int(tcw.get(qn("w:w"))) * f)))
            tblw = tbl.find(f"{qn('w:tblPr')}/{qn('w:tblW')}")
            if tblw is not None and tblw.get(qn("w:type")) == "dxa":
                tblw.set(qn("w:w"), str(room))
            fitted += 1
        pending = []
    # a picture stays on the page of its caption (the paragraph after it)
    kept = 0
    for p in body.iter(qn("w:p")):
        if p.find(f".//{qn('w:drawing')}") is None:
            continue
        nxt = p.getnext()
        text = "".join(x.text or "" for x in nxt.iter(qn("w:t"))) if nxt is not None and nxt.tag == qn("w:p") else ""
        if not re.match(r"\s*(Figure|Screenshot|Image)\b", text):
            continue
        ppr = p.find(qn("w:pPr"))
        if ppr is None:
            ppr = p.makeelement(qn("w:pPr"), {})
            p.insert(0, ppr)
        if ppr.find(qn("w:keepNext")) is None:
            ps = ppr.find(qn("w:pStyle"))
            kn = ppr.makeelement(qn("w:keepNext"), {})
            if ps is not None:
                ps.addnext(kn)
            else:
                ppr.insert(0, kn)
            kept += 1
    # the header row of every table repeated on each page the table runs to
    headers = 0
    for tbl in body.iter(qn("w:tbl")):
        rows = tbl.findall(qn("w:tr"))
        if len(rows) < 3:
            continue
        trpr = rows[0].find(qn("w:trPr"))
        if trpr is None:
            trpr = rows[0].makeelement(qn("w:trPr"), {})
            rows[0].insert(1 if rows[0].find(qn("w:tblPrEx")) is not None else 0, trpr)
        if trpr.find(qn("w:tblHeader")) is None:
            trpr.append(trpr.makeelement(qn("w:tblHeader"), {}))
            headers += 1
    doc.save(str(path))
    return {"guidance_removed": removed, "tables_fitted": fitted, "header_rows_repeated": headers,
            "figures_kept_with_caption": kept}


SCREENSHOT_WIDTH = 6.4  # inches: every screenshot of the screen annexes at the same width
SCREENSHOT_MAX_H = 8.0


def bordered(pic_par):
    """A light grey border around the picture of a paragraph (screenshots)."""
    ns_a = "http://schemas.openxmlformats.org/drawingml/2006/main"
    ns_pic = "http://schemas.openxmlformats.org/drawingml/2006/picture"
    for sppr in pic_par.iter(f"{{{ns_pic}}}spPr"):
        if sppr.find(f"{{{ns_a}}}ln") is None:
            ln = sppr.makeelement(f"{{{ns_a}}}ln", {"w": "6350"})
            fill = ln.makeelement(f"{{{ns_a}}}solidFill", {})
            fill.append(fill.makeelement(f"{{{ns_a}}}srgbClr", {"val": "BFBFBF"}))
            ln.append(fill)
            sppr.append(ln)
    return pic_par


def screenshot(b, png: Path):
    """A screenshot of a screen annex: the same width in every annex (narrower only when the screen is taller than
    the page allows), never upscaled beyond its pixels at 2x device scale, with a light border."""
    return bordered(b.picture(png, max_w=SCREENSHOT_WIDTH, max_h=SCREENSHOT_MAX_H))


def render_pages(pdf: Path, outdir: Path, dpi: int = 50) -> list[Path]:
    """Every page of a PDF as PNG (page-NNN.png) for the page-by-page review."""
    outdir = Path(outdir)
    outdir.mkdir(parents=True, exist_ok=True)
    subprocess.run(["pdftoppm", "-r", str(dpi), "-png", str(pdf), str(outdir / "page")], check=True)
    return sorted(outdir.glob("page-*.png"))


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    cmd, args = argv[1], argv[2:]
    if cmd == "scan":
        for f in args:
            found = reference_findings(Path(f))
            print(Path(f).name, {k: len(v) for k, v in found.items()} or "clean")
            for k, v in found.items():
                for s in v[:3]:
                    print(f"   {k}: ...{s}...")
    elif cmd == "toc":
        for f in args:
            print(Path(f).name, populate_toc(Path(f)), "entries")
    elif cmd == "render":
        print(len(render_pages(Path(args[0]), Path(args[1]))), "pages")
    else:
        print(__doc__)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
