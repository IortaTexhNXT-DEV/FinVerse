"""BDO Insure brand constants shared by the Word, Excel and PowerPoint builders.

Colours follow docs/design/BDO_UX_GUIDELINES.md section 2. Yellow is an accent only: thin rules,
never a fill behind text.
"""

from __future__ import annotations

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
# The client's logo is that of the theme pack; the platform's logo that of iorta TechNXT.
BRAND_DIR = REPO_ROOT / "frontend" / "src" / "theme" / "packs" / "bdoi"
BDO_LOGO = BRAND_DIR / "client-logo.png"
IORTA_LOGO = REPO_ROOT / "frontend" / "src" / "assets" / "platform" / "iorta-technxt.png"
DELIVERABLES = REPO_ROOT / "docs" / "deliverables"
SRC_DIR = DELIVERABLES / "src"
OUT_DIR = DELIVERABLES / "out"

# Hex without '#', as the Office libraries expect.
HEADER_BLUE = "004EA8"  # headings, table header rows, section slides
CTA_BLUE = "0072D8"  # links, secondary accents
YELLOW = "FDB913"  # thin accent rules only
FIELD_BLUE = "99C1E7"
BG_BLUE = "E5F5FF"  # banded rows, note callouts
NEAR_BLACK = "2E2E2E"  # headline text
TEXT = "4B4B4B"  # body text
MUTED = "656565"  # captions, support copy
PLACEHOLDER = "919191"
BORDER = "C2C2C1"  # table borders (0.5 pt)
BORDER_LIGHT = "E4E4E4"
DIRTY_WHITE = "F6F6F6"
WHITE = "FFFFFF"
DANGER = "C62828"  # warning callout bar
DANGER_BG = "FDECEA"
SUCCESS = "2E7D32"
SUCCESS_BG = "E8F5E9"
AMBER = "8A5A00"
AMBER_BG = "FFF4D6"

# Nunito is the BDO UX font. Office documents name Arial, the agreed fallback: Nunito is not a
# standard font on BDOI desktops, and LibreOffice renders Arial with the metric-compatible
# Liberation Sans, so page breaks match between Word and the generated PDF.
FONT = "Arial"

CLIENT = "BDO Insurance and Reinsurance Brokers, Inc."
CLIENT_SHORT = "BDOI"
SYSTEM = "BIBS – BDOI Broker System"
PLATFORM = "iNXT BrokerVerse"
VENDOR = "iorta TechNXT"
CLASSIFICATION = "Confidential"
FOOTER_TEXT = "Confidential – BDOI"

# Status values that get a conditional colour in Excel and in Word tables:
# value -> (fill hex, font hex). Keys are upper case.
STATUS_COLOURS: dict[str, tuple[str, str]] = {
    "FIT": (SUCCESS_BG, SUCCESS),
    "PASS": (SUCCESS_BG, SUCCESS),
    "PASSED": (SUCCESS_BG, SUCCESS),
    "DONE": (SUCCESS_BG, SUCCESS),
    "CLOSED": (SUCCESS_BG, SUCCESS),
    "ANSWERED": (SUCCESS_BG, SUCCESS),
    "CONFIGURE": (BG_BLUE, HEADER_BLUE),
    "RECOMMENDED": (BG_BLUE, HEADER_BLUE),
    "IN SCOPE": (BG_BLUE, HEADER_BLUE),
    "CHANGE": (AMBER_BG, AMBER),
    "PARTIAL": (AMBER_BG, AMBER),
    "IN PROGRESS": (AMBER_BG, AMBER),
    "OPEN": (AMBER_BG, AMBER),
    "NEW": ("EDE7F6", "4527A0"),
    "GAP": (DANGER_BG, DANGER),
    "FAIL": (DANGER_BG, DANGER),
    "FAILED": (DANGER_BG, DANGER),
    "BLOCKED": (DANGER_BG, DANGER),
    "OUT": (DIRTY_WHITE, MUTED),
    "PARKED": (DIRTY_WHITE, MUTED),
    "ON HOLD": (DIRTY_WHITE, MUTED),
    "N/A": (DIRTY_WHITE, MUTED),
}

