"""Sets the document properties of the Office files of Programme/Database_Design: author and last modified by
"iorta TechNXT", application "Microsoft Office"; content is left untouched.

  python3 docs/deliverables/src/programme/database_design/office_meta.py [--show] FILE...
"""
from __future__ import annotations

import re
import shutil
import sys
import tempfile
import zipfile
from pathlib import Path

AUTHOR = "iorta TechNXT"


def _core(xml: str) -> str:
    # a library's default description is not a property of the document
    xml = re.sub(r"<dc:description>[^<]*(?:python-docx|openpyxl)[^<]*</dc:description>", "", xml)
    for tag in ("dc:creator", "cp:lastModifiedBy"):
        if f"<{tag}" in xml:
            xml = re.sub(rf"<{tag}>.*?</{tag}>|<{tag}/>", f"<{tag}>{AUTHOR}</{tag}>", xml, flags=re.S)
        else:
            xml = xml.replace("</cp:coreProperties>", f"<{tag}>{AUTHOR}</{tag}></cp:coreProperties>")
    return xml


def _app(xml: str, word: bool) -> str:
    name = "Microsoft Office Word" if word else "Microsoft Excel"
    return re.sub(r"<Application>.*?</Application>", f"<Application>{name}</Application>", xml, flags=re.S)


def fix(path: Path) -> None:
    tmp = Path(tempfile.mkstemp(suffix=path.suffix, dir=path.parent)[1])
    with zipfile.ZipFile(path) as src, zipfile.ZipFile(tmp, "w", zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            data = src.read(item.filename)
            if item.filename == "docProps/core.xml":
                data = _core(data.decode("utf-8")).encode("utf-8")
            elif item.filename == "docProps/app.xml":
                data = _app(data.decode("utf-8"), path.suffix == ".docx").encode("utf-8")
            dst.writestr(item, data)
    shutil.move(tmp, path)


def show(path: Path) -> str:
    with zipfile.ZipFile(path) as z:
        text = z.read("docProps/core.xml").decode() + (z.read("docProps/app.xml").decode()
                                                       if "docProps/app.xml" in z.namelist() else "")
    return f"{path.name}: " + "; ".join(re.findall(r"<(?:dc:creator|cp:lastModifiedBy|Application)>[^<]*", text))


if __name__ == "__main__":
    args = sys.argv[1:]
    only_show = "--show" in args
    for a in (x for x in args if x != "--show"):
        if not only_show:
            fix(Path(a))
        print(show(Path(a)))
