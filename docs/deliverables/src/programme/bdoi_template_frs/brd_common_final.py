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
import math
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


def retext(p_element, text: str):
    """A paragraph element given another text in the format of its first run (the other runs dropped)."""
    from docx.oxml.ns import qn  # noqa: PLC0415

    runs = p_element.findall(qn("w:r"))
    if not runs:
        r = p_element.makeelement(qn("w:r"), {})
        p_element.append(r)
        runs = [r]
    for r in runs[1:]:
        p_element.remove(r)
    r = runs[0]
    for child in list(r):
        if child.tag != qn("w:rPr"):
            r.remove(child)
    t = r.makeelement(qn("w:t"), {"{http://www.w3.org/XML/1998/namespace}space": "preserve"})
    t.text = text
    r.append(t)
    return p_element


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
    # no blank page: a heading that starts a page right after a section break (next page) needs no page break
    # of its own; empty paragraphs right before such a heading, and at the end of the document, are dropped
    def empty(el):
        return (el.tag == qn("w:p") and not "".join(x.text or "" for x in el.iter(qn("w:t"))).strip()
                and el.find(f".//{qn('w:drawing')}") is None and el.find(f".//{qn('w:sectPr')}") is None
                and el.find(f".//{qn('w:br')}") is None)

    def break_only(el):
        # a paragraph holding nothing but a page break: redundant before a heading that starts a page itself
        return (el.tag == qn("w:p") and not "".join(x.text or "" for x in el.iter(qn("w:t"))).strip()
                and el.find(f".//{qn('w:drawing')}") is None and el.find(f".//{qn('w:sectPr')}") is None
                and any(b.get(qn("w:type")) == "page" for b in el.iter(qn("w:br"))))

    blanks = 0
    for p in list(body.iterchildren()):
        if p.tag != qn("w:p"):
            continue
        ppr = p.find(qn("w:pPr"))
        pbb = ppr.find(qn("w:pageBreakBefore")) if ppr is not None else None
        if pbb is None:
            continue
        prev = p.getprevious()
        while prev is not None and (empty(prev) or break_only(prev)):
            gone = prev
            prev = prev.getprevious()
            if prev is not None and prev.tag == qn("w:tbl"):
                break  # a table keeps the paragraph that follows it
            body.remove(gone)
            blanks += 1
        if prev is not None and prev.tag == qn("w:p") and prev.find(f"{qn('w:pPr')}/{qn('w:sectPr')}") is not None:
            ppr.remove(pbb)
            blanks += 1
    last = body.find(qn("w:sectPr"))
    prev = last.getprevious() if last is not None else None
    while prev is not None and empty(prev) and prev.getprevious() is not None and prev.getprevious().tag != qn("w:tbl"):
        gone = prev
        prev = prev.getprevious()
        body.remove(gone)
        blanks += 1
    # a column headed "Revision" or "Version" wide enough for its heading on one line (BDOI's revision log)
    widened = 0
    for tbl in body.iter(qn("w:tbl")):
        rows = tbl.findall(qn("w:tr"))
        grid = tbl.find(qn("w:tblGrid"))
        if not rows or grid is None:
            continue
        cells = rows[0].findall(qn("w:tc"))
        cols = grid.findall(qn("w:gridCol"))
        if len(cells) != len(cols):
            continue
        for k, cell in enumerate(cells):
            head = "".join(x.text or "" for x in cell.iter(qn("w:t"))).strip()
            width = int(cols[k].get(qn("w:w")) or 0)
            if head in ("Revision", "Version") and 0 < width < 1150:
                widest = max(range(len(cols)), key=lambda i: int(cols[i].get(qn("w:w")) or 0))
                delta = 1150 - width
                cols[k].set(qn("w:w"), "1150")
                cols[widest].set(qn("w:w"), str(int(cols[widest].get(qn("w:w"))) - delta))
                for row in rows:
                    tcs = row.findall(qn("w:tc"))
                    if len(tcs) == len(cols):
                        for i, tc in enumerate(tcs):
                            tcw = tc.find(f"{qn('w:tcPr')}/{qn('w:tcW')}")
                            if tcw is not None and tcw.get(qn("w:type")) in (None, "dxa"):
                                tcw.set(qn("w:w"), cols[i].get(qn("w:w")))
                widened += 1
    # a picture never taller than the page leaves room for its caption: scaled down to MAX_PICTURE_H at most
    ns_wp = "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
    ns_a = "http://schemas.openxmlformats.org/drawingml/2006/main"
    shrunk = 0
    limit_emu = int(MAX_PICTURE_H * 914400)
    for ext in body.iter(f"{{{ns_wp}}}extent"):
        cy = int(ext.get("cy") or 0)
        if cy > limit_emu:
            f = limit_emu / cy
            cx = int(int(ext.get("cx")) * f)
            ext.set("cx", str(cx))
            ext.set("cy", str(limit_emu))
            inline = ext.getparent()
            for xe in inline.iter(f"{{{ns_a}}}ext"):
                xe.set("cx", str(cx))
                xe.set("cy", str(limit_emu))
            shrunk += 1
    # a heading right before a picture stays with it (empty paragraphs between them dropped)
    for p in list(body.iter(qn("w:p"))):
        ppr = p.find(qn("w:pPr"))
        ps = ppr.find(qn("w:pStyle")) if ppr is not None else None
        style = (ps.get(qn("w:val")) or "") if ps is not None else ""
        if not style.lower().startswith("heading"):
            continue
        nxt = p.getnext()
        while nxt is not None and empty(nxt):
            gone = nxt
            nxt = nxt.getnext()
            body.remove(gone)
        if nxt is not None and nxt.tag == qn("w:p") and nxt.find(f".//{qn('w:drawing')}") is not None:
            if ppr.find(qn("w:keepNext")) is None:
                kn = ppr.makeelement(qn("w:keepNext"), {})
                ps.addnext(kn)
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
    # the header row of every table repeated on each page the table runs to; the last three rows kept together,
    # so that a table never leaves one row alone on the next page
    headers = 0
    for tbl in body.iter(qn("w:tbl")):
        rows = tbl.findall(qn("w:tr"))
        if len(rows) < 3:
            continue
        # a short row (a few lines) is never split across two pages; a long one (an observation of half a page)
        # may still break, so that no page is left half empty
        for tr in rows:
            text = "".join(x.text or "" for x in tr.iter(qn("w:t")))
            if len(text) > 600:
                continue
            trpr = tr.find(qn("w:trPr"))
            if trpr is None:
                trpr = tr.makeelement(qn("w:trPr"), {})
                tr.insert(1 if tr.find(qn("w:tblPrEx")) is not None else 0, trpr)
            if trpr.find(qn("w:cantSplit")) is None:
                trpr.insert(0, trpr.makeelement(qn("w:cantSplit"), {}))
        for tr in rows[-3:-1]:
            # only a short row is kept with the next one: a long row kept with its successor would leave a page
            # half empty
            if len("".join(x.text or "" for x in tr.iter(qn("w:t")))) > 600:
                continue
            for p in tr.iter(qn("w:p")):
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
        trpr = rows[0].find(qn("w:trPr"))
        if trpr is None:
            trpr = rows[0].makeelement(qn("w:trPr"), {})
            rows[0].insert(1 if rows[0].find(qn("w:tblPrEx")) is not None else 0, trpr)
        if trpr.find(qn("w:tblHeader")) is None:
            trpr.append(trpr.makeelement(qn("w:tblHeader"), {}))
            headers += 1
    # a line break at the end of a paragraph stretches the last line of a justified paragraph: dropped
    breaks = 0
    for p in body.iter(qn("w:p")):
        runs = [r for r in p.findall(qn("w:r")) if len(r) and any(c.tag != qn("w:rPr") for c in r)]
        while runs:
            last = runs[-1]
            kids = [c for c in last if c.tag != qn("w:rPr")]
            if kids and kids[-1].tag == qn("w:br") and kids[-1].get(qn("w:type")) in (None, "textWrapping"):
                last.remove(kids[-1])
                breaks += 1
                if not [c for c in last if c.tag != qn("w:rPr")]:
                    runs.pop()
                continue
            break
    # an empty row left at the end of a table (a template row to fill in) is dropped
    trailing = 0
    for tbl in body.iter(qn("w:tbl")):
        rows = tbl.findall(qn("w:tr"))
        while len(rows) > 2:
            last = rows[-1]
            text = "".join(x.text or "" for x in last.iter(qn("w:t"))).strip()
            if text or last.find(f".//{qn('w:drawing')}") is not None:
                break
            tbl.remove(last)
            rows = rows[:-1]
            trailing += 1
    # the line that introduces a table stays with it: a short paragraph right before a table is kept with the next
    intros = 0
    for tbl in body.findall(qn("w:tbl")):
        prev = tbl.getprevious()
        if prev is None or prev.tag != qn("w:p"):
            continue
        text = "".join(x.text or "" for x in prev.iter(qn("w:t"))).strip()
        if not text or len(text) > 240 or prev.find(f".//{qn('w:drawing')}") is not None:
            continue
        ppr = prev.find(qn("w:pPr"))
        if ppr is None:
            ppr = prev.makeelement(qn("w:pPr"), {})
            prev.insert(0, ppr)
        if ppr.find(qn("w:keepNext")) is None:
            ps = ppr.find(qn("w:pStyle"))
            kn = ppr.makeelement(qn("w:keepNext"), {})
            if ps is not None:
                ps.addnext(kn)
            else:
                ppr.insert(0, kn)
            intros += 1
    # a content row needs no minimum height: a row of text taller than a page would push the row before it alone
    # to the previous page (BDOI's rows carry the height of the row they were copied from)
    heights = 0
    for tr in body.iter(qn("w:tr")):
        text = "".join(x.text or "" for x in tr.iter(qn("w:t")))
        trpr = tr.find(qn("w:trPr"))
        h = trpr.find(qn("w:trHeight")) if trpr is not None else None
        if h is not None and len(text) > 600 and tr.find(f".//{qn('w:drawing')}") is None:
            trpr.remove(h)
            heights += 1
    # the section rows of a requirements table (one cell across the table) are numbered in the text, 1., 2., 3.,
    # so that Word and the PDF show the same numbers; an added section row without numbering gets its number
    numbered = 0
    for tbl in body.iter(qn("w:tbl")):
        rows = [tr for tr in tbl.findall(qn("w:tr")) if len(tr.findall(qn("w:tc"))) == 1]
        if not any(tr.find(f".//{qn('w:numPr')}") is not None for tr in rows):
            continue
        n = 0
        for tr in rows:
            text = "".join(x.text or "" for x in tr.iter(qn("w:t"))).strip()
            if not text or len(text) > 80:
                continue
            paras = [p for p in tr.iter(qn("w:p")) if "".join(x.text or "" for x in p.iter(qn("w:t"))).strip()]
            if not paras:
                continue
            p = paras[0]
            n += 1
            ppr = p.find(qn("w:pPr"))
            if ppr is None:
                ppr = p.makeelement(qn("w:pPr"), {})
                p.insert(0, ppr)
            num = ppr.find(qn("w:numPr"))
            if num is not None:
                ppr.remove(num)
            if ppr.find(qn("w:ind")) is None:
                ind = ppr.makeelement(qn("w:ind"), {qn("w:left"): "720", qn("w:hanging"): "360"})
                ppr.append(ind)
            first = p.find(qn("w:r"))
            lead = p.makeelement(qn("w:r"), {})
            rpr = first.find(qn("w:rPr")) if first is not None else None
            if rpr is not None:
                lead.append(copy.deepcopy(rpr))
            t = lead.makeelement(qn("w:t"), {})
            t.text = f"{n}."
            lead.append(t)
            lead.append(lead.makeelement(qn("w:tab"), {}))
            if first is not None:
                first.addprevious(lead)
            else:
                p.append(lead)
            numbered += 1
    # tracked changes left in BDOI's text are accepted: the final document carries no revision marks
    accepted = 0
    for d in list(body.iter(qn("w:del"))):
        d.getparent().remove(d)
        accepted += 1
    for ins in list(body.iter(qn("w:ins"))):
        parent = ins.getparent()
        at = parent.index(ins)
        for child in list(ins):
            parent.insert(at, child)
            at += 1
        parent.remove(ins)
        accepted += 1
    for mark in list(body.iter(qn("w:rPrChange"), qn("w:pPrChange"), qn("w:tblPrChange"), qn("w:trPrChange"),
                               qn("w:tcPrChange"), qn("w:sectPrChange"))):
        mark.getparent().remove(mark)
        accepted += 1
    doc.save(str(path))
    return {"guidance_removed": removed, "tables_fitted": fitted, "header_rows_repeated": headers,
            "figures_kept_with_caption": kept, "columns_widened": widened,
            "blank_pages_removed": blanks, "pictures_scaled": shrunk, "changes_accepted": accepted,
            "row_heights_removed": heights, "section_rows_numbered": numbered, "table_intros_kept": intros,
            "trailing_rows_removed": trailing, "trailing_breaks_removed": breaks}