# Tokens that .dot figure sources may use instead of hex values (replaced before rendering).
DOT_TOKENS: dict[str, str] = {
    "@HEADER_BLUE": "#" + HEADER_BLUE,
    "@CTA_BLUE": "#" + CTA_BLUE,
    "@YELLOW": "#" + YELLOW,
    "@FIELD_BLUE": "#" + FIELD_BLUE,
    "@BG_BLUE": "#" + BG_BLUE,
    "@NEAR_BLACK": "#" + NEAR_BLACK,
    "@TEXT": "#" + TEXT,
    "@MUTED": "#" + MUTED,
    "@BORDER": "#" + BORDER,
    "@DIRTY_WHITE": "#" + DIRTY_WHITE,
    "@DANGER_BG": "#" + DANGER_BG,
    "@DANGER": "#" + DANGER,
    "@SUCCESS_BG": "#" + SUCCESS_BG,
    "@SUCCESS": "#" + SUCCESS,
    "@AMBER_BG": "#" + AMBER_BG,
    "@WHITE": "#" + WHITE,
}


def rgb(hex_value: str) -> tuple[int, int, int]:
    """Converts 'RRGGBB' to an (r, g, b) tuple."""
    hex_value = hex_value.lstrip("#")
    return int(hex_value[0:2], 16), int(hex_value[2:4], 16), int(hex_value[4:6], 16)


# --------------------------------------------------------------------------- drops
# BDOI groups the BRDs, FRS, test plans and other collaterals under its drops (answer A5 of 26-Sep-2026;
# docs/source-documents/BDOI_DROP_PLAN.md; docs/architecture/PROGRAMME_ALIGNMENT.md section 2.3). This is the only
# drop map of the toolkit: the builders place their outputs with out_dir() / out_path() (one release-set folder per
# BRD inside its drop folder), and the register takes the
# default Drop of an item from it. A BRD that spans drops lives in its primary drop; the index README of the other
# drop points to it (DROP_SHARED), so no file is copied.

# Drop key -> output folder under docs/deliverables/out, title and the BDOI dates of the drop.
DROPS: dict[str, dict[str, str]] = {
    "Drop 0": {
        "folder": "Drop-0_Setup_and_Data_Migration",
        "title": "Drop 0 - Setup and Data Migration",
        "dates": "Setup with the Drop 1 requirements (Sep - Nov 2026); migration requirements and "
                 "mapping Sep - Nov 2026, development Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, full "
                 "migration and cut-over Nov 2027 - Jan 2028",
    },
    "Drop 1": {
        "folder": "Drop-1_Transactional",
        "title": "Drop 1 - Transactional (upstream and downstream)",
        "dates": "Requirements Sep - Nov 2026, development Nov 2026 - Feb 2027, SIT Jan - Jul 2027, UAT (end to end) "
                 "Aug - Dec 2027",
    },
    "Drop 2": {
        "folder": "Drop-2_Independent",
        "title": "Drop 2 - Independent",
        "dates": "Requirements Dec 2026 - Feb 2027, development Mar - Apr 2027, SIT Jul - Sep 2027, UAT Oct - Nov 2027",
    },
    "Programme": {
        "folder": "Programme",
        "title": "Programme (cross-drop)",
        "dates": "Performance and penetration test Nov - Dec 2027, ORR / PRR Dec 2027 - Jan 2028, go-live of all "
                 "modules together in January 2028 (proposed Monday 3 January 2028)",
    },
}

