"""BDO Insure brand constants shared by the Word, Excel and PowerPoint builders.

Colours follow docs/design/BDO_UX_GUIDELINES.md section 2. Yellow is an accent only: thin rules,
never a fill behind text.
"""

from __future__ import annotations

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
BRAND_DIR = REPO_ROOT / "frontend" / "src" / "assets" / "brand"
BDO_LOGO = BRAND_DIR / "bdo-insure.png"
IORTA_LOGO = BRAND_DIR / "iorta-technxt.png"
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
    "BUILT": (SUCCESS_BG, SUCCESS),
    "CONFIGURE": (BG_BLUE, HEADER_BLUE),
    "RECOMMENDED": (BG_BLUE, HEADER_BLUE),
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
# drop map of the toolkit: the builders place their outputs with out_dir() / out_path(), and the register takes the
# default Drop of an item from it. A BRD that spans drops lives in its primary drop; the index README of the other
# drop points to it (DROP_SHARED), so no file is copied.

# Drop key -> output folder under docs/deliverables/out, title and the BDOI dates of the drop.
DROPS: dict[str, dict[str, str]] = {
    "Drop 0": {
        "folder": "Drop-0_Setup_and_Data_Migration",
        "title": "Drop 0 - Setup and Data Migration",
        "dates": "Setup with the Drop 1 requirements (Sep - Nov 2026) and build wave 1; migration requirements and "
                 "mapping Sep - Nov 2026, build Nov 2026 - Mar 2027, SIT Apr - Jul 2027, UAT Aug - Oct 2027, full "
                 "migration and cut-over Nov 2027 - Jan 2028",
    },
    "Drop 1": {
        "folder": "Drop-1_Transactional",
        "title": "Drop 1 - Transactional (upstream and downstream)",
        "dates": "Requirements Sep - Nov 2026, build Nov 2026 - Feb 2027, SIT Jan - Jul 2027, UAT (end to end) "
                 "Aug - Dec 2027",
    },
    "Drop 2": {
        "folder": "Drop-2_Independent",
        "title": "Drop 2 - Independent",
        "dates": "Requirements Dec 2026 - Feb 2027, build Mar - Apr 2027, SIT Jul - Sep 2027, UAT Oct - Nov 2027",
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


def out_dir(brd: str, kind: str) -> Path:
    """Output folder of a document kind (FRS, TestPlans, Migration, Registers, Decks, Alignment) of a BRD."""
    return OUT_DIR / DROPS[drop_of(brd)]["folder"] / kind


def out_path(brd: str, kind: str, filename: str) -> Path:
    """Full output path of a client-pack file, in the drop folder of its BRD."""
    return out_dir(brd, kind) / filename


def output_name(doc_type: str, brd: str, name: str, version: str, ext: str) -> str:
    """File name of a client-pack document: BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>.

    >>> output_name("FRS", "BRD-03", "Product Maintenance", "1.0", "docx")
    'BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx'
    """
    safe = "_".join(part for part in name.replace("&", "and").replace("/", " ").split() if part)
    return f"BIBS_{doc_type}_{brd}_{safe}_v{version}.{ext.lstrip('.')}"
