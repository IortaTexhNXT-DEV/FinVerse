""""How to read this pack" note of a BRD pack: one page in BDOI's template listing the documents, their purpose and
reviewer, the programme documents every pack refers to, and the IDs used across the pack.
"""

from __future__ import annotations

from pathlib import Path

from pack_common import DocMeta, PackDoc, finish

PROGRAMME_FILES = {
    "Non-Functional and Security Requirements": "Programme/Common",
    "Glossary": "Programme/Common",
    "Decision and Observation Log": "Programme/Common",
    "Document Register": "Programme/Common",
}


def write(K, out: Path, tmp: Path, pdf: bool = True) -> tuple[Path, int]:
    meta = DocMeta(file=K.file_name("README"), cover=K.DOCS["README"]["cover"],
                   header_left=K.DOCS["README"]["header_left"], header_right=K.HEADER_RIGHT,
                   doc_no=K.DOCS["README"]["doc_no"], version=K.version("README"), date_us=K.DATE_US,
                   created_us=K.DATE_US,
                   revisions=[(K.DATE, K.version("README"), "First issue.", K.PREPARED)])
    d = PackDoc(meta)
    r = K.README
    d.h1("How to Read this Pack")
    d.para(r["intro"])
    rows = []
    order = r["order"] + [k for k, *_ in K.SET if k not in r["order"] and k != "README"]
    for i, key in enumerate(order, 1):
        _, _, _, ver, purpose, reviewer = next(x for x in K.SET if x[0] == key)
        rows.append([str(i), K.file_name(key).rsplit("_v", 1)[0].replace("BIBS_", "").replace("_", " "), ver,
                     purpose, reviewer])
    d.table(["No.", "Document", "Version", "Purpose", "Reviewer"], rows, [0.5, 2.8, 0.7, 3.4, 2.0],
            caption="Documents of the pack, in reading order")
    d.table(["Programme document", "Purpose", "Where"],
            [[n, p, PROGRAMME_FILES[n]] for n, p in r["programme"]], [2.6, 4.8, 1.8],
            caption="Programme documents referred to by every pack")
    d.table(["ID", "Meaning and home document"], [list(x) for x in r["ids"]], [2.4, 6.8], caption="IDs used")
    d.para(r["colour"])
    target = out / meta.file
    return target, finish(d, target, tmp, pdf=pdf)
