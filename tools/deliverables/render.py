"""PDF conversion and page previews for the BIBS client pack.

* ``to_pdf(path)`` converts .docx / .xlsx / .pptx to PDF next to the source with
  ``soffice --headless --convert-to pdf`` (a private LibreOffice profile, so parallel runs do not
  collide with an open LibreOffice).
* ``previews(pdf)`` renders every page to PNG (``pdftoppm``, or ``pypdfium2`` when poppler is not
  installed) and builds contact sheets of 6 pages each, so a whole document can be checked for
  alignment, overflow, table widths and page breaks at a glance.

Command line::

    python tools/deliverables/render.py docs/deliverables/out/Drop-1_Transactional/FRS/X.docx --previews
    python tools/deliverables/render.py some.pdf --previews --dpi 110 --pages 3-8
"""

from __future__ import annotations

import argparse
import shutil
import subprocess
import tempfile
from pathlib import Path

PREVIEW_DIR_NAME = "_previews"


def to_pdf(src: str | Path, outdir: str | Path | None = None, timeout: int = 300) -> Path:
    """Converts an Office file to PDF and returns the PDF path."""
    src = Path(src).resolve()
    outdir = Path(outdir).resolve() if outdir else src.parent
    outdir.mkdir(parents=True, exist_ok=True)
    soffice = shutil.which("soffice") or shutil.which("libreoffice")
    if not soffice:
        raise RuntimeError("LibreOffice (soffice) is not installed")
    with tempfile.TemporaryDirectory(prefix="bdoi-lo-") as profile:
        cmd = [soffice, f"-env:UserInstallation=file://{profile}", "--headless", "--norestore",
               "--convert-to", "pdf", "--outdir", str(outdir), str(src)]
        subprocess.run(cmd, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout)
    pdf = outdir / (src.stem + ".pdf")
    if not pdf.exists():
        raise RuntimeError(f"LibreOffice did not produce {pdf}")
    return pdf


def page_count(pdf: str | Path) -> int:
    import pypdfium2 as pdfium

    doc = pdfium.PdfDocument(str(pdf))
    try:
        return len(doc)
    finally:
        doc.close()


def _parse_pages(spec: str | None, total: int) -> list[int]:
    if not spec:
        return list(range(1, total + 1))
    pages: list[int] = []
    for part in spec.split(","):
        if "-" in part:
            a, b = part.split("-")
            pages.extend(range(int(a), min(int(b), total) + 1))
        else:
            pages.append(int(part))
    return [p for p in pages if 1 <= p <= total]


def render_pages(pdf: str | Path, outdir: str | Path | None = None, dpi: int = 80,
                 pages: str | None = None) -> list[Path]:
    """Renders pages to PNG files page-001.png, page-002.png, ... and returns their paths."""
    pdf = Path(pdf).resolve()
    outdir = Path(outdir) if outdir else pdf.parent / PREVIEW_DIR_NAME / pdf.stem
    if outdir.exists():
        shutil.rmtree(outdir)
    outdir.mkdir(parents=True)
    total = page_count(pdf)
    wanted = _parse_pages(pages, total)
    out: list[Path] = []
    if shutil.which("pdftoppm"):
        for p in wanted:
            target = outdir / f"page-{p:03d}"
            subprocess.run(["pdftoppm", "-png", "-r", str(dpi), "-f", str(p), "-l", str(p), "-singlefile",
                            str(pdf), str(target)], check=True)
            out.append(target.with_suffix(".png"))
    else:
        import pypdfium2 as pdfium

        doc = pdfium.PdfDocument(str(pdf))
        for p in wanted:
            img = doc[p - 1].render(scale=dpi / 72).to_pil()
            path = outdir / f"page-{p:03d}.png"
            img.save(path)
            out.append(path)
        doc.close()
    return out


def contact_sheets(images: list[Path], outdir: Path, per_sheet: int = 6, columns: int = 3) -> list[Path]:
    """Combines page images into labelled sheets (default 3 x 2 pages per sheet)."""
    from PIL import Image, ImageDraw

    sheets: list[Path] = []
    if not images:
        return sheets
    with Image.open(images[0]) as first:
        w, h = first.size
    gap, label_h = 16, 22
    for s in range(0, len(images), per_sheet):
        batch = images[s:s + per_sheet]
        rows = (len(batch) + columns - 1) // columns
        sheet = Image.new("RGB", (columns * (w + gap) + gap, rows * (h + gap + label_h) + gap), (120, 120, 120))
        draw = ImageDraw.Draw(sheet)
        for k, path in enumerate(batch):
            with Image.open(path) as im:
                im = im.convert("RGB")
                if im.size != (w, h):  # landscape pages: fit into the cell
                    im.thumbnail((w, h))
                x = gap + (k % columns) * (w + gap)
                y = gap + (k // columns) * (h + gap + label_h)
                draw.text((x, y), path.stem, fill=(255, 255, 255))
                sheet.paste(im, (x, y + label_h))
        target = outdir / f"sheet-{s // per_sheet + 1:02d}.png"
        sheet.save(target)
        sheets.append(target)
    return sheets


def previews(pdf: str | Path, dpi: int = 70, pages: str | None = None, sheets: bool = True) -> list[Path]:
    """Page PNGs plus contact sheets; returns the sheets (or the pages when sheets=False)."""
    images = render_pages(pdf, dpi=dpi, pages=pages)
    if not sheets:
        return images
    return contact_sheets(images, images[0].parent) if images else []


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="Convert Office files to PDF and render page previews")
    ap.add_argument("files", nargs="+", help=".docx / .xlsx / .pptx / .pdf files")
    ap.add_argument("--previews", action="store_true", help="render page PNGs and contact sheets")
    ap.add_argument("--dpi", type=int, default=70)
    ap.add_argument("--pages", help="page selection, e.g. 1-3,7")
    ap.add_argument("--no-sheets", action="store_true", help="page PNGs only")
    args = ap.parse_args(argv)
    for f in args.files:
        path = Path(f)
        pdf = path if path.suffix.lower() == ".pdf" else to_pdf(path)
        print(f"pdf: {pdf} ({page_count(pdf)} pages)")
        if args.previews:
            out = previews(pdf, dpi=args.dpi, pages=args.pages, sheets=not args.no_sheets)
            print(f"previews: {out[0].parent if out else '-'} ({len(out)} files)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