# Primary drop of each BRD (the folder its documents live in).
BRD_DROP: dict[str, str] = {
    "BRD-00": "Programme",  # umbrella BRD, register, process deck, alignment pack
    "BRD-01": "Drop 1",  # upstream 1.U1-1.U3, 1.U6, 1.U7, 1.U9
    "BRD-02": "Drop 1",  # downstream 1.D2-1.D4, 1.D7; Production Reconciliation in Drop 2
    "BRD-03": "Drop 0",  # setup 0.6; quotation 1.U2 in Drop 1
    "BRD-04": "Drop 1",  # direct-payment commission receivables 1.D7; marketing collection 2.1 in Drop 2
    "BRD-05": "Drop 1",  # 1.U8, 1.D1, 1.D5, 1.D6; GL accounts 0.4 in Drop 0
    "BRD-06": "Drop 1",  # 1.U5
    "BRD-07": "Drop 2",  # 2.2
    "BRD-08": "Drop 2",  # 2.4; EB placement and reports 1.U6, 1.U9 in Drop 1
    "BRD-09": "Drop 1",  # account maintenance 1.U3; service requests proposed for Drop 2
    "BRD-10": "Drop 1",  # proposed with client onboarding (not on the slide, IQ23)
    "BRD-11": "Drop 0",  # 0.1-0.3
    "BRD-12": "Drop 1",  # 1.U4
    "BRD-13": "Drop 0",  # migration stream
}

# BRDs that span drops: (BRD, other drop, what of it belongs there). Listed in the index of the other drop.
DROP_SHARED: list[tuple[str, str, str]] = [
    ("BRD-01", "Drop 0", "Client onboarding is also a migration object (clients C01-C03)"),
    ("BRD-02", "Drop 2", "Production Reconciliation chapter of the FRS and its test plan sheet (item 2.3)"),
    ("BRD-03", "Drop 1", "Quotation or proposal with packages (item 1.U2)"),
    ("BRD-04", "Drop 2", "Marketing Collection extraction: worklists, dispositions, daily files (item 2.1)"),
    ("BRD-05", "Drop 0", "GL accounts and reference tables (item 0.4)"),
    ("BRD-08", "Drop 1", "EB placement and ePolicy, EB upstream reports (items 1.U6, 1.U9)"),
    ("BRD-09", "Drop 2", "Service-request functions (proposed, IQ23)"),
    ("BRD-10", "Drop 2", "Bridger Insight XG integration"),
    ("BRD-13", "Drop 1", "Legacy invoices in cashiering, commission and BIR reports (items 1.U1, 1.D2, 1.D6)"),
]

DROP_ORDER = ["Drop 0", "Drop 1", "Drop 2", "Programme", "Phase 2"]


def drop_of(brd: str) -> str:
    """Primary drop of a BRD code (BRD-nn); anything else is programme level."""
    return BRD_DROP.get(brd, "Programme")


# Name of each BRD as used in its release-set folder and file names.
BRD_NAMES: dict[str, str] = {
    "BRD-01": "New Business",
    "BRD-02": "Operations",
    "BRD-03": "Product Maintenance",
    "BRD-04": "Collections",
    "BRD-05": "Accounting Disbursement ACSL",
    "BRD-06": "Renewal",
    "BRD-07": "Claims",
    "BRD-08": "Employee Benefits",
    "BRD-09": "Customer Servicing Facility",
    "BRD-10": "Sanction Screening",
    "BRD-11": "User Access Maintenance",
    "BRD-12": "Submitted Policies",
    "BRD-13": "Data Migration",
}


def brd_folder(brd: str) -> str:
    """Release-set folder name of a BRD: BRD-nn_<Name> (for example BRD-01_New_Business)."""
    name = BRD_NAMES.get(brd, "")
    safe = "_".join(part for part in name.replace("&", "and").replace("/", " ").split() if part)
    return f"{brd}_{safe}" if safe else brd


