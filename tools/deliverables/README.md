# BIBS deliverables toolkit

Generators for the BDOI client pack (plan: [`docs/deliverables/README.md`](../../docs/deliverables/README.md)).
Every Word, Excel and PowerPoint file uses the BDO Insure template and is rebuilt from sources kept in the
repository.

| File | Purpose |
|---|---|
| `brand.py` | Colours (Header Blue #004EA8, CTA Blue #0072D8, Yellow #FDB913, Background Blue #E5F5FF, border #C2C2C1), logos, client and system names, status colours, `output_name()`; the **drop map** (`DROPS`, `BRD_DROP`, `DROP_SHARED`, `out_dir()`, `out_path()`) that places every output in its BDOI drop folder |
| `bdoi_docx.py` | Word builder: Python API (`BdoiDocument`) and the Markdown-like source format (`build_markdown`, CLI) |
| `bdoi_xlsx.py` | Excel builder (`BdoiWorkbook`, `Column`) |
| `bdoi_pptx.py` | PowerPoint builder, 16:9 (`BdoiDeck`) |
| `render.py` | PDF conversion with LibreOffice and page PNG previews / contact sheets |

## Set-up

```bash
pip install -r tools/deliverables/requirements.txt
# system tools (Ubuntu): LibreOffice Writer, Calc and Impress, Graphviz, poppler
apt-get install -y libreoffice-writer libreoffice-calc libreoffice-impress graphviz poppler-utils
```

Without `pdftoppm`, previews fall back to `pypdfium2`. Without `dot`, a figure uses the PNG already rendered next to
its `.dot` source.

## Build and check a document

```bash
# lint the YAML blocks, then build the .docx (the editable master) and page previews
python tools/deliverables/bdoi_docx.py --check docs/deliverables/src/frs/FRS_BRD03_PRODUCT_MAINTENANCE.md
python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD03_PRODUCT_MAINTENANCE.md --previews

# at issue only: also keep the PDF next to the .docx
python tools/deliverables/bdoi_docx.py docs/deliverables/src/frs/FRS_BRD03_PRODUCT_MAINTENANCE.md --keep-pdf

# any Office file: PDF plus previews
python tools/deliverables/render.py docs/deliverables/out/Drop-0_Setup_and_Data_Migration/FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx --previews
```

Word and Excel files are the masters and are the only outputs kept in `docs/deliverables/out/`; PDFs are produced
when a document is issued (`--keep-pdf`, or `render.py`) and are not committed (`.gitignore`). `publish()` renders a
temporary PDF to read the page of every heading (from the PDF outline) and writes them into the table of contents;
the temporary PDF is then deleted. The TOC stays a real Word field
(Update Field in Word refreshes it).

Previews go to `<output folder>/_previews/<document>/` (`page-001.png`, ... and `sheet-01.png` with six pages per
sheet). They are not committed. **Look at the sheets before committing**: alignment, overflow, table widths, split
header tables, empty pages and figure legibility.

## Conventions for every client document

- **Sources** live in `docs/deliverables/src/<kind>/` (for example `src/frs/FRS_BRD03_PRODUCT_MAINTENANCE.md`),
  figures in `src/<kind>/figures/`.
- **Outputs** go to `docs/deliverables/out/<drop folder>/<DocType>/`, grouped by BDOI drop (answer A5 of 26-Sep-2026):
  `Drop-0_Setup_and_Data_Migration/`, `Drop-1_Transactional/`, `Drop-2_Independent/` and `Programme/` (BRD-00
  documents: umbrella FRS, register, process deck, alignment pack). The drop of a BRD is `brand.BRD_DROP`, the only
  drop map of the toolkit; every builder takes its folder from `brand.out_dir(brd, kind)`, so a rebuild lands in the
  drop folder. A BRD that spans drops lives in its primary drop and is listed in the other drop's index
  (`brand.DROP_SHARED`); nothing is copied. After a build or a move, regenerate the index `README.md` of every drop
  folder with `python tools/deliverables/drop_index.py`. Only Word, Excel and PowerPoint masters are committed.
- **File names**: `BIBS_<DocType>_BRD-nn_<Name>_v<version>.<ext>`, for example
  `BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx` (`brand.output_name("FRS", "BRD-03", "Product Maintenance", "1.0", "docx")`).
  Pack-wide documents use `BRD-00`.
- **Figures** are Graphviz `.dot` sources rendered at build time in BDO colours. Use the colour tokens `@HEADER_BLUE`,
  `@CTA_BLUE`, `@YELLOW`, `@BG_BLUE`, `@DIRTY_WHITE`, `@AMBER_BG`, `@SUCCESS_BG`, `@SUCCESS`, `@MUTED`, `@TEXT`,
  `@NEAR_BLACK`, `@WHITE` (list in `brand.DOT_TOKENS`) and `fontname="Arial"`. Prefer top-to-bottom or grid layouts:
  a figure is scaled to the text width (17.6 cm) and capped at 80 % of the page height, so a wide, flat graph ends
  up with unreadable text. PNG screenshots and matplotlib charts are used as they are.
- **Writing standard**: the one in `docs/deliverables/README.md` (specific, active voice, BRD IDs and pages, no
  filler). Error codes quoted in a document must exist in the code; use "-" for screen checks without a code.

## Source format (Word)

A source is YAML front matter plus a Markdown subset:

```markdown
---
title: Product Maintenance                     # cover title
subtitle: BRD-3 Product Maintenance (Package) and Workshop Addendum
doc_type: Functional Requirements Specification
doc_code: FRS
brd: BRD-03
name: Product Maintenance
doc_id: BIBS-FRS-BRD-03
version: "1.0"
date: 25 September 2026
status: Issued for BDOI review
header_title: FRS BRD-3 Product Maintenance    # running header
output: FRS/BIBS_FRS_BRD-03_Product_Maintenance_v1.0.docx   # <kind>/<file>; placed in out/<drop of brd>/
h1_page_break: true                            # each chapter on a new page (default)
control:                                       # document control table
  - {version: "1.0", date: 25 Sep 2026, author: ..., reviewer: ..., approver: ..., change: ...}
distribution:                                  # distribution list
  - {name: "Product Owner, MBS", role: Approver, organisation: BDOI, purpose: Review and sign-off}
---
```

The builder adds the cover, document control, distribution list and table of contents. The body supports:

| Syntax | Result |
|---|---|
| `#`, `##`, `###` | Headings numbered 1 / 1.1 / 1.1.1 (add `{-}` at the end for an unnumbered heading) |
| `####` | Small blue label, not in the TOC |
| paragraphs, `**bold**`, `*italic*`, `` `code` `` | Body text (10.5 pt Arial); code in Courier New |
| `- item`, two-space indent for level 2; `1. item` | Bullets / numbered list (a change of list kind starts a new list) |
| pipe table | BDOI table; put `<!-- table: widths=2,5,3 caption="..." status=Fit size=8.5 bold=first -->` on the line before it. `widths` are relative, `status` names the columns that get status colours |
| `> [!NOTE]`, `[!WARNING]`, `[!PARKED]`, `[!DECISION]`, `[!QUESTION]` (optional title after it) | Callout box with a coloured bar |
| `![Caption](figures/x.dot){width=9}` | Figure with "Figure n:" caption; `.dot` rendered with the BDO tokens; width in cm optional |
| `<!-- pagebreak -->`, `<!-- landscape -->`, `<!-- portrait -->` | Page break; new landscape / portrait section |
| ```` ```fr ```` YAML block | Functional requirement (below) |
| ```` ```glossary ```` YAML mapping | Glossary table, sorted |
| ```` ```signoff ```` YAML `rows: [{name, role, organisation}]` | Sign-off table with signing rows |
| ```` ```table ```` YAML `{headers, rows, widths, caption, status}` | Table with list cells (bullets inside a cell) |
| ```` ```keyvalues ```` YAML mapping | Label / value table |

### Functional requirement block

```yaml
id: FR-PM-020
title: Create and submit a Package Request Form
brd: [BRPM.008 (p.18-19), BRPM.011 (p.21)]      # BRD ID and page, always
actor: Marketing AO; TSU Officer
priority: Must have
fit: NEW                                          # FIT / CONFIGURE / CHANGE / NEW
screens: Package Requests; New Package Request
api: POST /api/v1/product-maintenance/requests
description:
  - One paragraph per item.
preconditions:
  - The user has PKG_REQUEST.
main_flow:
  - The requester clicks **New Package Request**.
  - [Sub-step a, Sub-step b]                      # a nested list = a) b) sub-steps of the previous step
alternate_flows:
  - Incomplete request. BIBS lists what is missing.
rules:          # [id, statement, Configurable | Fixed, where it is maintained]
  - [R1, "Request numbers are PKR-<yyyy>-<n>.", Configurable, Parameter PKG_REQUEST_PREFIX]
validations:    # [check, message shown, code or "-"]
  - [Name blank, Enter the package or programme name, PKG_REQUEST_INCOMPLETE]
fields_screen: New Package Request
fields:         # [label, type, Yes | No | Conditional, source / LOV, validation]
  - [Request type, List, "Yes", LOV PKG_REQUEST_TYPE, Fixed after first save]
notifications:
  - On submission, the approvers are notified.
audit:
  - Create, save, submit and void are recorded in the request history.
acceptance:
  - A complete request moves to FOR_MKT_APPROVAL.
```

YAML traps, caught by `--check`: a comma inside an unquoted flow-list cell splits the cell, and `text: more text`
becomes a mapping. Use block lists (`- item`) for prose and quote cells that contain commas, colons or brackets.

## Python API

```python
from bdoi_docx import BdoiDocument, DocMeta
doc = BdoiDocument(DocMeta(title="Data Migration Approach", doc_type="Approach", doc_code="DMA", brd="BRD-00"))
doc.cover(); doc.front_matter()
doc.heading("Introduction"); doc.paragraph("Plain text with **bold**.")
doc.table(["ID", "Item", "Fit"], [["X1", "Row", "FIT"]], widths=[2, 10, 2], caption="Items", status_cols=[2])
doc.callout("Parked until BDOI answers Q08.", kind="parked")
doc.figure("figures/flow.dot", "Flow")
doc.requirement({...})            # same keys as the fr block
doc.glossary({"TSU": "Technical Support Unit"})
doc.signoff([{"role": "Product Owner", "organisation": "BDOI"}])
doc.publish(brand.out_path("BRD-13", "Migration", "BIBS_Migration_BRD-13_Data_Migration_Approach_v1.0.docx"))
```

Excel (`python tools/deliverables/bdoi_xlsx.py out.xlsx` builds a sample):

```python
from bdoi_xlsx import BdoiWorkbook, Column
wb = BdoiWorkbook("Fit-Gap BRD-3", doc_type="Fit-gap workbook", brd="BRD-03", version="1.0")
wb.legend = [("FIT", "Works today"), ("NEW", "New capability")]
wb.sheet("Fit-Gap", [
    Column("id", "BRD ID", 12, "Requirement ID and page"),
    Column("fit", "Fit", 12, "Fit class", values=["FIT", "CONFIGURE", "CHANGE", "NEW", "OUT"], status=True),
], rows=[{"id": "BRPM.001", "fit": "FIT"}], description="One row per BRD requirement")
wb.save(brand.out_path("BRD-03", "FitGap", "BIBS_FitGap_BRD-03_Product_Maintenance_v1.0.xlsx"))
```

Each data sheet gets a title band, Header Blue header row (row 4), frozen panes, auto-filter, wrapped text,
drop-down lists for columns with `values`, conditional status fills, banded rows and landscape fit-to-width
printing with the header row repeated. The README sheet is generated from the column descriptions.

PowerPoint (`python tools/deliverables/bdoi_pptx.py out.pptx [screenshot.png]` builds a sample):

```python
from bdoi_pptx import BdoiDeck
deck = BdoiDeck("Product Maintenance walkthrough", version="1.0", date="25 September 2026")
deck.title(); deck.section("1. Package request", "Marketing AO, TSU")
deck.content("Scope", ["Point", (1, "Sub-point")])
deck.two_column("Roles and screens", "Personas", [...], "Screens", [...])
deck.screenshot("Submit the request", "tools/screenshots/out/x.png", caption="...", persona="Marketing AO",
                action="...", expected="...", observation="...")
deck.table("SLA", ["Stage", "Hours", "Status"], [["Negotiation", "120", "PASS"]], widths=[4, 1, 1])
deck.save(brand.out_path("BRD-03", "Decks", "BIBS_Deck_BRD-03_Product_Maintenance_v1.0.pptx"))
```
