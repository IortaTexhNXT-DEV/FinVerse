"""Sets the document properties of the delivered Word and Excel files.

Every .docx / .xlsx under docs/deliverables/out/ carries iorta TechNXT as author and last editor and the Office
application name, so no template placeholder ("Author's Name", "Ministry/Agency Name"), earlier editor or library
name stays in the file properties. Only docProps/core.xml and docProps/app.xml are rewritten; the content is not
touched. drop_index.py runs it after every build:

    python tools/deliverables/doc_metadata.py
"""

from __future__ import annotations

import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand  # noqa: E402

OWNER = "iorta TechNXT"
APPS = {".docx": "Microsoft Office Word", ".xlsx": "Microsoft Excel"}


CORE_NS = ('xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" '
           'xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" '
           'xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"')


def _core(xml: str, title: str) -> str:
    """A clean core-properties part: owner, title and subject set; the created / modified dates and the revision kept."""
    keep = []
    for local in ("created", "modified"):
        m = re.search(rf"<(?:\w+:)?{local}\b[^>]*>([^<]*)</(?:\w+:)?{local}>", xml)
        if m:
            keep.append(f'<dcterms:{local} xsi:type="dcterms:W3CDTF">{m.group(1)}</dcterms:{local}>')
    m = re.search(r"<(?:\w+:)?revision>([^<]*)</(?:\w+:)?revision>", xml)
    rev = f"<cp:revision>{m.group(1)}</cp:revision>" if m else ""
    return ("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n"
            f"<cp:coreProperties {CORE_NS}><dc:title>{title}</dc:title>"
            f"<dc:subject>BDO Insure Brokering System (BIBS)</dc:subject><dc:creator>{OWNER}</dc:creator>"
            f"<cp:lastModifiedBy>{OWNER}</cp:lastModifiedBy>{rev}{''.join(keep)}</cp:coreProperties>")


def _app(xml: str, app: str) -> str:
    xml = re.sub(r"<Application>[^<]*</Application>", f"<Application>{app}</Application>", xml)
    xml = re.sub(r"<Company>[^<]*</Company>", f"<Company>{OWNER}</Company>", xml)
    return xml


def fix(path: Path) -> bool:
    """Rewrites the properties of one file; True when something changed."""
    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        parts = {n: z.read(n) for n in names}
    changed = False
    if "docProps/core.xml" in parts:
        old = parts["docProps/core.xml"].decode("utf-8")
        new = _core(old, path.stem.replace("_", " "))
        if new != old:
            parts["docProps/core.xml"], changed = new.encode("utf-8"), True
    if "docProps/app.xml" in parts:
        old = parts["docProps/app.xml"].decode("utf-8")
        new = _app(old, APPS[path.suffix])
        if new != old:
            parts["docProps/app.xml"], changed = new.encode("utf-8"), True
    if changed:
        with tempfile.NamedTemporaryFile(delete=False, dir=path.parent, suffix=path.suffix) as tmp:
            with zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as z:
                for n in names:
                    z.writestr(n, parts[n])
        shutil.move(tmp.name, path)
    return changed


def main() -> int:
    files = [p for ext in APPS for p in brand.OUT_DIR.rglob(f"*{ext}") if not p.name.startswith("~$")]
    done = sum(fix(p) for p in sorted(files))
    print(f"doc_metadata: {done} of {len(files)} files updated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