MAX_PICTURE_H = 7.9  # inches: no picture taller than this, so that its caption stays on its page
SCREENSHOT_WIDTH = 6.4  # inches: every screenshot of the screen annexes at the same width
SCREENSHOT_PART_H = 4.4  # inches: the parts of a tall screen, top to bottom; two parts fill a page, one part fits under the text of its screen
SCREENSHOT_WHOLE_H = 5.4  # inches: a screen up to this height at that width is shown whole (it fits under the text of its screen)
DOCUMENT_WIDTH = 4.6  # inches: every document print (A4 portrait) at the same width, so that its table and caption fit on its page


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


def _parts(png: Path, width: float, part_h: float, work: Path, whole_h: float | None = None) -> list[Path]:
    """A tall screen cut into bands of equal width, top to bottom, each at most part_h inches tall at the
    given width; the cut falls in a gap of blank rows where one is near, so that no line of text is split.
    A screen up to whole_h inches (part_h when not given) stays whole."""
    from PIL import Image  # noqa: PLC0415

    with Image.open(png) as im:
        w, h = im.size
        inches = h / w * width
        if inches <= (whole_h or part_h):
            return [png]
        n = math.ceil(inches / part_h)
        px = h // n
        work.mkdir(parents=True, exist_ok=True)
        rgb = im.convert("RGB")
        # a row is blank when nothing dark crosses it (no text, no dark line, no button): the darkest pixel of
        # the row is light; the cut is made in a gap of blank rows nearest to the even cut, so that no line of
        # text, no table row and no field is split between two parts
        import numpy as np  # noqa: PLC0415

        a = np.asarray(rgb).astype(np.int32)
        lum = (a[:, :, 0] * 299 + a[:, :, 1] * 587 + a[:, :, 2] * 114) // 1000
        blank = lum.min(axis=1) >= 200
        cuts = [0]
        for k in range(1, n):
            target = px * k
            win = max(80, px // 3)
            best = None
            for gap in (4, 2, 0):
                for d in range(0, win):
                    for y in (target - d, target + d):
                        if 0 < y < h and blank[max(0, y - gap):y + gap + 1].all():
                            best = y
                            break
                    if best is not None:
                        break
                if best is not None:
                    break
            cuts.append(best if best is not None else target)
        cuts.append(h)
        out = []
        for k in range(n):
            part = work / f"{png.stem}-part{k + 1}.png"
            rgb.crop((0, cuts[k], w, cuts[k + 1])).save(part)
            out.append(part)
        return out


def _shot_width(png: Path) -> float:
    """The width of a screenshot in the document: a whole screen (crop main or full) at the standard width; a
    dialog or a region of a screen at its natural size (2 device pixels per screen pixel, 96 per inch), never
    wider than the standard width, so that a small window is not blown up."""
    from PIL import Image  # noqa: PLC0415

    with Image.open(png) as im:
        kind = (im.text or {}).get("bibs-crop", "") if hasattr(im, "text") else ""
        if kind in ("dialog", "region"):
            return min(SCREENSHOT_WIDTH, max(3.0, im.size[0] / 192))
    return SCREENSHOT_WIDTH


def screenshot(b, png: Path, work: Path | None = None) -> list:
    """A screenshot of a screen annex at the standard width of every annex, with a light border; a screen
    taller than a page is shown in parts from top to bottom, each at the same width, with "(continued)" between
    them. Returns the paragraphs to insert."""
    work = work or Path(tempfile.gettempdir()) / "frs_shots"
    width = _shot_width(Path(png))
    parts = _parts(Path(png), width, SCREENSHOT_PART_H, work, SCREENSHOT_WHOLE_H)
    out = []
    for k, part in enumerate(parts):
        out.append(bordered(b.picture(part, max_w=width, max_h=SCREENSHOT_WHOLE_H + 0.2)))
        if k < len(parts) - 1:
            out.append(b.para("(continued below)", italic=True, size=7.5, jc="center", space_after=40))
    return out


def document_image(b, png: Path):
    """A document print (an A4 page) at the standard width of every document, with a light border."""
    return bordered(b.picture(png, max_w=DOCUMENT_WIDTH, max_h=DOCUMENT_WIDTH * 1.5))


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
