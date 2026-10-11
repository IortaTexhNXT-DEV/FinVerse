"""Writes the index README.md of each drop folder under docs/deliverables/out/.

BDOI groups the BRDs, FRS, test plans and other collaterals under its drops (answer A5 of 26-Sep-2026). Inside a drop
folder every BRD has one release-set folder (BRD-nn_<Name>/) with all its documents; programme-level items stay grouped
by kind under Programme/. The folders and the BRD-to-drop map are in brand.py (DROPS, BRD_DROP, BRD_NAMES,
DROP_SHARED). This script lists every file of each drop folder by BRD release set with its kind and version, the BDOI
dates of the drop, the parts of BRDs that live in another drop, and the documents still to write. Run it after a build
or a move:

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
        ("Bill of materials, technical and deployment architecture (items 4, 12)", "BRD-00", "From the programme alignment pack (chapter 6), the IER and the architecture option decision"),
        ("Security and data-protection controls mapping (item 26)", "BRD-00", "EIAM, UIDM-ISC, S3 encryption, masking"),
        ("Interface specifications of the Drop 0 integrations", "-", "After BDOI IT answers the IQ questions"),
        ("Technical Specification of each Drop 0 sign-off set (BRD-03, BRD-11, BRD-13)", "-", "The technical content kept out of the business sets (interfaces, access set-up, data storage, extract transfer); reviewed by BDOI IT"),
    ],
    "Drop 1": [
        ("UAT plan and sign-off forms, Drop 1 (item 30)", "-", "By 16-Jul-2027; readiness statement by 30-Jul-2027"),
        ("Interface specifications of the Drop 1 channels", "-", "After BDOI IT answers the IQ questions"),
        ("Technical Specification of each Drop 1 sign-off set", "-", "The technical content kept out of the business sets; reviewed by BDOI IT"),
    ],
    "Drop 2": [
        ("UAT plan and sign-off forms, Drop 2 (item 30)", "-", "Readiness statement by 30-Sep-2027"),
        ("Interface specifications of the Drop 2 integrations (EDP, EGL, CARMS, Insurer System)", "-", "Specifications from BDOI IT by 31-Jan-2027"),
    ],
    "Programme": [
        ("UAT readiness programme and readiness statements (deliverables README)", "-", "Drop 1 by 30-Jul-2027, Drop 2 by 30-Sep-2027"),
        ("End-to-end UAT script", "-", "UAT runs end to end (BDOI answer A1)"),
        ("Performance, penetration test and ORR / PRR evidence (items 28, 37)", "-", "Nov 2027 - Jan 2028"),
        ("Requirements traceability matrix (item 21) and the final refresh of the documents", "-", "Before UAT"),
    ],
}

KINDS = {
    "StartHere": "Start here guide",
    "GuideDeck": "Sign-off pack guide deck",
    "FRS": "FRS",
    "TestPlans": "Test plan",
    "TestPlan": "Test plan",
    "Signoff": "Sign-off workbook (Excel)",
    "ReleaseNote": "Release note",
    "Handbook": "Data Migration Handbook",
    "Workbook": "Migration Workbook (Excel)",
    "Registers": "Register",
    "Register": "Register",
    "Decks": "Deck",
    "Deck": "Deck",
    "Alignment": "Alignment pack",
    "Change_Management": "Change register",
    "Change_Register": "Change register",
    "UXDeck": "UX screen deck (PowerPoint)",
    "UXScreens": "UX screen register (Excel)",
}
# Order of the kinds inside a release set.
KIND_ORDER = ["StartHere", "GuideDeck", "ReleaseNote", "FRS", "Handbook", "Signoff", "Workbook", "TestPlan",
              "Templates", "UXDeck", "UXScreens"]
NAME_RE = re.compile(r"(?:\d\d_)?BIBS_(?P<type>[A-Za-z_]+?)_(?P<brd>BRD-\d\d)_(?P<name>.+?)_v(?P<ver>\d+\.\d+)\.(?P<ext>\w+)$")
# A document of a drop-level set (brand.DROP_SETS): <nn>_BIBS_<Drop-n>_<Name>_v<version>.<ext>.
DROP_NAME_RE = re.compile(r"(?:(?P<order>\d\d)_)?BIBS_(?P<drop>Drop-\d)_(?P<name>.+?)_v(?P<ver>\d+\.\d+)\.(?P<ext>\w+)$")


def describe(path: Path, kind: str | None = None) -> tuple[str, str, str, str]:
    """(document, BRD, kind, version) of an output file; the kind comes from the file name when not given."""
    d = DROP_NAME_RE.match(path.name)
    if d:
        drop = d["drop"].replace("-", " ")
        files = brand.DROP_SETS.get(drop, {}).get("files", {})
        label = files[d["order"]][2] if d["order"] in files else "Drop document"
        return d["name"].replace("_", " "), "-", label, d["ver"]
    m = NAME_RE.match(path.name)
    if m:
        kind = kind or m["type"]
        label = KINDS.get(kind, kind)
        name = m["name"]
        if kind == "UXScreens" and m["ext"] == "zip":
            label = "UX screen images, 2x PNG with the register as CSV (ZIP)"
        if kind in ("TestPlans", "TestPlan", "Change_Management", "Change_Register"):
            label = f"{label} summary (Word)" if name.endswith("_Summary") else f"{label} workbook (Excel)"
            label = label[0].upper() + label[1:]
            name = name.removesuffix("_Summary")
        return name.replace("_", " "), m["brd"], label, m["ver"]
    return path.stem.replace("_", " "), "-", KINDS.get(kind, kind), "-"


def brd_sort(row: tuple[str, str, str, str, str]) -> tuple:
    if DROP_NAME_RE.match(row[4].rsplit("/", 1)[-1]):  # the drop-level set first, in its reading order
        return ("", 0, row[4].rsplit("/", 1)[-1], row[0], row[2], row[3])
    m = NAME_RE.match(row[4].rsplit("/", 1)[-1])
    kind = m["type"] if m else "~"
    order = KIND_ORDER.index(kind) if kind in KIND_ORDER else len(KIND_ORDER)
    return (row[1], order, row[4].rsplit("/", 1)[-1], row[0], row[2], row[3])


def is_listed(path: Path, root: Path) -> bool:
    """False for folders and for working files that are never committed: page previews (`_previews/`),
    hidden files, Office lock files (`~$...`) and PDFs rendered at issue."""
    if path.is_dir():
        return False
    parts = path.relative_to(root).parts
    if any(p.startswith(("_", ".")) for p in parts[:-1]):
        return False
    return not path.name.startswith(("~$", ".")) and path.suffix.lower() != ".pdf"


def closure_note(key: str) -> list[str]:
    """The paragraph on the drop-level set of a drop (brand.DROP_SETS), when it has one."""
    spec = brand.DROP_SETS.get(key)
    if not spec:
        return []
    files = "; ".join(f"{o} {v[2]}" for o, v in spec["files"].items())
    return ["",
            f"The drop-level set `{spec['folder']}/` (v{spec['version']}) covers the whole drop: {files}. Its "
            "workbook lists every configuration input BDOI provides before go-live with its owner, due date and one "
            "route (screen, template or data migration object)."]


def retired_note(key: str) -> list[str]:
    """The BRDs of the drop whose old-format set is superseded by an FRS in BDOI's format (brand.BDOI_FRS_SETS)."""
    brds = [b for b in sorted(brand.BDOI_FRS_SETS) if brand.drop_of(b) == key]
    if not brds:
        return []
    parts = "; ".join(f"{b} {brand.BRD_NAMES[b]} (FRS v{brand.BDOI_FRS_SETS[b]['version']})" for b in brds)
    return ["",
            f"For {parts} the FRS in BDOI's format and its review workbooks (business unit requirements collection, "
            "fit-gap, test cases and traceability, change request register, walkthrough users) in "
            "`../Programme/BDOI_Template_FRS/` are the only documents (decision of the BIBS Product Owner of "
            "10-Oct-2026); the earlier sign-off set of each is withdrawn and its row below points to that folder. The "
            "signatories of such a document are on the Signoff Sheet of its FRS."]


