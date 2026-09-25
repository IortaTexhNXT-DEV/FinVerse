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


def output_name(doc_type: str, brd: str, name: str, version: str, ext: str) -> str:
    """File name of a client-pack document: BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>.

    >>> output_name("FRS", "BRD-03", "Product Maintenance", "1.0", "docx")
    'BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx'
    """
    safe = "_".join(part for part in name.replace("&", "and").replace("/", " ").split() if part)
    return f"BIBS_{doc_type}_{brd}_{safe}_v{version}.{ext.lstrip('.')}"