def src_dir(brd: str) -> Path:
    """Source folder of a BRD: docs/deliverables/src/<BRD-nn_Name>/, holding its FRS and test plan sources, the
    FRS figures (figures/), the business sign-off pack data (pack/), the pack screenshots (screenshots/) and the Start
    Here guide; BRD-13 keeps its migration catalogue and cut-over plan in pack/ with its builder build_dm_pack.py.
    Shared builders and programme-level sources
    (alignment, change, decks, registers, signoff and testplans builders) keep their own folders."""
    if brd == "BRD-00":
        return SRC_DIR / "BRD-00_Core_Replacement"
    return SRC_DIR / brd_folder(brd)


OWNER_CAPACITIES = ["Prepared by", "Input provided by", "Reviewed by", "Approved by"]


def ownership(brd: str) -> dict:
    """Who signs what in the sign-off set of a BRD (pack/ownership.yaml of its source folder: the roles of the BRD
    approval sheet and the parts of the set each prepares, provides input to, reviews or approves); {} without one."""
    import yaml  # noqa: PLC0415

    path = src_dir(brd) / "pack" / "ownership.yaml"
    return yaml.safe_load(path.read_text(encoding="utf-8")) if path.exists() else {}


def owners_by_capacity(brd: str) -> dict[str, list[str]]:
    """The roles of ownership(brd) per capacity of the approval sheet, in the order of OWNER_CAPACITIES."""
    roles = ownership(brd).get("roles") or []
    return {c: [r["role"] for r in roles if r.get("capacity") == c] for c in OWNER_CAPACITIES}


def brd_of_code(code: str) -> str:
    """BRD-nn of a short code such as brd01 or BRD01 (or BRD-01 itself)."""
    digits = "".join(ch for ch in code if ch.isdigit())[:2]
    return f"BRD-{digits}"


def out_dir(brd: str, kind: str) -> Path:
    """Output folder of a document of a BRD.

    Every BRD has one release-set folder in its primary drop, holding all its documents (FRS, test plan, sign-off
    workbook, release note, migration pack): out/<drop folder>/<BRD-nn_Name>/. The kind (FRS, TestPlans, Signoff,
    Migration...) is part of the file name, not of the folder. Programme-level items (BRD-00) stay grouped by kind:
    out/Programme/<kind>/.
    """
    drop = drop_of(brd)
    if drop == "Programme":
        return OUT_DIR / DROPS[drop]["folder"] / kind
    return OUT_DIR / DROPS[drop]["folder"] / brd_folder(brd)


def out_path(brd: str, kind: str, filename: str) -> Path:
    """Full output path of a client-pack file, in the release-set folder of its BRD (see out_dir)."""
    return out_dir(brd, kind) / filename


# Business sign-off release sets issued so far: BRD -> version of the set. The documents of such a set carry a
# two-digit prefix so that they sort in reading order in the BRD folder (deliverables README, "Release and sign-off
# per BRD").
SIGNOFF_SETS = {"BRD-01": "2.0", "BRD-02": "2.1", "BRD-03": "2.0", "BRD-04": "2.0", "BRD-05": "2.0", "BRD-11": "2.0",
                "BRD-13": "2.0"}
READING_ORDER = {"StartHere": "00", "GuideDeck": "01", "FRS": "02", "Signoff": "03", "TestPlan": "04",
                 "TestPlanSummary": "05"}
# Further documents of a set that only some BRDs have (not required by check_pack): the configuration input templates
# of a setup BRD (BRD-3 Product Maintenance); the Data Migration Handbook and the Migration Workbook, which are the 02
# and 03 of the BRD-13 set in place of the FRS and the sign-off workbook.
READING_ORDER_EXTRA = {"Templates": "06", "Handbook": "02", "Workbook": "03"}
# The UX screen documents of a set, for the BDOI UX Design team (client request of 28-Sep-2026): 07 the UX Screen
# Deck (PowerPoint), 08 the UX screen register (Excel) and 09 the image package (ZIP of every screen image at twice
# the screen resolution, with the register as CSV). Two kinds share the name UXScreens, so the order is by kind and
# extension. build_ux_deck.py builds the three; check_pack requires them in the sets of UX_SETS.
READING_ORDER_UX = {("UXDeck", "pptx"): "07", ("UXScreens", "xlsx"): "08", ("UXScreens", "zip"): "09"}
# Sign-off sets that carry the UX screen documents (07 to 09): the Drop 0 sets, BRD-02 Operations, BRD-04 Collections
# and BRD-01 New Business first; the other Drop 1 sets follow when they are re-issued.
UX_SETS = {"BRD-01", "BRD-02", "BRD-03", "BRD-04", "BRD-05", "BRD-11", "BRD-13"}


