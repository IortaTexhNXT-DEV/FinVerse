"""Writes the index README.md of each drop folder under docs/deliverables/out/.

BDOI groups the BRDs, FRS, test plans and other collaterals under its drops (answer A5 of 26-Sep-2026). The folders
and the BRD-to-drop map are in brand.py (DROPS, BRD_DROP, DROP_SHARED). This script lists every file of each drop
folder with its BRD, kind and version, the BDOI dates of the drop, the parts of BRDs that live in another drop, and the
documents still to write. Run it after a build or a move:

    python tools/deliverables/drop_index.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import brand  # noqa: E402

STATUS_AS_OF = "26-Sep-2026"

# BDOI drop plan items per drop (docs/source-documents/BDOI_DROP_PLAN.md).
SCOPE = {
    "Drop 0": "Setup: 0.1 Accessibility and Login, 0.2 Authorization, 0.3 User Maintenance, 0.4 Data Management (GL "
              "accounts, reference tables), 0.5 Workflow, 0.6 Product Maintenance; the Data Migration stream; the Drop 0 "
              "integrations (EIAM, UIDM-ISC, LMS, HL-LOAS, PMS, CMS / New BOB, OBPCS, AFTS, Old BOB, ECM, CCM, M365, TFS).",
    "Drop 1": "Upstream (product inherent): 1.U1 Client Onboarding, 1.U2 Quotation or Proposal, 1.U3 Account Creation and "
              "Maintenance, 1.U4 Submitted Policy, 1.U5 Renewal, 1.U6 Placement and ePolicy, 1.U7 Booking, 1.U8 "
              "Accounting / GL, 1.U9 Reports. Downstream (product agnostic): 1.D1 Disbursement, 1.D2 Cashiering, 1.D3 "
              "Remittance (with the reinsurance transactions), 1.D4 Adjustment / Cancellation, 1.D5 Accounting / GL, 1.D6 "
              "Reports (BIR / regulatory), 1.D7 Collection of Commission Receivables (Direct Payment).",
    "Drop 2": "2.1 Marketing Collection (extraction), 2.2 Claims, 2.3 Production Reconciliation, 2.4 Employee Benefits (no "
              "portal feature), 2.5 Other Reports; the Drop 2 integrations (EDP, EGL, CARMS, HL-LOAS, Insurer System, "
              "Bridger Insight XG).",
    "Programme": "Documents that cover every drop: the umbrella BRD-00 FRS, the discrepancy and clarification register, "
                 "the business process deck, the programme alignment pack with the integration inventory and the IER "
                 "diagrams, and the UAT readiness programme.",
}

# Documents still to write, per drop (PROGRAMME_ALIGNMENT.md section 2.3; deliverables README).
TO_WRITE = {
    "Drop 0": [
        ("Bill of materials, technical and deployment architecture (items 4, 12)", "BRD-00", "From PROGRAMME_ALIGNMENT section 6, the IER and ARCHITECTURE_OPTION_DECISION.md"),
        ("Security and data-protection controls mapping (item 26)", "BRD-00", "EIAM, UIDM-ISC, S3 encryption, masking"),
        ("Interface specifications of the Drop 0 integrations", "-", "After BDOI IT answers the IQ questions"),
    ],
    "Drop 1": [
        ("UAT plan and sign-off forms, Drop 1 (item 30)", "-", "By 16-Jul-2027; readiness statement by 30-Jul-2027"),
        ("Interface specifications of the Drop 1 channels", "-", "After BDOI IT answers the IQ questions"),
    ],
    "Drop 2": [
        ("UAT plan and sign-off forms, Drop 2 (item 30)", "-", "Readiness statement by 30-Sep-2027"),
        ("Interface specifications of the Drop 2 integrations (EDP, EGL, CARMS, Insurer System)", "-", "Specifications from BDOI IT by 31-Jan-2027"),
    ],
    "Programme": [
        ("UAT readiness programme and readiness statements (deliverables README)", "-", "Drop 1 by 30-Jul-2027, Drop 2 by 30-Sep-2027"),
        ("End-to-end UAT script", "-", "UAT runs end to end (BDOI answer A1)"),
        ("Performance, penetration test and ORR / PRR evidence (items 28, 37)", "-", "Nov 2027 - Jan 2028"),
        ("Requirements traceability matrix (item 21) and the final as-built refresh", "-", "At build completion"),
    ],
}

KINDS = {
    "FRS": "FRS",
    "TestPlans": "Test plan",
    "Migration": "Migration pack",
    "Registers": "Register",
    "Decks": "Deck",
    "Alignment": "Alignment pack",
}
NAME_RE = re.compile(r"BIBS_(?P<type>[A-Za-z]+)_(?P<brd>BRD-\d\d)_(?P<name>.+?)_v(?P<ver>\d+\.\d+)\.(?P<ext>\w+)$")


def describe(path: Path, kind: str) -> tuple[str, str, str, str]:
    """(document, BRD, kind, version) of an output file."""
    m = NAME_RE.match(path.name)
    if m:
        label = KINDS.get(kind, kind)
        name = m["name"]
        if kind == "TestPlans":
            label = "Test plan summary (Word)" if name.endswith("_Summary") else "Test plan workbook (Excel)"
            name = name.removesuffix("_Summary")
        return name.replace("_", " "), m["brd"], label, m["ver"]
    return path.stem.replace("_", " "), "-", KINDS.get(kind, kind), "-"


def brd_sort(row: tuple[str, str, str, str, str]) -> tuple:
    return (row[1], row[0], row[2])


def write_index(key: str) -> Path:
    drop = brand.DROPS[key]
    root = brand.OUT_DIR / drop["folder"]
    rows = []
    for kind_dir in sorted(p for p in root.iterdir() if p.is_dir()):
        kind = kind_dir.name
        for f in sorted(kind_dir.rglob("*")):
            if f.is_dir():
                continue
            rel = f.relative_to(root).as_posix()
            if "/templates/" in f"/{rel}" and f.name != "README.md":
                continue  # the templates folder is listed once through its README
            doc, brd, label, ver = describe(f, kind)
            if f.name == "README.md" and "templates" in rel:
                doc, label = "Migration extract templates (CSV headers and control file)", "Templates"
                brd = "BRD-13"
                rel = rel.rsplit("/", 1)[0] + "/"
            elif f.suffix == ".png":
                label, brd = "IER diagram (PNG)", "BRD-00"
            rows.append((doc, brd, label, ver, rel))
    rows.sort(key=brd_sort)

    lines = [
        f"# {drop['title']}: deliverables index",
        "",
        "BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).",
        "Generated by `python tools/deliverables/drop_index.py` from the files of this folder and the drop map in",
        "`tools/deliverables/brand.py`; do not edit by hand.",
        "",
        "| | |",
        "|---|---|",
        f"| BDOI dates | {drop['dates']} |",
        f"| Scope (BDOI drop plan) | {SCOPE[key]} |",
        f"| Status | Status as of {STATUS_AS_OF}; final refresh at build completion (deliverables README, \"Document status and the final as-built refresh\") |",
        "",
        "## Documents in this drop",
        "",
        "| Document | BRD | Kind | Version | File |",
        "|---|---|---|---|---|",
    ]
    for doc, brd, label, ver, rel in rows:
        lines.append(f"| {doc} | {brd} | {label} | {ver} | [`{rel}`]({rel.replace(' ', '%20')}) |")
    shared_here = [(b, what) for b, other, what in brand.DROP_SHARED if other == key]
    if shared_here:
        lines += [
            "",
            "## Also part of this drop (documents kept in their primary drop)",
            "",
            "A BRD that spans drops lives in the folder of its primary drop; nothing is copied.",
            "",
            "| BRD | Part in this drop | Documents (in the primary drop folder) |",
            "|---|---|---|",
        ]
        for b, what in shared_here:
            home = brand.DROPS[brand.drop_of(b)]["folder"]
            files = sorted(f for kind in ("FRS", "TestPlans") for f in (brand.OUT_DIR / home / kind).glob(f"BIBS_*_{b}_*"))
            links = "<br>".join(f"[`{f.name}`](../{home}/{f.parent.name}/{f.name})" for f in files)
            lines.append(f"| {b} | {what} | {links or f'[{home}/](../{home}/README.md)'} |")
    lines += ["", "## Still to write", "", "| Document | BRD | Note |", "|---|---|---|"]
    for doc, brd, note in TO_WRITE[key]:
        lines.append(f"| {doc} | {brd} | {note} |")
    lines.append("")
    target = root / "README.md"
    target.write_text("\n".join(lines), encoding="utf-8")
    return target


def main() -> int:
    for key in brand.DROPS:
        root = brand.OUT_DIR / brand.DROPS[key]["folder"]
        root.mkdir(parents=True, exist_ok=True)
        print(write_index(key).relative_to(brand.REPO_ROOT))
    return 0


if __name__ == "__main__":
    sys.exit(main())