def ux_note(key: str) -> list[str]:
    """The UX screen documents 07 to 09 of the sets of the drop (brand.UX_SETS), for the BDOI UX Design team."""
    sets = [b for b in sorted(brand.UX_SETS) if brand.drop_of(b) == key]
    if not sets:
        return []
    return ["",
            f"The sets of {', '.join(sets)} also carry the UX screen documents for the BDOI UX Design team: 07 the UX "
            "Screen Deck (every screen persona by persona and flow by flow, with all its states), 08 the UX screen "
            "register (one row per screen image with the FRS section, the UXD status and the change flag) and 09 the "
            "image package (every screen image at twice the screen resolution, with the register as CSV). A change "
            "of the FRS names the screens it affects, so the UX Design team revises only those screens."]


def write_index(key: str) -> Path:
    drop = brand.DROPS[key]
    root = brand.OUT_DIR / drop["folder"]
    rows = []
    for kind_dir in sorted(p for p in root.iterdir() if p.is_dir() and not p.name.startswith(("_", "."))):
        # A BRD release-set folder (BRD-nn_Name) and the drop-level set folder (Drop-n_Closure) take the kind from
        # each file name; Programme keeps kind folders.
        kind = None if kind_dir.name.startswith(("BRD-", "Drop-")) else kind_dir.name
        for f in sorted(kind_dir.rglob("*")):
            if not is_listed(f, root):
                continue
            rel = f.relative_to(root).as_posix()
            doc, brd, label, ver = describe(f, kind)
            if f.suffix == ".png":
                label, brd = "IER diagram (PNG)", "BRD-00"
            rows.append((doc, brd, label, ver, rel))
    if key != "Programme":  # the retired sets: one row per BRD pointing to its BDOI-format pack
        for b in sorted(brand.BDOI_FRS_SETS):
            if brand.drop_of(b) == key:
                rows.append((brand.BRD_NAMES[b], b, brand.BDOI_FRS_LABEL, brand.BDOI_FRS_SETS[b]["version"],
                             "../" + brand.bdoi_frs_rel(b)))
    rows.sort(key=brd_sort)

    lines = [
        f"# {drop['title']}: deliverables index",
        "",
        "BIBS client pack for BDO Insurance and Reinsurance Brokers (BDOI), grouped by BDOI drop (answer A5 of 26-Sep-2026).",
        "",
        "| | |",
        "|---|---|",
        f"| BDOI dates | {drop['dates']} |",
        f"| Scope (BDOI drop plan) | {SCOPE[key]} |",
        f"| Status | Status as of {STATUS_AS_OF}; the documents are refreshed before UAT |",
        "",
        "## Documents in this drop",
        "",
        "One folder per BRD release set (`BRD-nn_<Name>/`): every file of the BRD (Start Here, guide deck, FRS, sign-off",
        "workbook, test plan and summary; for BRD-13 the Data Migration Handbook and the Migration Workbook), released",
        "and signed off together; in an",
        "issued sign-off set the files carry the reading-order prefix 00_ to 05_ (deliverables README, \"Release and",
        "sign-off per BRD\"). Each document is kept once, in its latest version.",
        *retired_note(key),
        *ux_note(key),
        *closure_note(key),
        "",
        "| Document | BRD | Kind | Version | File |",
        "|---|---|---|---|---|",
    ]
    for doc, brd, label, ver, rel in rows:
        lines.append(f"| {doc} | {brd} | {label} | {ver} | [`{rel}`]({rel.replace(' ', '%20')}) |")
    owned = [b for b in brand.BRD_NAMES if brand.BRD_DROP.get(b) == key and brand.ownership(b)]
    if owned:
        lines += [
            "",
            "## Who signs what",
            "",
            "The sign-off sets hold business content only (screens, fields, list and template columns, validations, rules,",
            "messages, notifications, documents, walkthroughs, reports); the technical content is in the Technical",
            "Specification of each set, reviewed by BDOI IT. The signatories are the roles of the BRD approval sheet;",
            "the matrix per part of the set is in the 00 Start Here and the 01 guide deck of each set, and on the",
            "Signoff Sheet of an FRS in BDOI's format.",
            "",
            "| Set | Prepared by | Input provided by | Reviewed by | Approved by | Approval sheet |",
            "|---|---|---|---|---|---|",
        ]
        for b in owned:
            caps = brand.owners_by_capacity(b)
            cells = ["<br>".join(caps[c]) or "-" for c in brand.OWNER_CAPACITIES]
            lines.append(f"| {b} {brand.BRD_NAMES[b]} | " + " | ".join(cells) + f" | {brand.ownership(b).get('source', '')} |")
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
            if b in brand.BDOI_FRS_SETS:
                lines.append(f"| {b} | {what} | [`{brand.bdoi_frs_rel(b)}`](../{brand.bdoi_frs_rel(b)}) "
                             f"({brand.BDOI_FRS_LABEL}) |")
                continue
            home = brand.DROPS[brand.drop_of(b)]["folder"]
            folder = brand.out_dir(b, "FRS")
            files = sorted(f for f in folder.glob(f"*BIBS_*_{b}_*")
                           if (m := NAME_RE.match(f.name)) and m["type"] in ("FRS", "Handbook", "TestPlan"))
            links = "<br>".join(f"[`{f.name}`](../{home}/{folder.name}/{f.name})" for f in files)
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