def output_name(doc_type: str, brd: str, name: str, version: str, ext: str) -> str:
    """File name of a client-pack document: BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>, with the reading-order
    prefix (00_ to 09_) for the documents of a business sign-off release set.

    >>> output_name("FRS", "BRD-03", "Product Maintenance", "1.0", "docx")
    'BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx'
    >>> output_name("FRS", "BRD-01", "New Business", "2.0", "docx")
    '02_BIBS_FRS_BRD-01_New_Business_v2.0.docx'
    >>> output_name("UXScreens", "BRD-03", "Product Maintenance", "2.0", "zip")
    '09_BIBS_UXScreens_BRD-03_Product_Maintenance_v2.0.zip'
    """
    safe = "_".join(part for part in name.replace("&", "and").replace("/", " ").split() if part)
    key = doc_type + ("Summary" if safe.endswith("_Summary") else "")
    prefix = ""
    order = {**READING_ORDER, **READING_ORDER_EXTRA}
    ux = READING_ORDER_UX.get((doc_type, ext.lstrip(".")))
    if SIGNOFF_SETS.get(brd) == str(version) and ux:
        prefix = ux + "_"
    elif SIGNOFF_SETS.get(brd) == str(version) and key in order:
        prefix = order[key] + "_"
    return f"{prefix}BIBS_{doc_type}_{brd}_{safe}_v{version}.{ext.lstrip('.')}"


# Drop-level sets: documents that cover a whole drop rather than one BRD (the closure set of a drop). They live in
# their own folder of the drop folder, carry the reading-order prefix of the set and the drop in place of the BRD in
# their name: <nn>_BIBS_<Drop-n>_<Name>_v<version>.<ext>. check_pack requires every file of an issued set;
# drop_index lists them.
DROP_SETS: dict[str, dict[str, object]] = {
    "Drop 0": {
        "folder": "Drop-0_Closure",
        "version": "2.0",
        "files": {"01": ("Configuration_Inputs", "xlsx", "Configuration inputs workbook (Excel)"),
                  "02": ("Closure_Summary", "docx", "Closure summary (Word)")},
    },
}


def drop_code(drop: str) -> str:
    """Drop-0 for "Drop 0"."""
    return drop.replace(" ", "-")


def drop_set_dir(drop: str) -> Path:
    """Folder of the drop-level set of a drop: out/<drop folder>/<Drop-n>_Closure/."""
    return OUT_DIR / DROPS[drop]["folder"] / str(DROP_SETS[drop]["folder"])


def drop_output_name(drop: str, name: str, version: str, ext: str) -> str:
    """File name of a document of a drop-level set, with its reading-order prefix.

    >>> drop_output_name("Drop 0", "Configuration_Inputs", "2.0", "xlsx")
    '01_BIBS_Drop-0_Configuration_Inputs_v2.0.xlsx'
    """
    files = DROP_SETS[drop]["files"]
    order = next((k for k, v in files.items() if v[0] == name), None)  # type: ignore[union-attr]
    prefix = f"{order}_" if order and str(DROP_SETS[drop]["version"]) == str(version) else ""
    return f"{prefix}BIBS_{drop_code(drop)}_{name}_v{version}.{ext.lstrip('.')}"
